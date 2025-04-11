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
    private lateinit var progressBar: View


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
        progressBar = view.findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE // Show initially


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
            .whereEqualTo("stage", "request") // ✅ Only fetch "request" stage
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { bookedUserSnapshots, error ->
                if (error != null) {
                    Log.e("RequestTabFragment", "Error listening for artist requests: ${error.message}")
                    return@addSnapshotListener
                }

                pendingJobs.clear() // ✅ Clear list before adding new items
                jobCount = 0 // Reset job count

                bookedUserSnapshots?.let { snapshots ->
                    if (snapshots.isEmpty) {
                        fetchBookingUserJobs(userId, pendingJobs) // If no jobs, check for landowner jobs
                        return@let
                    }

                    snapshots.documents.forEach { doc ->
                        val job = createJobFromDocument(doc)

                        // ✅ Ensure job is still in "request" stage before adding
                        if (job.stage != "request") return@forEach

                        val pdfUrl = doc.getString("pdfUrl")
                        job.pdfFileName = pdfUrl?.let { extractFileNameFromUrl(it) }
                        job.pdfUrl = pdfUrl ?: ""

                        convertCoordinatesToAddress(job.latitude, job.longitude) { address ->
                            job.address = address
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

    private fun fetchBookingUserJobs(userId: String, pendingJobs: MutableList<Job>) {
        var jobCount = 0 // Track completed address conversions

        firestore.collection("bookings")
            .whereEqualTo("landOwnerUserId", userId)
            .whereEqualTo("stage", "request") // ✅ Only fetch "request" stage
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { bookingUserSnapshots, clientError ->
                if (clientError != null) {
                    Log.e("RequestTabFragment", "Error listening for client requests: ${clientError.message}")
                    return@addSnapshotListener
                }

                bookingUserSnapshots?.let { snapshots ->
                    if (snapshots.isEmpty) {
                        updateAdapter(pendingJobs) // ✅ No new jobs, update adapter immediately
                        return@let
                    }

                    snapshots.documents.forEach { doc ->
                        val job = createJobFromDocument(doc)

                        // ✅ Ensure job is still in "request" stage before adding
                        if (job.stage != "request") return@forEach

                        val pdfUrl = doc.getString("pdfUrl")
                        job.pdfFileName = pdfUrl?.let { extractFileNameFromUrl(it) }
                        job.pdfUrl = pdfUrl ?: ""

                        convertCoordinatesToAddress(job.latitude, job.longitude) { address ->
                            job.address = address
                            pendingJobs.add(job)
                            jobCount++

                            if (jobCount == snapshots.size()) {
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
            emailAddress = doc.getString("emailAddress") ?: ""
        )
    }

    private fun updateAdapter(jobs: MutableList<Job>) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
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