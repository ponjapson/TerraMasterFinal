package com.example.terramaster

import android.app.Activity
import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.concurrent.thread

class RegisterActivity : AppCompatActivity() {

    private lateinit var ivFrontID: ImageView
    private lateinit var ivBackID: ImageView
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etAddress: EditText
    private lateinit var rgUserType: RadioGroup
    private lateinit var btnSignUp: Button
    private lateinit var btnUploadFront: Button
    private lateinit var btnUploadBack: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var streetAddress: EditText
    private lateinit var city: EditText
    private lateinit var province: EditText
    private lateinit var postalCode: EditText
    private lateinit var storage: FirebaseStorage
    private lateinit var etBarangay: EditText

    private var frontIDBitmap: Bitmap? = null
    private var backIDBitmap: Bitmap? = null

    private val REQUEST_IMAGE_CAPTURE_FRONT = 1
    private val REQUEST_IMAGE_CAPTURE_BACK = 2
    private val CAMERA_PERMISSION_CODE = 100
    private val DEFAULT_PROFILE_PICTURE_URL =
        "https://static.vecteezy.com/system/resources/thumbnails/020/765/399/small/default-profile-account-unknown-icon-black-silhouette-free-vector.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        btnUploadFront = findViewById(R.id.btnUploadFront)
        btnUploadBack = findViewById(R.id.btnUploadBack)
        ivFrontID = findViewById(R.id.ivFrontID)
        ivBackID = findViewById(R.id.ivBackID)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        rgUserType = findViewById(R.id.rgUserType)
        btnSignUp = findViewById(R.id.btnSignUp)
        progressBar = findViewById(R.id.progressBar)
        streetAddress = findViewById(R.id.etStreetAddress)
        city = findViewById(R.id.etCity)
        postalCode = findViewById(R.id.etPostalCode)
        province = findViewById(R.id.etProvince)
        etBarangay = findViewById(R.id.etBarangay)

        val tvSignIn: TextView = findViewById(R.id.tvSignIn)

        tvSignIn.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnUploadFront.setOnClickListener {
            checkCameraPermission(REQUEST_IMAGE_CAPTURE_FRONT)
        }

        btnUploadBack.setOnClickListener {
            checkCameraPermission(REQUEST_IMAGE_CAPTURE_BACK)
        }


        rgUserType.setOnCheckedChangeListener { _, checkedId ->
            when (findViewById<RadioButton>(checkedId).text.toString()) {
                "Landowner" -> {
                    btnUploadFront.visibility = View.GONE
                    btnUploadBack.visibility = View.GONE
                    ivFrontID.visibility = View.GONE
                    ivBackID.visibility = View.GONE
                }
                "Processor", "Surveyor" -> {
                    btnUploadFront.visibility = View.VISIBLE
                    btnUploadBack.visibility = View.VISIBLE
                    ivFrontID.visibility = View.VISIBLE
                    ivBackID.visibility = View.VISIBLE
                }
            }
        }

