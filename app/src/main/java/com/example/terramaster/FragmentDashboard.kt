package com.example.terramaster

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder

class FragmentDashboard: Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SurveyorRecommendationAdapter
    private lateinit var adapterProcessor: SurveyorRecommendationAdapter
    private val recommendationList = mutableListOf<Recommendation>()
    private val processorRecommendationList = mutableListOf<Recommendation>()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var processorRecyclerView: RecyclerView
    private lateinit var spinner: Spinner
    private lateinit var recommendationTextView: TextView
    private var currentUser: FirebaseUser? = null
    private var userType: String? = null

    private lateinit var scheduleRecyclerView: RecyclerView
    private var schedulesAdapter: SchedulesAdapter? = null
    private val auth = FirebaseAuth.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Set up the toolbar
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (requireActivity() as MainActivity).setSupportActionBar(toolbar)

        scheduleRecyclerView = view.findViewById(R.id.schedulesRecyclerView)
        scheduleRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        schedulesAdapter = SchedulesAdapter(mutableListOf(), requireContext())
        scheduleRecyclerView.adapter = schedulesAdapter

        // Enable options menu in Fragment
        setHasOptionsMenu(true)
        recyclerView = view.findViewById(R.id.recommendationRecyclerView)
        processorRecyclerView = view.findViewById(R.id.processorRecyclerView)
        spinner = view.findViewById(R.id.spinnerSort)
        recommendationTextView = view.findViewById(R.id.recommendationTextView)

        firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser

        if (currentUser != null) {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            currentUser?.let { user ->
                val userId = user.uid
                val db = FirebaseFirestore.getInstance()

                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            userType = document.getString("user_type") ?: ""

                            val isRestrictedUser = userType.equals("Surveyor", ignoreCase = true) ||
                                    userType.equals("Processor", ignoreCase = true)

                            processorRecyclerView.visibility =
                                if (isRestrictedUser) View.GONE else View.VISIBLE
                            recyclerView.visibility =
                                if (isRestrictedUser) View.GONE else View.VISIBLE
                            recommendationTextView.visibility =
                                if (isRestrictedUser) View.GONE else View.VISIBLE
                            spinner.visibility = if (isRestrictedUser) View.GONE else View.VISIBLE
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreError", "Error fetching user type: ${e.message}")
                    }
            } ?: Log.e("FirebaseAuth", "User is not logged in")

        }

        adapter = SurveyorRecommendationAdapter(recommendationList, requireActivity()) { userId ->
            val bundle = Bundle().apply {
                putString("userId", userId)
            }
            val profileFragment = FragmentUserProfile()
            profileFragment.arguments = bundle

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        adapterProcessor = SurveyorRecommendationAdapter(
            processorRecommendationList,
            requireActivity()
        ) { userId ->
            val bundle = Bundle().apply {
                putString("userId", userId)
            }
            val profileFragment = FragmentUserProfile()
            profileFragment.arguments = bundle

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter

        processorRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        processorRecyclerView.adapter = adapterProcessor


        val sortOptions = listOf("Sort by Distance", "Sort by Ratings")

        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedSort = parent.getItemAtPosition(position).toString()
                when (position) {
                    0 -> fetchLandownerLocationAndRecommendSurveyors(selectedSort) // Sort by distance
                    1 -> fetchLandownerLocationAndRecommendSurveyors(selectedSort)  // Sort by ratings
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        fetchLandownerLocationAndRecommendSurveyors("Processor")

        val userId = auth.currentUser?.uid
        if (userId != null) {
            fetchOngoingSchedules(userId)
        }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.jobs_tool_bar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun fetchOngoingSchedules(currentUserId: String) {
        val bookedSchedules = mutableListOf<Schedules>()
        val landOwnerSchedules = mutableListOf<Schedules>()
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("bookings")
            .whereEqualTo("bookedUserId", currentUserId)
            .whereEqualTo("stage", "ongoing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SchedulesFetch", "Error fetching bookings: ${error.message}")
                    return@addSnapshotListener
                }

                bookedSchedules.clear()

                snapshot?.documents?.forEach { doc ->
                    val bookingId = doc.id
                    val landOwnerId = doc.getString("landOwnerUserId") ?: return@forEach
                    val documentStatus = doc.getString("documentStatus") ?: ""
                    val startDateTime = doc.getTimestamp("startDateTime")

                    // 🔹 First fetch landowner details (for display)
                    fetchUserData(firestore, landOwnerId) { landOwnerData ->
                        landOwnerData?.let {
                            val userName = "${it["first_name"] ?: ""} ${it["last_name"] ?: ""}".trim()
                            val profileImageUrl = it["profile_picture"] as? String

                            // 🔹 Then fetch current user's user_type separately
                            fetchUserData(firestore, currentUserId) { currentUserData ->
                                val userType = currentUserData?.get("user_type") as? String
                                val isProcessor = userType == "Processor"

                                // 🔍 Debug log
                                Log.d("SchedulesAdapter", "User: $userName, isProcessor: $isProcessor")

                                val schedule = Schedules(
                                    userName = userName,
                                    profileImageUrl = profileImageUrl,
                                    startDateTime = startDateTime,
                                    documentStatus = documentStatus,
                                    bookingId = bookingId,
                                    isProcessor = isProcessor
                                )
                                bookedSchedules.add(schedule)
                                updateSchedulesAdapter(bookedSchedules + landOwnerSchedules)
                            }
                        }
                    }
                }
            }


    // 2. Fetch bookings where user is the landOwnerUserId
        firestore.collection("bookings")
            .whereEqualTo("landOwnerUserId", currentUserId)
            .whereEqualTo("stage", "ongoing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SchedulesFetch", "Error fetching bookings: ${error.message}")
                    return@addSnapshotListener
                }

                landOwnerSchedules.clear() // ✅ Clear before re-adding to prevent duplicates

                snapshot?.documents?.forEach { doc ->
                    val bookingId = doc.id
                    val bookedUserId = doc.getString("bookedUserId") ?: return@forEach
                    val documentStatus = doc.getString("documentStatus") ?: ""
                    val startDateTime = doc.getTimestamp("startDateTime")

                    // Fetch booked user data
                    fetchUserData(firestore, bookedUserId) { userData ->
                        userData?.let {
                            val userName = "${it["first_name"] ?: ""} ${it["last_name"] ?: ""}".trim()
                            val profileImageUrl = it["profile_picture"] as? String
                            val userType = it["user_type"] as? String
                            val isProcessor = userType == "Processor"

                            val schedule = Schedules(
                                userName = userName,
                                profileImageUrl = profileImageUrl,
                                startDateTime = startDateTime,
                                documentStatus = documentStatus,
                                bookingId = bookingId,
                                isProcessor = isProcessor
                            )
                            landOwnerSchedules.add(schedule)
                            updateSchedulesAdapter(bookedSchedules + landOwnerSchedules)
                        }
                    }
                }
            }
    }
    private fun updateSchedulesAdapter(schedulesList: List<Schedules>) {
        if (scheduleRecyclerView.adapter == null) {
            scheduleRecyclerView.adapter = SchedulesAdapter(schedulesList, requireContext())
        } else {
            (scheduleRecyclerView.adapter as SchedulesAdapter).updateData(schedulesList)
        }
    }



    private fun fetchUserData(
        firestore: FirebaseFirestore,
        userId: String,
        callback: (Map<String, Any>?) -> Unit
    ) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    callback(document.data)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }







    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                // Navigate to the SearchFragment
                val fragment = SearchFragment()
                val transaction = requireActivity().supportFragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container, fragment)
                transaction.addToBackStack(null)
                transaction.commit()

                // Optionally show bottom navigation bar (if needed)
                (requireActivity() as MainActivity).showBottomNavigationBar()

                true
            }
            R.id.action_notification -> {
                val fragment = FragmentNotification()
                val transaction = requireActivity().supportFragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container, fragment)
                transaction.addToBackStack(null)
                transaction.commit()

                // Optionally show bottom navigation bar (if needed)
                (requireActivity() as MainActivity).showBottomNavigationBar()

                true
            }
            R.id.action_knowledge -> {
                val fragment = FragmentHome()
                val transaction = requireActivity().supportFragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container, fragment)
                transaction.addToBackStack(null)
                transaction.commit()

                // Optionally show bottom navigation bar (if needed)
                (requireActivity() as MainActivity).showBottomNavigationBar()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fetchLandownerLocationAndRecommendSurveyors(selectedSort: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val currentUserId = currentUser.uid

            firestore.collection("users").document(currentUserId).get()
                .addOnSuccessListener { document ->
                    val landownerLat = document.getDouble("latitude") ?: 0.0
                    val landownerLon = document.getDouble("longitude") ?: 0.0

                    if (landownerLat != 0.0 && landownerLon != 0.0) {
                        if (selectedSort == "Sort by Distance") {
                            fetchNearestSurveyors(landownerLat, landownerLon)
                        } else if (selectedSort == "Sort by Ratings") {
                            fetchHighRatingsSurveyors(landownerLat, landownerLon)
                        } else if (selectedSort == "Processor") {
                            convertCoordinatesToAddress(landownerLat, landownerLon) { address ->
                                Log.d(
                                    "AddressDebug",
                                    "Extracted Address: $address"
                                ) // Log the address here

                                getMunicipality(address) { municipality ->
                                    if (isAdded) { // Check if fragment is attached
                                        requireActivity().runOnUiThread {
                                            if (municipality != null) {
                                                fetchProcessorsByMunicipality(municipality)
                                            } else {
                                                Log.e(
                                                    "AddressDebug",
                                                    "Failed to extract municipality from: $address"
                                                ) // Log failure
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Failed to extract municipality",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (isAdded) {
                            Toast.makeText(
                                requireContext(),
                                "Landowner location not found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .addOnFailureListener {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Error fetching landowner data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            if (isAdded) {
                Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchProcessorsByMunicipality(municipality: String) {
        val municipalityLowerCase = municipality.trim().lowercase()

        firestore.collection("users")
            .whereEqualTo("user_type", "Processor")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(
                    "Check",
                    "Extracted Municipality: '$municipalityLowerCase' (Length: ${municipalityLowerCase.length})"
                )
                Log.d("Check", "Total documents retrieved: ${documents.size()}")

                processorRecommendationList.clear()
                val tempList = mutableListOf<Recommendation>()
                var pendingRequests = 0  // Counter to track geocode requests

                if (documents.isEmpty) {
                    if (isAdded) {
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "No processors found in $municipality",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val dbCity = doc.getString("City")?.trim()?.lowercase() ?: ""

                    Log.d(
                        "Check",
                        "Checking: dbCity='$dbCity' vs municipality='$municipalityLowerCase'"
                    )

                    if (dbCity.equals(municipalityLowerCase, ignoreCase = true)) {
                        Log.d("Check", "Match found for ${doc.id}!")

                        val firstName = doc.getString("first_name") ?: ""
                        val lastName = doc.getString("last_name") ?: ""
                        val profileUrl = doc.getString("profile_picture") ?: ""
                        val userType = doc.getString("user_type") ?: ""
                        val processorLat = doc.getDouble("latitude") ?: 0.0
                        val processorLon = doc.getDouble("longitude") ?: 0.0
                        val userId = doc.getString("uid") ?: ""
                        val ratings = doc.getDouble("ratings") ?: 0.0

                        if (tempList.any { it.userId == userId }) {
                            Log.d("Check", "Duplicate user detected, skipping: $userId")
                            continue // Skip duplicate users
                        }

                        pendingRequests++  // Increase counter for each geocode request

                        convertCoordinatesToAddress(processorLat, processorLon) { address ->
                            Log.d("DEBUG", "Processor: $firstName $lastName, Ratings: $ratings")

                            val processor = Recommendation(
                                firstName,
                                lastName,
                                userType,
                                address,
                                profileUrl,
                                0.0,
                                processorLat,
                                processorLon,
                                userId,
                                ratings
                            )

                            synchronized(tempList) {
                                tempList.add(processor)
                            }

                            pendingRequests--  // Decrease counter when request completes

                            if (pendingRequests == 0) {
                                if (isAdded) {
                                    updateUI(tempList)
                                }
                            }
                        }
                    } else {
                        Log.e(
                            "Check",
                            "Mismatch: dbCity='$dbCity' vs municipality='$municipalityLowerCase'"
                        )
                    }
                }

                if (pendingRequests == 0) {
                    if (isAdded) {
                        updateUI(tempList)
                    }
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Error fetching processors",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }


    /**
     * Updates UI with the processed list of processors.
     */
    private fun updateUI(processors: MutableList<Recommendation>) {
        // Check if the fragment is attached to an activity
        if (isAdded && activity != null) {
            requireActivity().runOnUiThread {
                processorRecommendationList.clear()
                processorRecommendationList.addAll(processors.sortedBy { it.distance }
                    .distinctBy { it.userId }) // Remove duplicates
                adapterProcessor.notifyDataSetChanged()
                Log.d("Check", "UI Updated: ${processorRecommendationList.size} items added")
            }
        } else {
            Log.e("FragmentError", "Fragment is not attached, skipping UI update")
        }
    }


    private fun getMunicipality(address: String, callback: (String?) -> Unit) {
        val client = OkHttpClient()
        val url = "https://nominatim.openstreetmap.org/search?q=${
            URLEncoder.encode(
                address,
                "UTF-8"
            )
        }&format=json&addressdetails=1"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GetMunicipality", "Failed to fetch municipality: ${e.message}", e)
                requireActivity().runOnUiThread {
                    Log.e("GetMunicipality", "Failed to fetch municipality",)
                }
                callback(null) // Return null
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string()

                    if (responseBody.isNullOrEmpty()) {
                        Log.w("GetMunicipality", "No municipality found")
                        requireActivity().runOnUiThread {
                            Log.e("No municipality", "No municipality found")
                        }
                        callback(null)
                        return
                    }

                    try {
                        val json = JSONArray(responseBody)
                        if (json.length() > 0) {
                            val firstResult = json.getJSONObject(0)
                            val addressDetails = firstResult.optJSONObject("address")

                            // Extract City → Town → County in order of priority
                            val municipality = when {
                                !addressDetails?.optString("city")
                                    .isNullOrEmpty() -> addressDetails?.optString("city")

                                !addressDetails?.optString("town")
                                    .isNullOrEmpty() -> addressDetails?.optString("town")

                                !addressDetails?.optString("county")
                                    .isNullOrEmpty() -> addressDetails?.optString("county")

                                else -> "Unknown Municipality"
                            }

                            // Log extracted values
                            Log.i(
                                "GetMunicipality",
                                "Extracted - City: ${addressDetails?.optString("city")}, Town: ${
                                    addressDetails?.optString("town")
                                }, County: ${addressDetails?.optString("county")}"
                            )
                            Log.i("GetMunicipality", "Final Municipality: $municipality")



                            callback(municipality)
                        } else {
                            Log.w("GetMunicipality", "Empty JSON response")

                            callback(null)
                        }
                    } catch (e: Exception) {
                        Log.e("GetMunicipality", "Error parsing response: ${e.message}", e)

                        callback(null)
                    }
                }
            }
        })
    }

    private fun fetchHighRatingsSurveyors(landownerLat: Double, landownerLon: Double) {
        firestore.collection("users")
            .whereEqualTo("user_type", "Surveyor")
            .get()
            .addOnSuccessListener { documents ->
                val tempList = mutableListOf<Recommendation>()
                var pendingCallbacks = 0

                for (doc in documents) {
                    val firstName = doc.getString("first_name") ?: ""
                    val lastName = doc.getString("last_name") ?: ""
                    val profileUrl = doc.getString("profile_picture") ?: ""
                    val userType = doc.getString("user_type") ?: ""
                    val surveyorLat = doc.getDouble("latitude") ?: 0.0
                    val surveyorLon = doc.getDouble("longitude") ?: 0.0
                    val userId = doc.getString("uid") ?: ""
                    val ratings = doc.getDouble("ratings") ?: 0.0
                    val distance =
                        calculateDistance(landownerLat, landownerLon, surveyorLat, surveyorLon)

                    if (distance <= 50.0) {
                        pendingCallbacks++
                        convertCoordinatesToAddress(surveyorLat, surveyorLon) { address ->
                            tempList.add(
                                Recommendation(
                                    firstName, lastName, userType, address, profileUrl,
                                    distance, surveyorLat, surveyorLon, userId, ratings
                                )
                            )
                            pendingCallbacks--

                            if (pendingCallbacks == 0) {
                                updateAdapter(tempList.sortedByDescending { it.ratings })
                            }
                        }
                    }
                }

                if (pendingCallbacks == 0) {
                    updateAdapter(tempList.sortedByDescending { it.ratings })
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error fetching surveyors", Toast.LENGTH_SHORT)
                    .show()
            }
    }


    private fun fetchNearestSurveyors(landownerLat: Double, landownerLon: Double) {
        firestore.collection("users")
            .whereEqualTo("user_type", "Surveyor")
            .get()
            .addOnSuccessListener { documents ->
                val tempList = mutableListOf<Recommendation>()
                var pendingCallbacks = 0

                for (doc in documents) {
                    val firstName = doc.getString("first_name") ?: ""
                    val lastName = doc.getString("last_name") ?: ""
                    val profileUrl = doc.getString("profile_picture") ?: ""
                    val userType = doc.getString("user_type") ?: ""
                    val surveyorLat = doc.getDouble("latitude") ?: 0.0
                    val surveyorLon = doc.getDouble("longitude") ?: 0.0
                    val userId = doc.getString("uid") ?: ""
                    val ratings = doc.getDouble("ratings") ?: 0.0
                    val distance =
                        calculateDistance(landownerLat, landownerLon, surveyorLat, surveyorLon)

                    if (distance <= 50.0) {
                        pendingCallbacks++
                        convertCoordinatesToAddress(surveyorLat, surveyorLon) { address ->
                            tempList.add(
                                Recommendation(
                                    firstName, lastName, userType, address, profileUrl,
                                    distance, surveyorLat, surveyorLon, userId, ratings
                                )
                            )
                            pendingCallbacks--

                            if (pendingCallbacks == 0) {
                                updateAdapter(tempList.sortedBy { it.distance })
                            }
                        }
                    }
                }

                if (pendingCallbacks == 0) {
                    updateAdapter(tempList.sortedBy { it.distance })
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error fetching surveyors", Toast.LENGTH_SHORT)
                    .show()
            }
    }


    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    private fun convertCoordinatesToAddress(lat: Double, lon: Double, callback: (String) -> Unit) {
        if (!isAdded || context == null) {
            Log.e(
                "FragmentError",
                "convertCoordinatesToAddress() called when fragment is not attached"
            )
            callback("Unknown Address") // Return a default value to avoid breaking the flow
            return
        }

        val geocoder = OpenStreetMapGeocoder(requireContext())
        geocoder.getAddressFromCoordinates(lat, lon) { address ->
            callback(address ?: "Unknown Address")
        }
    }


    private fun updateAdapter(sortedList: List<Recommendation>) {
        // Check if fragment is still attached to its activity
        if (isAdded && activity != null) {
            requireActivity().runOnUiThread {
                recommendationList.clear()
                recommendationList.addAll(sortedList)
                adapter.notifyDataSetChanged()
            }
        } else {
            Log.e("FragmentError", "Fragment is not attached, skipping UI update")
        }

    }
}