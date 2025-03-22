package com.example.terramaster

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class FragmentEditProfile : Fragment() {

    private lateinit var profilePictureImageView: CircleImageView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var progressBar: ProgressBar
    private var currentUserUid: String? = null
    private lateinit var fName: EditText
    private lateinit var lName: EditText

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profiile, container, false)

        (requireActivity() as MainActivity).hideBottomNavigationBar()
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        progressBar = view.findViewById(R.id.progressBar)

        currentUserUid = firebaseAuth.currentUser?.uid
        Log.d("FragmentEditProfile", "Current user UID: $currentUserUid")

        profilePictureImageView = view.findViewById(R.id.profile_image)
        val defaultProfilePictureUrl = "https://static.vecteezy.com/system/resources/thumbnails/020/765/399/small/default-profile-account-unknown-icon-black-silhouette-free-vector.jpg"
        Picasso.get().load(defaultProfilePictureUrl).into(profilePictureImageView)

        val backButton: ImageButton = view.findViewById(R.id.backButton)
        val editProfile: ImageButton = view.findViewById(R.id.change_photo_icon)
        val buttonSaveChanges: Button = view.findViewById(R.id.buttonSaveChanges)

        fName = view.findViewById(R.id.editTextFName)
        lName = view.findViewById(R.id.editTextLName)

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()

        }

        editProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        buttonSaveChanges.setOnClickListener {
            progressBar.visibility = View.VISIBLE

            val newFName = fName.text.toString()
            val newLName = lName.text.toString()

            currentUserUid?.let { uid ->
                // Retrieve current profile picture URL if not explicitly changed
                val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
                userRef.get().addOnSuccessListener { document ->
                    if (document != null) {
                        // Get the existing profile picture URL or default
                        val currentProfilePictureUrl = document.getString("profile_picture") ?: defaultProfilePictureUrl

                        // Determine the URL to update (new or existing)
                        val newProfilePictureUrl = profilePictureImageView.tag as? String ?: currentProfilePictureUrl

                        // Call the function to update Firestore
                        updateProfilePictureInFirestore(
                            profilePictureUrl = newProfilePictureUrl,
                            uid = uid,
                            newFName = newFName,
                            newLName = newLName
                        )
                    } else {
                        progressBar.visibility = View.GONE
                        Log.e("FragmentEditProfile", "Document not found for UID: $uid")
                    }
                }.addOnFailureListener { exception ->
                    progressBar.visibility = View.GONE
                    Log.e("FragmentEditProfile", "Error fetching profile: $exception")
                }
            }
        }

        fetchAndDisplayUserData()
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            // Handle the image picked by the user
            val selectedImageUri = data.data

            // Ensure that selectedImageUri is not null
            selectedImageUri?.let { uri ->
                // Upload the image to Firebase Storage
                val storageRef = firebaseStorage.reference
                val imagesRef = storageRef.child("profile_images/${UUID.randomUUID()}")

                imagesRef.putFile(uri)
                    .addOnSuccessListener { taskSnapshot ->
                        // Get the download URL of the uploaded image
                        imagesRef.downloadUrl
                            .addOnSuccessListener { downloadUri ->
                                // Convert the download URL to a string
                                val imageUriString = downloadUri.toString()

                                // Set the tag of profilePictureImageView to the download URL
                                profilePictureImageView.tag = imageUriString

                                // Load the selected image into profilePictureImageView using Picasso
                                Picasso.get().load(downloadUri).into(profilePictureImageView)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FragmentEditProfile", "Failed to get download URL: $exception")
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FragmentEditProfile", "Failed to upload image: $exception")
                    }
            } ?: run {
                // Handle the case where selectedImageUri is null
                Log.e("FragmentEditProfile", "Selected image URI is null")
            }
        }
    }

    private fun updateProfilePictureInFirestore(profilePictureUrl: String, uid: String, newFName: String,newLName: String) {
        Log.d("FragmentEditProfile", "Updating profile information...")
        Log.d("FragmentEditProfile", "UID: $uid")
        Log.d("FragmentEditProfile", "ProfilePicture: $profilePictureUrl")

        if (uid.isNotBlank()) { // Check if uid is not null or empty
            val updatedProfileInfo: MutableMap<String, Any?> = hashMapOf(
                "profile_picture" to profilePictureUrl,
                "first_name" to newFName,
                "last_name" to newLName
            )

            firebaseFirestore.collection("users").document(uid)
                .update(updatedProfileInfo)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile Information Updated Successfully", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()

                }
                .addOnFailureListener { exception ->
                    Log.e("FragmentEditProfile", "Error updating profile information: $exception")
                    progressBar.visibility = View.GONE
                    // Handle failure to update profile information
                }
        } else {
            Log.e("FragmentEditProfile", "Invalid user UID")
        }
    }
    private fun fetchAndDisplayUserData() {
        currentUserUid?.let { uid ->
            val userDocRef = firebaseFirestore.collection("users").document(uid)
            userDocRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val userProfilePictureUrl = documentSnapshot.getString("profile_picture")
                        val userFName = documentSnapshot.getString("first_name")
                        val userLName = documentSnapshot.getString("last_name")

                        // Set the retrieved data to the views
                        userProfilePictureUrl?.let {
                            Picasso.get().load(it).into(profilePictureImageView)
                        }

                        fName.setText(userFName)
                        lName.setText(userLName)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FragmentEditProfile", "Error fetching user data: $exception")
                }
        }
    }
}
