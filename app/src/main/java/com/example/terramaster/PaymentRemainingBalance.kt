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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

class PaymentRemainingBalance : Fragment() {

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
                fetchRemainingBalance(bookingId!!) // Fetch down payment amount based on bookingId
            } else {
                Toast.makeText(context, "Invalid booking ID", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun fetchRemainingBalance(bookingId: String) {
        Log.d("PaymentFragment", "Fetching remaining balance for booking ID: $bookingId")

        // Fetch down payment and contract price from Firestore based on bookingId
        FirebaseFirestore.getInstance()
            .collection("bookings")
            .document(bookingId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val contractPrice = documentSnapshot.getLong("contractPrice")?.toInt() // Fetch contract price
                    val downPayment = documentSnapshot.getLong("downpayment")?.toInt() // Fetch down payment

                    if (contractPrice != null && downPayment != null) {
                        val remainingBalance = contractPrice - downPayment // Calculate remaining balance
                        Log.d("PaymentFragment", "Remaining balance: $remainingBalance")
                        fetchPaymentIntent(remainingBalance) // Pass remaining balance to fetch payment intent
                    } else {
                        Toast.makeText(context, "Contract price or down payment not found", Toast.LENGTH_SHORT).show()
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
        PaymentConfiguration.init(requireContext(), "pk_test_51PF6IMIZ3mDt4x0n2lDCSQxYQKkuHVBMvm9ehS78BVPxCCL084G6bm4JOz6qZlWHXtfkXjw2sU2dggRiFuuwakey00ajmt9jA3")
        paymentSheet.presentWithPaymentIntent(clientSecret, PaymentSheet.Configuration("CreativeClique"))
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                Toast.makeText(context, "Payment Complete", Toast.LENGTH_SHORT).show()

                // Call the function to send email notifications, passing bookingId
                bookingId?.let {
                    // Send email notification
                    updateBookingStatus(it) // Update booking status in Firestore
                    navigateToPendingTabFragment()
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
        val jobsFragment = FragmentBookingManagement()
        val bundle = Bundle().apply {
            putInt("selectedTab", 2)
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
            "status" to "completed",
            "stage" to "completed"
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
        fun newInstance(bookingId: String): PaymentRemainingBalance {
            return PaymentRemainingBalance().apply {
                arguments = Bundle().apply {
                    putString("bookingId", bookingId) // Only pass the booking ID
                }
            }
        }
    }
}