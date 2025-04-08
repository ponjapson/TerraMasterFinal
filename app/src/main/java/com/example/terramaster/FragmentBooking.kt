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
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
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

        val addressEditText = view.findViewById<EditText>(R.id.Address)
        val startDateTimeButton = view.findViewById<Button>(R.id.startDateTimeButton)
        val submitBookingButton = view.findViewById<Button>(R.id.submitBookingButton)
        val selectedStartDateTimeTextView = view.findViewById<TextView>(R.id.selectedStartDateTimeTextView)
        val fullName = view.findViewById<EditText>(R.id.fullName)
        val contactNumber = view.findViewById<EditText>(R.id.contactNumber)
        val emailAddress = view.findViewById<EditText>(R.id.emailAddress)
        val tinNumber = view.findViewById<EditText>(R.id.tinNumber)
        val age = view.findViewById<EditText>(R.id.age)
        val propertyTypeGroup = view.findViewById<RadioGroup>(R.id.propertyTypeGroup)
        val purposeOfSurveyGroup = view.findViewById<RadioGroup>(R.id.purposeOfSurveyGroup)
        val residentialRadioButton = view.findViewById<RadioButton>(R.id.residential)
        val commercialRadioButton = view.findViewById<RadioButton>(R.id.commercial)
        val agriculturalRadioButton = view.findViewById<RadioButton>(R.id.agricultural)
        val vacantLandRadioButton = view.findViewById<RadioButton>(R.id.vacantLand)
        val otherPropertyRadioButton = view.findViewById<RadioButton>(R.id.otherProperty)
        val purposeLabel = view.findViewById<TextView>(R.id.purposeLabel)
        val propertyLabel = view.findViewById<TextView>(R.id.propertyLabel)

        val propertySaleRadioButton = view.findViewById<RadioButton>(R.id.propertySale)
        val legalDisputeRadioButton = view.findViewById<RadioButton>(R.id.legalDispute)
        val propertyDevelopmentRadioButton = view.findViewById<RadioButton>(R.id.propertyDevelopment)
        val landSubdivisionRadioButton = view.findViewById<RadioButton>(R.id.landSubdivision)
        val environmentalAssessmentRadioButton = view.findViewById<RadioButton>(R.id.environmentalAssessment)
        val otherPurposeRadioButton = view.findViewById<RadioButton>(R.id.otherPurpose)
        val ageLabel = view.findViewById<TextView>(R.id.ageLabel)
        val tinLabel = view.findViewById<TextView>(R.id.tinLabel)
        val notice = view.findViewById<TextView>(R.id.notice)
        val notes = view.findViewById<TextView>(R.id.notes)
        val notesProcessor = view.findViewById<TextView>(R.id.notesProcessor)
        val firestore = FirebaseFirestore.getInstance()
        val notesRef = firestore.collection("notes") // Replace "notes" with your collection name
        val noteId = "q3liLgg9khwBKjTdcN6q" // Replace this with the actual document ID you want to fetch



        notesRef.document(noteId).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val processorBookingNote = documentSnapshot.getString("processorBookingNote")
                    val processorDocumentsNote = documentSnapshot.getString("processorDocumentsNote")
                    val surveyorDocumentNotes = documentSnapshot.getString("surveyorDocumentNotes")

                    if (processorBookingNote != null || processorDocumentsNote != null || surveyorDocumentNotes != null) {
                        // Set the note to the TextView
                        val formattedText = processorDocumentsNote?.replace("\\n", "\n")
                        val formattedTextSurveyor = surveyorDocumentNotes?.replace("\\n", "\n")
                        val noticeTextView: TextView = notice // Replace with your TextView ID
                        noticeTextView.text = processorBookingNote
                        notes.text = formattedTextSurveyor
                        notesProcessor.text = formattedText
                    } else {
                        // Handle the case where the field is missing or null
                        Log.e("Firestore", "processorBookingNote field is missing or null.")
                    }
                } else {
                    // Handle the case where the document does not exist
                    Log.e("Firestore", "Document not found.")
                }
            }
            .addOnFailureListener { e ->
                // Handle the error
                Log.e("Firestore", "Error fetching document", e)
            }


        scanButton = view.findViewById(R.id.scanButton)
        pdfFileNameTextView = view.findViewById(R.id.pdfFileNameTextView)

        setupDateTimePickers(startDateTimeButton, selectedStartDateTimeTextView)


        scanButton.setOnClickListener {
            launchScanner()
        }

        fetchUserType(bookedUserId) { userType ->
            // Check if userType is "Processor" and disable the appropriate views
            if (userType == "Processor") {
                /*contractAmount.isEnabled = false
                downpaymentEditText.isEnabled = false
                contractAmount.visibility = View.GONE
                downpaymentEditText.visibility = View.GONE*/
                notice.visibility = View.VISIBLE
                notesProcessor.visibility = View.VISIBLE
                tinNumber.visibility = View.VISIBLE
                age.visibility = View.VISIBLE
                ageLabel.visibility = View.VISIBLE
                tinLabel.visibility = View.VISIBLE
            } else {
               /* contractAmount.isEnabled = true
                downpaymentEditText.isEnabled = true*/
                notice.visibility = View.GONE
                notes.visibility = View.VISIBLE
                /*contractAmount.visibility = View.GONE
                downpaymentEditText.visibility = View.GONE*/
                propertyLabel.visibility = View.VISIBLE
                purposeLabel.visibility = View.VISIBLE
                propertyTypeGroup.visibility = View.VISIBLE
                purposeOfSurveyGroup.visibility = View.VISIBLE

            }
        }

        // Pass the EditText views and RadioButton views directly
        fetchUserTypeForStatus(bookedUserId) { userType ->
            if (userType == "Surveyor") {
                // Pass EditText and RadioButton views to the function
                setupSubmitBookingButton(
                    submitBookingButton,
                    fullName,  // EditText
                    contactNumber,  // EditText
                    emailAddress,  // EditText
                    addressEditText,  // EditText
                    propertyTypeGroup,  // RadioGroup for Property Type
                    purposeOfSurveyGroup,  // RadioGroup for Purpose of Survey
                    bookedUserId,
                    "new surveyor request"
                )
            } else if (userType == "Processor") {
                // For Processor, include tinNumber as well
                setupSubmitBookingButtonProcessor(
                    submitBookingButton,
                    fullName,  // EditText
                    contactNumber,  // EditText
                    emailAddress,  // EditText// EditText
                    addressEditText,  // EditText
                    bookedUserId,
                    "new processor request",
                    tinNumber,
                    age
                )
            } else {
                // If neither Surveyor nor Processor, log an error
                Log.e("Status", userType.toString())
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
        fullNameEditText: EditText,
        contactNumberEditText: EditText,
        emailAddressEditText: EditText,
        addressEditText: EditText,
        propertyTypeGroup: RadioGroup,
        purposeOfSurveyGroup: RadioGroup,
        bookedUserId: String,
        status: String
    ) {
        submitBookingButton.setOnClickListener {
            // Extract values directly from EditText views
            val fullName = fullNameEditText.text.toString()
            val contactNumber = contactNumberEditText.text.toString()
            val emailAddress = emailAddressEditText.text.toString()
            val address = addressEditText.text.toString()

            // Get selected property type from RadioGroup
            val selectedPropertyTypeId = propertyTypeGroup.checkedRadioButtonId
            val propertyTypeRadioButton = propertyTypeGroup.findViewById<RadioButton>(selectedPropertyTypeId)
            val propertyType = propertyTypeRadioButton?.text.toString()

            // Get selected purpose of survey from RadioGroup
            val selectedPurposeOfSurveyId = purposeOfSurveyGroup.checkedRadioButtonId
            val purposeOfSurveyRadioButton = purposeOfSurveyGroup.findViewById<RadioButton>(selectedPurposeOfSurveyId)
            val purposeOfSurvey = purposeOfSurveyRadioButton?.text.toString()

            // Validate input and convert address to coordinates
            if (validateInput(fullName, contactNumber, emailAddress, address, propertyType, purposeOfSurvey)) {
                convertLocationToCoordinates(address) { lat: Double, lon: Double ->
                    // Show loading toast and save booking to Firestore
                    showLoadingToast()
                    saveBookingToFirestore(
                        address, lat, lon, bookedUserId, status, fullName,
                        contactNumber, emailAddress, propertyType, purposeOfSurvey
                    )
                }
            }
        }
    }

    private fun setupSubmitBookingButtonProcessor(
        submitBookingButton: Button,
        fullNameEditText: EditText,
        contactNumberEditText: EditText,
        emailAddressEditText: EditText,
        addressEditText: EditText,
        bookedUserId: String,
        status: String,
        tinNumber: EditText,
        age: EditText
    ) {
        submitBookingButton.setOnClickListener {
            // Extract values directly from EditText views
            val fullName = fullNameEditText.text.toString()
            val contactNumber = contactNumberEditText.text.toString()
            val emailAddress = emailAddressEditText.text.toString()
            val address = addressEditText.text.toString()
            val tinNumber = tinNumber.text.toString()
            val age = age.text.toString()
            val ageInt = if (age.isNotBlank()) age.toIntOrNull() ?: 0 else 0



            // Validate input and convert address to coordinates
            if (validateInputProcessor(fullName, contactNumber, emailAddress, address, tinNumber, age)) {
                convertLocationToCoordinates(address) { lat: Double, lon: Double ->
                    // Show loading toast and save booking to Firestore
                    showLoadingToast()
                    saveBookingToFirestoreProcessor(
                        address, lat, lon, bookedUserId, status, fullName,
                        contactNumber, emailAddress, tinNumber, ageInt
                    )
                }
            }
        }
    }

    private fun validateInput(
        fullName: String,
        contactNumber: String,
        emailAddress: String,
        address: String,
        propertyType: String,
        purposeOfSurvey: String
    ): Boolean {
        return when {
            startDateTime == null -> {
                showToast("Please select start and end times.")
                false
            }
            fullName.isBlank() -> {
                showToast("Please enter your full name.")
                false
            }
            contactNumber.isBlank() -> {
                showToast("Please enter your contact number.")
                false
            }
            !contactNumber.matches(Regex("^\\d{7,15}$")) -> {
                showToast("Contact number must be between 7 to 15 digits.")
                false
            }
            emailAddress.isBlank() -> {
                showToast("Please enter your email address.")
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches() -> {
                showToast("Please enter a valid email address.")
                false
            }
            address.isBlank() -> {
                showToast("Please provide an address.")
                false
            }
            propertyType.isBlank() -> {
                showToast("Please select a property type.")
                false
            }
            purposeOfSurvey.isBlank() -> {
                showToast("Please select a purpose of survey.")
                false
            }
            else -> true
        }
    }

    private fun validateInputProcessor(
        fullName: String,
        contactNumber: String,
        emailAddress: String,
        address: String,
        tinNumber: String,
        age: String
    ): Boolean {
        return when {
            fullName.isBlank() -> {
                showToast("Please enter your full name.")
                false
            }
            contactNumber.isBlank() -> {
                showToast("Please enter your contact number.")
                false
            }
            !contactNumber.matches(Regex("^\\d{7,15}$")) -> {
                showToast("Contact number must be between 7 to 15 digits.")
                false
            }
            emailAddress.isBlank() -> {
                showToast("Please enter your email address.")
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches() -> {
                showToast("Please enter a valid email address.")
                false
            }
            address.isBlank() -> {
                showToast("Please provide an address.")
                false
            }
            tinNumber.isBlank() -> {
                showToast("Please enter your TIN number.")
                false
            }
            age.isBlank() -> {
                showToast("Please enter your age.")
                false
            }
            age.toIntOrNull() == null -> {
                showToast("Age must be a number.")
                false
            }
            age.toInt() < 18 -> {
                showToast("You must be at least 18 years old.")
                false
            }
            else -> true
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



    private fun showLoadingToast() {
        showToast("Booking in progress...")
    }

    private fun saveBookingToFirestore(
        address: String,
        lat: Double,
        lon: Double,
        bookedUserId: String,
        status: String,
        fullName: String,  // Now passing String values instead of EditText
        contactNumber: String,  // Same here
        emailAddress: String,  // Same here
        propertyType: String,  // Added propertyType
        purposeOfSurvey: String  // Added purposeOfSurvey
    ) {
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
                        saveBookingWithPdf(
                            address, pdfDownloadUrl.toString(), lat, lon, bookedUserId, status,
                            fullName, contactNumber, emailAddress, propertyType, purposeOfSurvey
                        )
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Failed to upload PDF: ${e.message}")
                }
        } else {
            saveBookingWithPdf(
                address, null, lat, lon, bookedUserId, status, fullName, contactNumber,
                emailAddress, propertyType, purposeOfSurvey
            )
        }
    }
    private fun saveBookingToFirestoreProcessor(
        address: String,
        lat: Double,
        lon: Double,
        bookedUserId: String,
        status: String,
        fullName: String,  // Now passing String values instead of EditText
        contactNumber: String,  // Same here
        emailAddress: String,  // Same here
        tinNumber: String,
        age: Int
    ) {
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
                        saveBookingWithPdfProcessor(
                            address, pdfDownloadUrl.toString(), lat, lon, bookedUserId, status,
                            fullName, contactNumber, emailAddress, tinNumber, age
                        )
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Failed to upload PDF: ${e.message}")
                }
        } else {
            saveBookingWithPdfProcessor(
                address, null, lat, lon, bookedUserId, status, fullName, contactNumber,
                emailAddress, tinNumber, age
            )
        }
    }


    private fun saveBookingWithPdf(
        address: String,
        pdfDownloadUrl: String?,
        lat: Double,
        lon: Double,
        bookedUserId: String,
        status: String,
        fullName: String,  // Receive fullName as String, not EditText
        contactNumber: String,  // Receive contactNumber as String
        emailAddress: String,  // Receive emailAddress as String
        propertyType: String,  // Receive propertyType as String
        purposeOfSurvey: String  // Receive purposeOfSurvey as String
    ) {
        val db = FirebaseFirestore.getInstance()
        val bookingUserId = FirebaseAuth.getInstance().currentUser?.uid

        val bookingData = hashMapOf(
            "landOwnerUserId" to bookingUserId,
            "bookedUserId" to bookedUserId,
            "timestamp" to FieldValue.serverTimestamp(),
            "address" to address,
            "status" to status,
            "stage" to "request",
            "startDateTime" to Timestamp(startDateTime!!.timeInMillis / 1000, 0),
            "pdfUrl" to (pdfDownloadUrl ?: ""),
            "latitude" to lat,
            "longitude" to lon,
            "fullName" to fullName,  // Added fullName
            "contactNumber" to contactNumber,  // Added contactNumber
            "emailAddress" to emailAddress,  // Added emailAddress
            "propertyType" to propertyType,  // Added propertyType
            "purposeOfSurvey" to purposeOfSurvey  // Added purposeOfSurvey
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

    private fun saveBookingWithPdfProcessor(
        address: String,
        pdfDownloadUrl: String?,
        lat: Double,
        lon: Double,
        bookedUserId: String,
        status: String,
        fullName: String,  // Receive fullName as String, not EditText
        contactNumber: String,  // Receive contactNumber as String
        emailAddress: String,  // Receive emailAddress as String
        tinNumber: String,
        age: Int
    ) {
        val db = FirebaseFirestore.getInstance()
        val bookingUserId = FirebaseAuth.getInstance().currentUser?.uid

        val bookingData = hashMapOf(
            "landOwnerUserId" to bookingUserId,
            "bookedUserId" to bookedUserId,
            "timestamp" to FieldValue.serverTimestamp(),
            "address" to address,
            "status" to status,
            "stage" to "request",
            "startDateTime" to Timestamp(startDateTime!!.timeInMillis / 1000, 0),
            "pdfUrl" to (pdfDownloadUrl ?: ""),
            "latitude" to lat,
            "longitude" to lon,
            "fullName" to fullName,  // Added fullName
            "contactNumber" to contactNumber,  // Added contactNumber
            "emailAddress" to emailAddress,  // Added emailAddress
            "tinNumber" to tinNumber,
            "age" to age
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