        btnSignUp.setOnClickListener {
            validateAndRegisterUser()
        }
    }

    private fun getCoordinatesFromAddress(address: String, callback: (Double?, Double?) -> Unit){
        val urlString = "https://nominatim.openstreetmap.org/search?q=${address.replace(" ", "+")}&format=json"


        thread {
            var found = false

            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val scanner = Scanner(connection.inputStream)
                val response = StringBuilder()

                while (scanner.hasNext()) {
                    response.append(scanner.nextLine())
                }
                scanner.close()

                val jsonArray = JSONArray(response.toString())
                if (jsonArray.length() > 0) {
                    val firstResult = jsonArray.getJSONObject(0)
                    val lat = firstResult.getDouble("lat")
                    val lon = firstResult.getDouble("lon")
                    found = true
                    callback(lat, lon)

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if(!found)
            {
            callback(null, null)
            }
        }
    }

    private fun openCamera(requestCode: Int) {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, requestCode)
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }


    private fun checkCameraPermission(requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            openCamera(requestCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            val imageBitmap = data.extras?.get("data") as Bitmap
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE_FRONT -> {
                    frontIDBitmap = imageBitmap
                    ivFrontID.setImageBitmap(imageBitmap)
                }
                REQUEST_IMAGE_CAPTURE_BACK -> {
                    backIDBitmap = imageBitmap
                    ivBackID.setImageBitmap(imageBitmap)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun validateAndRegisterUser() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val selectedUserTypeId = rgUserType.checkedRadioButtonId
        val StreetAddress = streetAddress.text.toString().trim()
        val City = city.text.toString().trim()
        val Provnce = province.text.toString().trim()
        val PostalCode = postalCode.text.toString().trim()
        val Barangay = etBarangay.text.toString().trim()

        if (firstName.isEmpty()) {
            etFirstName.error = "First Name is required"
            return
        }

        if (lastName.isEmpty()) {
            etLastName.error = "Last Name is required"
            return
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Valid Email is required"
            return
        }

        if (password.isEmpty() || password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            return
        }

        if (PostalCode.isEmpty()) {
            postalCode.error = "Postal Code is required"
            return
        }

        if(Provnce.isEmpty()){
            province.error = "Province is required"
        }

        if(City.isEmpty()){
            city.error = "City is required"
        }

      /*  if(StreetAddress.isEmpty()){
            streetAddress.error = "Street Address is required"
        }*/
        if(Barangay.isEmpty()){
            etBarangay.error = "Street Address is required"
        }

        if (selectedUserTypeId == -1) {
            Toast.makeText(this, "Please select a user type", Toast.LENGTH_SHORT).show()
            return
        }

        val userType = findViewById<RadioButton>(selectedUserTypeId).text.toString()

        if ((userType == "Processor" || userType == "Surveyor") && (frontIDBitmap == null || backIDBitmap == null)) {
            Toast.makeText(this, "Please upload both front and back ID images", Toast.LENGTH_SHORT).show()
            return
        }

        var fullAddress = "$Barangay, $StreetAddress, $City, $Provnce, $PostalCode, Philippines"

        progressBar.visibility = View.VISIBLE
        btnSignUp.isEnabled = false

        getCoordinatesFromAddress(fullAddress) {lat, lon ->

            if(lat != null && lon != null){
                uploadIDImagesAndRegister(firstName, lastName, email, password, Barangay, StreetAddress, City, Provnce, PostalCode, userType, lat, lon)
            }else
                runOnUiThread {
                    Toast.makeText(this, "Failed to get coordinates. Please check your address.", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    btnSignUp.isEnabled = true
                }

        }


    }

   

    private fun uploadIDImagesAndRegister(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        barangay: String,
        StreetAddress: String,
        City: String,
        Province: String,
        PostalCode: String,
        userType: String,
        latitude: Double,
        longitude: Double,

    ) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
            if (tokenTask.isSuccessful) {
                val fcmToken = tokenTask.result
                val storageRef = storage.reference

                val frontIDRef = storageRef.child("id_images/${UUID.randomUUID()}_front.jpg")
                val backIDRef = storageRef.child("id_images/${UUID.randomUUID()}_back.jpg")

                val frontIDStream = ByteArrayOutputStream()
                frontIDBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, frontIDStream)
                val frontIDData = frontIDStream.toByteArray()

                val backIDStream = ByteArrayOutputStream()
                backIDBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, backIDStream)
                val backIDData = backIDStream.toByteArray()

                frontIDRef.putBytes(frontIDData).addOnSuccessListener { frontTask ->
                    frontIDRef.downloadUrl.addOnSuccessListener { frontUri ->
                        backIDRef.putBytes(backIDData).addOnSuccessListener { backTask ->
                            backIDRef.downloadUrl.addOnSuccessListener { backUri ->
                                registerUserWithFirebase(
                                    firstName,
                                    lastName,
                                    email,
                                    password,
                                    barangay,
                                    StreetAddress,
                                    City,
                                    Province,
                                    PostalCode,
                                    userType,
                                    fcmToken,
                                    frontUri.toString(),
                                    backUri.toString(),
                                    latitude,
                                    longitude

                                )
                            }
                        }.addOnFailureListener { e ->
                            handleFailure("Error uploading back ID: ${e.message}")
                        }
                    }
                }.addOnFailureListener { e ->
                    handleFailure("Error uploading front ID: ${e.message}")
                }
            } else {
                handleFailure("Error fetching FCM token: ${tokenTask.exception?.message}")
            }
        }
    }

    private fun registerUserWithFirebase(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        Barangay: String,
        StreetAddress: String,
        City: String,
        Province: String,
        PostalCode: String,
        userType: String,
        fcmToken: String,
        frontIDUrl: String?,
        backIDUrl: String?,
        latitude: Double,
        longitude: Double
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: UUID.randomUUID().toString() // Use Firebase UID or generate one
                    val user = hashMapOf(
                        "uid" to userId, // Add UID explicitly to the document
                        "first_name" to firstName,
                        "last_name" to lastName,
                        "email" to email,
                        "Barangay" to Barangay,
                        "Street_Address" to StreetAddress,
                        "City" to City,
                        "Province" to Province,
                        "Postal_Code" to PostalCode,
                        "user_type" to userType,
                        "status" to "Pending", // Set to Pending initially
                        "profile_picture" to DEFAULT_PROFILE_PICTURE_URL,
                        "fcmToken" to fcmToken,
                        "frontIDUrl" to frontIDUrl,
                        "backIDUrl" to backIDUrl,
                        "longitude" to longitude,
                        "latitude" to latitude
                    )

                    db.collection("users").document(userId).set(user)
                        .addOnSuccessListener {
                            auth.currentUser?.sendEmailVerification()
                                ?.addOnSuccessListener {
                                    progressBar.visibility = View.GONE
                                    btnSignUp.isEnabled = true
                                    Toast.makeText(
                                        this,
                                        "Registration successful! Verify your email.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                ?.addOnFailureListener { e ->
                                    handleFailure("Error sending verification email: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            handleFailure("Error saving user: ${e.message}")
                        }
                } else {
                    handleFailure("Error registering user: ${task.exception?.message}")
                }
            }
    }


    private fun handleFailure(message: String) {
        Log.e("RegisterActivity", message)
        progressBar.visibility = View.GONE
        btnSignUp.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
