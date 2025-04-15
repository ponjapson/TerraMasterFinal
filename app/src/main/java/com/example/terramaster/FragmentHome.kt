package com.example.terramaster

import FragmentDisplayPDF
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terramaster.databinding.ActivityBookingBinding.inflate
import com.google.android.play.integrity.internal.s
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects

class FragmentHome: Fragment() {
    private lateinit var rvGuide: RecyclerView
    private lateinit var guideAdapter: GuideAdapter
    private lateinit var guideAdapterSurveyor: GuideAdapter
    private val guideList = mutableListOf<Guide>()
    private val guideListProcessor = mutableListOf<Guide>()
    private lateinit var etSearch: EditText
    private lateinit var recyclerViewKnowledgeSurveyor: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (requireActivity() as MainActivity).setSupportActionBar(toolbar)

        // Enable options menu in Fragment
        setHasOptionsMenu(true)


        rvGuide = view.findViewById(R.id.recyclerViewKnowledge)
        etSearch = view.findViewById(R.id.etSearch)
        recyclerViewKnowledgeSurveyor = view.findViewById(R.id.recyclerViewKnowledgeSurveyor)

        guideAdapter = GuideAdapter(requireContext(), guideList) { guideId, guideType ->
            navigateToGuide(guideId, guideType)
        }
        guideAdapterSurveyor = GuideAdapter(requireContext(), guideListProcessor) { guideId, guideType ->
            navigateToGuide(guideId, guideType)
        }

        rvGuide.adapter = guideAdapter
        rvGuide.layoutManager = LinearLayoutManager(requireContext())

        recyclerViewKnowledgeSurveyor.adapter = guideAdapterSurveyor
        recyclerViewKnowledgeSurveyor.layoutManager = LinearLayoutManager(requireContext())


        loadGuidesFromFirestore()
        loadGuidesFromFirestoreSurveyor()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                guideAdapter.filter(s.toString())
                guideAdapterSurveyor.filter(s.toString())
            }
        })



        return view
    }

    private fun navigateToGuide(guideId: String, guideType: String) {
        val bundle = Bundle().apply {
            putString("guideId", guideId)
        }
        Log.e("PDF", "No PDF URL found or it is empty for guide $guideId")
        val fragment = if (guideType == "StepByStep") {
            FragmentDisplayStepByStepGuide()

        } else {
            FragmentDisplayPDF()
        }

        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }


    private fun loadGuidesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("knowledge_guide")
            .whereEqualTo("userType", "Processor") // 👈 Filter only Processor guides
            .get()
            .addOnSuccessListener { documents ->
                guideList.clear()
                for (document in documents) {
                    val id = document.id
                    val title = document.getString("title") ?: ""
                    val guideType = document.getString("guideType") ?: ""

                    if (guideType.isNotEmpty()) {
                        guideList.add(Guide(id, title, mutableListOf(), guideType))
                    }
                }
                guideAdapter.setData(guideList)
                guideAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error loading guides: ", exception)
            }
    }

    private fun loadGuidesFromFirestoreSurveyor() {
        val db = FirebaseFirestore.getInstance()
        db.collection("knowledge_guide")
            .whereEqualTo("userType", "Surveyor") // 👈 Filter only Processor guides
            .get()
            .addOnSuccessListener { documents ->
                guideListProcessor.clear()
                for (document in documents) {
                    val id = document.id
                    val title = document.getString("title") ?: ""
                    val guideType = document.getString("guideType") ?: ""

                    if (guideType.isNotEmpty()) {
                        guideListProcessor.add(Guide(id, title, mutableListOf(), guideType))
                    }
                }
                guideAdapterSurveyor.setData(guideListProcessor)
                guideAdapterSurveyor.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error loading guides: ", exception)
            }
    }




    private var menuItem: MenuItem? = null

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.knowledge_guide_tool_bar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)

        menuItem = menu.findItem(R.id.action_add) // Store reference to MenuItem

        updateMenuVisibility() // Call function to handle visibility logic
    }

    private fun updateMenuVisibility() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userType = document.getString("user_type") ?: ""

                        // Ensure menuItem is not null before modifying
                        menuItem?.isVisible = userType.equals("Processor", ignoreCase = true) || userType.equals("Surveyor", ignoreCase = true)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreError", "Error fetching user type: ${e.message}")
                }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                // Navigate using FragmentTransaction
                val fragment =  AddGuideDialogFragment()
                val transaction = requireActivity().supportFragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container, fragment)
                transaction.addToBackStack(null)
                transaction.commit()

                // Show bottom navigation bar (if needed)
                (requireActivity() as MainActivity).showBottomNavigationBar()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).showBottomNavigationBar()
    }
}