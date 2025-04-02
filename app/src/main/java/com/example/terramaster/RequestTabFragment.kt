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

class RequestTabFragment : Fragment(), OnPaymentClickListener {

    private lateinit var recyclerView: RecyclerView
    private var pendingAdapter: JobsAdapter? = null  // Use nullable type initially
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
        pendingAdapter = JobsAdapter(mutableListOf(), requireContext(), this, requireActivity())
        recyclerView.adapter = pendingAdapter

        val userId = auth.currentUser?.uid
        if (userId != null) {
            loadPendingJobs(userId)
        }

        return view
    }
    private fun loadPendingJobs(userId: String) {
        val pendingJobs = mutableListOf<Job>()
        var jobCount = 0 // Track completed address conversions

        firestore.collection("bookings")
            .whereEqualTo("bookedUserId", userId)
            .whereEqualTo("stage", "request")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { bookedUserSnapshots, error ->
                if (error != null) {
                    Log.e("RequestTabFragment", "Error listening for artist requests: ${error.message}")
                    return@addSnapshotListener
                }

                pendingJobs.clear()
                bookedUserSnapshots?.let { snapshots ->
                    if (snapshots.isEmpty) {
                        fetchBookingUserJobs(userId, pendingJobs) // No jobs, move to next fetch
                        return@let
                    }

                    snapshots.documents.forEach { doc ->
                        val job = createJobFromDocument(doc)

                        // Extract PDF file name if it exists
                        val pdfUrl = doc.getString("pdfUrl")
                        job.pdfFileName = pdfUrl?.let { extractFileNameFromUrl(it) } // Set PDF file name

                        convertCoordinatesToAddress(job.latitude, job.longitude) { address ->
                            job.address = address // Update job with address
                            Log.d("RequestTabFragment", "Updated job with address: ${job.address}")

                            pendingJobs.add(job)
                            jobCount++

                            Log.d("RequestTabFragment", "JobCount: $jobCount, Total Jobs: ${snapshots.size()}")

                            if (jobCount == snapshots.size()) {
                                Log.d("RequestTabFragment", "All geocoding complete, updating adapter")
                                fetchBookingUserJobs(userId, pendingJobs)
                            }
                        }
                    }
                }
            }
    }

    private fun fetchBookingUserJobs(userId: String, pendingJobs: MutableList<Job>) {
        var jobCount = 0 // Track completed address conversions

        firestore.collection("bookings")
            .whereEqualTo("landOwnerUserId", userId)
            .whereEqualTo("stage", "request")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { bookingUserSnapshots, clientError ->
                if (clientError != null) {
                    Log.e("RequestTabFragment", "Error listening for client requests: ${clientError.message}")
                    return@addSnapshotListener
                }

                bookingUserSnapshots?.let { snapshots ->
                    if (snapshots.isEmpty) {
                        updateAdapter(pendingJobs) // No new jobs, update adapter immediately
                        return@let
                    }

                    snapshots.documents.forEach { doc ->
                        val job = createJobFromDocument(doc)

                        // Extract PDF file name if it exists
                        val pdfUrl = doc.getString("pdfUrl")
                        job.pdfFileName = pdfUrl?.let { extractFileNameFromUrl(it) } // Set PDF file name


                        convertCoordinatesToAddress(job.latitude, job.longitude) { address ->
                            job.address = address // Update job with address
                            Log.d("RequestTabFragment", "Updated job with address: ${job.address}")

                            pendingJobs.add(job)
                            jobCount++

                            Log.d("RequestTabFragment", "JobCount: $jobCount, Total Jobs: ${snapshots.size()}")

                            if (jobCount == snapshots.size()) {
                                Log.d("RequestTabFragment", "All geocoding complete, updating adapter")
                                updateAdapter(pendingJobs)
                            }
                        }
                    }
                }
            }
    }


    private fun createJobFromDocument(doc: DocumentSnapshot): Job {
        return Job(
            bookingId = doc.id,
            bookedUserId = doc.getString("bookedUserId") ?: "",
            landOwnerUserId = doc.getString("landOwnerUserId") ?: "",
            contractPrice = doc.getDouble("contractPrice") ?: 0.0,
            downpayment = doc.getDouble("downpayment") ?: 0.0,
            startDateTime = doc.getTimestamp("startDateTime"),
            status = doc.getString("status") ?: "",
            timestamp = doc.getTimestamp("timestamp"),
            stage = doc.getString("stage") ?: "",
            latitude = doc.getDouble("latitude") ?: 0.0,
            longitude = doc.getDouble("longitude") ?: 0.0,
            address = "" // Address will be updated later
        )
    }

    private fun updateAdapter(jobs: MutableList<Job>) {
        pendingAdapter?.updateJobs(jobs)
        pendingAdapter?.notifyDataSetChanged()
    }

    private fun extractFileNameFromUrl(url: String?): String {
        return Uri.parse(url).lastPathSegment ?: "Unknown File"
    }


    private fun convertCoordinatesToAddress(lat: Double, lon: Double, callback: (String) -> Unit) {
        val geocoder = OpenStreetMapGeocoder(requireContext())
        geocoder.getAddressFromCoordinates(lat, lon) { address ->
            Log.d("RequestTabFragment", "Geocoding result: $address for coordinates: ($lat, $lon)")

            // Ensure UI update happens on the main thread
            activity?.runOnUiThread {
                callback(address ?: "Unknown Address")
            }
        }
    }





    /*private fun loadPendingJobs(userId: String) {
        val pendingJobs = mutableListOf<Job>() // Reset list each time

        firestore.collection("bookings")
            .whereEqualTo("bookedUserId", userId)
            .whereEqualTo("stage", "request")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { bookedUserSnapshots, error ->
                if (error != null) {
                    Log.e("RequestTabFragment", "Error listening for artist requests: ${error.message}")
                    return@addSnapshotListener
                }

                // Clear previous data to prevent duplication
                pendingJobs.clear()

                bookedUserSnapshots?.let {
                    // Add jobs only if they are not already in the list
                    it.documents.forEach { doc ->
                        val job = doc.toObject(Job::class.java)?.copy(bookingId = doc.id)
                        if (job != null && !pendingJobs.any { it.bookingId == job.bookingId }) {
                            pendingJobs.add(job)
                        }
                    }
                }

                // Fetch booking user jobs and update adapter
                fetchBookingUserJobs(userId, pendingJobs)
            }
    }

    private fun fetchBookingUserJobs(userId: String, pendingJobs: MutableList<Job>) {
        firestore.collection("bookings")
            .whereEqualTo("landOwnerUserId", userId)
            .whereEqualTo("stage", "request")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { bookingUserSnapshots, clientError ->
                if (clientError != null) {
                    Log.e("RequestTabFragment", "Error listening for client requests: ${clientError.message}")
                    return@addSnapshotListener
                }

                bookingUserSnapshots?.let {
                    // Add jobs only if they are not already in the list
                    it.documents.forEach { doc ->
                        val job = doc.toObject(Job::class.java)?.copy(bookingId = doc.id)
                        if (job != null && !pendingJobs.any { it.bookingId == job.bookingId }) {
                            pendingJobs.add(job)
                        }
                    }
                }

                Log.d("RequestTabFragment", "Total Pending Jobs: ${pendingJobs.size}")
                // Update the adapter with the new job list
                pendingAdapter?.updateJobs(pendingJobs)
                pendingAdapter?.notifyDataSetChanged() // Notify adapter to refresh the data
            }
    }*/



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