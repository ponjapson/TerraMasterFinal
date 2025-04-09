package com.example.terramaster

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePassword : AppCompatActivity() {

    private lateinit var oldPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var savePasswordButton: Button
    private lateinit var backToProfileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        // Initialize views
        oldPasswordEditText = findViewById(R.id.oldPassword)
        newPasswordEditText = findViewById(R.id.newPassword)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)
        savePasswordButton = findViewById(R.id.savePasswordButton)
        backToProfileButton = findViewById(R.id.backToProfileButton)

        // Set onClickListener for Save Changes button
        savePasswordButton.setOnClickListener {
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Validate input
            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the new password is at least 6 characters long
            if (newPassword.length < 6) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if new passwords match
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call the function to change the password
            changePassword(oldPassword, newPassword)
        }

        // Set onClickListener for Back to Profile button
        backToProfileButton.setOnClickListener {
            finish()  // Simply close the activity (return to the previous screen)
        }
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        // Get the current user
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Reauthenticate the user with the old password
        val credential = EmailAuthProvider.getCredential(currentUser.email!!, oldPassword)

        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                // Reauthentication successful, update the password
                currentUser.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                        finish()  // Close the activity after password change
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error changing password: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                // Reauthentication failed (old password incorrect)
                Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
