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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
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

class BookingHistoryAdapter(private val jobs: MutableList<BookingHistory>, private val context: Context,    private val listener: OnPaymentClickListener, private val fragmentActivity: FragmentActivity) :
    RecyclerView.Adapter<BookingHistoryAdapter.JobsViewHolder>() {

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

        val step1: View = view.findViewById(R.id.step1)
        val step2: View = view.findViewById(R.id.step2)
        val step3: View = view.findViewById(R.id.step3)
        val step4: View = view.findViewById(R.id.step4)
        val step5: View = view.findViewById(R.id.step5)
        val cardViewProcessor: CardView = view.findViewById(R.id.cardViewProcessor)
        val cardViewSurveyor: CardView = view.findViewById(R.id.cardViewSurveyor)

        val step1Processor: View = view.findViewById(R.id.step1Processor)
        val step2Processor: View = view.findViewById(R.id.step2Processor)
        val step3Processor: View = view.findViewById(R.id.step3Processor)
        val step4Processor: View = view.findViewById(R.id.step4Processor)

        val btnFeedbackProcessor: Button = view.findViewById(R.id.btnFeedbackProcessor)
        val btnFeedbackSurveyor: Button = view.findViewById(R.id.btnFeedbackSurveyor)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.booking_history, parent, false)
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

        // Fetch the user document for the bookedUserId
        // FIRST: Hide feedback buttons by default
        holder.btnFeedbackProcessor.visibility = View.GONE
        holder.btnFeedbackSurveyor.visibility = View.GONE

