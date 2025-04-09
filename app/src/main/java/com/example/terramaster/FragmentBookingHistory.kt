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

class FragmentBookingHistory : Fragment(), OnPaymentClickListener {

    private lateinit var recyclerView: RecyclerView
    private var pendingAdapter: BookingHistoryAdapter? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_request_tab, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        pendingAdapter = BookingHistoryAdapter(mutableListOf(), requireContext(), this, requireActivity())
        recyclerView.adapter = pendingAdapter

        auth.currentUser?.uid?.let { userId ->
            loadCompletedJobs(userId)
        }

        return view
    }

    private fun loadCompletedJobs(userId: String) {
        val allJobs = mutableListOf<BookingHistory>()

        // First fetch: as booked user
        firestore.collection("bookings")
            .whereEqualTo("bookedUserId", userId)
            .whereEqualTo("stage", "Completed")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { bookedUserSnapshots ->
                processJobDocuments(bookedUserSnapshots.documents, allJobs) {
                    // Second fetch: as landowner
                    firestore.collection("bookings")
                        .whereEqualTo("landOwnerUserId", userId)
                        .whereEqualTo("stage", "Completed")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { landOwnerSnapshots ->
                            processJobDocuments(landOwnerSnapshots.documents, allJobs) {
                                updateAdapter(allJobs)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("BookingHistory", "Error fetching bookings: ${e.message}")
            }
    }

    private fun processJobDocuments(
        documents: List<DocumentSnapshot>,
        jobsList: MutableList<BookingHistory>,
        onComplete: () -> Unit
    ) {
        if (documents.isEmpty()) {
            onComplete()
            return
        }

        var completedCount = 0

        for (doc in documents) {
            val job = createJobFromDocument(doc)

            val pdfUrl = doc.getString("pdfUrl")
            job.pdfFileName = pdfUrl?.let { extractFileNameFromUrl(it) }
            job.pdfUrl = pdfUrl ?: ""

            convertCoordinatesToAddress(job.latitude, job.longitude) { address ->
                job.address = address
                jobsList.add(job)
                completedCount++
                if (completedCount == documents.size) {
                    onComplete()
                }
            }
        }
    }

    private fun updateAdapter(jobs: MutableList<BookingHistory>) {
        pendingAdapter?.updateJobs(jobs)
        pendingAdapter?.notifyDataSetChanged()
    }

    private fun createJobFromDocument(doc: DocumentSnapshot): BookingHistory {
        return BookingHistory(
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
            address = "",
            tinNumber = doc.getString("tinNumber") ?: "",
            age = doc.getLong("age")?.toInt()?.toString() ?: "",
            propertyType = doc.getString("propertyType") ?: "",
            purposeOfSurvey = doc.getString("purposeOfSurvey") ?: "",
            contactNumber = doc.getString("contactNumber") ?: "",
            emailAddress = doc.getString("emailAddress") ?: "",
            documentStatus = doc.getString("documentStatus") ?: "",
        )
    }

    private fun extractFileNameFromUrl(url: String?): String {
        return Uri.parse(url).lastPathSegment ?: "Unknown File"
    }

    private fun convertCoordinatesToAddress(lat: Double, lon: Double, callback: (String) -> Unit) {
        val context = context ?: return

        val geocoder = OpenStreetMapGeocoder(context)
        geocoder.getAddressFromCoordinates(lat, lon) { address ->
            activity?.runOnUiThread {
                if (isAdded) {
                    callback(address ?: "Unknown Address")
                } else {
                    Log.e("Geocoder", "Fragment not attached, skipping address callback")
                }
            }
        }
    }

    override fun onPayNowClicked(bookingId: String) {
        val paymentFragment = PaymentFragment.newInstance(bookingId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, paymentFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onPayRemaining(bookingId: String) {
        val paymentFragment = PaymentRemainingBalance.newInstance(bookingId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, paymentFragment)
            .addToBackStack(null)
            .commit()
    }
}
