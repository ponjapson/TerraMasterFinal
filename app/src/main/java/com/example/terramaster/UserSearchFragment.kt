package com.example.terramaster

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class UserSearchFragment : Fragment() {

    private lateinit var searchView: EditText
    private lateinit var recentListView: ListView
    private lateinit var displayedListView: ListView
    private lateinit var recentHeadingTextView: TextView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recentAdapter: RecentSearchAdapter
    private lateinit var displayedAdapter: MessageDisplayAdapter
    private lateinit var recentItems: ArrayList<SearchItem>
    private lateinit var displayedItems: ArrayList<SearchItem>
    private lateinit var sharedPreferences: SharedPreferences
    private var currentSearchText: String = ""
    private var profilePicUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_search_fragment, container, false)

        searchView = view.findViewById(R.id.searchView)
        recentListView = view.findViewById(R.id.recentListView)
        displayedListView = view.findViewById(R.id.displayedListView)
        recentHeadingTextView = view.findViewById(R.id.recentHeadingTextView)
        firestore = FirebaseFirestore.getInstance()
        sharedPreferences = requireContext().getSharedPreferences("search_prefs", Context.MODE_PRIVATE)

        // Initialize adapters
        recentItems = ArrayList()
        displayedItems = ArrayList()
        recentAdapter = RecentSearchAdapter(requireContext(), recentItems)
        displayedAdapter = MessageDisplayAdapter(requireContext(), displayedItems, object : ClickListener {
            override fun onItemClick(userId: String, fullName: String, profilePicUrl: String) {
                navigateToPrivateMessage(userId, fullName, profilePicUrl)
            }
        })

        // Set adapters to ListViews
        recentListView.adapter = recentAdapter
        displayedListView.adapter = displayedAdapter

        // Initially hide the displayed ListView
        displayedListView.visibility = View.GONE

        // Request focus on the EditText
        searchView.requestFocus()

        // Show the keyboard
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)

        // TextChangedListener for EditText
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString().trim().toLowerCase()
                currentSearchText = s.toString().trim().toLowerCase()
                if (searchText.isNotEmpty()) {
                    // User started typing, hide recent searches and show displayed results
                    recentListView.visibility = View.GONE
                    recentHeadingTextView.visibility = View.GONE
                    displayedListView.visibility = View.VISIBLE
                    filterNames(searchText)
                } else {
                    // User cleared search text, show recent searches and hide displayed results
                    recentListView.visibility = View.VISIBLE
                    recentHeadingTextView.visibility = View.VISIBLE
                    displayedListView.visibility = View.GONE
                    recentAdapter.notifyDataSetChanged() // Notify adapter about visibility change
                }
            }
        })

        recentListView.setOnItemLongClickListener { _, _, position, _ ->
            val searchTextToRemove = recentItems[position].name
            removeRecentSearch(searchTextToRemove)
            true // Indicate that the long click event is consumed
        }

        return view
    }

    private fun navigateToPrivateMessage(userId: String, fullName: String, profilePicUrl: String) {
        val bundle = Bundle().apply {
            putString("otherUserId", userId)
            putString("fullName", fullName)
            putString("profilePicUrl", profilePicUrl)
        }
        val fragment = FragmentPrivateMessage()
        fragment.arguments = bundle
        (requireActivity() as MainActivity).hideBottomNavigationBar()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun filterNames(searchText: String) {
        displayedItems.clear()

        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val firstName = document.getString("first_name") ?: ""
                    val lastName = document.getString("last_name") ?: ""
                    val fullName = "$firstName $lastName".trim() // Combine first and last name

                    // Check if the user is banned or suspended
                    val status = document.getString("status") ?: "active" // Default to "active"


                    // Check if the full name matches the search text
                    if (fullName.toLowerCase().startsWith(searchText.toLowerCase())) {
                        val profilePicUrl = document.getString("profile_picture") ?: ""
                        val userId = document.getString("uid") ?: ""
                        displayedItems.add(SearchItem(userId, profilePicUrl, fullName))

                        // Save recent search with profilePicUrl and fullName
                        saveRecentSearch(fullName, profilePicUrl, fullName)
                    }
                }
                displayedAdapter.notifyDataSetChanged() // Notify adapter about data change
            }
            .addOnFailureListener { exception ->
                // Handle failure
                displayedItems.clear() // Clear previous items
                displayedAdapter.notifyDataSetChanged() // Notify adapter about data change
            }
    }


    private fun saveRecentSearch(searchText: String, profilePicUrl: String, fullName: String) {
        val editor = sharedPreferences.edit()
        val recentSearches = sharedPreferences.getStringSet("recent_searches", HashSet())?.toMutableSet()
        recentSearches?.add(searchText)
        editor.putStringSet("recent_searches", recentSearches)
        editor.putString(searchText + "_profile_pic", profilePicUrl)
        editor.putString(searchText + "_full_name", fullName)
        editor.apply()
    }

    private fun removeRecentSearch(searchText: String) {
        val editor = sharedPreferences.edit()
        val recentSearches = sharedPreferences.getStringSet("recent_searches", HashSet())?.toMutableSet()
        recentSearches?.remove(searchText)
        editor.putStringSet("recent_searches", recentSearches)
        editor.remove(searchText + "_profile_pic")
        editor.remove(searchText + "_full_name")
        val result = editor.commit()

        if (result) {
            // Success
        } else {
            // Failure
        }

        recentItems.removeAll { it.name == searchText }
        recentAdapter.notifyDataSetChanged()
    }
}