// Fetch booked user's type (still needed for conditional view visibility)
        firestore.collection("users").document(bookedUserId)
            .get()
            .addOnSuccessListener { userSnapshot ->
                val bookedUserType = userSnapshot.getString("user_type")

                when (bookedUserType) {
                    "Processor" -> {
                        // Show processor card and related info
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
                        holder.cardViewProcessor.visibility = View.VISIBLE
                        holder.btnFeedbackProcessor.visibility = View.GONE
                    }

                    "Surveyor" -> {
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
                        holder.cardViewSurveyor.visibility = View.VISIBLE
                        holder.btnFeedbackSurveyor.visibility = View.GONE
                    }

                    else -> {
                        // Optional fallback
                    }
                }
            }
        if (currentUserId != null) {
            firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener { userSnapshot ->
                    userType = userSnapshot.getString("user_type")

                    // ✅ Log the user type for debugging
                    Log.d("BookingAdapter", "Current user type: $userType")

                    // Check the userType and adjust the visibility accordingly
                    when (userType) {
                        "Processor" -> {
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
                            holder.cardViewProcessor.visibility = View.VISIBLE
                            holder.cardViewSurveyor.visibility = View.GONE
                            holder.btnFeedbackSurveyor.visibility = View.GONE
                            holder.btnFeedbackProcessor.visibility = View.GONE
                        }

                        "Surveyor" -> {
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
                            holder.cardViewSurveyor.visibility = View.VISIBLE
                            holder.cardViewProcessor.visibility = View.GONE
                            holder.btnFeedbackSurveyor.visibility = View.GONE
                            holder.btnFeedbackProcessor.visibility = View.GONE
                        }

                        "Landowner" -> {
                            holder.btnFeedbackSurveyor.visibility = View.VISIBLE
                            holder.btnFeedbackProcessor.visibility = View.VISIBLE
                            holder.cardViewProcessor.visibility = View.GONE
                            holder.cardViewSurveyor.visibility = View.GONE
                        }

                        else -> {
                            holder.contractPrice.visibility = View.VISIBLE
                            holder.downpayment.visibility = View.VISIBLE
                            holder.labelDown.visibility = View.VISIBLE
                            holder.labelPrice.visibility = View.VISIBLE
                            holder.btnFeedbackSurveyor.visibility = View.GONE
                            holder.btnFeedbackProcessor.visibility = View.GONE
                            holder.cardViewProcessor.visibility = View.GONE
                            holder.cardViewSurveyor.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BookingAdapter", "Error fetching user data: ${e.message}")
                }
        } else {
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

        updateStepColor(job.documentStatus, holder, position)
        updateStepColorProcessor(job.documentStatus, holder, position)

        holder.btnFeedbackProcessor.setOnClickListener {
            val context = it.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_feedback, null)

            val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
            val feedbackEditText = dialogView.findViewById<EditText>(R.id.feedbackEditText)

            AlertDialog.Builder(context)
                .setTitle("Leave Feedback")
                .setView(dialogView)
                .setPositiveButton("Submit") { dialog, _ ->
                    val rating = ratingBar.rating
                    val feedbackText = feedbackEditText.text.toString()

                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    val bookingId = job.bookingId
                    val bookedUserId = job.bookedUserId
                    val landOwnerUserId = job.landOwnerUserId

                    if (userId != null && bookingId.isNotEmpty()) {
                        val feedbackData = hashMapOf(
                            "userId" to userId,
                            "bookedUserId" to bookedUserId,
                            "landOwnerUserId" to landOwnerUserId,
                            "bookingId" to bookingId,
                            "rating" to rating,
                            "feedback" to feedbackText,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )

                        val db = FirebaseFirestore.getInstance()

                        db.collection("Feedback")
                            .add(feedbackData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Feedback submitted!", Toast.LENGTH_SHORT).show()
                                holder.btnFeedbackProcessor.visibility = View.GONE
                                // ✅ Calculate average rating
                                db.collection("Feedback")
                                    .whereEqualTo("bookedUserId", bookedUserId)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        var totalRating = 0f
                                        val count = snapshot.size()

                                        for (doc in snapshot) {
                                            val r = doc.getDouble("rating")?.toFloat() ?: 0f
                                            totalRating += r
                                        }

                                        if (count > 0) {
                                            val averageRating = totalRating / count

                                            // ✅ Update user's ratings
                                            db.collection("users")
                                                .document(bookedUserId)
                                                .update("ratings", averageRating)
                                                .addOnSuccessListener {
                                                    Log.d("RatingUpdate", "User rating updated to $averageRating")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("RatingUpdate", "Rating update failed: ${e.message}")
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("RatingQuery", "Failed to fetch feedbacks: ${e.message}")
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }


        val bookingId = job.bookingId

        if (currentUserId != null && bookingId.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("Feedback")
                .whereEqualTo("landOwnerUserId", currentUserId)
                .whereEqualTo("bookingId", bookingId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        // ✅ Feedback already exists for this booking — hide button
                        holder.btnFeedbackSurveyor.visibility = View.GONE
                    } else {
                        // ✅ No feedback yet — show button
                        holder.btnFeedbackSurveyor.visibility = View.VISIBLE
                    }
                }
                .addOnFailureListener {
                    // Optional: log error
                    holder.btnFeedbackSurveyor.visibility = View.VISIBLE // Default to visible on error
                }
        }


        if (currentUserId != null && bookingId.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("Feedback")
                .whereEqualTo("landOwnerUserId", currentUserId)
                .whereEqualTo("bookingId", bookingId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        // ✅ Feedback already exists for this booking — hide button
                        holder.btnFeedbackProcessor.visibility = View.GONE
                    } else {
                        // ✅ No feedback yet — show button
                        holder.btnFeedbackProcessor.visibility = View.VISIBLE
                    }
                }
                .addOnFailureListener {
                    // Optional: log error
                    holder.btnFeedbackProcessor.visibility = View.VISIBLE // Default to visible on error
                }
        }



        holder.btnFeedbackSurveyor.setOnClickListener {
            val context = it.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_feedback, null)

            val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
            val feedbackEditText = dialogView.findViewById<EditText>(R.id.feedbackEditText)

            AlertDialog.Builder(context)
                .setTitle("Leave Feedback")
                .setView(dialogView)
                .setPositiveButton("Submit") { dialog, _ ->
                    val rating = ratingBar.rating
                    val feedbackText = feedbackEditText.text.toString()

                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    val bookingId = job.bookingId
                    val bookedUserId = job.bookedUserId
                    val landOwnerUserId = job.landOwnerUserId

                    if (userId != null && bookingId.isNotEmpty()) {
                        val feedbackData = hashMapOf(
                            "userId" to userId,
                            "bookedUserId" to bookedUserId,
                            "landOwnerUserId" to landOwnerUserId,
                            "bookingId" to bookingId,
                            "rating" to rating,
                            "feedback" to feedbackText,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )

                        val db = FirebaseFirestore.getInstance()

                        db.collection("Feedback")
                            .add(feedbackData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Feedback submitted!", Toast.LENGTH_SHORT).show()
                                holder.btnFeedbackSurveyor.visibility = View.GONE

                                // ✅ Calculate average rating
                                db.collection("Feedback")
                                    .whereEqualTo("bookedUserId", bookedUserId)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        var totalRating = 0f
                                        val count = snapshot.size()

                                        for (doc in snapshot) {
                                            val r = doc.getDouble("rating")?.toFloat() ?: 0f
                                            totalRating += r
                                        }

                                        if (count > 0) {
                                            val averageRating = totalRating / count

                                            // ✅ Update user's ratings
                                            db.collection("users")
                                                .document(bookedUserId)
                                                .update("ratings", averageRating)
                                                .addOnSuccessListener {
                                                    Log.d("RatingUpdate", "User rating updated to $averageRating")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("RatingUpdate", "Rating update failed: ${e.message}")
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("RatingQuery", "Failed to fetch feedbacks: ${e.message}")
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }




    override fun getItemCount(): Int = jobs.size

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
    }

    interface OnTabNavigationListener {
        fun navigateToTab(tabIndex: Int)
    }
    private fun updateStepColor(status: String, holder: BookingHistoryAdapter.JobsViewHolder, position: Int) {
        val defaultColor = ContextCompat.getColor(context, R.color.DarkYellow)
        val activeColor = ContextCompat.getColor(context, R.color.YellowGreen)

        // Reset all to default color first
        holder.step1.setBackgroundColor(defaultColor)
        holder.step2.setBackgroundColor(defaultColor)
        holder.step3.setBackgroundColor(defaultColor)
        holder.step4.setBackgroundColor(defaultColor)
        holder.step5.setBackgroundColor(defaultColor)

        // Set progress based on current status
        when (status) {
            "Prepare Blueprint" -> {
                holder.step1.setBackgroundColor(activeColor)
            }

            "Submit Blueprint" -> {
                holder.step1.setBackgroundColor(activeColor)
                holder.step2.setBackgroundColor(activeColor)
            }

            "Follow-up Approval" -> {
                holder.step1.setBackgroundColor(activeColor)
                holder.step2.setBackgroundColor(activeColor)
                holder.step3.setBackgroundColor(activeColor)
            }

            "Ready to Claim" -> {
                holder.step1.setBackgroundColor(activeColor)
                holder.step2.setBackgroundColor(activeColor)
                holder.step3.setBackgroundColor(activeColor)
                holder.step4.setBackgroundColor(activeColor)
            }

            "Completed" -> {
                // Update all steps to active color
                holder.step1.setBackgroundColor(activeColor)
                holder.step2.setBackgroundColor(activeColor)
                holder.step3.setBackgroundColor(activeColor)
                holder.step4.setBackgroundColor(activeColor)
                holder.step5.setBackgroundColor(activeColor)
            }
        }
    }
    private fun updateStepColorProcessor(status: String, holder: BookingHistoryAdapter.JobsViewHolder, position: Int) {
        val defaultColor = ContextCompat.getColor(context, R.color.DarkYellow)
        val activeColor = ContextCompat.getColor(context, R.color.YellowGreen)

        // Reset all to default color first
        holder.step1Processor.setBackgroundColor(defaultColor)
        holder.step2Processor.setBackgroundColor(defaultColor)
        holder.step3Processor.setBackgroundColor(defaultColor)
        holder.step4Processor.setBackgroundColor(defaultColor)

        // Set progress based on current status
        when (status) {
            "Prepare the Tax Declaration" -> {
                holder.step1Processor.setBackgroundColor(activeColor)
            }

            "Approval Department Head" -> {
                holder.step1Processor.setBackgroundColor(activeColor)
                holder.step2Processor.setBackgroundColor(activeColor)
            }

            "Ready to Claim" -> {
                holder.step1Processor.setBackgroundColor(activeColor)
                holder.step2Processor.setBackgroundColor(activeColor)
                holder.step3Processor.setBackgroundColor(activeColor)
            }

            "Completed" -> {
                // Update all steps to active color
                holder.step1Processor.setBackgroundColor(activeColor)
                holder.step2Processor.setBackgroundColor(activeColor)
                holder.step3Processor.setBackgroundColor(activeColor)
                holder.step4Processor.setBackgroundColor(activeColor)


            }
        }
    }


    fun updateJobs(newJobs: List<BookingHistory>) {
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

