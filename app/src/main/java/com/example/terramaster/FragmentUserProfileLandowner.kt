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

class FragmentUserProfileLandowner: Fragment() {

    private lateinit var userId: String
    private lateinit var firstNameTextView: TextView
    private lateinit var lastNameTextView: TextView
    private lateinit var userTypeTextView: TextView
    private lateinit var profilePictureUrl: CircleImageView



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_userprofile_landowner, container, false)

        firstNameTextView = view.findViewById(R.id.first_name)
        lastNameTextView = view.findViewById(R.id.last_name)
        userTypeTextView = view.findViewById(R.id.userType)
        profilePictureUrl = view.findViewById(R.id.profile)

        var message = view.findViewById<Button>(R.id.Message)

        userId = arguments?.getString("userId")?: ""
        fetchUserProfile(userId)

        message.setOnClickListener {
            navigateToPrivateMessage(userId)
        }


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


    private fun fetchUserProfile(userId: String){
        var db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if(document != null){
                    val firstName = document.getString("first_name")
                    val lastName = document.getString("last_name")
                    val userType = document.getString("user_type")
                    val profilePicture = document.getString("profile_picture")

                    firstNameTextView.text = firstName
                    lastNameTextView.text = lastName
                    userTypeTextView.text = userType

                    Glide.with(this)
                        .load(profilePicture)
                        .placeholder(R.drawable.profilefree)
                        .into(profilePictureUrl)

                }


            }

    }


}