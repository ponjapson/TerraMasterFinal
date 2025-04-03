package com.example.terramaster

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import org.w3c.dom.Text
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.contracts.contract

class FragmentBooking : Fragment() {

    private var startDateTime: Calendar? = null
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    //private var bookedUserId: String? = null

    // New UI elements for scanning
    private lateinit var scanButton: Button
    private lateinit var pdfFileNameTextView: TextView
    private var scannedPdfUri: Uri? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_booking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookedUserId = arguments?.getString("bookedUserId")
        if (bookedUserId.isNullOrEmpty()) {
            showToast("No professional or service provider selected.")
            requireActivity().onBackPressed()
            return
        }

        // Initialize the views
        val downpaymentEditText = view.findViewById<EditText>(R.id.downpaymentEditText)
        val addressEditText = view.findViewById<EditText>(R.id.Address)
        val contractAmount = view.findViewById<EditText>(R.id.contractAmount)
        val startDateTimeButton = view.findViewById<Button>(R.id.startDateTimeButton)
        val submitBookingButton = view.findViewById<Button>(R.id.submitBookingButton)
        val selectedStartDateTimeTextView = view.findViewById<TextView>(R.id.selectedStartDateTimeTextView)
        val notice = view.findViewById<TextView>(R.id.notice)



        scanButton = view.findViewById(R.id.scanButton)
        pdfFileNameTextView = view.findViewById(R.id.pdfFileNameTextView)

        downpaymentEditText.setText("0.00")
        setupDateTimePickers(startDateTimeButton, selectedStartDateTimeTextView)


        scanButton.setOnClickListener {
            launchScanner()
        }

        fetchUserType(bookedUserId) { userType ->
            // Check if userType is "Processor" and disable the appropriate views
            if (userType == "Processor") {
                contractAmount.isEnabled = false
                downpaymentEditText.isEnabled = false
                context?.let {

                    downpaymentEditText.setTextColor(ContextCompat.getColor(it, android.R.color.darker_gray))
                }
                context?.let {
                    contractAmount.setTextColor(ContextCompat.getColor(it, android.R.color.darker_gray))

                }
                notice.visibility = View.VISIBLE
            } else {
                contractAmount.isEnabled = true
                downpaymentEditText.isEnabled = true
                notice.visibility = View.GONE
            }
        }

