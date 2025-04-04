package com.example.terramaster

import FragmentDisplayPDF
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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

        val confirmButton: Button = view.findViewById(R.id.btn_confirm)
        val reviseButton: Button = view.findViewById(R.id.btn_revise)
        val declinedButton: Button = view.findViewById(R.id.btn_declined)
        val payButton: Button = view.findViewById(R.id.payButton)
        val esign: Button = view.findViewById(R.id.btn_esign)
        val btn_confirm_processor: Button = view.findViewById(R.id.btn_confirm_processor)
        val btn_revise_processor: Button = view.findViewById(R.id.btn_revise_processor)
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
                    }
                    "Surveyor" -> {
                        // If the user is a "Surveyor", show contract price and downpayment
                        holder.contractPrice.visibility = View.VISIBLE
                        holder.downpayment.visibility = View.VISIBLE
                        holder.labelDown.visibility = View.VISIBLE
                        holder.labelPrice.visibility = View.VISIBLE
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

        // Bind job details

        holder.startDate.text = "Start: ${formatTimestamp(job.startDateTime)}"
        holder.downpayment.text = job.downpayment.toString() // Only display amount

        // Bind booking date
        holder.bookingDate.text = "Booking Date: ${formatTimestamp(job.timestamp)}"
        holder.contractPrice.text = job.contractPrice.toString()
        holder.status.text = job.status
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


        when (bookingStatus) {
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

            "artist_approved" -> {
                if (currentUserId == bookingUserId) {
                    // Current user is the one who booked, show Edit and Decline buttons
                    holder.reviseButton.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                    holder.confirmButton.visibility = View.VISIBLE
                } else if (currentUserId == bookedUserId) {
                    // Current user is the one being booked, show Confirm, Edit, and Decline buttons
                    holder.reviseButton.visibility = View.VISIBLE
                    holder.declinedButton.visibility = View.VISIBLE
                    holder.confirmButton.visibility = View.GONE
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
                    holder.confirmButton.visibility =
                        View.VISIBLE  // No confirm button for the artist
                } else {
                    // Hide all buttons if current user is neither the artist nor client
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
        // Accept button functionality with confirmation dialog
        holder.confirmButton.setOnClickListener {
            // Get the bookingId from your data model, e.g., a Booking object
            val bookingId = jobs[position].bookingId

            val currentUser = FirebaseAuth.getInstance().currentUser
            val currentUserId = currentUser?.uid

            // Assuming you're using an adapter and have a booking list
            if (currentUserId != null) {
                confirmBooking(bookingId, currentUserId, position)
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
                confirmBookingProcessor(bookingId, currentUserId, position)
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
                    val currentDownpayment = document.getDouble("downpayment") ?: 0.0
                    val currentContractPrice = document.getDouble("contractPrice") ?: 0.0
                    val currentStartDateTimeTimestamp = document.getTimestamp("startDateTime")
                    val currentStartDateTime = currentStartDateTimeTimestamp?.toDate()?.let {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
                    } ?: ""
                    val lat = document.getDouble("latitude") ?: 0.0
                    val lon = document.getDouble("longitude") ?: 0.0

                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_booking, null)
                    val addressEditText = dialogView.findViewById<EditText>(R.id.Address)

                    convertCoordinatesToAddress(context, lat, lon) { address ->
                        addressEditText.setText(address)
                    }

                    val startDateTimeButton = dialogView.findViewById<Button>(R.id.startDateTimeButton)
                    val selectedStartDateTimeTextView = dialogView.findViewById<TextView>(R.id.selectedStartDateTimeTextView)
                    val downpaymentEditText = dialogView.findViewById<EditText>(R.id.downpaymentEditText)
                    val contractPriceEditText = dialogView.findViewById<EditText>(R.id.contractAmount)

                    downpaymentEditText.setText(currentDownpayment.toString())
                    contractPriceEditText.setText(currentContractPrice.toString())
                    selectedStartDateTimeTextView.text = "Start Date and Time: $currentStartDateTime"

                    startDateTimeButton.setOnClickListener {
                        showDateTimePicker { dateTime ->
                            selectedStartDateTimeTextView.text = "Start Date and Time: $dateTime"
                        }
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

                                    updateBookingDetails(
                                        newAddress,
                                        newDownpayment,
                                        newStartDateTimeDate,
                                        bookingId,
                                        newContractPrice
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
                    val currentStartDateTimeTimestamp = document.getTimestamp("startDateTime")
                    val currentStartDateTime = currentStartDateTimeTimestamp?.toDate()?.let {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
                    } ?: ""

                    val lat = document.getDouble("latitude") ?: 0.0
                    val lon = document.getDouble("longitude") ?: 0.0

                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_booking, null)
                    val addressEditText = dialogView.findViewById<EditText>(R.id.Address)

                    convertCoordinatesToAddress(context, lat, lon) { address ->
                        addressEditText.setText(address)
                    }

                    val startDateTimeButton = dialogView.findViewById<Button>(R.id.startDateTimeButton)
                    val selectedStartDateTimeTextView = dialogView.findViewById<TextView>(R.id.selectedStartDateTimeTextView)
                    val downpaymentEditText = dialogView.findViewById<EditText>(R.id.downpaymentEditText)
                    val contractPriceEditText = dialogView.findViewById<EditText>(R.id.contractAmount)

                    downpaymentEditText.visibility = View.GONE
                    contractPriceEditText.visibility = View.GONE

                    selectedStartDateTimeTextView.text = "Start Date and Time: $currentStartDateTime"

                    startDateTimeButton.setOnClickListener {
                        showDateTimePicker { dateTime ->
                            selectedStartDateTimeTextView.text = "Start Date and Time: $dateTime"
                        }
                    }

                    // 🔥 Check current user's type from Firestore
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                    firestore.collection("users").document(currentUserId).get()
                        .addOnSuccessListener { userDoc ->
                            val userType = userDoc.getString("user_type") ?: ""
                            val isProcessor = userType.equals("Processor", ignoreCase = true)

                            // Disable address field if the user is a Processor
                            addressEditText.isEnabled = !isProcessor
                            addressEditText.isFocusable = !isProcessor
                            addressEditText.isFocusableInTouchMode = !isProcessor

                            val dialog = AlertDialog.Builder(context)
                                .setTitle("Edit Booking Details")
                                .setView(dialogView)
                                .setPositiveButton("Save") { _, _ ->
                                    val newAddress = addressEditText.text.toString()
                                    val newStartDateTime = selectedStartDateTimeTextView.text.toString()
                                        .replace("Start Date and Time: ", "")
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    val newStartDateTimeDate = dateFormat.parse(newStartDateTime)

                                    updateBookingDetailsProcessor(
                                        newAddress,
                                        newStartDateTimeDate,
                                        bookingId,
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
    }

    override fun getItemCount(): Int = jobs.size

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
    }

    interface OnTabNavigationListener {
        fun navigateToTab(tabIndex: Int)
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
        contractPrice: Double
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Get current user ID
        val bookingRef = firestore.collection("bookings").document(bookingId)

        // Fetch the booking document to determine the user role
        bookingRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val bookedUserId = document.getString("bookedUserId") // The artist
                val bookingUserId = document.getString("landOwnerUserId") // The client

                if (bookedUserId == null || bookingUserId == null) {
                    Toast.makeText(context, "Error: Missing user data in booking document.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Determine status based on the current user
                val updatedStatus = when (currentUserId) {
                    bookedUserId -> "professional edit details"
                    bookingUserId -> "landowner edit details"
                    else -> {
                        Toast.makeText(context, "User is not authorized to update this booking.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                }

                // Geocode the address and get coordinates
                convertLocationToCoordinates(newAddress) { lat, lon ->
                    if (lat == 0.0 || lon == 0.0) {
                        Toast.makeText(context, "Failed to get location coordinates.", Toast.LENGTH_SHORT).show()
                        return@convertLocationToCoordinates
                    }

                    // Prepare the data to update
                    val updatedBookingData = mapOf(
                        "address" to newAddress,
                        "downpayment" to newDownpayment,
                        "startDateTime" to newStartDateTime,
                        "status" to updatedStatus,
                        "lastModifiedBy" to currentUserId,
                        "contractPrice" to contractPrice,
                        "latitude" to lat,
                        "longitude" to lon
                    )

                    // Update the booking in Firestore
                    bookingRef.update(updatedBookingData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Booking updated successfully.", Toast.LENGTH_SHORT).show()
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
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Get current user ID
        val bookingRef = firestore.collection("bookings").document(bookingId)

        // Fetch the booking document to determine the user role
        bookingRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val bookedUserId = document.getString("bookedUserId") // The artist
                val bookingUserId = document.getString("landOwnerUserId") // The client

                if (bookedUserId == null || bookingUserId == null) {
                    Toast.makeText(context, "Error: Missing user data in booking document.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Determine status based on the current user
                val updatedStatus = when (currentUserId) {
                    bookedUserId -> "processor edit details"
                    bookingUserId -> "landowner edit detail"
                    else -> {
                        Toast.makeText(context, "User is not authorized to update this booking.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                }

                // Geocode the address and get coordinates
                convertLocationToCoordinates(newAddress) { lat, lon ->
                    if (lat == 0.0 || lon == 0.0) {
                        Toast.makeText(context, "Failed to get location coordinates.", Toast.LENGTH_SHORT).show()
                        return@convertLocationToCoordinates
                    }

                    // Prepare the data to update
                    val updatedBookingData = mapOf(
                        "address" to newAddress,
                        "startDateTime" to newStartDateTime,
                        "status" to updatedStatus,
                        "lastModifiedBy" to currentUserId,
                        "latitude" to lat,
                        "longitude" to lon
                    )

                    // Update the booking in Firestore
                    bookingRef.update(updatedBookingData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Booking updated successfully.", Toast.LENGTH_SHORT).show()
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
        notifyDataSetChanged()
    }

    // Utility function to format Timestamp to String
    private fun formatTimestamp(timestamp: Timestamp?): String {
        return timestamp?.toDate()?.let {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(it)
        } ?: "N/A"
    }

    fun confirmBooking(bookingId: String, userId: String, position: Int) {
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

                    }

                    userId == surveyorId -> {
                        Log.d("ConfirmBooking", "Surveyor confirmed booking. Current Status: $bookingStatus")

                        when (bookingStatus) {
                            "new processor request" -> {
                                Log.d("ConfirmBooking", "Payment is submitted, moving to ongoing")
                                updatedBooking["status"] = "Waiting for processor document verification"
                                jobs[position].status = "Waiting for processor document verification"
                            }

                            else -> {
                                Log.d("ConfirmBooking", "Booking status not payment_submitted, setting artist_approved")

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

                // ✅ Remove item from list if it is now "ongoing"
                Handler(Looper.getMainLooper()).post {
                    if (jobs[position].stage == "ongoing") {
                        jobs.removeAt(position)
                        notifyItemRemoved(position)

                        // Now use the context to navigate
                        val fragmentTransaction = (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                        val ongoingFragment = OnGoingFragment() // Your target fragment

                        fragmentTransaction?.replace(R.id.fragment_container, ongoingFragment) // Replace with your container ID
                        fragmentTransaction?.addToBackStack(null) // Optional: if you want back navigation
                        fragmentTransaction?.commit()
                    } else {
                        notifyItemChanged(position)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ConfirmBooking", "Error updating booking: ", e)
            }
    }

    fun confirmBookingProcessor(bookingId: String, userId: String, position: Int) {
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

                    }

                    userId == surveyorId -> {
                        Log.d("ConfirmBooking", "Surveyor confirmed booking. Current Status: $bookingStatus")

                        when (bookingStatus) {
                            "new processor request" -> {
                                Log.d("ConfirmBooking", "Payment is submitted, moving to ongoing")
                                updatedBooking["status"] = "Waiting for processor document verification"
                                jobs[position].status = "Waiting for processor document verification"
                            }

                            else -> {
                                Log.d("ConfirmBooking", "Booking status not payment_submitted, setting artist_approved")
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

                // ✅ Remove item from list if it is now "ongoing"
                Handler(Looper.getMainLooper()).post {
                    if (jobs[position].stage == "ongoing") {
                        jobs.removeAt(position)
                        notifyItemRemoved(position)

                        // Now use the context to navigate
                        val fragmentTransaction = (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                        val ongoingFragment = OnGoingFragment() // Your target fragment

                        fragmentTransaction?.replace(R.id.fragment_container, ongoingFragment) // Replace with your container ID
                        fragmentTransaction?.addToBackStack(null) // Optional: if you want back navigation
                        fragmentTransaction?.commit()
                    } else {
                        notifyItemChanged(position)
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

}