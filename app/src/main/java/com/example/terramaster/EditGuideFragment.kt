package com.example.terramaster


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class EditGuideFragment : Fragment() {

    private lateinit var etGuideTitle: EditText
    private lateinit var etStepTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnSave: Button

    private var guideId: String? = null
    private var guideTitle: String? = null
    private var stepTitle: String? = null
    private var description: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.edit_guide_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        // Initialize UI elements
        etGuideTitle = view.findViewById(R.id.etGuideTitle)
        etStepTitle = view.findViewById(R.id.etStepTitle)
        etDescription = view.findViewById(R.id.etDescription)
        btnSave = view.findViewById(R.id.btnSave)

        // Retrieve passed data from Bundle
        arguments?.let {
            guideId = it.getString("guideId")
            guideTitle = it.getString("guideTitle")
            stepTitle = it.getString("stepTitle")
            description = it.getString("description")

            // Populate EditText fields
            etGuideTitle.setText(guideTitle)
            etStepTitle.setText(stepTitle)
            etDescription.setText(description)
        }

        // Save button click listener
        btnSave.setOnClickListener {
            updateGuideData()
        }
    }

    private fun updateGuideData() {
        val updatedGuideTitle = etGuideTitle.text.toString().trim()
        val updatedStepTitle = etStepTitle.text.toString().trim()
        val updatedDescription = etDescription.text.toString().trim()

        if (guideId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Error: Guide ID is missing!", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val guideRef = db.collection("knowledge_guide").document(guideId!!)

        // Fetch the guide and update only the relevant step
        guideRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val guide = document.toObject(Guide::class.java)
                guide?.let {
                    val updatedSteps = it.steps.map { step ->
                        if (step.title == stepTitle) {
                            Step(updatedStepTitle, updatedDescription) // Update step
                        } else {
                            step // Keep other steps unchanged
                        }
                    }

                    // Update the Firestore document
                    guideRef.update("title", updatedGuideTitle, "steps", updatedSteps)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Guide updated successfully!", Toast.LENGTH_SHORT).show()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Update failed!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }
}
