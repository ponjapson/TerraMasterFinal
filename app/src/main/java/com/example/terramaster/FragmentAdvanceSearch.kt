package com.example.terramaster

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terramaster.Suggested
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder

class FragmentAdvanceSearch : Fragment() {
    private lateinit var searchButton: ImageButton
    private lateinit var searchView: EditText
    private lateinit var locationInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SuggestionAdapter
    private val suggestionList = mutableListOf<Suggested>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_advance_search, container, false)

        searchButton = view.findViewById(R.id.search_button)
        searchView = view.findViewById(R.id.searchView)
        locationInput = view.findViewById(R.id.location_input)
        recyclerView = view.findViewById(R.id.suggestions_recycler_view)

        adapter = SuggestionAdapter(suggestionList, requireActivity()) { userId ->
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

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        searchButton.setOnClickListener {
            val location = locationInput.text.toString().trim()
            val searchQuery = searchView.text.toString().trim()

            when {
                searchQuery.equals("Surveyor", ignoreCase = true) -> {
                    if (location.isNotEmpty()) {
                        convertLocationToCoordinates(location) { lat, lon ->
                            fetchNearestSurveyors(searchQuery, lat, lon)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Please enter a location", Toast.LENGTH_SHORT).show()
                    }
                }

                searchQuery.equals("Processor", ignoreCase = true) -> {
                    if (location.isNotEmpty()) {
                        getMunicipality(location) { municipality ->
                            requireActivity().runOnUiThread {
                                if (municipality != null) {
                                    fetchProcessorsByMunicipality(municipality)
                                } else {
                                    Toast.makeText(requireContext(), "Failed to extract municipality", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                    } else {
                        Toast.makeText(requireContext(), "Please enter a location", Toast.LENGTH_SHORT).show()
                    }
                }

                else -> {
                    Toast.makeText(requireContext(), "Invalid search type", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun fetchNearestSurveyors(query: String, lat: Double, lon: Double) {
        firestore.collection("users")
            .whereEqualTo("user_type", "Surveyor")
            .get()
            .addOnSuccessListener { documents ->
                suggestionList.clear()
                val tempList = mutableListOf<Suggested>()
                var pendingRequests = 0  // Counter to track pending geocode requests

                if (documents.isEmpty) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "No surveyors found", Toast.LENGTH_SHORT).show()
                    }
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val firstName = doc.getString("first_name") ?: ""
                    val lastName = doc.getString("last_name") ?: ""
                    val profileUrl = doc.getString("profile_picture") ?: ""
                    val userType = doc.getString("user_type") ?: ""
                    val surveyorLat = doc.getDouble("latitude") ?: 0.0
                    val surveyorLon = doc.getDouble("longitude") ?: 0.0
                    val userId = doc.getString("uid") ?: ""
                    val distance = calculateDistance(lat, lon, surveyorLat, surveyorLon)
                    val ratings = doc.getDouble("ratings") ?: 0.0

                    if (distance <= 50.0) {
                        pendingRequests++  // Increase counter for each geocode request
                        convertCoordinatesToAddress(surveyorLat, surveyorLon) { address ->
                            val surveyor = Suggested(
                                firstName, lastName, userType, address, profileUrl, distance, surveyorLat, surveyorLon, userId, ratings
                            )
                            tempList.add(surveyor)

                            pendingRequests--  // Decrease counter when request completes
                            if (pendingRequests == 0) {
                                requireActivity().runOnUiThread {
                                    suggestionList.clear()
                                    suggestionList.addAll(tempList.sortedBy { it.distance })
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }

                if (pendingRequests == 0) {
                    // If no requests were made, update UI immediately
                    requireActivity().runOnUiThread {
                        suggestionList.clear()
                        suggestionList.addAll(tempList.sortedBy { it.distance })
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            .addOnFailureListener {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Error fetching surveyors", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchProcessorsByMunicipality(municipality: String) {
        val municipalityLowerCase = municipality.trim().lowercase()

        firestore.collection("users")
            .whereEqualTo("user_type", "Processor")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "No processors found in $municipality", Toast.LENGTH_SHORT).show()
                    }
                    return@addOnSuccessListener
                }

                suggestionList.clear()
                val tempList = mutableListOf<Suggested>()
                var pendingRequests = 0  // Counter to track geocode requests

                for (doc in documents) {
                    val dbCity = doc.getString("City")?.trim()?.lowercase() ?: ""

                    if (dbCity.equals(municipalityLowerCase, ignoreCase = true)) {
                        val firstName = doc.getString("first_name") ?: ""
                        val lastName = doc.getString("last_name") ?: ""
                        val profileUrl = doc.getString("profile_picture") ?: ""
                        val userType = doc.getString("user_type") ?: ""
                        val processorLat = doc.getDouble("latitude") ?: 0.0
                        val processorLon = doc.getDouble("longitude") ?: 0.0
                        val userId = doc.getString("uid") ?: ""
                        val ratings = doc.getDouble("ratings") ?: 0.0

                        pendingRequests++  // Increase counter for each geocode request

                        convertCoordinatesToAddress(processorLat, processorLon) { address ->
                            val processor = Suggested(
                                firstName, lastName, userType, address, profileUrl, 0.0, processorLat, processorLon, userId, ratings
                            )
                            tempList.add(processor)

                            pendingRequests--  // Decrease counter when request completes
                            if (pendingRequests == 0) {
                                requireActivity().runOnUiThread {
                                    suggestionList.clear()
                                    suggestionList.addAll(tempList.sortedBy { it.distance })
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }

                if (pendingRequests == 0) {
                    // If no requests were made, update UI immediately
                    requireActivity().runOnUiThread {
                        suggestionList.clear()
                        suggestionList.addAll(tempList.sortedBy { it.distance })
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            .addOnFailureListener {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Error fetching processors", Toast.LENGTH_SHORT).show()
                }
            }
    }



    private fun getMunicipality(address: String, callback: (String?) -> Unit) {
        val client = OkHttpClient()
        val formattedAddress = "$address, Cebu, Philippines"
        val url = "https://nominatim.openstreetmap.org/search?q=${URLEncoder.encode(formattedAddress, "UTF-8")}&format=json&addressdetails=1"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GetMunicipality", "Failed to fetch municipality: ${e.message}", e)
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to fetch municipality", Toast.LENGTH_SHORT).show()
                }
                callback(null) // Return null
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string()

                    if (responseBody.isNullOrEmpty()) {
                        Log.w("GetMunicipality", "No municipality found")
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "No municipality found", Toast.LENGTH_SHORT).show()
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

                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "Municipality: $municipality", Toast.LENGTH_SHORT).show()
                            }

                            callback(municipality)
                        } else {
                            Log.w("GetMunicipality", "Empty JSON response")
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "Empty response from server", Toast.LENGTH_SHORT).show()
                            }
                            callback(null)
                        }
                    } catch (e: Exception) {
                        Log.e("GetMunicipality", "Error parsing response: ${e.message}", e)
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Error parsing response", Toast.LENGTH_SHORT).show()
                        }
                        callback(null)
                    }
                }
            }
        })
    }

    private fun convertLocationToCoordinates(locationName: String, callback: (Double, Double) -> Unit) {
        val geocoder = OpenStreetMapGeocoder(requireContext())
        geocoder.getCoordinatesFromAddress(locationName) { coordinates ->
            if (coordinates != null) {
                callback(coordinates.latitude, coordinates.longitude)
            } else {
                Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertCoordinatesToAddress(lat: Double, lon: Double, callback: (String) -> Unit) {
        val geocoder = OpenStreetMapGeocoder(requireContext())
        geocoder.getAddressFromCoordinates(lat, lon) { address ->
            callback(address ?: "Unknown Address")
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
}
