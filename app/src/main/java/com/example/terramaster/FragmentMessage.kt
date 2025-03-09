package com.example.terramaster


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


class FragmentMessage : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var emptyTextView: TextView
    private val chats = mutableListOf<Chat>()
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageButton
    private lateinit var searchBar: View

    private var isSearchBarVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message, container, false)

        recyclerView = view.findViewById(R.id.messageRecyclerView)
        chatAdapter = ChatAdapter(chats, object : ChatAdapter.OnClickListener {
            override fun onItemClick(chat: Chat) {
                val otherUserId = chat.participants.firstOrNull { it != FirebaseAuth.getInstance().currentUser?.uid }
                otherUserId?.let { userId ->
                    navigateToPrivateMessageFragment(chat.chatId, userId, )
                }
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = chatAdapter

        emptyTextView = view.findViewById(R.id.textViewEmpty)
        searchEditText = view.findViewById(R.id.searchChat)
        searchIcon = view.findViewById(R.id.searchIcon)
        searchBar = view.findViewById(R.id.searchBar)

        // Initially hide the search bar
        searchBar.visibility = View.GONE

        searchIcon.setOnClickListener {
            toggleSearchBarVisibility()
        }

        searchEditText.setOnClickListener {
            navigateToUserSearchFragment()
        }

        fetchChats()

        return view
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).showBottomNavigationBar()
    }

    private fun fetchChats() {
        Log.d("FragmentMessage", "Fetching chats...")
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        // Clear the existing chat list to avoid duplicates
        chats.clear()

        db.collection("chats")
            .whereArrayContains("participants", currentUser?.uid ?: "")
            .get()
            .addOnSuccessListener { chatDocuments ->
                Log.d("FragmentMessage", "Fetched ${chatDocuments.size()} chat documents.")
                var chatProcessedCount = 0
                for (chatDocument in chatDocuments) {
                    val chatId = chatDocument.id
                    val participants = chatDocument.get("participants") as List<String>
                    val otherParticipantId = participants.firstOrNull { it != currentUser?.uid }
                    if (otherParticipantId != null) {
                        // Fetch other participant's info
                        db.collection("users").document(otherParticipantId)
                            .get()
                            .addOnSuccessListener { userDocument ->
                                val firstName = userDocument.getString("first_name")
                                val lastName = userDocument.getString("last_name")
                                val otherParticipantFullName = "$firstName $lastName" // Concatenate first and last name
                                val otherParticipantProfilePicture = userDocument.getString("profile_picture")
                                if (otherParticipantFullName != null && otherParticipantProfilePicture != null) {
                                    // Fetch latest message for the chat
                                    fetchLatestMessageForChat(chatId, participants, otherParticipantFullName, otherParticipantProfilePicture) { success ->
                                        chatProcessedCount++
                                        if (success) {
                                            if (chatProcessedCount == chatDocuments.size()) {
                                                checkEmptyView()
                                            }
                                        } else {
                                            // Handle failure to fetch latest message
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FragmentMessage", "Error getting user document for $otherParticipantId", exception)
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FragmentMessage", "Error getting chat documents: ", exception)
            }
    }

    private fun fetchLatestMessageForChat(chatId: String, participants: List<String>, otherParticipantFullName: String, otherParticipantProfilePicture: String, onComplete: (Boolean) -> Unit) {
        FirebaseFirestore.getInstance().collection("messages")
            .whereEqualTo("chatRoomId", chatId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { messageDocuments ->
                if (!messageDocuments.isEmpty) {
                    val lastMessage = messageDocuments.documents[0].toObject(Message::class.java)
                    if (lastMessage != null) {
                        val chat = Chat(
                            chatId = chatId,
                            participants = participants,
                            profilePicture = otherParticipantProfilePicture,
                            fullName = otherParticipantFullName,
                            content = lastMessage.text,
                            timestamp = lastMessage.timestamp
                        )

                        // Ensure the chat is not already in the list to avoid duplicates
                        if (!chats.any { it.chatId == chat.chatId }) {
                            chats.add(chat)
                        }

                        chatAdapter.notifyDataSetChanged()
                        onComplete(true)
                        return@addOnSuccessListener
                    }
                }
                onComplete(false)
            }
            .addOnFailureListener { exception ->
                Log.e("FragmentMessage", "Error getting last message for chat $chatId", exception)
                onComplete(false)
            }
    }

    private fun navigateToPrivateMessageFragment(chatId: String, otherParticipantId: String) {
        val fragment = FragmentPrivateMessage()
        val bundle = Bundle().apply {
            putString("chatId", chatId)
            putString("otherUserId", otherParticipantId)
            putBoolean("fromFragmentMessage", true)
        }
        fragment.arguments = bundle
        (requireActivity() as MainActivity).hideBottomNavigationBar()
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun checkEmptyView() {
        if (chats.isEmpty()) {
            emptyTextView.visibility = View.VISIBLE
        } else {
            emptyTextView.visibility = View.GONE
        }
    }

    private fun toggleSearchBarVisibility() {
        if (isSearchBarVisible) {
            searchBar.visibility = View.GONE
            isSearchBarVisible = false
        } else {
            // Fade in animation for search bar
            val fadeIn = AlphaAnimation(0f, 1f)
            fadeIn.duration = 500
            searchBar.startAnimation(fadeIn)
            searchBar.visibility = View.VISIBLE
            isSearchBarVisible = true
            searchEditText.requestFocus()
        }
    }

    private fun navigateToUserSearchFragment() {
        val fragment = UserSearchFragment()
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        (requireActivity() as MainActivity).hideBottomNavigationBar()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}