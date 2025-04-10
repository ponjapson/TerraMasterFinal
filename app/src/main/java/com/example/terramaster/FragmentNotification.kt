package com.example.terramaster

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FragmentNotification : Fragment(), NotificationAdapter.OnNotificationClickListener {

    private lateinit var notificationAdapter: NotificationAdapter
    private val notifications: MutableList<Notification> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val notificationsRecyclerView: RecyclerView = view.findViewById(R.id.notificationsRecyclerView)

        setupRecyclerView(notificationsRecyclerView)
        loadNotifications()
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        notificationAdapter = NotificationAdapter(requireContext(), notifications, this)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
    }
    override fun onNotificationClick(notification: Notification) {
        when (notification.type) {
            //if pisliton ni nya ang type sa notification is booking_request mo open para i confirm
            "booking_request" -> {
                navigateToRequestTabFragment()
            }
            "message" -> {
                navigateToPrivateMessage(notification.senderId!!)
            }



            else -> {
                Log.d("FragmentNotification", "Unhandled notification type: ${notification.type}")
            }
        }
    }

    private fun navigateToPrivateMessage(otherUserId: String){
        var currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if(currentUserId != null){
            val privateMessage = FragmentPrivateMessage()

            val chatRoomId = generateChatId(currentUserId, otherUserId)

            val bundle = Bundle()

            bundle.putString("otherUserId", otherUserId)
            bundle.putString("chatId", chatRoomId)

            privateMessage.arguments = bundle

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, privateMessage)
                .addToBackStack(null)
                .commit()

            (requireActivity() as MainActivity).hideBottomNavigationBar()
        } else {
            Toast.makeText(requireContext(), "User ID is missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateChatId(currentUserId: String, otherUserId: String): String{
        return if (currentUserId < otherUserId){
            "$currentUserId-$otherUserId"
        } else{
            "$otherUserId-$currentUserId"
        }
    }
    private fun navigateToJobsRevisionFragment() {
        val jobsFragment = FragmentJobs()
        val bundle = Bundle().apply {
            putInt("selectedTab", 3)
        }
        jobsFragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, jobsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToHistoryFragment() {
        val jobsFragment = FragmentBookingHistory()

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, jobsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToRequestTabFragment() {
        val jobsFragment = FragmentJobs()
        val bundle = Bundle().apply {
            putInt("selectedTab", 0)
        }
        jobsFragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, jobsFragment)
            .addToBackStack(null)
            .commit()
    }
    private fun navigateToOnGoingTabFragment() {
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

    fun openPaymentScreen(bookingId: String) {
        val paymentFragment = PaymentFragment.newInstance(bookingId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, paymentFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadNotifications() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e("FragmentNotification", "Error fetching notifications", e)
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        val newNotifications = mutableListOf<Notification>()

                        // Fetch sender profile data in a more efficient manner
                        snapshots.documents.forEach { doc ->
                            val notification = doc.toObject(Notification::class.java)?.apply {
                                notificationId = doc.id
                            }

                            if (notification != null && notification.senderId != null) {
                                db.collection("users").document(notification.senderId!!).get()
                                    .addOnSuccessListener { userDoc ->
                                        val senderProfileImageUrl = userDoc.getString("profile_picture") ?: ""
                                        notification.profilePicture = senderProfileImageUrl
                                        newNotifications.add(notification)

                                        // Update the UI once all notifications are processed
                                        if (newNotifications.size == snapshots.size()) {
                                            notifications.clear()
                                            notifications.addAll(newNotifications)
                                            notificationAdapter.notifyDataSetChanged()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("FragmentNotification", "Error fetching sender profile", e)
                                    }
                            }
                        }
                    } else {
                        Log.w("FragmentNotification", "No notifications found")
                    }
                }
        } else {
            Log.w("FragmentNotification", "User ID is null; cannot load notifications")
        }
    }
}