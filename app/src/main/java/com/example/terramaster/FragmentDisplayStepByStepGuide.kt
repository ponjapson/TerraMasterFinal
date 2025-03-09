package com.example.terramaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class FragmentDisplayStepByStepGuide : Fragment() {

    private lateinit var tvGuideTitle: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var stepAdapter: StepAdapter
    private val stepList = mutableListOf<Step>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_display_stepbystep_guide, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvGuideTitle = view.findViewById(R.id.tvGuideTitle) // Display guide title
        recyclerView = view.findViewById(R.id.recyclerViewSteps)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())



        val guideId = arguments?.getString("guideId")
        guideId?.let {
            fetchGuideData(it)
        }
    }

    private fun fetchGuideData(guideId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("knowledge_guide").document(guideId).get()
            .addOnSuccessListener { document ->
                val guide = document.toObject(Guide::class.java)
                guide?.let {
                    tvGuideTitle.text = it.title // Set the guide title
                    stepList.clear()
                    stepList.addAll(it.steps)

                    stepAdapter = StepAdapter(stepList, it) { step, guideTitle ->
                        navigateToEditGuideFragment(guideId, step, guideTitle)
                    }
                    recyclerView.adapter = stepAdapter
                    stepAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun navigateToEditGuideFragment(guideId: String, step: Step, guideTitle: String) {
        val bundle = Bundle().apply {
            putString("guideId", guideId)         // Pass guide ID
            putString("guideTitle", guideTitle)   // Pass guide title
            putString("stepTitle", step.title)    // Pass step title
            putString("description", step.description) // Pass description
        }

        val editFragment = EditGuideFragment()
        editFragment.arguments = bundle

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, editFragment)
            .addToBackStack(null)
            .commit()
    }

}
