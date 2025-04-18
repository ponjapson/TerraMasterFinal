package com.example.terramaster

import FragmentDisplayPDF
import android.net.Uri
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
    private lateinit var address: TextView
    private lateinit var pdfFile: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_payment, container, false)

        // Get booking ID from arguments
        bookingId = arguments?.getString("bookingId")

        val bookingIdTextView: TextView = view.findViewById(R.id.bookingIdTextView)
        address = view.findViewById(R.id.landAddress)
        pdfFile = view.findViewById(R.id.pdfFileName)

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
                    downPaymentAmount = documentSnapshot.getLong("downPayment")?.toInt()
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
        val jobsFragment = FragmentBookingManagement()
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
                    val lat = documentSnapshot.getDouble("latitude") ?: 0.0
                    val lon = documentSnapshot.getDouble("longitude") ?: 0.0
                    val pdfUrl = documentSnapshot.getString("pdfUrl")
                    val downpayment = documentSnapshot.getDouble("downPayment") ?: 0.0

                    val fileName = extractFileNameFromUrl(pdfUrl)
                    convertCoordinatesToAddress(lat, lon) { address ->
                        Log.d("Address", "Received address: $address")
                        activity?.runOnUiThread {
                            view?.findViewById<TextView>(R.id.landAddress)?.text = address
                        }
                    }


                    view?.findViewById<TextView>(R.id.pdfFileName)?.text = fileName
                    view?.findViewById<TextView>(R.id.booking_downpayment)?.text = "PHP $downpayment"
                    val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val formattedStartDate = startDateTime?.let { dateFormatter.format(it) } ?: "N/A"

                    view?.findViewById<TextView>(R.id.contractPrice)?.text = "PHP $contractPrice"
                    view?.findViewById<TextView>(R.id.booking_start_date)?.text = formattedStartDate

                    bookedUserId?.let {
                        fetchUserDetails(it)
                    } ?: run {
                        Log.e("PaymentFragment", "Booked User ID is null")
                    }

                    address.setOnClickListener {
                        val fragment = FragmentMap()

                        // Pass latitude & longitude to the map fragment
                        val args = Bundle().apply {
                            putDouble("latitude", lat)
                            putDouble("longitude", lon)
                        }
                        fragment.arguments = args

                        // Replace current fragment with MapFragment
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment) // Make sure R.id.fragment_container exists in your activity layout
                            .addToBackStack(null) // Enables back navigation
                            .commit()
                    }
                    pdfFile.setOnClickListener {
                        // Fetch PDF URL from the database (Firestore or your data source)
                        val pdfUrl = documentSnapshot.getString("pdfUrl") // Replace with your actual data fetching method

                        if (pdfUrl.isNullOrEmpty()) {
                            Log.e("PDF_ERROR", "Invalid PDF URL: $pdfUrl") // Log error if URL is empty
                            Toast.makeText(requireContext(), "PDF not available", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener // Stop execution if PDF URL is invalid
                        }

                        Log.d("PDF_DEBUG", "Opening PDF with URL: $pdfUrl") // Log valid PDF URL


                        val fragment = FragmentDisplayPDF().apply {
                            arguments = Bundle().apply {
                                putString("pdfUrl", pdfUrl)
                            }
                        }


                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment) // Ensure R.id.fragment_container is the correct container ID
                            .addToBackStack(null) // Allows user to go back to the previous fragment
                            .commit()

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
    private fun extractFileNameFromUrl(url: String?): String {
        return Uri.parse(url).lastPathSegment ?: "Unknown File"
    }
    private fun convertCoordinatesToAddress(lat: Double, lon: Double, callback: (String) -> Unit) {
        val geocoder = OpenStreetMapGeocoder(requireContext())
        geocoder.getAddressFromCoordinates(lat, lon) { address ->
            callback(address ?: "Unknown Address")
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
                    val profilePicUrl = userSnapshot.getString("profile_picture")

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