package com.example.terramaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class FragmentProfile: Fragment() {

    private lateinit var circleProfile: CircleImageView
    private lateinit var firstNameTextView: TextView
    private lateinit var lastNameTextView: TextView
    private lateinit var usertypeTextView: TextView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        var Rating = view.findViewById<RatingBar>(R.id.ratingBar)
        var editProfile = view.findViewById<Button>(R.id.editProfile)

        circleProfile = view.findViewById(R.id.profile)
        firstNameTextView = view.findViewById(R.id.first_name)
        lastNameTextView = view.findViewById(R.id.last_name)
        usertypeTextView = view.findViewById(R.id.userType)

        if(userId != null){
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if(document != null) {
                        val firstName = document.getString("first_name")
                        val lastName = document.getString("last_name")
                        val userType = document.getString("userType")
                        val profilePictureUrl = document.getString("profile_picture")
                        val rating = document.getDouble("Rating")?.toFloat() ?: 0f

                        firstNameTextView.text = firstName
                        lastNameTextView.text = lastName
                        usertypeTextView.text = userType
                        Rating.rating = rating

                        Glide.with(this)
                            .load(profilePictureUrl)
                            .placeholder(R.drawable.profilefree)
                            .into(circleProfile)
                    }else {
                        Toast.makeText(requireContext(), "Document does not exist", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error fetching data: $exception", Toast.LENGTH_SHORT).show()
                }

        }

        editProfile.setOnClickListener {
            navigateToEditProfile()
        }

        return view
    }

    private fun navigateToEditProfile() {
        // Create instance of EditProfileFragment
        val editProfileFragment = FragmentEditProfile()

        // Get FragmentManager
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager

        // Begin FragmentTransaction
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()

        // Replace current fragment with EditProfileFragment
        transaction.replace(R.id.fragment_container, editProfileFragment)

        // Add transaction to back stack
        transaction.addToBackStack(null)

        // Commit the transaction
        transaction.commit()
    }
}