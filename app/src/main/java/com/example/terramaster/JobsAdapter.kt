package com.example.terramaster

import FragmentDisplayPDF
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.gcacace.signaturepad.views.SignaturePad
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class JobsAdapter(private val jobs: MutableList<Job>, private val context: Context,    private val listener: OnPaymentClickListener, private val fragmentActivity: FragmentActivity) :
    RecyclerView.Adapter<JobsAdapter.JobsViewHolder>() {

    private var onItemClickListener: ((String) -> Unit)? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userType: String? = null

    class JobsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.profile_image)
        val userName: TextView = view.findViewById(R.id.user_name)
        val bookingDate: TextView = view.findViewById(R.id.booking_date)
        val startDate: TextView = view.findViewById(R.id.booking_start_date)
        val contractPrice: TextView = view.findViewById(R.id.contractPrice)
        val downpayment: TextView = view.findViewById(R.id.booking_downpayment)
        val status: TextView = view.findViewById(R.id.requestStatus)
        val address: TextView = view.findViewById(R.id.address)
        val pdfFile: TextView = view.findViewById(R.id.pdfFileName)
        val labelPrice: TextView = view.findViewById(R.id.labelPrice)
        val labelDown: TextView = view.findViewById(R.id.labeldown)
        val labelAge: TextView = view.findViewById(R.id.labelAge)
        val age: TextView = view.findViewById(R.id.age)
        val labelTin: TextView = view.findViewById(R.id.labelTin)
        val tin: TextView = view.findViewById(R.id.Tin)
        val purposeLabel: TextView = view.findViewById(R.id.purposeLabel)
        val purposeOfSurvey: TextView = view.findViewById(R.id.purposeOfSuurvey)
        val propertyTypeLabel: TextView = view.findViewById(R.id.propertyTypeLabel)
        val propertyLabel: TextView = view.findViewById(R.id.propertyLabel)
        val contactNumber: TextView = view.findViewById(R.id.contactNumber)
        val emailAddress: TextView = view.findViewById(R.id.emailAddress)


        val confirmButton: Button = view.findViewById(R.id.btn_confirm)
        val reviseButton: Button = view.findViewById(R.id.btn_revise)
        val declinedButton: Button = view.findViewById(R.id.btn_declined)
        val payButton: Button = view.findViewById(R.id.payButton)
        val esign: Button = view.findViewById(R.id.btn_esign)
        val btn_confirm_processor: Button = view.findViewById(R.id.btn_confirm_processor)
        val btn_revise_processor: Button = view.findViewById(R.id.btn_revise_processor)
        val btn_quotation: Button = view.findViewById(R.id.btn_quotation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.request_booking_item, parent, false)
        return JobsViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobsViewHolder, position: Int) {
        val job = jobs[position]
        val loggedInUserId = auth.currentUser?.uid

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid  // Get the current user ID
        val bookingUserId = job.landOwnerUserId // ID of the user who booked
        val bookedUserId = job.bookedUserId // ID of the user who is booked
        val bookingStatus = job.status

        // Determine the other user's ID based on the logged-in user's role
        val otherUserId =
            if (loggedInUserId == job.bookedUserId) job.landOwnerUserId else job.bookedUserId

        // Fetch the other user's details from the users collection
        firestore.collection("users").document(otherUserId)
            .get()
            .addOnSuccessListener { userSnapshot ->
                val firstName = userSnapshot.getString("first_name") ?: "Unknown"
                val lastName = userSnapshot.getString("last_name") ?: "User"
                val profilePicture = userSnapshot.getString("profile_picture")

                // Set the user details
                holder.userName.text = "$firstName $lastName"

                if (!profilePicture.isNullOrEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(profilePicture)
                        .placeholder(R.drawable.profile_pic) // Replace with your actual placeholder
                        .into(holder.profileImage)
                } else {
                    holder.profileImage.setImageResource(R.drawable.profile_pic)
                }
            }
            .addOnFailureListener {
                holder.userName.text = "Unknown User"
                holder.profileImage.setImageResource(R.drawable.profile_pic) // Default placeholder
            }



        // Fetch the user document for the bookedUserId (the artist)
        firestore.collection("users").document(bookedUserId)
            .get()
            .addOnSuccessListener { userSnapshot ->
                userType = userSnapshot.getString("user_type")

                // Check the userType and adjust the visibility accordingly
                when (userType) {
                    "Processor" -> {
                        // If the user is a "Processor", hide contract price and downpayment
                        holder.contractPrice.visibility = View.GONE
                        holder.downpayment.visibility = View.GONE
                        holder.labelDown.visibility = View.GONE
                        holder.labelPrice.visibility = View.GONE
                        holder.labelAge.visibility = View.VISIBLE
                        holder.age.visibility = View.VISIBLE
                        holder.labelTin.visibility = View.VISIBLE
                        holder.tin.visibility = View.VISIBLE
                        holder.purposeLabel.visibility = View.GONE
                        holder.purposeOfSurvey.visibility = View.GONE
                        holder.propertyTypeLabel.visibility = View.GONE
                        holder.propertyLabel.visibility = View.GONE
                    }
                    "Surveyor" -> {
                        // If the user is a "Surveyor", show contract price and downpayment
                        holder.contractPrice.visibility = View.VISIBLE
                        holder.downpayment.visibility = View.VISIBLE
                        holder.labelDown.visibility = View.VISIBLE
                        holder.labelPrice.visibility = View.VISIBLE
                        holder.labelAge.visibility = View.GONE
                        holder.age.visibility = View.GONE
                        holder.labelTin.visibility = View.GONE
                        holder.tin.visibility = View.GONE
                        holder.purposeLabel.visibility = View.VISIBLE
                        holder.purposeOfSurvey.visibility = View.VISIBLE
                        holder.propertyTypeLabel.visibility = View.VISIBLE
                        holder.propertyLabel.visibility = View.VISIBLE
                    }
                    else -> {
                        // Default case if there is no specific userType
                        holder.contractPrice.visibility = View.VISIBLE
                        holder.downpayment.visibility = View.VISIBLE
                        holder.labelDown.visibility = View.VISIBLE
                        holder.labelPrice.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle the error if the user document can't be fetched
                Log.e("BookingAdapter", "Error fetching user data: ${e.message}")
            }

        if (currentUserId != null) {
            // Proceed with your logic using currentUserId
            updateButtonsBasedOnStatus(job, position, holder, currentUserId)
        } else {
            // Handle the case where the user is not signed in
            Log.e("ConfirmBooking", "No user is currently signed in.")
        }
        // Bind job details

        holder.startDate.text = "Start: ${formatTimestamp(job.startDateTime)}"
        holder.downpayment.text = job.downpayment.toString() // Only display amount

        // Bind booking date
        holder.bookingDate.text = "Booking Date: ${formatTimestamp(job.timestamp)}"
        holder.contractPrice.text = job.contractPrice.toString()
        holder.status.text = job.status
        holder.tin.text = job.tinNumber
        holder.age.text = job.age.toString()
        holder.purposeOfSurvey.text = job.purposeOfSurvey
        holder.propertyTypeLabel.text = job.propertyType
        holder.emailAddress.text = job.emailAddress
        holder.contactNumber.text = job.contactNumber
        holder.address.text = job.address
        holder.address.setOnClickListener {
            val fragment = FragmentMap()

            // Pass latitude & longitude to the fragment
            val args = Bundle().apply {
                putDouble("latitude", job.latitude)
                putDouble("longitude", job.longitude)
            }
            fragment.arguments = args

            // Replace current fragment with MapFragment
            fragmentActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment) // Make sure R.id.fragment_container exists in your activity layout
                .addToBackStack(null) // Enables back navigation
                .commit()
        }

        holder.pdfFile.text = job.pdfFileName
        holder.pdfFile.setOnClickListener {
            val pdfUrl = jobs[position].pdfUrl

            if (pdfUrl.isNullOrEmpty()) {
                Log.e("PDF_ERROR", "Invalid PDF URL: $pdfUrl") // Log error if URL is empty
                Toast.makeText(holder.itemView.context, "PDF not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Stop execution if PDF URL is invalid
            }

            Log.d("PDF_DEBUG", "Opening PDF with URL: $pdfUrl") // Log valid PDF URL

            val fragment = FragmentDisplayPDF().apply {
                arguments = Bundle().apply {
                    putString("pdfUrl", pdfUrl)
                    putString("userType", userType)
                    putString("bookingId", job.bookingId)
                }
            }

            val fragmentManager = (holder.itemView.context as AppCompatActivity).supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }



        holder.btn_quotation.setOnClickListener {
            // Create the Dialog
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_contract_details, null)

            val contractPriceEditText = dialogView.findViewById<EditText>(R.id.edit_contract_price)
            val downPaymentEditText = dialogView.findViewById<EditText>(R.id.edit_down_payment)


            val builder = AlertDialog.Builder(context)
                .setTitle("Enter Contract Details")
                .setView(dialogView)
                .setPositiveButton("Save") { dialog, _ ->
                    val contractPrice = contractPriceEditText.text.toString().toDoubleOrNull()
                    val downPayment = downPaymentEditText.text.toString().toDoubleOrNull()

                    // Validate input
                    if (contractPrice != null && downPayment != null) {
                        if (currentUserId != null) {
                            // Proceed with your logic using currentUserId
                            updateBookingInDatabase(job.bookingId, contractPrice, downPayment, position, holder, currentUserId)
                        } else {
                            // Handle the case where the user is not signed in
                            Log.e("ConfirmBooking", "No user is currently signed in.")
                        }

                    } else {
                        Toast.makeText(context, "Please enter valid values", Toast.LENGTH_SHORT).show()
                    }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }

            builder.create().show()
        }

        // Accept button functionality with confirmation dialog
        holder.confirmButton.setOnClickListener {
            // Get the bookingId from your data model, e.g., a Booking object
            val bookingId = jobs[position].bookingId

            val currentUser = FirebaseAuth.getInstance().currentUser
            val currentUserId = currentUser?.uid

            // Assuming you're using an adapter and have a booking list
            if (currentUserId != null) {
                confirmBooking(bookingId, currentUserId, position, holder, currentUserId)
            } else {
                // Ensure context is available and show the toast
                context?.let {
                    Toast.makeText(it, "No user logged in.", Toast.LENGTH_SHORT).show()
                } ?: run {
                    // Fallback: show a message if context is unavailable
                    Toast.makeText(
                        holder.itemView.context,
                        "No user logged in.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        holder.btn_confirm_processor.setOnClickListener {
            // Get the bookingId from your data model, e.g., a Booking object
            val bookingId = jobs[position].bookingId

            val currentUser = FirebaseAuth.getInstance().currentUser
            val currentUserId = currentUser?.uid

            // Assuming you're using an adapter and have a booking list
            if (currentUserId != null) {
                confirmBookingProcessor(bookingId, currentUserId, position, holder, currentUserId)
            } else {
                // Ensure context is available and show the toast
                context?.let {
                    Toast.makeText(it, "No user logged in.", Toast.LENGTH_SHORT).show()
                } ?: run {
                    // Fallback: show a message if context is unavailable
                    Toast.makeText(
                        holder.itemView.context,
                        "No user logged in.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


        holder.reviseButton.setOnClickListener {

            val bookingId = jobs[position].bookingId
            val bookingRef = firestore.collection("bookings").document(bookingId)

            bookingRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentDownpayment = document.getDouble("downPayment") ?: 0.0
                    val currentContractPrice = document.getDouble("contractPrice") ?: 0.0
                    val currentStartDateTimeTimestamp = document.getTimestamp("startDateTime")
                    val currentStartDateTime = currentStartDateTimeTimestamp?.toDate()?.let {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
                    } ?: ""
                    val lat = document.getDouble("latitude") ?: 0.0
                    val lon = document.getDouble("longitude") ?: 0.0
                    val tinNumber = document.getString("tinNumber")
                    val age = document.getLong("age")?.toInt()?.toString() ?: ""
                    val propertyType = document.getString("propertyType") ?: ""
                    val purposeOfSurvey = document.getString("purposeOfSurvey") ?: ""
                    val emailAddress = document.getString("emailAddress")
                    val contactNumber = document.getString("contactNumber")

                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_booking, null)

                    val startDateTimeButton = dialogView.findViewById<Button>(R.id.startDateTimeButton)
                    val selectedStartDateTimeTextView = dialogView.findViewById<TextView>(R.id.selectedStartDateTimeTextView)
                    val downpaymentEditText = dialogView.findViewById<EditText>(R.id.downpaymentEditText)
                    val contractPriceEditText = dialogView.findViewById<EditText>(R.id.contractAmount)
                    val contactNumberEditText = dialogView.findViewById<EditText>(R.id.contactNumber)
                    val ageEditText = dialogView.findViewById<EditText>(R.id.age)
                    val emailAddressEditText = dialogView.findViewById<EditText>(R.id.emailAddress)
                    val tinNumberEditText = dialogView.findViewById<EditText>(R.id.tinNumber)
                    val propertyTypeGroup = dialogView.findViewById<RadioGroup>(R.id.propertyTypeGroup)
                    val purposeOfSurveyGroup = dialogView.findViewById<RadioGroup>(R.id.purposeOfSurveyGroup)
                    val addressEditText = dialogView.findViewById<EditText>(R.id.Address)

                    val residentialRadioButton = dialogView.findViewById<RadioButton>(R.id.residential)
                    val commercialRadioButton = dialogView.findViewById<RadioButton>(R.id.commercial)
                    val agriculturalRadioButton = dialogView.findViewById<RadioButton>(R.id.agricultural)
                    val vacantLandRadioButton = dialogView.findViewById<RadioButton>(R.id.vacantLand)
                    val otherPropertyRadioButton = dialogView.findViewById<RadioButton>(R.id.otherProperty)

// Purpose of Survey RadioButtons
                    val propertySaleRadioButton = dialogView.findViewById<RadioButton>(R.id.propertySale)
                    val legalDisputeRadioButton = dialogView.findViewById<RadioButton>(R.id.legalDispute)
                    val propertyDevelopmentRadioButton = dialogView.findViewById<RadioButton>(R.id.propertyDevelopment)
                    val landSubdivisionRadioButton = dialogView.findViewById<RadioButton>(R.id.landSubdivision)
                    val environmentalAssessmentRadioButton = dialogView.findViewById<RadioButton>(R.id.environmentalAssessment)
                    val otherPurposeRadioButton = dialogView.findViewById<RadioButton>(R.id.otherPurpose)

                    val ageLabel: TextView = dialogView.findViewById(R.id.ageLabel)
                    val tinLabel: TextView = dialogView.findViewById(R.id.tinLabel)
                    val propertyLabel: TextView = dialogView.findViewById(R.id.propertyLabel)
                    val purposeLabel: TextView = dialogView.findViewById(R.id.purposeLabel)
                    val contractLabel: TextView = dialogView.findViewById(R.id.contractLabel)
                    val labelDown: TextView = dialogView.findViewById(R.id.labelDown)


                    convertCoordinatesToAddress(context, lat, lon) { address ->
                        addressEditText.setText(address)
                    }



                    downpaymentEditText.setText(currentDownpayment.toString())
                    contractPriceEditText.setText(currentContractPrice.toString())
                    selectedStartDateTimeTextView.text = "Start Date and Time: $currentStartDateTime"

                    startDateTimeButton.setOnClickListener {
                        showDateTimePicker { dateTime ->
                            selectedStartDateTimeTextView.text = "Start Date and Time: $dateTime"
                        }
                    }

                    contactNumberEditText.setText(contactNumber.toString())
                    ageEditText.setText(age.toString())
                    tinNumberEditText.setText(tinNumber.toString())
                    emailAddressEditText.setText(emailAddress.toString())

                    when (propertyType) {
                        "Residential" -> residentialRadioButton.isChecked = true
                        "Commercial" -> commercialRadioButton.isChecked = true
                        "Agricultural" -> agriculturalRadioButton.isChecked = true
                        "Vacant Land" -> vacantLandRadioButton.isChecked = true
                        "Other" -> otherPropertyRadioButton.isChecked = true
                        else -> {
                            // Handle if propertyType doesn't match any known value
                        }
                    }


                    when (purposeOfSurvey) {
                        "Property sale or purchase" -> propertySaleRadioButton.isChecked = true
                        "Legal dispute or boundary issue" -> legalDisputeRadioButton.isChecked = true
                        "Property development or construction" -> propertyDevelopmentRadioButton.isChecked = true
                        "Land subdivision" -> landSubdivisionRadioButton.isChecked = true
                        "Environmental or flood assessment" -> environmentalAssessmentRadioButton.isChecked = true
                        "Other" -> otherPurposeRadioButton.isChecked = true
                        else -> {
                            // Handle if purposeOfSurvey doesn't match any known value
                        }
                    }

                    firestore.collection("users").document(bookedUserId)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            userType = userSnapshot.getString("user_type")

                            // Check the userType and adjust the visibility accordingly
                            when (userType) {
                                "Processor" -> {
                                    // If the user is a "Processor", hide contract price and downpayment
                                    contactNumberEditText.visibility = View.GONE
                                    downpaymentEditText.visibility = View.GONE
                                    labelDown.visibility = View.GONE
                                    contractLabel.visibility = View.GONE
                                    ageLabel.visibility = View.VISIBLE
                                    ageEditText.visibility = View.VISIBLE
                                    tinLabel.visibility = View.VISIBLE
                                    tinNumberEditText.visibility = View.VISIBLE
                                    purposeLabel.visibility = View.GONE
                                    purposeOfSurveyGroup.visibility = View.GONE
                                    propertyTypeGroup.visibility = View.GONE
                                    propertyLabel.visibility = View.GONE
                                }
                                "Surveyor" -> {
                                    // If the user is a "Surveyor", show contract price and downpayment
                                    contactNumberEditText.visibility = View.VISIBLE
                                    downpaymentEditText.visibility = View.VISIBLE
                                    labelDown.visibility = View.VISIBLE
                                    contractLabel.visibility = View.VISIBLE
                                    ageLabel.visibility = View.GONE
                                    ageEditText.visibility = View.GONE
                                    tinLabel.visibility = View.GONE
                                    tinNumberEditText.visibility = View.GONE
                                    purposeLabel.visibility = View.VISIBLE
                                    purposeOfSurveyGroup.visibility = View.VISIBLE
                                    propertyTypeGroup.visibility = View.VISIBLE
                                    propertyLabel.visibility = View.VISIBLE
                                    contractPriceEditText.visibility = View.VISIBLE
                                }
                                else -> {
                                    // Default case if there is no specific userType
                                   /* contractPrice.visibility = View.VISIBLE
                                    downpayment.visibility = View.VISIBLE
                                    labelDown.visibility = View.VISIBLE
                                    labelPrice.visibility = View.VISIBLE*/
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            // Handle the error if the user document can't be fetched
                            Log.e("BookingAdapter", "Error fetching user data: ${e.message}")
                        }
                    // 🔥 Check current user's type from Firestore
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                    firestore.collection("users").document(currentUserId).get()
                        .addOnSuccessListener { userDoc ->
                            val userType = userDoc.getString("user_type") ?: ""
                            val isSurveyor = userType.equals("Surveyor", ignoreCase = true)

                            // Disable address editing if user is Surveyor
                            addressEditText.isEnabled = !isSurveyor
                            addressEditText.isFocusable = !isSurveyor
                            addressEditText.isFocusableInTouchMode = !isSurveyor

                            val dialog = AlertDialog.Builder(context)
                                .setTitle("Edit Booking Details")
                                .setView(dialogView)
                                .setPositiveButton("Save") { _, _ ->
                                    val newAddress = addressEditText.text.toString()
                                    val newDownpayment = downpaymentEditText.text.toString().toDoubleOrNull() ?: 0.0
                                    val newContractPrice = contractPriceEditText.text.toString().toDoubleOrNull() ?: 0.0
                                    val newStartDateTime = selectedStartDateTimeTextView.text.toString()
                                        .replace("Start Date and Time: ", "")

                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    val newStartDateTimeDate = dateFormat.parse(newStartDateTime)
                                    // Retrieve the selected property type
                                    val selectedPropertyTypeId = propertyTypeGroup.checkedRadioButtonId
                                    val selectedPropertyType = if (selectedPropertyTypeId != -1) {
                                        dialogView.findViewById<RadioButton>(selectedPropertyTypeId).text.toString()
                                    } else {
                                        ""
                                    }
                                    val selectedPurposeOfSurveyId = purposeOfSurveyGroup.checkedRadioButtonId
                                    val selectedPurposeOfSurvey = if (selectedPurposeOfSurveyId != -1) {
                                        dialogView.findViewById<RadioButton>(selectedPurposeOfSurveyId).text.toString()
                                    } else {
                                        ""
                                    }

                                    val contactNumber = contactNumberEditText.text.toString()
                                    val emailAddressEditText = emailAddressEditText.text.toString()
                                    updateBookingDetails(
                                        newAddress,
                                        newDownpayment,
                                        newStartDateTimeDate,
                                        bookingId,
                                        newContractPrice,
                                        selectedPropertyType,
                                        selectedPurposeOfSurvey,
                                        position,
                                        holder,
                                        currentUserId,
                                        contactNumber,
                                        emailAddressEditText

                                    )

                                }
                                .setNegativeButton("Cancel", null)
                                .create()

                            dialog.show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to fetch user type.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Booking not found.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching booking details: ${e.message}", Toast.LENGTH_SHORT).show()
            }


        }


        holder.btn_revise_processor.setOnClickListener {
            val bookingId = jobs[position].bookingId
            val bookingRef = firestore.collection("bookings").document(bookingId)

            bookingRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentDownpayment = document.getDouble("downPayment") ?: 0.0
                    val currentContractPrice = document.getDouble("contractPrice") ?: 0.0
                    val currentStartDateTimeTimestamp = document.getTimestamp("startDateTime")
                    val currentStartDateTime = currentStartDateTimeTimestamp?.toDate()?.let {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
                    } ?: ""
                    val lat = document.getDouble("latitude") ?: 0.0
                    val lon = document.getDouble("longitude") ?: 0.0
                    val tinNumber = document.getString("tinNumber")
                    val age = document.getLong("age")?.toInt()
                    val propertyType = document.getString("propertyType") ?: ""
                    val purposeOfSurvey = document.getString("purposeOfSurvey") ?: ""
                    val emailAddress = document.getString("emailAddress")
                    val contactNumber = document.getString("contactNumber")

                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_booking, null)

                    val startDateTimeButton = dialogView.findViewById<Button>(R.id.startDateTimeButton)
                    val selectedStartDateTimeTextView = dialogView.findViewById<TextView>(R.id.selectedStartDateTimeTextView)
                    val downpaymentEditText = dialogView.findViewById<EditText>(R.id.downpaymentEditText)
                    val contractPriceEditText = dialogView.findViewById<EditText>(R.id.contractAmount)
                    val contactNumberEditText = dialogView.findViewById<EditText>(R.id.contactNumber)
                    val ageEditText = dialogView.findViewById<EditText>(R.id.age)
                    val emailAddressEditText = dialogView.findViewById<EditText>(R.id.emailAddress)
                    val tinNumberEditText = dialogView.findViewById<EditText>(R.id.tinNumber)
                    val propertyTypeGroup = dialogView.findViewById<RadioGroup>(R.id.propertyTypeGroup)
                    val purposeOfSurveyGroup = dialogView.findViewById<RadioGroup>(R.id.purposeOfSurveyGroup)
                    val addressEditText = dialogView.findViewById<EditText>(R.id.Address)

                    val residentialRadioButton = dialogView.findViewById<RadioButton>(R.id.residential)
                    val commercialRadioButton = dialogView.findViewById<RadioButton>(R.id.commercial)
                    val agriculturalRadioButton = dialogView.findViewById<RadioButton>(R.id.agricultural)
                    val vacantLandRadioButton = dialogView.findViewById<RadioButton>(R.id.vacantLand)
                    val otherPropertyRadioButton = dialogView.findViewById<RadioButton>(R.id.otherProperty)

// Purpose of Survey RadioButtons
                    val propertySaleRadioButton = dialogView.findViewById<RadioButton>(R.id.propertySale)
                    val legalDisputeRadioButton = dialogView.findViewById<RadioButton>(R.id.legalDispute)
                    val propertyDevelopmentRadioButton = dialogView.findViewById<RadioButton>(R.id.propertyDevelopment)
                    val landSubdivisionRadioButton = dialogView.findViewById<RadioButton>(R.id.landSubdivision)
                    val environmentalAssessmentRadioButton = dialogView.findViewById<RadioButton>(R.id.environmentalAssessment)
                    val otherPurposeRadioButton = dialogView.findViewById<RadioButton>(R.id.otherPurpose)

                    val ageLabel: TextView = dialogView.findViewById(R.id.ageLabel)
                    val tinLabel: TextView = dialogView.findViewById(R.id.tinLabel)
                    val propertyLabel: TextView = dialogView.findViewById(R.id.propertyLabel)
                    val purposeLabel: TextView = dialogView.findViewById(R.id.purposeLabel)
                    val contractLabel: TextView = dialogView.findViewById(R.id.contractLabel)
                    val labelDown: TextView = dialogView.findViewById(R.id.labelDown)


                    convertCoordinatesToAddress(context, lat, lon) { address ->
                        addressEditText.setText(address)
                    }



                    downpaymentEditText.setText(currentDownpayment.toString())
                    contractPriceEditText.setText(currentContractPrice.toString())
                    selectedStartDateTimeTextView.text = "Start Date and Time: $currentStartDateTime"

                    startDateTimeButton.setOnClickListener {
                        showDateTimePicker { dateTime ->
                            selectedStartDateTimeTextView.text = "Start Date and Time: $dateTime"
                        }
                    }

                    contactNumberEditText.setText(contactNumber.toString())
                    ageEditText.setText(age.toString())
                    tinNumberEditText.setText(tinNumber.toString())
                    emailAddressEditText.setText(emailAddress.toString())

                    when (propertyType) {
                        "Residential" -> residentialRadioButton.isChecked = true
                        "Commercial" -> commercialRadioButton.isChecked = true
                        "Agricultural" -> agriculturalRadioButton.isChecked = true
                        "Vacant Land" -> vacantLandRadioButton.isChecked = true
                        "Other" -> otherPropertyRadioButton.isChecked = true
                        else -> {
                            // Handle if propertyType doesn't match any known value
                        }
                    }


                    when (purposeOfSurvey) {
                        "Property sale or purchase" -> propertySaleRadioButton.isChecked = true
                        "Legal dispute or boundary issue" -> legalDisputeRadioButton.isChecked = true
                        "Property development or construction" -> propertyDevelopmentRadioButton.isChecked = true
                        "Land subdivision" -> landSubdivisionRadioButton.isChecked = true
                        "Environmental or flood assessment" -> environmentalAssessmentRadioButton.isChecked = true
                        "Other" -> otherPurposeRadioButton.isChecked = true
                        else -> {
                            // Handle if purposeOfSurvey doesn't match any known value
                        }
                    }

                    firestore.collection("users").document(bookedUserId)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            userType = userSnapshot.getString("user_type")

                            // Check the userType and adjust the visibility accordingly
                            when (userType) {
                                "Processor" -> {
                                    // If the user is a "Processor", hide contract price and downpayment
                                    contactNumberEditText.visibility = View.VISIBLE
                                    downpaymentEditText.visibility = View.GONE
                                    labelDown.visibility = View.GONE
                                    contractLabel.visibility = View.GONE
                                    ageLabel.visibility = View.VISIBLE
                                    ageEditText.visibility = View.VISIBLE
                                    tinLabel.visibility = View.VISIBLE
                                    tinNumberEditText.visibility = View.VISIBLE
                                    purposeLabel.visibility = View.GONE
                                    purposeOfSurveyGroup.visibility = View.GONE
                                    propertyTypeGroup.visibility = View.GONE
                                    propertyLabel.visibility = View.GONE
                                }
                                "Surveyor" -> {
                                    // If the user is a "Surveyor", show contract price and downpayment
                                    contactNumberEditText.visibility = View.VISIBLE
                                    downpaymentEditText.visibility = View.VISIBLE
                                    labelDown.visibility = View.VISIBLE
                                    contractLabel.visibility = View.VISIBLE
                                    ageLabel.visibility = View.GONE
                                    ageEditText.visibility = View.GONE
                                    tinLabel.visibility = View.GONE
                                    tinNumberEditText.visibility = View.GONE
                                    purposeLabel.visibility = View.VISIBLE
                                    purposeOfSurveyGroup.visibility = View.VISIBLE
                                    propertyTypeGroup.visibility = View.VISIBLE
                                    propertyLabel.visibility = View.VISIBLE
                                    contractPriceEditText.visibility = View.VISIBLE
                                }
                                else -> {
                                    // Default case if there is no specific userType
                                    /* contractPrice.visibility = View.VISIBLE
                                     downpayment.visibility = View.VISIBLE
                                     labelDown.visibility = View.VISIBLE
                                     labelPrice.visibility = View.VISIBLE*/
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            // Handle the error if the user document can't be fetched
                            Log.e("BookingAdapter", "Error fetching user data: ${e.message}")
                        }
                    // 🔥 Check current user's type from Firestore
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                    firestore.collection("users").document(currentUserId).get()
                        .addOnSuccessListener { userDoc ->
                            val userType = userDoc.getString("user_type") ?: ""
                            val isSurveyor = userType.equals("Surveyor", ignoreCase = true)

                            // Disable address editing if user is Surveyor
                            addressEditText.isEnabled = !isSurveyor
                            addressEditText.isFocusable = !isSurveyor
                            addressEditText.isFocusableInTouchMode = !isSurveyor

                            val dialog = AlertDialog.Builder(context)
                                .setTitle("Edit Booking Details")
                                .setView(dialogView)
                                .setPositiveButton("Save") { _, _ ->
                                    val newAddress = addressEditText.text.toString()
                                    val newDownpayment = downpaymentEditText.text.toString().toDoubleOrNull() ?: 0.0
                                    val newContractPrice = contractPriceEditText.text.toString().toDoubleOrNull() ?: 0.0
                                    val newStartDateTime = selectedStartDateTimeTextView.text.toString()
                                        .replace("Start Date and Time: ", "")

                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    val newStartDateTimeDate = dateFormat.parse(newStartDateTime)
                                    // Retrieve the selected property type
                                    val selectedPropertyTypeId = propertyTypeGroup.checkedRadioButtonId
                                    val selectedPropertyType = if (selectedPropertyTypeId != -1) {
                                        dialogView.findViewById<RadioButton>(selectedPropertyTypeId).text.toString()
                                    } else {
                                        ""
                                    }
                                    val selectedPurposeOfSurveyId = purposeOfSurveyGroup.checkedRadioButtonId
                                    val selectedPurposeOfSurvey = if (selectedPurposeOfSurveyId != -1) {
                                        dialogView.findViewById<RadioButton>(selectedPurposeOfSurveyId).text.toString()
                                    } else {
                                        ""
                                    }

                                    val contactNumber = contactNumberEditText.text.toString()
                                    val emailAddressEditText = emailAddressEditText.text.toString()
                                    val newTinNumber = tinNumberEditText.text.toString()
                                    val newAge = ageEditText.text.toString()

                                    val ageInt = if (newAge.isNotBlank()) newAge.toIntOrNull() ?: 0 else 0
                                    Log.d("Debug", "Age input: $newAge")

                                    updateBookingDetailsProcessor(
                                        newAddress,
                                        newStartDateTimeDate,
                                        bookingId,
                                        ageInt,
                                        newTinNumber,
                                        position,
                                        holder,
                                        currentUserId,
                                        contactNumber,
                                        emailAddressEditText

                                    )

                                }

                                .setNegativeButton("Cancel", null)
                                .create()

                            dialog.show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to fetch user type.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Booking not found.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching booking details: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }

        holder.declinedButton.setOnClickListener {
            val bookingId = jobs[position].bookingId
            val currentUser = FirebaseAuth.getInstance().currentUser
            val currentUserId = currentUser?.uid

            if (currentUserId != null) {
                // Reference to the booking document
                val bookingRef =
                    FirebaseFirestore.getInstance().collection("bookings").document(bookingId)

                // Prepare the updated data
                val updatedData = mapOf(
                    "status" to "Declined",
                    "stage" to "completed",
                    "lastModifiedBy" to currentUserId
                )

                // Update the document in Firestore
                bookingRef.update(updatedData)
                    .addOnSuccessListener {
                        // Handle success, e.g., show a toast or update the UI
                        Toast.makeText(
                            holder.itemView.context,
                            "Booking has been declined",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Remove the item from the list (if needed)
                        jobs.removeAt(position)

                        // Notify the adapter that the item has been removed
                        notifyItemRemoved(position)

                        // Optionally, notify the adapter that the dataset has changed
                        notifyItemRangeChanged(position, jobs.size)

                    }
                    .addOnFailureListener { e ->
                        // Handle failure (e.g., show an error message)
                        Toast.makeText(
                            holder.itemView.context,
                            "Failed to decline booking: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                // Handle case where the user is not logged in (currentUser is null)
                Toast.makeText(holder.itemView.context, "User not logged in", Toast.LENGTH_SHORT)
                    .show()
            }
        }


        holder.payButton.setOnClickListener {
            val jobs = jobs[position]
            listener.onPayNowClicked(jobs.bookingId) // Pass the booking ID when clicked
        }


        // Handle item click
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(job.bookingId) // Pass bookingId to the listener
        }

        holder.esign.setOnClickListener {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_signature, null)
            val signaturePad = dialogView.findViewById<SignaturePad>(R.id.signature_pad)
            val btnClear = dialogView.findViewById<Button>(R.id.btnClearSignature)
            val btnSave = dialogView.findViewById<Button>(R.id.btnSaveSignature)
            val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
            val bookingId = job.bookingId

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // Show the dialog
            dialog.show()

            // Clear the signature pad
            btnClear.setOnClickListener {
                signaturePad.clear()
            }

            // Save the signature
            btnSave.setOnClickListener {
                val signatureBitmap = signaturePad.signatureBitmap
                if (signatureBitmap != null) {
                    // Show the progress bar before starting the upload
                    progressBar.visibility = View.VISIBLE

                    // Compress and upload the signature in the background
                    GlobalScope.launch(Dispatchers.Main) {
                        try {
                            val compressedData = compressBitmap(signatureBitmap)
                            // Upload the signature to Firebase
                            uploadSignatureToFirebase(compressedData, bookingId, progressBar)

                            // Dismiss the dialog after upload starts
                            dialog.dismiss()

                        } catch (e: Exception) {
                            Log.e("Signature", "Error compressing signature", e)
                        }
                    }
                }
            }
        }

    }

    override fun getItemCount(): Int = jobs.size

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
    }

    interface OnTabNavigationListener {
        fun navigateToTab(tabIndex: Int)
    }


    private suspend fun compressBitmap(bitmap: Bitmap): ByteArray {
        return withContext(Dispatchers.IO) {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        }
    }

    private suspend fun uploadSignatureToFirebase(compressedData: ByteArray, bookingId: String, progressBar: ProgressBar) {
        val storageReference = FirebaseStorage.getInstance().reference
        val signatureRef = storageReference.child("signatures/processed_signature.png")

        // Ensure the code runs within the coroutine scope
        withContext(Dispatchers.IO) {
            try {
                // Show the progress bar during the upload
                val uploadTask = signatureRef.putBytes(compressedData)

                // Monitor the upload progress
                uploadTask.addOnProgressListener { snapshot ->
                    val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()

                    // Update the progress bar on the main thread
                    GlobalScope.launch(Dispatchers.Main) {
                        progressBar.progress = progress
                    }
                }

                // Await the completion of the upload
                uploadTask.await()

                // Retrieve the URL of the uploaded signature
                val downloadUrl = signatureRef.downloadUrl.await()
                Log.d("Signature", "Signature uploaded successfully. Download URL: $downloadUrl")

                // Update the booking with the signature URL
                updateBookingWithSignature(downloadUrl.toString(), bookingId)

            } catch (e: Exception) {
                Log.e("Firebase", "Error uploading signature", e)
            } finally {
                // Hide the progress bar once upload completes or fails
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    // Update the booking with the signature URL and other details
    private fun updateBookingWithSignature(signatureUrl: String, bookingId: String) {
        val db = FirebaseFirestore.getInstance()

        // Reference to the booking document
        val bookingRef = db.collection("bookings").document(bookingId)

        // Create a map for the updates
        val updates = mutableMapOf<String, Any>(
            "signatureUrl" to signatureUrl,
            "signatureTimestamp" to System.currentTimeMillis(), // Optionally add a timestamp
            "status" to "verified",
            "stage" to "ongoing"
        )

        // Perform the update in Firestore
        bookingRef.update(updates)
            .addOnSuccessListener {
                Log.d("Booking", "Booking updated with signature URL: $signatureUrl")

                // Navigate to the "Request" tab (Tab 1) after a successful update
                navigateToRequestTabFragment()
            }
            .addOnFailureListener { e ->
                Log.e("Booking", "Error updating booking", e)
            }
    }

    // Navigate to the "Request" tab (Tab 1)
    private fun navigateToRequestTabFragment() {
        val fragment = FragmentJobs().apply {
            arguments = Bundle().apply {
                putInt("selectedTab", 1) // Pass tab index for "Request" tab
            }
        }

        // Replace the current fragment with the "Request" tab fragment
        fragmentActivity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateBookingInDatabase(bookingId: String, contractPrice: Double, downPayment: Double, position: Int, holder: JobsViewHolder, currentUserId: String) {
        val db = FirebaseFirestore.getInstance()

        val bookingRef = db.collection("bookings").document(bookingId)

        // Explicitly cast HashMap to Map<String, Any>
        val updatedData = hashMapOf(
            "contractPrice" to contractPrice,
            "downPayment" to downPayment,
            "status" to "Waiting for landowners confirmation"
        ) as Map<String, Any> // Cast here

        // Update the booking in Firestore
        bookingRef.update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(context, "Booking updated successfully", Toast.LENGTH_SHORT).show()
                val job = jobs[position]
                // Optionally update the local data if needed
                job.contractPrice = contractPrice
                job.downpayment = downPayment
                job.status = "Waiting for landowners confirmation"

                updateButtonsBasedOnStatus(job, position, holder, currentUserId)
                // Notify the adapter that the data at that specific position has changed
                notifyItemChanged(position)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating booking: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    // Function to display a DateTimePicker and return the selected date and time
    private fun showDateTimePicker(onDateTimeSelected: (String) -> Unit) {
        val currentDate = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // TimePicker after date selection
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDate.set(Calendar.MINUTE, minute)

                        // Validate if the selected date is in the future
                        if (selectedDate.time.after(currentDate.time)) {
                            val dateTimeFormat =
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            val formattedDateTime = dateTimeFormat.format(selectedDate.time)
                            onDateTimeSelected(formattedDateTime)
                        } else {
                            Toast.makeText(
                                context,
                                "Please select a future date and time.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    currentDate.get(Calendar.HOUR_OF_DAY),
                    currentDate.get(Calendar.MINUTE),
                    true
                ).show()
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate =
            System.currentTimeMillis() // Disables past dates in the DatePicker
        datePickerDialog.show()
    }


    // Function to update booking details
    fun updateBookingDetails(
        newAddress: String,
        newDownpayment: Double,
        newStartDateTime: Date?,
        bookingId: String,
        contractPrice: Double,
        selectedPropertyType: String,
        selectedPurposeOfSurvey: String,
        position: Int,
        holder: JobsViewHolder,
        currentUserId: String,
        contactNumber: String,
        emailAddress: String
    ) {

        val bookingRef = firestore.collection("bookings").document(bookingId)
        val job = jobs[position]

        bookingRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val bookedUserId = document.getString("bookedUserId")
                val bookingUserId = document.getString("landOwnerUserId")

                if (bookedUserId == null || bookingUserId == null) {
                    Toast.makeText(context, "Error: Missing user data in booking document.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val updatedStatus = when (currentUserId) {
                    bookedUserId -> "professional edit details"
                    bookingUserId -> "landowner edit details"
                    else -> {
                        Toast.makeText(context, "User is not authorized to update this booking.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                }

                convertLocationToCoordinates(newAddress) { lat, lon ->
                    if (lat == 0.0 || lon == 0.0) {
                        Toast.makeText(context, "Failed to get location coordinates.", Toast.LENGTH_SHORT).show()
                        return@convertLocationToCoordinates
                    }

                    // Prepare Firestore update
                    val updatedBookingData = mapOf(
                        "address" to newAddress,
                        "downPayment" to newDownpayment,
                        "startDateTime" to newStartDateTime?.let { Timestamp(it) },
                        "status" to updatedStatus,
                        "lastModifiedBy" to currentUserId,
                        "contractPrice" to contractPrice,
                        "latitude" to lat,
                        "longitude" to lon,
                        "propertyType" to selectedPropertyType,
                        "purposeOfSurvey" to selectedPurposeOfSurvey,
                        "contactNumber" to contactNumber,
                        "emailAddress" to emailAddress
                    )

                    bookingRef.update(updatedBookingData)
                        .addOnSuccessListener {
                            // ✅ Update the local job object
                            job.address = newAddress
                            job.downpayment = newDownpayment
                            job.startDateTime = newStartDateTime?.let { Timestamp(it) }
                            job.status = updatedStatus
                            job.contractPrice = contractPrice
                            job.latitude = lat
                            job.longitude = lon
                            job.propertyType = selectedPropertyType
                            job.purposeOfSurvey = selectedPurposeOfSurvey
                            job.contactNumber = contactNumber
                            job.emailAddress = emailAddress

                            Toast.makeText(context, "Booking updated successfully.", Toast.LENGTH_SHORT).show()
                            updateButtonsBasedOnStatus(job, position, holder, currentUserId)
                            notifyItemChanged(position)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error updating booking: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(context, "Booking not found.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error fetching booking details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    fun updateBookingDetailsProcessor(
        newAddress: String,
        newStartDateTime: Date?,
        bookingId: String,
        age: Int,
        tinNumber: String,
        position: Int,
        holder: JobsViewHolder,
        currentUserId: String,
        contactNumber: String,
        emailAddress: String
    ) {

        val bookingRef = firestore.collection("bookings").document(bookingId)
        val job = jobs[position]

        bookingRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val bookedUserId = document.getString("bookedUserId")
                val bookingUserId = document.getString("landOwnerUserId")

                if (bookedUserId == null || bookingUserId == null) {
                    Toast.makeText(context, "Error: Missing user data in booking document.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val updatedStatus = when (currentUserId) {
                    bookedUserId -> "processor edit details"
                    bookingUserId -> "landowner edit detail"
                    else -> {
                        Toast.makeText(context, "User is not authorized to update this booking.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                }

                convertLocationToCoordinates(newAddress) { lat, lon ->
                    if (lat == 0.0 || lon == 0.0) {
                        Toast.makeText(context, "Failed to get location coordinates.", Toast.LENGTH_SHORT).show()
                        return@convertLocationToCoordinates
                    }

                    // Prepare Firestore update
                    val updatedBookingData = mapOf(
                        "address" to newAddress,
                        "startDateTime" to newStartDateTime?.let { Timestamp(it) },
                        "status" to updatedStatus,
                        "lastModifiedBy" to currentUserId,
                        "latitude" to lat,
                        "longitude" to lon,
                        "contactNumber" to contactNumber,
                        "emailAddress" to emailAddress,
                        "age" to age,
                        "tinNumber" to tinNumber
                    )

                    bookingRef.update(updatedBookingData)
                        .addOnSuccessListener {
                            // ✅ Update the local job object
                            job.address = newAddress
                            job.startDateTime = newStartDateTime?.let { Timestamp(it) }
                            job.status = updatedStatus
                            job.latitude = lat
                            job.longitude = lon
                            job.contactNumber = contactNumber
                            job.emailAddress = emailAddress
                            job.age = age.toString()
                            job.tinNumber = tinNumber

                            Toast.makeText(context, "Booking updated successfully.", Toast.LENGTH_SHORT).show()
                            updateButtonsBasedOnStatus(job, position, holder, currentUserId)
                            notifyItemChanged(position)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error updating booking: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(context, "Booking not found.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error fetching booking details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertLocationToCoordinates(locationName: String, callback: (Double, Double) -> Unit) {
        val geocoder = OpenStreetMapGeocoder(context)
        geocoder.getCoordinatesFromAddress(locationName) { coordinates ->
            if (coordinates != null) {
                callback(coordinates.latitude, coordinates.longitude)
            } else {
                Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateJobs(newJobs: List<Job>) {
        jobs.clear()
        jobs.addAll(newJobs)
        notifyDataSetChanged()  // Refresh UI
    }

    // Utility function to format Timestamp to String
    private fun formatTimestamp(timestamp: Timestamp?): String {
        return timestamp?.toDate()?.let {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(it)
        } ?: "N/A"
    }

    fun confirmBooking(bookingId: String, userId: String, position: Int, holder: JobsViewHolder, currentUserId: String) {
        val bookingRef = FirebaseFirestore.getInstance().collection("bookings").document(bookingId)

        // Start Firestore transaction
        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val document = transaction.get(bookingRef)

            if (document.exists()) {
                // Extract the relevant fields from Firestore
                val artistId = document.getString("bookedUserId") ?: ""
                val clientId = document.getString("landOwnerUserId") ?: ""
                val bookingStatus = document.getString("status") ?: ""

                // Prepare the updates to Firestore
                val updatedBooking: MutableMap<String, Any> = HashMap()

                when {
                    userId == clientId -> {
                        val job = jobs[position]
                        if (job.downpayment != 0.0) {
                            listener.onPayNowClicked(bookingId)
                            updatedBooking["status"] = "pending_payment"
                            job.status = "pending_payment"
                        } else {
                            updatedBooking["status"] = "pending"
                            job.status = "pending"
                            updatedBooking["stage"] = "ongoing"
                            job.stage = "ongoing"
                        }

                        // Update the job status in the UI (main thread)
                        Handler(Looper.getMainLooper()).post {
                            // Pass the holder to update buttons based on the status
                            updateButtonsBasedOnStatus(job, position, holder, currentUserId)
                            notifyItemChanged(position)
                        }
                    }

                    userId == artistId -> {
                        val job = jobs[position]
                        when (bookingStatus) {
                            "payment_submitted" -> {
                                updatedBooking["status"] = "pending"
                                updatedBooking["stage"] = "ongoing"
                                job.stage = "ongoing"
                                job.status = "pending"
                            }
                            "landowner edit details" -> {
                                updatedBooking["status"] = "Surveyor Confirmed"
                                job.status = "Surveyor Confirmed"
                            }
                            else -> {
                                updatedBooking["status"] = "Surveyor Confirmed Waiting for quotation"
                                job.status = "Surveyor Confirmed Waiting for quotation"
                            }
                        }

                        // Update the job status in the UI (main thread)
                        Handler(Looper.getMainLooper()).post {
                            // Pass the holder to update buttons based on the status
                            updateButtonsBasedOnStatus(job, position, holder, currentUserId)
                            notifyItemChanged(position)
                        }
                    }

                    else -> {
                        Log.w("ConfirmBooking", "User is neither client nor artist. No action taken.")
                    }
                }

                // Only apply update if changes were made
                if (updatedBooking.isNotEmpty()) {
                    transaction.update(bookingRef, updatedBooking)
                }

            } else {
                Log.e("ConfirmBooking", "Booking document not found.")
            }

            null // Must return null to complete transaction
        }
            .addOnSuccessListener {
                // After successful transaction, handle UI updates on the main thread
                Log.d("ConfirmBooking", "Booking $bookingId status successfully updated.")
                Handler(Looper.getMainLooper()).post {
                    val job = jobs[position]
                    if (job.stage == "ongoing") {
                        // Job is ongoing, remove from the list
                        jobs.removeAt(position)
                        notifyItemRemoved(position)

                        val fragmentTransaction = (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()

                        // Switch to Ongoing tab in FragmentJobs
                        val fragmentJobs = FragmentJobs().apply {
                            arguments = Bundle().apply {
                                putInt("selectedTab", 1) // Ongoing tab index
                            }
                        }

                        fragmentTransaction?.replace(R.id.fragment_container, fragmentJobs)
                        fragmentTransaction?.addToBackStack(null)
                        fragmentTransaction?.commit()
                    } else {
                        // Job not ongoing, refresh item at position
                        updateButtonsBasedOnStatus(job, position, holder, currentUserId)
                        notifyItemChanged(position)
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle failure by logging and possibly reverting UI changes if necessary
                Log.e("ConfirmBooking", "Error updating booking: ", e)
            }
    }



    fun confirmBookingProcessor(
        bookingId: String,
        userId: String,
        position: Int,
        holder: JobsViewHolder,
        currentUserId: String
    ) {
        val bookingRef = FirebaseFirestore.getInstance().collection("bookings").document(bookingId)

        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val document = transaction.get(bookingRef)

            if (document.exists()) {
                val surveyorId = document.getString("bookedUserId")
                val landownerId = document.getString("landOwnerUserId")
                val bookingStatus = document.getString("status")

                val updatedBooking: MutableMap<String, Any> = HashMap()

                when {
                    userId == landownerId -> {
                        val job = jobs[position] // Get current job object

                        Log.d("ConfirmBooking", "Client booking confirmed without downpayment.")
                        updatedBooking["status"] = "Waiting for processor document verification"
                        job.status = "Waiting for processor document verification"
                        updateButtonsBasedOnStatus(job, position, holder, currentUserId) // Update button status
                    }

                    userId == surveyorId -> {
                        Log.d("ConfirmBooking", "Surveyor confirmed booking. Current Status: $bookingStatus")

                        when (bookingStatus) {
                            "new processor request" -> {
                                updatedBooking["status"] = "Waiting for processor document verification"
                                jobs[position].status = "Waiting for processor document verification"
                            }

                            "landowner edit detail" -> {
                                updatedBooking["status"] = "Waiting for processor document verification"
                                jobs[position].status = "Waiting for processor document verification"
                            }

                            else -> {
                                updatedBooking["status"] = "Waiting for processor document verification"
                                jobs[position].status = "Waiting for processor document verification"
                            }
                        }
                    }

                    else -> {
                        Log.e("ConfirmBooking", "Neither landowner nor surveyor matched. UserId: $userId")
                    }
                }

                if (updatedBooking.isNotEmpty()) {
                    transaction.update(bookingRef, updatedBooking)
                }
            } else {
                Log.e("ConfirmBooking", "Booking document not found.")
            }
            return@runTransaction null
        }
            .addOnSuccessListener {
                Log.d("ConfirmBooking", "Booking $bookingId status updated.")

                // UI update after Firestore transaction completes
                Handler(Looper.getMainLooper()).post {
                    val job = jobs[position]
                    job.status = "Waiting for processor document verification"  // Update status in the job object

                    // Call notifyItemChanged to update the RecyclerView item
                    notifyItemChanged(position)

                    // If the job is "ongoing", remove it from the list and navigate
                    if (job.stage == "ongoing") {
                        jobs.removeAt(position)
                        notifyItemRemoved(position)

                        val fragmentTransaction = (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                        val ongoingFragment = OnGoingFragment() // Your target fragment

                        fragmentTransaction?.replace(R.id.fragment_container, ongoingFragment)
                        fragmentTransaction?.addToBackStack(null)
                        fragmentTransaction?.commit()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ConfirmBooking", "Error updating booking: ", e)
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

    fun convertCoordinatesToAddress(
        context: Context, // Pass context explicitly
        lat: Double,
        lon: Double,
        callback: (String) -> Unit
    ) {
        val geocoder = OpenStreetMapGeocoder(context)
        geocoder.getAddressFromCoordinates(lat, lon) { address ->
            Log.d("Adapter", "Geocoding result: $address for coordinates: ($lat, $lon)")

            // Ensure UI update happens on the main thread
            Handler(Looper.getMainLooper()).post {
                callback(address ?: "Unknown Address")
            }
        }
    }

    private fun updateButtonsBasedOnStatus(job: Job, position: Int,  holder: JobsViewHolder, currentUserId: String) {
        val bookingUserId = job.landOwnerUserId
        val bookedUserId = job.bookedUserId
        when (job.status) {
            //Surveyor
            "new surveyor request" -> {
                if (currentUserId == bookingUserId) {
                    //Current user is the one who booked, show Edit and Decline buttons
                    holder.reviseButton.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                    holder.confirmButton.visibility = View.GONE
                } else if (currentUserId == bookedUserId) {
                    // Current user is the one being booked, show Confirm, Edit, and Decline buttons
                    holder.reviseButton.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                    holder.confirmButton.visibility = View.VISIBLE
                } else {
                    // Hide all buttons if the current user is neither
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }
            "Surveyor Confirmed Waiting for quotation" -> {
                if (currentUserId == bookingUserId) {
                    // Current user is the one who booked, show Edit and Decline buttons
                    holder.reviseButton.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                } else if (currentUserId == bookedUserId) {
                    // Current user is the one being booked, show Confirm, Edit, and Decline buttons
                    holder.btn_quotation.visibility = View.VISIBLE
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                } else {
                    // Hide all buttons if the current user is neither
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }
            "Waiting for landowners confirmation" -> {
                if (currentUserId == bookingUserId) {
                    // Current user is the one who booked, show Edit and Decline buttons
                    holder.reviseButton.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                    holder.confirmButton.visibility = View.VISIBLE
                } else if (currentUserId == bookedUserId) {
                    holder.reviseButton.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                } else {
                    // Hide all buttons if the current user is neither
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }
            "pending_payment" -> {
                if (currentUserId == bookingUserId) {
                    // Current user is the one who booked, show Edit and Decline buttons
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.VISIBLE
                    holder.payButton.visibility = View.VISIBLE
                    holder.confirmButton.visibility = View.GONE
                } else if (currentUserId == bookedUserId) {
                    // Current user is the one being booked, show Confirm, Edit, and Decline buttons
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.VISIBLE
                    holder.confirmButton.visibility = View.GONE
                } else {
                    // Hide all buttons if the current user is neither
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }
            "payment_submitted" -> {
                if (currentUserId == bookingUserId) {
                    // Current user is the one who booked, show Edit and Decline buttons
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.payButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                } else if (currentUserId == bookedUserId) {
                    // Current user is the one being booked, show Confirm, Edit, and Decline buttons
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.VISIBLE
                } else {
                    // Hide all buttons if the current user is neither
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }

            "accepted" -> {
                if (currentUserId == bookingUserId) {
                    // Current user is the one who booked, show Edit and Decline buttons
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.payButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                } else if (currentUserId == bookedUserId) {
                    // Current user is the one being booked, show Confirm, Edit, and Decline buttons
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                } else {
                    // Hide all buttons if the current user is neither
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }

            "professional edit details" -> {
                if (currentUserId == bookingUserId) {
                    // Client (Booking User) sees Pay Now button
                    holder.reviseButton.visibility = View.VISIBLE  // Optional: Client can revise the booking
                    holder.declinedButton.visibility = View.VISIBLE  // Client can cancel the booking
                    holder.payButton.visibility = View.GONE  // Pay Now button for client to pay
                    holder.confirmButton.visibility = View.VISIBLE  // No confirm button for the client
                } else if (currentUserId == bookedUserId) {
                    // Artist (Booked User) sees only Decline and Revise buttons
                    holder.reviseButton.visibility = View.VISIBLE  // Artist can revise the booking
                    holder.declinedButton.visibility = View.VISIBLE  // Artist can decline the booking
                    // Pay Now button is hidden for the artist
                    holder.confirmButton.visibility = View.GONE  // No confirm button for the artist
                } else {
                    // Hide all buttons if current user is neither the artist nor client
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }

            "landowner edit details" -> {
                if (currentUserId == bookingUserId) {
                    // Client (Booking User) sees Pay Now button
                    holder.reviseButton.visibility = View.VISIBLE  // Optional: Client can revise the booking
                    holder.declinedButton.visibility = View.VISIBLE  // Client can cancel the booking
                    holder.payButton.visibility = View.GONE  // Pay Now button for client to pay
                    holder.confirmButton.visibility = View.GONE  // No confirm button for the client
                } else if (currentUserId == bookedUserId) {
                    // Artist (Booked User) sees only Decline and Revise buttons
                    holder.reviseButton.visibility = View.VISIBLE  // Artist can revise the booking
                    holder.declinedButton.visibility = View.VISIBLE
                    holder.payButton.visibility = View.GONE// Artist can decline the booking
                    // Pay Now button is hidden for the artist
                    holder.confirmButton.visibility = View.VISIBLE  // No confirm button for the artist
                } else {
                    // Hide all buttons if current user is neither the artist nor client
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }
            "Surveyor Confirmed" -> {
                if (currentUserId == bookingUserId) {
                    // Current user is the one who booked, show Edit and Decline buttons
                    holder.reviseButton.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                    holder.confirmButton.visibility = View.VISIBLE
                } else if (currentUserId == bookedUserId) {
                    // Current user is the one being booked, show Confirm, Edit, and Decline buttons
                    holder.reviseButton.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                } else {
                    // Hide all buttons if the current user is neither
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }

            //Processor
            "new processor request" -> {
                if (currentUserId == bookingUserId) {
                    //Current user is the one who booked, show Edit and Decline buttons
                    holder.btn_revise_processor.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                } else if (currentUserId == bookedUserId) {
                    // Current user is the one being booked, show Confirm, Edit, and Decline buttons
                    holder.btn_revise_processor.visibility = View.VISIBLE
                    holder.btn_confirm_processor.visibility = View.VISIBLE
                } else {
                    // Hide all buttons if the current user is neither
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }
            "Waiting for processor document verification" -> {
                if (currentUserId == bookingUserId) {
                    //Current user is the one who booked, show Edit and Decline buttons
                    holder.btn_revise_processor.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                } else if (currentUserId == bookedUserId) {
                    // Current user is the one being booked, show Confirm, Edit, and Decline buttons
                    holder.esign.visibility = View.VISIBLE
                } else {
                    // Hide all buttons if the current user is neither
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }

            "landowner edit detail" -> {
                if (currentUserId == bookingUserId) {
                    //Current user is the one who booked, show Edit and Decline buttons
                    holder.btn_revise_processor.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                } else if (currentUserId == bookedUserId) {
                    // Current user is the one being booked, show Confirm, Edit, and Decline buttons
                    holder.btn_revise_processor.visibility = View.VISIBLE
                    holder.btn_confirm_processor.visibility = View.VISIBLE
                } else {
                    // Hide all buttons if the current user is neither
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }
            "processor edit details" -> {
                if (currentUserId == bookingUserId) {
                    //Current user is the one who booked, show Edit and Decline buttons
                    holder.btn_revise_processor.visibility = View.VISIBLE
                    holder.btn_confirm_processor.visibility = View.VISIBLE

                } else if (currentUserId == bookedUserId) {
                    // Current user is the one being booked, show Confirm, Edit, and Decline buttons
                    holder.btn_revise_processor.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                } else {
                    // Hide all buttons if the current user is neither
                    holder.reviseButton.visibility = View.GONE
                    holder.declinedButton.visibility = View.GONE
                    holder.confirmButton.visibility = View.GONE
                }
            }
        }

    }

}