        fetchUserTypeForStatus(bookedUserId) {userType ->
            if(userType == "Surveyor") {
                setupSubmitBookingButton(
                    submitBookingButton,
                    addressEditText,
                    downpaymentEditText,
                    contractAmount,
                    bookedUserId,
                    "new surveyor request"
                )
            }else if(userType == "Processor"){

                setupSubmitBookingButton(
                    submitBookingButton,
                    addressEditText,
                    downpaymentEditText,
                    contractAmount,
                    bookedUserId,
                    "new processor request"
                )
            }else{
                Log.e("Status", userType.toString());
            }
        }
    }

    private fun fetchUserType(bookedUserId: String, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(bookedUserId)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userType = document.getString("user_type") // Safe fetching
                    callback(userType) // Execute the callback with userType
                } else {
                    callback(null) // No user found
                }
            }
            .addOnFailureListener { e ->
                callback(null) // Handle error and pass null to callback
                Log.e("fetchUserType", "Error fetching user type", e)
            }
    }

    private fun fetchUserTypeForStatus(bookedUserId: String, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(bookedUserId)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fetchedUserType = document.getString("user_type")
                    if (fetchedUserType != null) {
                        Log.d("fetchUserType", "User type fetched: $fetchedUserType")
                    } else {
                        Log.e("fetchUserType", "User type field is missing in Firestore")
                    }
                    callback(fetchedUserType) // Pass the value to callback
                } else {
                    Log.e("fetchUserType", "Document does not exist for user ID: $bookedUserId")
                    callback(null) // Return null if document does not exist
                }
            }
            .addOnFailureListener { e ->
                Log.e("fetchUserType", "Error fetching user type", e)
                callback(null) // Return null if query fails
            }
    }




    private fun setupDateTimePickers(startDateTimeButton: Button, selectedStartDateTimeTextView: TextView) {
        startDateTimeButton.setOnClickListener {
            pickDateTime { selectedCalendar ->
                startDateTime = selectedCalendar
                selectedStartDateTimeTextView.text = "Start: ${dateTimeFormat.format(startDateTime!!.time)}"
            }
        }
    }

    private fun setupSubmitBookingButton(
        submitBookingButton: Button,
        addressEditText: EditText,
        downpaymentEditText: EditText,
        contractAmount: EditText,
        bookedUserId: String,
        status: String
    ) {
        submitBookingButton.setOnClickListener {

            val address = addressEditText.text.toString()
            convertLocationToCoordinates(address) { lat: Double, lon: Double ->
                val downpayment = downpaymentEditText.text.toString().toDoubleOrNull() ?: 0.00
                val contractPrice = contractAmount.text.toString().toDoubleOrNull() ?: 0.00

                if (validateInput(address, downpayment, contractPrice)) {
                    showLoadingToast()
                    saveBookingToFirestore(address, downpayment, contractPrice, lat, lon, bookedUserId, status)
                }
            }
        }
    }

    private fun convertLocationToCoordinates(locationName: String, callback: (Double, Double) -> Unit) {
        val geocoder = OpenStreetMapGeocoder(requireContext())
        geocoder.getCoordinatesFromAddress(locationName) { coordinates ->
            if (coordinates != null) {
                callback(coordinates.latitude, coordinates.longitude)
            } else {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun pickDateTime(onDateTimeSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        val currentDate = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, day ->
            calendar.set(year, month, day)

            if (calendar.before(currentDate)) {
                showToast("Selected date cannot be before the current date.")
                return@DatePickerDialog
            }

            TimePickerDialog(requireContext(), { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)

                if (calendar.before(currentDate)) {
                    showToast("Selected time cannot be before the current time.")
                    return@TimePickerDialog
                }

                onDateTimeSelected(calendar)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.datePicker.minDate = currentDate.timeInMillis
        datePickerDialog.show()
    }

    private fun validateInput(address: String, downpayment: Double?, contractPrice: Double?): Boolean {
        return when {
            startDateTime == null -> {
                showToast("Please select start and end times.")
                false
            }
            address.isEmpty() -> {
                showToast("Please provide an address.")
                false
            }
            contractPrice == null || contractPrice < 0 -> {
                showToast("Enter a Contract Price.")
                false
            }
            else -> true
        }
    }

    private fun showLoadingToast() {
        showToast("Booking in progress...")
    }

    private fun saveBookingToFirestore(address: String, downpayment: Double, contractPrice: Double, lat: Double, lon: Double, bookedUserId: String, status: String) {

        val db = FirebaseFirestore.getInstance()
        val bookingUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (bookingUserId == null) {
            showToast("User not logged in.")
            return
        }

        // Upload the scanned PDF to Firebase Storage
        if (scannedPdfUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference
            val pdfRef = storageRef.child("bookings/${UUID.randomUUID()}.pdf")

            pdfRef.putFile(scannedPdfUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { pdfDownloadUrl ->
                        saveBookingWithPdf(address, downpayment, contractPrice, pdfDownloadUrl.toString(), lat, lon, bookedUserId, status)
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Failed to upload PDF: ${e.message}")
                }
        } else {
            saveBookingWithPdf(address, downpayment, contractPrice, null, lat, lon, bookedUserId, status)
        }
    }

    private fun saveBookingWithPdf(
        address: String, downpayment: Double, contractPrice: Double, pdfDownloadUrl: String?, lat: Double, lon: Double, bookedUserId: String, status: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val bookingUserId = FirebaseAuth.getInstance().currentUser?.uid

        val bookingData = hashMapOf(
            "landOwnerUserId" to bookingUserId,
            "bookedUserId" to bookedUserId!!,
            "timestamp" to FieldValue.serverTimestamp(),
            "address" to address,
            "downpayment" to downpayment,
            "status" to status,
            "stage" to "request",
            "startDateTime" to Timestamp(startDateTime!!.timeInMillis / 1000, 0),
            "contractPrice" to contractPrice,
            "pdfUrl" to (pdfDownloadUrl ?: ""),
            "latitude" to lat,
            "longitude" to lon
        )

        db.collection("bookings")
            .add(bookingData)
            .addOnSuccessListener { documentReference ->
                documentReference.update("bookingId", documentReference.id)
                showToast("Booking successfully created.")
                requireActivity().onBackPressed()
            }
            .addOnFailureListener { e ->
                showToast("Error adding booking: ${e.message}")
            }
    }

    private fun showDownpaymentAlertDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Invalid Downpayment")
            .setMessage("The downpayment cannot be 0.00. Please enter a valid downpayment.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Launch scanner
    private fun launchScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)  // Allow importing from gallery
            .setPageLimit(2)               // Max 2 pages
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF) // Only PDF format
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        scanner.getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to start scanner: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(data)

                scanningResult?.pdf?.let { pdf ->
                    scannedPdfUri = pdf.uri
                    val fileName = generateFileName()
                    pdfFileNameTextView.text = "Scanned PDF: $fileName"
                }
            } else {
                Toast.makeText(requireContext(), "Scanning canceled", Toast.LENGTH_SHORT).show()
            }
        }

    private fun generateFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "booking_scan_$timeStamp.pdf"
    }
}
