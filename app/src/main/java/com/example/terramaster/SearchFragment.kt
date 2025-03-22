package com.example.terramaster

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terramaster.Suggested
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder

class SearchFragment : Fragment() {

    private lateinit var searchView: EditText
    private lateinit var recentListView: ListView
    private lateinit var displayedListView: ListView
    private lateinit var recentHeadingTextView: TextView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recentAdapter: RecentSearchAdapter
    private lateinit var displayedAdapter: DisplayedSearchAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var recentItems: ArrayList<SearchItem>
    private lateinit var displayedItems: ArrayList<SearchItem>
    private var currentSearchText: String = ""
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SurveyorRecommendationAdapter
    private lateinit var adapterProcessor: SurveyorRecommendationAdapter
    private val recommendationList = mutableListOf<Recommendation>()
    private val processorRecommendationList = mutableListOf<Recommendation>()
    private lateinit var processorRecyclerView: RecyclerView
    private lateinit var spinner: Spinner
    private lateinit var recommendationTextView: TextView
    private lateinit var advanceSearch: Button
    private var currentUser: FirebaseUser? = null
    private lateinit var userType: String



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        val auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        advanceSearch = view.findViewById(R.id.advanceSearch)
        recyclerView = view.findViewById(R.id.recommendationRecyclerView)
        processorRecyclerView = view.findViewById(R.id.processorRecyclerView)
        spinner = view.findViewById(R.id.spinnerSort)
        recommendationTextView = view.findViewById(R.id.recommendationTextView)

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

