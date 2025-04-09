package com.example.terramaster

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class FragmentUserProfile: Fragment() {

    private lateinit var userId: String
    private lateinit var firstNameTextView: TextView
    private lateinit var lastNameTextView: TextView
    private lateinit var userTypeTextView: TextView
    private lateinit var profilePictureUrl: CircleImageView
    private lateinit var Rating: RatingBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var feedbackAdapter: FeedbackAdapter
    private val feedbackList = mutableListOf<Feedback>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_profile, container, false)

        firstNameTextView = view.findViewById(R.id.first_name)
        lastNameTextView = view.findViewById(R.id.last_name)
        userTypeTextView = view.findViewById(R.id.userType)
        profilePictureUrl = view.findViewById(R.id.profile)
        Rating = view.findViewById(R.id.ratingBar)

        feedbackAdapter = FeedbackAdapter(feedbackList)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = feedbackAdapter

        var message = view.findViewById<Button>(R.id.Message)

        userId = arguments?.getString("userId")?: ""
        fetchUserProfile(userId)

        message.setOnClickListener {
            navigateToPrivateMessage(userId)
        }

        fetchFeedback(userId)
        return view
    }

    private fun navigateToPrivateMessage(otherUserId: String){
        var currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
          if(userId != null){
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


    private fun fetchUserProfile(userId: String) {
        val db = FirebaseFirestore.getInstance()

        if (userId.isEmpty()) {
            Log.e("Firestore", "User ID is empty!")
            return
        }

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val firstName = document.getString("first_name")
                    val lastName = document.getString("last_name")
                    val userType = document.getString("user_type")
                    val profilePicture = document.getString("profile_picture")
                    val rating = document.getDouble("ratings")?.toFloat() ?: 0f

                    firstNameTextView.text = firstName
                    lastNameTextView.text = lastName
                    userTypeTextView.text = userType
                    Rating.rating = rating

                    if (!profilePicture.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profilePicture)
                            .placeholder(R.drawable.profilefree)
                            .into(profilePictureUrl)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching user profile", exception)
                Toast.makeText(requireContext(), "Error fetching data: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchFeedback(professionalId: String?) {
        val db = FirebaseFirestore.getInstance()

        if (professionalId.isNullOrEmpty()) {
            Log.e("Firestore", "Professional ID is null or empty!")
            return
        }

        // 🔥 Clear list before fetching to prevent duplicates
        feedbackList.clear()
        feedbackAdapter.notifyDataSetChanged()

        db.collection("Feedback")
            .whereEqualTo("bookedUserId", professionalId) // Query feedbacks for a particular professional
            .get()
            .addOnSuccessListener { feedbackDocuments ->
                if (feedbackDocuments.isEmpty) return@addOnSuccessListener

                var remainingTasks = feedbackDocuments.size()

                feedbackDocuments.forEach { document ->
                    val feedback = document.toObject(Feedback::class.java)
                    val landOwnerId = feedback.landOwnerUserId ?: return@forEach // Skip if landOwnerId is missing

                    Log.d("Firestore", "Fetching user details for landOwnerId: $landOwnerId")

                    // Correctly format and access Firestore document reference
                    if (landOwnerId.isNotEmpty()) {
                        // Correct way to access a specific document in "users" collection
                        db.collection("users").document(landOwnerId)
                            .get()
                            .addOnSuccessListener { userDocument ->
                                if (userDocument.exists()) {
                                    feedback.first_name = userDocument.getString("first_name") ?: ""
                                    feedback.last_name = userDocument.getString("last_name") ?: ""
                                    feedback.profile_picture = userDocument.getString("profile_picture") ?: ""
                                    feedback.rating = document.getDouble("rating")?.toFloat() ?: 0f // Use 0f if null
                                    // assuming 'rating' is a float in Firestore
                                    feedback.feedback = document.getString("feedback") ?: ""

                                }
                                feedbackList.add(feedback)

                                if (--remainingTasks == 0) {
                                    feedbackAdapter.notifyDataSetChanged()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Error fetching user details for feedback", e)
                            }
                    } else {
                        Log.e("Firestore", "landOwnerId is empty for feedback document")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching feedback", e)
            }
    }


}