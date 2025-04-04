package com.example.terramaster

import FragmentDisplayPDF
import FragmentOnGoingPDF
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

            val fragment = FragmentOnGoingPDF().apply {
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

        }
    }

    override fun getItemCount(): Int = jobs.size

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
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


}