                            advanceSearch.visibility = if (isRestrictedUser) View.GONE else View.VISIBLE
                            processorRecyclerView.visibility = if (isRestrictedUser) View.GONE else View.VISIBLE
                            recyclerView.visibility = if (isRestrictedUser) View.GONE else View.VISIBLE
                            recommendationTextView.visibility = if (isRestrictedUser) View.GONE else View.VISIBLE
                            spinner.visibility = if (isRestrictedUser) View.GONE else View.VISIBLE
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreError", "Error fetching user type: ${e.message}")
                    }
            } ?: Log.e("FirebaseAuth", "User is not logged in")

        }


        advanceSearch.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FragmentAdvanceSearch())
                .addToBackStack(null)
                .commit()
        }
        // Initialize views
        searchView = view.findViewById(R.id.searchView)
        recentListView = view.findViewById(R.id.recentListView)
        displayedListView = view.findViewById(R.id.displayedListView)
        recentHeadingTextView = view.findViewById(R.id.recentHeadingTextView)

        // Initialize Firestore and SharedPreferences
        firestore = FirebaseFirestore.getInstance()
        sharedPreferences = requireActivity().getSharedPreferences("search_prefs", Context.MODE_PRIVATE)

        // Initialize lists and adapters
        recentItems = ArrayList()
        displayedItems = ArrayList()


        recentAdapter = RecentSearchAdapter(requireContext(), recentItems)

        displayedAdapter = DisplayedSearchAdapter(requireContext(), displayedItems, object : OnItemClickListener {
            override fun onItemClick(userId: String) {
                navigateToProfileFragment(userId)
            }
        })

        recentListView.adapter = recentAdapter
        displayedListView.adapter = displayedAdapter



        // Initially hide the displayed ListView
        displayedListView.visibility = View.GONE

        loadRecentSearches()

        // Show keyboard when search field is focused
        searchView.requestFocus()
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)

        // Add TextWatcher for search input
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString().trim().lowercase()
                currentSearchText = searchText
                Log.d("Search", "Search text: $searchText")

                val isRestrictedUser = userType.equals("Surveyor", ignoreCase = true) ||
                        userType.equals("Processor", ignoreCase = true)

                if (searchText.isNotEmpty()) {
                    Log.d("Search", "Showing Displayed ListView")
                    recentListView.visibility = View.GONE
                    recentHeadingTextView.visibility = View.GONE
                    displayedListView.visibility = View.VISIBLE
                    advanceSearch.visibility = View.GONE
                    processorRecyclerView.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    recommendationTextView.visibility = View.GONE
                    spinner.visibility = View.GONE

                    // Only hide these views if the user is restricted
                    if (isRestrictedUser) {
                        advanceSearch.visibility = View.GONE
                        processorRecyclerView.visibility = View.GONE
                        recyclerView.visibility = View.GONE
                        recommendationTextView.visibility = View.GONE
                        spinner.visibility = View.GONE
                    } else {
                        advanceSearch.visibility = View.GONE
                        processorRecyclerView.visibility = View.GONE
                        recyclerView.visibility = View.GONE
                        recommendationTextView.visibility = View.GONE
                        spinner.visibility = View.GONE
                    }

                    filterNames(searchText)
                } else {
                    Log.d("Search", "Showing Recent ListView")
                    recentListView.visibility = View.VISIBLE
                    recentHeadingTextView.visibility = View.VISIBLE
                    displayedListView.visibility = View.GONE

                    // If the user is restricted, keep them hidden
                    if (!isRestrictedUser) {
                        advanceSearch.visibility = View.VISIBLE
                        processorRecyclerView.visibility = View.VISIBLE
                        recyclerView.visibility = View.VISIBLE
                        recommendationTextView.visibility = View.VISIBLE
                        spinner.visibility = View.VISIBLE
                    }
                }
            }
        })


        // Handle long click to remove recent search
        recentListView.setOnItemLongClickListener { _, _, position, _ ->
            val searchTextToRemove = recentItems[position].name
            removeRecentSearch(searchTextToRemove)
            true
        }

        if (searchView.text.isEmpty()) {
            recentListView.visibility = View.VISIBLE
            recentHeadingTextView.visibility = View.VISIBLE
        }

        //Recommendation System code
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

        adapterProcessor = SurveyorRecommendationAdapter(processorRecommendationList, requireActivity()) { userId ->
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
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter

        processorRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        processorRecyclerView.adapter = adapterProcessor


        val sortOptions = listOf("Sort by Distance", "Sort by Ratings")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedSort = parent.getItemAtPosition(position).toString()
                when (position) {
                    0 -> fetchLandownerLocationAndRecommendSurveyors(selectedSort) // Sort by distance
                    1 -> fetchLandownerLocationAndRecommendSurveyors(selectedSort)  // Sort by ratings
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        fetchLandownerLocationAndRecommendSurveyors("Processor")
        return view
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
                        if(selectedSort == "Sort by Distance") {
                            fetchNearestSurveyors(landownerLat, landownerLon)
                        }else if(selectedSort == "Sort by Ratings"){
                            fetchHighRatingsSurveyors(landownerLat, landownerLon)
                        }else if(selectedSort == "Processor"){
                            convertCoordinatesToAddress(landownerLat, landownerLon) { address ->
                                Log.d("AddressDebug", "Extracted Address: $address") // Log the address here

                                getMunicipality(address) { municipality ->
                                    requireActivity().runOnUiThread {
                                        if (municipality != null) {
                                            fetchProcessorsByMunicipality(municipality)
                                        } else {
                                            Log.e("AddressDebug", "Failed to extract municipality from: $address") // Log failure
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
                    } else {
                        Toast.makeText(requireContext(), "Landowner location not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error fetching landowner data", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchProcessorsByMunicipality(municipality: String) {
        val municipalityLowerCase = municipality.trim().lowercase()

        firestore.collection("users")
            .whereEqualTo("user_type", "Processor")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("Check", "Extracted Municipality: '$municipalityLowerCase' (Length: ${municipalityLowerCase.length})")
                Log.d("Check", "Total documents retrieved: ${documents.size()}")

                processorRecommendationList.clear()
                val tempList = mutableListOf<Recommendation>()
                var pendingRequests = 0  // Counter to track geocode requests

                if (documents.isEmpty) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "No processors found in $municipality", Toast.LENGTH_SHORT).show()
                    }
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val dbCity = doc.getString("City")?.trim()?.lowercase() ?: ""

                    Log.d("Check", "Checking: dbCity='$dbCity' vs municipality='$municipalityLowerCase'")

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
                                firstName, lastName, userType, address, profileUrl, 0.0, processorLat, processorLon, userId, ratings
                            )

                            synchronized(tempList) {
                                tempList.add(processor)
                            }

                            pendingRequests--  // Decrease counter when request completes

                            if (pendingRequests == 0) {
                                updateUI(tempList)
                            }
                        }
                    } else {
                        Log.e("Check", "Mismatch: dbCity='$dbCity' vs municipality='$municipalityLowerCase'")
                    }
                }

                if (pendingRequests == 0) {
                    updateUI(tempList)
                }
            }
            .addOnFailureListener {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Error fetching processors", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Updates UI with the processed list of processors.
     */
    private fun updateUI(processors: MutableList<Recommendation>) {
        requireActivity().runOnUiThread {
            processorRecommendationList.clear()
            processorRecommendationList.addAll(processors.sortedBy { it.distance }.distinctBy { it.userId }) // Remove duplicates
            adapterProcessor.notifyDataSetChanged()
            Log.d("Check", "UI Updated: ${processorRecommendationList.size} items added")
        }
    }

    private fun getMunicipality(address: String, callback: (String?) -> Unit) {
        val client = OkHttpClient()
        val url = "https://nominatim.openstreetmap.org/search?q=${URLEncoder.encode(address, "UTF-8")}&format=json&addressdetails=1"

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
                                !addressDetails?.optString("city").isNullOrEmpty() -> addressDetails?.optString("city")
                                !addressDetails?.optString("town").isNullOrEmpty() -> addressDetails?.optString("town")
                                !addressDetails?.optString("county").isNullOrEmpty() -> addressDetails?.optString("county")
                                else -> "Unknown Municipality"
                            }

                            // Log extracted values
                            Log.i("GetMunicipality", "Extracted - City: ${addressDetails?.optString("city")}, Town: ${addressDetails?.optString("town")}, County: ${addressDetails?.optString("county")}")
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
                    val distance = calculateDistance(landownerLat, landownerLon, surveyorLat, surveyorLon)

                    if (distance <= 50.0) {
                        pendingCallbacks++
                        convertCoordinatesToAddress(surveyorLat, surveyorLon) { address ->
                            tempList.add(
                                Recommendation(firstName, lastName, userType, address, profileUrl,
                                    distance, surveyorLat, surveyorLon, userId, ratings)
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
                Toast.makeText(requireContext(), "Error fetching surveyors", Toast.LENGTH_SHORT).show()
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
                    val distance = calculateDistance(landownerLat, landownerLon, surveyorLat, surveyorLon)

                    if (distance <= 50.0) {
                        pendingCallbacks++
                        convertCoordinatesToAddress(surveyorLat, surveyorLon) { address ->
                            tempList.add(
                                Recommendation(firstName, lastName, userType, address, profileUrl,
                                    distance, surveyorLat, surveyorLon, userId, ratings)
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
                Toast.makeText(requireContext(), "Error fetching surveyors", Toast.LENGTH_SHORT).show()
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
        val geocoder = OpenStreetMapGeocoder(requireContext())
        geocoder.getAddressFromCoordinates(lat, lon) { address ->
            callback(address ?: "Unknown Address")
        }
    }

    private fun updateAdapter(sortedList: List<Recommendation>) {
        requireActivity().runOnUiThread {
            recommendationList.clear()
            recommendationList.addAll(sortedList)
            adapter.notifyDataSetChanged()
        }
    }





































































    private fun filterNames(searchText: String) {
        displayedItems.clear()
        val uniqueNames = LinkedHashSet<String>()

        firestore.collection("users").get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val firstName = document.getString("first_name") ?: ""
                    val lastName = document.getString("last_name") ?: ""
                    val fullName = "$firstName $lastName".trim()

                    if (fullName.lowercase().startsWith(searchText) && uniqueNames.add(fullName)) {
                        val profilePicUrl = document.getString("profile_picture")
                        val userId = document.getString("uid") ?: ""

                        displayedItems.add(SearchItem(userId, profilePicUrl ?: "", fullName))

                        if (displayedItems.size >= 5) break
                    }
                }
                displayedAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                displayedItems.clear()
                displayedAdapter.notifyDataSetChanged()
            }
    }

    private fun loadRecentSearches() {
        val recentSearches = sharedPreferences.getStringSet("recent_searches", LinkedHashSet()) ?: emptySet()
        recentItems.clear()
        val uniqueItems = LinkedHashSet<String>()

        recentSearches.forEach { searchText ->
            val profilePicUrl = sharedPreferences.getString("${searchText}_profile_pic", "")
            val fullName = sharedPreferences.getString("${searchText}_full_name", "")

            if (!profilePicUrl.isNullOrEmpty() && !fullName.isNullOrEmpty()) {
                uniqueItems.add(searchText)
            }
        }

        uniqueItems.take(5).forEach { searchText ->
            val profilePicUrl = sharedPreferences.getString("${searchText}_profile_pic", "")
            val fullName = sharedPreferences.getString("${searchText}_full_name", "")
            recentItems.add(SearchItem("", profilePicUrl ?: "", fullName ?: ""))
        }

        Log.d("RecentSearches", "Recent searches: $recentSearches")


        recentAdapter.notifyDataSetChanged()
    }

    private fun removeRecentSearch(searchText: String) {
        val recentSearches = sharedPreferences.getStringSet("recent_searches", HashSet())?.toMutableSet()
        recentSearches?.remove(searchText)
        with(sharedPreferences.edit()) {
            putStringSet("recent_searches", recentSearches)
            remove("${searchText}_profile_pic")
            remove("${searchText}_full_name")
            apply()
        }
        recentItems.removeAll { it.name == searchText }
        recentAdapter.notifyDataSetChanged()
    }

    private var listenerRegistration: ListenerRegistration? = null



    override fun onPause() {
        super.onPause()
        listenerRegistration?.remove() // Detach the listener to prevent memory leaks
    }

    private fun navigateToProfileFragment(userId: String){

            val currentUser = FirebaseAuth.getInstance().currentUser
            val fragment = if (currentUser != null && userId == currentUser.uid) {
                FragmentProfile()
            } else {
                FragmentUserProfile()
            }
            val bundle = Bundle().apply { putString("userId", userId) }
            fragment.arguments = bundle

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()

            (requireActivity() as MainActivity).showBottomNavigationBar()

    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).showBottomNavigationBar()
        advanceSearch.visibility = View.GONE
        processorRecyclerView.visibility = View.GONE
        recyclerView.visibility = View.GONE
        recommendationTextView.visibility = View.GONE
        spinner.visibility = View.GONE
    }
}
