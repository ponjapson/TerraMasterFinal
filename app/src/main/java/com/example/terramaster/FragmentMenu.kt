package com.example.terramaster


import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class FragmentMenu : DialogFragment() {

    private lateinit var userNameTextView: TextView
    private lateinit var profileImageView: CircleImageView
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Set the background of the dialog to be 80% opaque
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val view = inflater.inflate(R.layout.fragment_menu, container, false)

        // Initialize views
        userNameTextView = view.findViewById(R.id.user_name)
        profileImageView = view.findViewById(R.id.profile_picture)

        // Fetch user details
        fetchUserDetails()

        // Set click listeners for buttons
        view.findViewById<ImageButton>(R.id.btnBackToMain).setOnClickListener {
            dismiss()
        }


        view.findViewById<LinearLayout>(R.id.btnChangePassword).setOnClickListener {
            val intent = Intent(requireContext(), ChangePassword::class.java)
            startActivity(intent)
        }

        view.findViewById<LinearLayout>(R.id.btnLogButt).setOnClickListener {
            logout()
        }

        return view
    }

    private fun fetchUserDetails() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("first_name") ?: "Unknown"
                        val lastName = document.getString("last_name") ?: "User"
                        val profilePicture = document.getString("profile_picture")

                        // Set name
                        userNameTextView.text = "$firstName $lastName"

                        // Load profile picture using Glide
                        if (!profilePicture.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(profilePicture)
                                .placeholder(R.drawable.profile_pic)
                                .error(R.drawable.profile_pic)
                                .into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.profile_pic)
                        }
                    } else {
                        Toast.makeText(context, "User details not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Error fetching user details: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logout() {


        // Start the Signin activity
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        dismiss()
    }

    override fun onStart() {
        super.onStart()

        // Position the dialog to the top right corner
        dialog?.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val window = dialog?.window ?: return
        val params: WindowManager.LayoutParams = window.attributes
        params.gravity = Gravity.TOP or Gravity.END

        // Calculate the position to set it near the top right corner
        val display = window.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        params.x = 10 // Adjust as needed for horizontal offset
        params.y = 16 // Adjust as needed for vertical offset

        window.attributes = params
    }
}