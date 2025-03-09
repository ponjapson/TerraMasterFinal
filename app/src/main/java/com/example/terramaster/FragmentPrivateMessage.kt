package com.example.terramaster

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class FragmentPrivateMessage : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var messageAdapter: PrivateMessageAdapter
    private val messages = mutableListOf<PrivateMessageSealed>()
    private lateinit var profileImageView: CircleImageView
    private lateinit var chatRecipientNameTextView: TextView
    private lateinit var moreImgBtn: ImageButton
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private lateinit var chatRoomId: String
    private var isFirstMessage = false
    private var otherParticipantFullName: String? = null
    private var otherParticipantProfilePicture: String? = null
    private var messageListener: ListenerRegistration? = null
    private var otherUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_private_message, container, false)

        recyclerView = view.findViewById(R.id.messageRecyclerView)
        messageEditText = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)
        moreImgBtn = view.findViewById(R.id.bookingButton)

        messageAdapter = PrivateMessageAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = messageAdapter

        profileImageView = view.findViewById(R.id.profileImageView)
        chatRecipientNameTextView = view.findViewById(R.id.chatRecipientNameTextView)
        chatRoomId = arguments?.getString("chatId") ?: ""
        otherUserId = arguments?.getString("otherUserId")
        otherParticipantProfilePicture = arguments?.getString("profilePicUrl")

        val backBtn: ImageButton = view.findViewById(R.id.back_btn)
        backBtn.setOnClickListener {
            if (arguments?.getBoolean("fromFragmentMessage") == true) {
                val fragment = FragmentMessage()
                (requireActivity() as MainActivity).showBottomNavigationBar()
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
            } else {
                requireActivity().onBackPressed()
            }
        }

        moreImgBtn.setOnClickListener {
            val intent = Intent(requireContext(), BookingActivity::class.java)
            intent.putExtra("bookedUserId", otherUserId)
            startActivity(intent)
        }

        if (chatRoomId.isEmpty() && otherUserId != null) {
            chatRoomId = generateChatRoomId(currentUser!!.uid, otherUserId!!)
            isFirstMessage = true
        }

        otherUserId?.let {
            fetchOtherParticipantInfo(it)
        } ?: updateUI()

        checkIfChatRoomExists()
        loadMessages()

        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageEditText.text.clear()
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messageListener?.remove()
    }

    private fun fetchOtherParticipantInfo(otherUserId: String) {
        firestore.collection("users").document(otherUserId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val firstName = documentSnapshot.getString("first_name")
                val lastName = documentSnapshot.getString("last_name")

                otherParticipantFullName = if (firstName != null && lastName != null) {
                    "$firstName $lastName"
                } else {
                    "Unknown User"
                }

                otherParticipantProfilePicture = documentSnapshot.getString("profile_picture")

                val fcmToken = documentSnapshot.getString("fcmToken")
                if (!fcmToken.isNullOrEmpty()) {
                    Log.d("FragmentPrivateMessage", "FCM Token: $fcmToken")
                    sendNotificationToOtherUser(fcmToken)
                } else {
                    Log.w("FragmentPrivateMessage", "FCM Token not found for user: $otherUserId")
                }

                updateUI()
            }
            .addOnFailureListener { exception ->
                Log.e("FragmentPrivateMessage", "Error fetching other participant's info", exception)
            }
    }

    private fun updateUI() {
        if (!otherParticipantProfilePicture.isNullOrEmpty()) {
            Picasso.get().load(otherParticipantProfilePicture).into(profileImageView)
        }

        otherParticipantFullName?.let {
            chatRecipientNameTextView.text = it
        }
    }

    private fun checkIfChatRoomExists() {
        firestore.collection("chats").document(chatRoomId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    isFirstMessage = true
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FragmentPrivateMessage", "Error checking chat room existence", exception)
            }
    }

    private fun loadMessages() {
        messageListener = firestore.collection("messages")
            .whereEqualTo("chatRoomId", chatRoomId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    Log.e("FragmentPrivateMessage", "Error fetching messages", exception)
                    return@addSnapshotListener
                }

                messages.clear()
                snapshots?.forEach { document ->
                    val message = document.toObject(Message::class.java)
                    if (message.senderId == currentUser!!.uid) {
                        messages.add(PrivateMessageSealed.SentMessage(message))
                    } else {
                        messages.add(PrivateMessageSealed.ReceivedMessage(message))
                    }
                }
                messageAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messages.size - 1)
            }
    }

    private fun sendMessage(messageText: String) {
        val timestamp = System.currentTimeMillis()
        val message = Message(currentUser!!.uid, messageText, timestamp, chatRoomId)

        messages.add(PrivateMessageSealed.SentMessage(message))
        messageAdapter.notifyDataSetChanged()
        recyclerView.scrollToPosition(messages.size - 1)

        if (isFirstMessage) {
            val chat = Chat(
                participants = listOf(currentUser!!.uid, otherUserId ?: ""),
                content = messageText,
                timestamp = timestamp
            )

            firestore.collection("chats").document(chatRoomId).set(chat)
        } else {
            firestore.collection("chats").document(chatRoomId)
                .update("content", messageText, "timestamp", timestamp)
        }

        firestore.collection("messages").add(message)
    }

    private fun sendNotificationToOtherUser(fcmToken: String) {
        val notificationTitle = "New Message"
        val notificationBody = "You have a new message from $otherParticipantFullName"

        val data = mapOf(
            "title" to notificationTitle,
            "body" to notificationBody,
            "senderId" to currentUser?.uid,
            "chatRoomId" to chatRoomId
        )

        FirebaseMessaging.getInstance().send(
            com.google.firebase.messaging.RemoteMessage.Builder("$fcmToken@fcm.googleapis.com")
                .setMessageId(System.currentTimeMillis().toString())
                .setData(data)
                .build()
        )
    }

    private fun generateChatRoomId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
    }

    override fun onStart() {
        super.onStart()
        loadMessages()
    }

    override fun onStop() {
        super.onStop()
        messageListener?.remove()
    }
}
