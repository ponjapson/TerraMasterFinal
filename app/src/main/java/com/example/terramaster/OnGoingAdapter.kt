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

class OnGoingAdapter(private val jobs: MutableList<OnGoingJobs>, private val context: Context,    private val listener: OnPaymentClickListener, private val fragmentActivity: FragmentActivity) :
    RecyclerView.Adapter<OnGoingAdapter.JobsViewHolder>() {

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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ongoing_booking_item, parent, false)
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
            //updateButtonsBasedOnStatus(job, position, holder, currentUserId)
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
                .replace(
                    R.id.fragment_container,
                    fragment
                ) // Make sure R.id.fragment_container exists in your activity layout
                .addToBackStack(null) // Enables back navigation
                .commit()
        }

        holder.pdfFile.text = job.pdfFileName
        holder.pdfFile.setOnClickListener {
            val pdfUrl = jobs[position].pdfUrl

            if (pdfUrl.isNullOrEmpty()) {
                Log.e("PDF_ERROR", "Invalid PDF URL: $pdfUrl") // Log error if URL is empty
                Toast.makeText(holder.itemView.context, "PDF not available", Toast.LENGTH_SHORT)
                    .show()
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

            val fragmentManager =
                (holder.itemView.context as AppCompatActivity).supportFragmentManager
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


    private fun convertLocationToCoordinates(
        locationName: String,
        callback: (Double, Double) -> Unit
    ) {
        val geocoder = OpenStreetMapGeocoder(context)
        geocoder.getCoordinatesFromAddress(locationName) { coordinates ->
            if (coordinates != null) {
                callback(coordinates.latitude, coordinates.longitude)
            } else {
                Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateJobs(newJobs: List<OnGoingJobs>) {
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


    private fun updateButtonsBasedOnStatus(
        job: Job,
        position: Int,
        holder: JobsViewHolder,
        currentUserId: String
    ) {
        /* val bookingUserId = job.landOwnerUserId
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
        }*/

    }
}

