package com.example.terramaster

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class OnGoingFragment : Fragment(), OnPaymentClickListener {

    private lateinit var recyclerView: RecyclerView
    private var pendingAdapter: OnGoingAdapter? = null  // Use nullable type initially
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_request_tab, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize adapter only once the view is created
        pendingAdapter = OnGoingAdapter(mutableListOf(), requireContext(), this, requireActivity())
        recyclerView.adapter = pendingAdapter

        val userId = auth.currentUser?.uid
        if (userId != null) {
            loadPendingJobs(userId)
        }

        return view
    }
    private fun loadPendingJobs(userId: String) {
        val pendingJobs = mutableListOf<OnGoingJobs>()
        var jobCount = 0 // Track completed address conversions

        Log.d("RequestTabFragment", "Loading jobs for userId (bookedUser): $userId")

        // First: Fetch jobs where user is the bookedUser (e.g., an artist)
        firestore.collection("bookings")
            .whereEqualTo("bookedUserId", userId)
            .whereEqualTo("stage", "ongoing")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { bookedUserSnapshots, error ->
                if (error != null) {
                    Log.e("RequestTabFragment", "Error listening for artist requests: ${error.message}")
                    return@addSnapshotListener
                }

                Log.d("RequestTabFragment", "Snapshot listener triggered for bookedUserId")
                bookedUserSnapshots?.let { snapshots ->
                    Log.d("RequestTabFragment", "BookedUser snapshot size: ${snapshots.size()}")
                    if (snapshots.isEmpty) {
                        // If no jobs found for bookedUser, check for landOwner
                        fetchBookingUserJobs(userId, pendingJobs)
                        return@let
                    }

                    snapshots.documents.forEach { doc ->
                        val job = createJobFromDocument(doc)

                        // Skip if not in the correct stage (safety check)
                        if (job.stage != "ongoing") return@forEach

                        val pdfUrl = doc.getString("pdfUrl")
                        job.pdfFileName = pdfUrl?.let { extractFileNameFromUrl(it) }
                        job.pdfUrl = pdfUrl ?: ""

                        convertCoordinatesToAddress(job.latitude, job.longitude) { address ->
                            job.address = address
                            Log.d("RequestTabFragment", "Geocoding result: $address for coordinates: (${job.latitude}, ${job.longitude})")
                            pendingJobs.add(job)
                            jobCount++

                            if (jobCount == snapshots.size()) {
                                fetchBookingUserJobs(userId, pendingJobs)
                            }
                        }
                    }
                }
            }
    }


    private fun fetchBookingUserJobs(userId: String, pendingJobs: MutableList<OnGoingJobs>) {
        var jobCount = 0 // Track completed address conversions

        // Fetch bookings where user is the landOwnerUserId
        firestore.collection("bookings")
            .whereEqualTo("landOwnerUserId", userId)
            .whereEqualTo("stage", "ongoing")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { bookingUserSnapshots, clientError ->
                if (clientError != null) {
                    Log.e(
                        "RequestTabFragment",
                        "Error listening for client requests: ${clientError.message}"
                    )
                    return@addSnapshotListener
                }

                bookingUserSnapshots?.let { snapshots ->
                    if (snapshots.isEmpty) {
                        activity?.runOnUiThread {
                            updateAdapter(pendingJobs)
                        }
                        return@let
                    }


                    pendingJobs.clear()
                    jobCount = 0

                    Log.d("Firestore", "Snapshot listener triggered")

                    snapshots.documents.forEach { doc ->
                        val job = createJobFromDocument(doc)
                        if (job.stage != "ongoing") return@forEach

                        val pdfUrl = doc.getString("pdfUrl")
                        job.pdfFileName = pdfUrl?.let { extractFileNameFromUrl(it) }
                        job.pdfUrl = pdfUrl ?: ""

                        convertCoordinatesToAddress(job.latitude, job.longitude) { address ->
                            job.address = address
                            pendingJobs.add(job)
                            jobCount++

                            if (jobCount == snapshots.size()) {
                                activity?.runOnUiThread {
                                    updateAdapter(pendingJobs)
                                }
                            }
                        }
                    }
                }

            }
    }

    private fun createJobFromDocument(doc: DocumentSnapshot): OnGoingJobs {
        return OnGoingJobs(
            bookingId = doc.id,
            bookedUserId = doc.getString("bookedUserId") ?: "",
            landOwnerUserId = doc.getString("landOwnerUserId") ?: "",
            contractPrice = doc.getDouble("contractPrice") ?: 0.0,
            downpayment = doc.getDouble("downPayment") ?: 0.0,
            startDateTime = doc.getTimestamp("startDateTime"),
            status = doc.getString("status") ?: "",
            timestamp = doc.getTimestamp("timestamp"),
            stage = doc.getString("stage") ?: "",
            latitude = doc.getDouble("latitude") ?: 0.0,
            longitude = doc.getDouble("longitude") ?: 0.0,
            address = "", // Address will be updated later
            tinNumber = doc.getString("tinNumber") ?: "",
            age = doc.getLong("age")?.toInt()?.toString() ?: "",
            propertyType = doc.getString("propertyType") ?: "",
            purposeOfSurvey = doc.getString("purposeOfSurvey") ?: "",
            contactNumber = doc.getString("contactNumber") ?: "",
            emailAddress = doc.getString("emailAddress") ?: "",
            documentStatus = doc.getString("documentStatus") ?: "",
        )
    }

    private fun updateAdapter(jobs: MutableList<OnGoingJobs>) {
        // Update your adapter with the new data
        pendingAdapter?.updateJobs(jobs)
        pendingAdapter?.notifyDataSetChanged()
    }


    private fun extractFileNameFromUrl(url: String?): String {
        return Uri.parse(url).lastPathSegment ?: "Unknown File"
    }


    private fun convertCoordinatesToAddress(lat: Double, lon: Double, callback: (String) -> Unit) {
        val context = context ?: return // Avoid crash if fragment is not attached

        val geocoder = OpenStreetMapGeocoder(context)
        geocoder.getAddressFromCoordinates(lat, lon) { address ->
            Log.d("RequestTabFragment", "Geocoding result: $address for coordinates: ($lat, $lon)")

            // Ensure UI update happens on the main thread and only if fragment is still attached
            activity?.runOnUiThread {
                if (isAdded) { // Prevent crash if fragment is detached
                    callback(address ?: "Unknown Address")
                } else {
                    Log.e("convertCoordinatesToAddress", "Fragment not attached, skipping callback")
                }
            }
        }
    }

    override fun onPayNowClicked(bookingId: String) {
        // Open the PaymentFragment and pass the bookingId as an argument
        val paymentFragment = PaymentFragment.newInstance(bookingId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, paymentFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onPayRemaining(bookingId: String) {
        // Open the PaymentFragment and pass the bookingId as an argument
        val paymentFragment = PaymentRemainingBalance.newInstance(bookingId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, paymentFragment)
            .addToBackStack(null)
            .commit()
    }
}