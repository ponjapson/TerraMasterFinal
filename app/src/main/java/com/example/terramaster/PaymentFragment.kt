package com.example.terramaster

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Locale

class PaymentFragment : Fragment() {

    private lateinit var paymentSheet: PaymentSheet
    private var clientSecret: String? = null
    private var bookingId: String? = null
    private var downPaymentAmount: Int? = null // This will be fetched based on bookingId

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_payment, container, false)

        // Get booking ID from arguments
        bookingId = arguments?.getString("bookingId")

        val bookingIdTextView: TextView = view.findViewById(R.id.bookingIdTextView)

        bookingId?.let { nonNullBookingId ->
            fetchBookingDetails(nonNullBookingId) // Now nonNullBookingId is guaranteed to be non-null
        } ?: run {
            Log.e("PaymentFragment", "Booking ID is null")
        }


        bookingId?.let {
            bookingIdTextView.text = "Booking ID: $it" // Display the bookingId
        } ?: run {
            bookingIdTextView.text = "Booking ID not available"
        }


        val payNowButton: Button = view.findViewById(R.id.payButton)
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        payNowButton.setOnClickListener {
            Log.d("PaymentFragment", "Pay Now button clicked")

            if (bookingId != null) {
                fetchDownPaymentAmount(bookingId!!) // Fetch down payment amount based on bookingId
            } else {
                Toast.makeText(context, "Invalid booking ID", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun fetchDownPaymentAmount(bookingId: String) {
        Log.d("PaymentFragment", "Fetching down payment for booking ID: $bookingId")

        // Fetch down payment amount from Firestore based on bookingId
        FirebaseFirestore.getInstance()
            .collection("bookings")
            .document(bookingId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    downPaymentAmount = documentSnapshot.getLong("downpayment")?.toInt()
                    if (downPaymentAmount != null) {
                        fetchPaymentIntent(downPaymentAmount!!) // Pass down payment amount to fetch payment intent
                    } else {
                        Toast.makeText(context, "Down payment amount not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Booking not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PaymentFragment", "Error fetching booking data", e)
                Toast.makeText(context, "Error fetching booking data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchPaymentIntent(downPaymentAmount: Int) {
        Log.d("PaymentFragment", "Fetching Payment Intent with amount: $downPaymentAmount")

        // Send down payment amount to Firebase Function
        FirebaseFunctions.getInstance()
            .getHttpsCallable("createPaymentIntent")
            .call(mapOf("downPaymentAmount" to downPaymentAmount, "bookingId" to bookingId))
            .addOnSuccessListener { result ->
                val data = result.data as Map<*, *>
                clientSecret = data["clientSecret"] as String?

                if (clientSecret != null) {
                    presentPaymentSheet(clientSecret!!) // Present payment sheet if client secret is received
                } else {
                    Toast.makeText(context, "Failed to fetch Payment Intent", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PaymentFragment", "Failed to fetch Payment Intent", e)
                Toast.makeText(context, "Failed to fetch Payment Intent", Toast.LENGTH_SHORT).show()
            }
    }

    private fun presentPaymentSheet(clientSecret: String) {
        PaymentConfiguration.init(requireContext(), "pk_test_51PF6IQP3oTGDNBHAPGISejKVeBmhtjTLL7kkC7uDJnk0q5aVtalV9Eeerl8Id1cpuykEULe7CyTYGGxG6s0iUQ0J00xxb1BpiS")
        paymentSheet.presentWithPaymentIntent(clientSecret, PaymentSheet.Configuration("TerraMaster"))
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                Toast.makeText(context, "Payment Complete", Toast.LENGTH_SHORT).show()

                // Call the function to send email notifications, passing bookingId
                bookingId?.let {

                    updateBookingStatus(it) // Update booking status in Firestore
                } ?: run {
                    Log.e("PaymentFragment", "Booking ID is null")
                }

            }
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(context, "Payment Canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(context, "Payment Failed", Toast.LENGTH_SHORT).show()
                Log.e("PaymentFragment", "Payment failed: ${paymentSheetResult.error}")
            }
        }
    }

    private fun navigateToPendingTabFragment() {
        val jobsFragment = FragmentJobs()
        val bundle = Bundle().apply {
            putInt("selectedTab", 1)
        }
        jobsFragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, jobsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateBookingStatus(bookingId: String) {
        val bookingRef = FirebaseFirestore.getInstance().collection("bookings").document(bookingId)

        // Data to update
        val updateData = mapOf(
            "status" to "payment_submitted"
        )

        bookingRef.update(updateData)
            .addOnSuccessListener {
                Log.d("PaymentFragment", "Booking status updated successfully")
                Toast.makeText(context, "Booking status updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("PaymentFragment", "Failed to update booking status", e)
                Toast.makeText(context, "Failed to update booking status", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        fun newInstance(bookingId: String): PaymentFragment {
            return PaymentFragment().apply {
                arguments = Bundle().apply {
                    putString("bookingId", bookingId) // Only pass the booking ID
                }
            }
        }
    }

    private fun fetchBookingDetails(bookingId: String) {
        Log.d("PaymentFragment", "Fetching booking details for ID: $bookingId")

        FirebaseFirestore.getInstance()
            .collection("bookings")
            .document(bookingId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d("PaymentFragment", "Booking document: ${documentSnapshot.data}")

                    val bookedUserId = documentSnapshot.getString("bookedUserId")
                    val contractPrice = documentSnapshot.getLong("contractPrice")?.toInt()
                    val startDateTime = documentSnapshot.getTimestamp("startDateTime")?.toDate()
                    val endDateTime = documentSnapshot.getTimestamp("endDateTime")?.toDate()
                    val description = documentSnapshot.getString("details")

                    val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val formattedStartDate = startDateTime?.let { dateFormatter.format(it) } ?: "N/A"
                    val formattedEndDate = endDateTime?.let { dateFormatter.format(it) } ?: "N/A"

                    view?.findViewById<TextView>(R.id.contractPrice)?.text = "PHP $contractPrice"
                    view?.findViewById<TextView>(R.id.booking_start_date)?.text = formattedStartDate
                    view?.findViewById<TextView>(R.id.booking_end_date)?.text = formattedEndDate
                    view?.findViewById<TextView>(R.id.booking_description)?.text = description

                    bookedUserId?.let {
                        fetchUserDetails(it)
                    } ?: run {
                        Log.e("PaymentFragment", "Booked User ID is null")
                    }
                } else {
                    Log.e("PaymentFragment", "No booking document found for ID: $bookingId")
                    Toast.makeText(context, "Booking details not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PaymentFragment", "Error fetching booking details", e)
                Toast.makeText(context, "Error fetching booking details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserDetails(bookedUserId: String) {
        Log.d("PaymentFragment", "Fetching user details for ID: $bookedUserId")

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(bookedUserId)
            .get()
            .addOnSuccessListener { userSnapshot ->
                if (userSnapshot.exists()) {
                    Log.d("PaymentFragment", "User document: ${userSnapshot.data}")

                    val firstName = userSnapshot.getString("first_name") ?: "N/A"
                    val lastName = userSnapshot.getString("last_name") ?: "N/A"
                    val profilePicUrl = userSnapshot.getString("profile_pic")

                    view?.findViewById<TextView>(R.id.user_name)?.text = "$firstName $lastName"

                    val profileImageView = view?.findViewById<CircleImageView>(R.id.profile_image)
                    profilePicUrl?.let { url ->
                        Glide.with(requireContext())
                            .load(url)
                            .placeholder(R.drawable.profile_pic)
                            .error(R.drawable.profile_pic)
                            .into(profileImageView!!)
                    }
                } else {
                    Log.e("PaymentFragment", "No user document found for ID: $bookedUserId")
                    Toast.makeText(context, "User details not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PaymentFragment", "Error fetching user details", e)
                Toast.makeText(context, "Error fetching user details", Toast.LENGTH_SHORT).show()
            }
    }
}