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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

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
    private lateinit var advanceSearch: Button
    private var currentUser: FirebaseUser? = null
    private var userType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        val auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        advanceSearch = view.findViewById(R.id.advanceSearch)

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
        sharedPreferences =
            requireActivity().getSharedPreferences("search_prefs", Context.MODE_PRIVATE)

        // Initialize lists and adapters
        recentItems = ArrayList()
        displayedItems = ArrayList()

        recentAdapter = RecentSearchAdapter(requireContext(), recentItems)
        displayedAdapter =
            DisplayedSearchAdapter(requireContext(), displayedItems, object : OnItemClickListener {
                override fun onItemClick(userId: String) {
                    fetchUserTypeAndNavigate(userId)
                }
            })

        recentListView.adapter = recentAdapter
        displayedListView.adapter = displayedAdapter

        // Fetch user type
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val userId = currentUser!!.uid

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userType = document.getString("user_type") ?: ""

                        // Set visibility of advanceSearch button based on userType
                        val isRestrictedUser = userType.equals("Surveyor", ignoreCase = true) ||
                                userType.equals("Processor", ignoreCase = true)

                        advanceSearch.visibility = if (isRestrictedUser) View.GONE else View.VISIBLE
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreError", "Error fetching user type: ${e.message}")
                }
        }

        // Initially hide the displayed ListView
        displayedListView.visibility = View.GONE

        loadRecentSearches()

        // Show keyboard when search field is focused
        searchView.requestFocus()
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)

        // Add TextWatcher for search input
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString().trim().lowercase()
                currentSearchText = searchText
                Log.d("Search", "Search text: $searchText")

                if (searchText.isNotEmpty()) {
                    Log.d("Search", "Showing Displayed ListView")
                    recentListView.visibility = View.GONE
                    recentHeadingTextView.visibility = View.GONE
                    displayedListView.visibility = View.VISIBLE
                    advanceSearch.visibility = View.GONE


                    filterNames(searchText)
                } else {
                    Log.d("Search", "Showing Recent ListView")
                    recentListView.visibility = View.VISIBLE
                    recentHeadingTextView.visibility = View.VISIBLE
                    displayedListView.visibility = View.GONE
                    advanceSearch.visibility = View.VISIBLE
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
            advanceSearch.visibility = View.VISIBLE
        }

        return view
    }

    private fun filterNames(searchText: String) {
        displayedItems.clear()
        val uniqueNames = LinkedHashSet<String>()
        val allowedUserTypes = when (userType) {
            "Landowner" -> listOf("Surveyor", "Processor")
            "Surveyor", "Processor" -> listOf("Landowner")
            else -> emptyList()
        }

        if (allowedUserTypes.isNotEmpty()) {
            firestore.collection("users")
                .whereIn("user_type", allowedUserTypes)
                .get()
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
    }

    private fun fetchUserTypeAndNavigate(userId: String?) {
        if (userId.isNullOrBlank()) {
            Log.e("SearchFragment", "fetchUserTypeAndNavigate: userId is null or empty.")
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userType = document.getString("user_type") ?: "defaultType"
                    navigateToProfileFragment(userId, userType)
                } else {
                    Log.e("SearchFragment", "User document does not exist for ID: $userId")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("SearchFragment", "Failed to fetch userType: $exception")
            }
    }

    private fun navigateToProfileFragment(userId: String, userType: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val fragment = when {
            currentUser != null && userId == currentUser.uid && userType == "Landowner" -> FragmentProfileLandowner()
            currentUser != null && userId == currentUser.uid && (userType == "Surveyor" || userType == "Processor") -> FragmentProfile()
            currentUser != null && userId != currentUser.uid && userType == "Landowner" -> FragmentUserProfileLandowner()
            else -> FragmentUserProfile()
        }
        val bundle = Bundle().apply { putString("userId", userId) }
        fragment.arguments = bundle

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        (requireActivity() as MainActivity).showBottomNavigationBar()
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
            recentItems.add(SearchItem(searchText, profilePicUrl ?: "", fullName ?: ""))
        }

        recentAdapter.notifyDataSetChanged()
    }

    private fun removeRecentSearch(searchText: String) {
        val recentSearches = sharedPreferences.getStringSet("recent_searches", LinkedHashSet()) ?: mutableSetOf()
        recentSearches.remove(searchText)

        sharedPreferences.edit().apply {
            putStringSet("recent_searches", recentSearches)
            apply()
        }

        loadRecentSearches()
    }



    private var listenerRegistration: ListenerRegistration? = null



    override fun onPause() {
        super.onPause()
        listenerRegistration?.remove() // Detach the listener to prevent memory leaks
    }



    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).showBottomNavigationBar()
        advanceSearch.visibility = View.GONE
    }
}



/*package com.example.terramaster

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
    private lateinit var advanceSearch: Button
    private var currentUser: FirebaseUser? = null
    private var userType: String? = null   // ✅ Avoids crashes

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        val auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        advanceSearch = view.findViewById(R.id.advanceSearch)

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
        sharedPreferences =
            requireActivity().getSharedPreferences("search_prefs", Context.MODE_PRIVATE)

        // Initialize lists and adapters
        recentItems = ArrayList()
        displayedItems = ArrayList()


        recentAdapter = RecentSearchAdapter(requireContext(), recentItems)

        displayedAdapter =
            DisplayedSearchAdapter(requireContext(), displayedItems, object : OnItemClickListener {
                override fun onItemClick(userId: String) {
                    fetchUserTypeAndNavigate(userId)
                }
            })

        recentListView.adapter = recentAdapter
        displayedListView.adapter = displayedAdapter
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

                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreError", "Error fetching user type: ${e.message}")
                    }
            } ?: Log.e("FirebaseAuth", "User is not logged in")

        }



        // Initially hide the displayed ListView
        displayedListView.visibility = View.GONE

        loadRecentSearches()

        // Show keyboard when search field is focused
        searchView.requestFocus()
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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

                    // Only hide these views if the user is restricted
                    if (isRestrictedUser) {
                        advanceSearch.visibility = View.GONE
                    } else {
                        advanceSearch.visibility = View.GONE
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

        return view
    }

    private fun fetchUserTypeAndNavigate(userId: String?) {
        if (userId.isNullOrBlank()) {
            Log.e("SearchFragment", "fetchUserTypeAndNavigate: userId is null or empty.")
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userType = document.getString("user_type") ?: "defaultType"
                    navigateToProfileFragment(userId, userType)
                } else {
                    Log.e("SearchFragment", "User document does not exist for ID: $userId")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("SearchFragment", "Failed to fetch userType: $exception")
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

    // Helper function to get the userType of a user based on their userId
    private fun getUserType(userId: String): String {
        var userType = ""
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                userType = document.getString("user_type") ?: ""
            }
        return userType
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
    }*/
/*
    private var listenerRegistration: ListenerRegistration? = null



    override fun onPause() {
        super.onPause()
        listenerRegistration?.remove() // Detach the listener to prevent memory leaks
    }

    private fun navigateToProfileFragment(userId: String, userType: String){

            val currentUser = FirebaseAuth.getInstance().currentUser
            val fragment = if (currentUser != null && userId == currentUser.uid && userType == "Landowner") {
                FragmentProfileLandowner()
            } else if(currentUser != null && userId == currentUser.uid && userType == "Surveyor" || currentUser != null && userId == currentUser.uid &&userType == "Processor"){
                FragmentProfile()
            } else if(currentUser != null && userId != currentUser.uid && userType == "Landowner"){
                FragmentUserProfileLandowner()
            }else{
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
    }
}*/
