package com.example.terramaster

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class FragmentProfile: Fragment() {

    private lateinit var circleProfile: CircleImageView
    private lateinit var firstNameTextView: TextView
    private lateinit var lastNameTextView: TextView
    private lateinit var usertypeTextView: TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var feedbackAdapter: FeedbackAdapter
    private val feedbackList = mutableListOf<Feedback>()

    private lateinit var db: FirebaseFirestore  // 🔹 Move db here
    private var userId: String? = null  // 🔹 Move userId here

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)


        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid  // 🔹 Now accessible in onViewCreated

        var Rating = view.findViewById<RatingBar>(R.id.ratingBar)
        var editProfile = view.findViewById<Button>(R.id.editProfile)
        val btnShowMenu = view.findViewById<ImageButton>(R.id.menuBurger)
        btnShowMenu.setOnClickListener {
            val dialogFragment = FragmentMenu()
            dialogFragment.show(childFragmentManager, "menu_dialog")
        }

        circleProfile = view.findViewById(R.id.profile)
        firstNameTextView = view.findViewById(R.id.first_name)
        lastNameTextView = view.findViewById(R.id.last_name)
        usertypeTextView = view.findViewById(R.id.userType)

        feedbackAdapter = FeedbackAdapter(feedbackList)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = feedbackAdapter
        (requireActivity() as MainActivity).showBottomNavigationBar()

        if (userId != null) {
            db.collection("users").document(userId!!)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && isAdded) {
                        val firstName = document.getString("first_name")
                        val lastName = document.getString("last_name")
                        val userType = document.getString("user_type")
                        val profilePictureUrl = document.getString("profile_picture")
                        val rating = document.getDouble("ratings")?.toFloat() ?: 0f

                        firstNameTextView.text = firstName
                        lastNameTextView.text = lastName
                        usertypeTextView.text = userType
                        Rating.rating = rating

                        if (!profilePictureUrl.isNullOrEmpty()) {
                            Glide.with(requireContext())
                                .load(profilePictureUrl)
                                .placeholder(R.drawable.profilefree)
                                .into(circleProfile)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error fetching data: $exception", Toast.LENGTH_SHORT).show()
                }
        }

        editProfile.setOnClickListener {
            navigateToEditProfile()
        }

        fetchFeedback(userId)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists() && isAdded) {
                        val imageUrl = document.getString("profile_image_url") ?: ""

                        if (imageUrl.isNotEmpty() && context != null) {
                            Glide.with(requireContext())
                                .load(imageUrl)
                                .into(circleProfile)  // 🔹 Use circleProfile instead of profileImageView
                        }
                    }
                }
        }
    }

    private fun navigateToEditProfile() {
        val editProfileFragment = FragmentEditProfile()
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()

        transaction.replace(R.id.fragment_container, editProfileFragment)
        transaction.addToBackStack(null)
        transaction.commit()
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

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).showBottomNavigationBar()
    }
}
