package com.example.terramaster

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AddGuideDialogFragment : Fragment() {
    private lateinit var etGuideTitle: EditText
    private lateinit var etStepTitle: EditText
    private lateinit var etStepDescription: EditText
    private lateinit var btnAddStep: Button
    private lateinit var btnSaveGuide: Button
    private lateinit var rvSteps: RecyclerView
    private lateinit var stepAdapter: StepAdapter
    private lateinit var btnModeGuide: Button
    private lateinit var btnModePdf: Button
    private lateinit var guideSection: LinearLayout
    private lateinit var pdfSection: LinearLayout
    private lateinit var btnUploadPdf: Button
    private lateinit var tvPdfFileName: TextView

    private var pdfUri: Uri? = null

    private val stepList = mutableListOf<Step>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_guide, container, false)

        etGuideTitle = view.findViewById(R.id.etGuideTitle)
        etStepTitle = view.findViewById(R.id.etStepTitle)
        etStepDescription = view.findViewById(R.id.etStepDescription)
        btnAddStep = view.findViewById(R.id.btnAddStep)
        btnSaveGuide = view.findViewById(R.id.btnSaveGuide)
        rvSteps = view.findViewById(R.id.rvSteps)
        btnModeGuide = view.findViewById(R.id.btnModeGuide)
        btnModePdf = view.findViewById(R.id.btnModePdf)
        pdfSection = view.findViewById(R.id.pdfSection)
        guideSection = view.findViewById(R.id.guideSection)
        btnUploadPdf = view.findViewById(R.id.btnUploadPdf)
        tvPdfFileName = view.findViewById(R.id.tvSelectedPdf)

        // Pass the necessary parameters to the adapter
        val guide = Guide(knowledgeGuideId = "", title = "", steps = stepList, guideType = "StepByStep") // Create a default or valid Guide object
        stepAdapter = StepAdapter(stepList, guide)

        rvSteps.layoutManager = LinearLayoutManager(requireContext())
        rvSteps.adapter = stepAdapter

        btnAddStep.setOnClickListener {
            AddStep()
        }

        btnSaveGuide.setOnClickListener {
            val guideTitle = etGuideTitle.text.toString().trim()

            if (guideTitle.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a guide title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (stepList.isNotEmpty()) {
                // Save Guide with Steps (if steps exist)
                saveGuideToFirestore()
            } else if (pdfUri != null) {
                // Save PDF Guide (if only a PDF is selected)
                saveGuidePDFToFirestore()
            } else {
                // Show error if neither steps nor PDF are provided
                Toast.makeText(requireContext(), "Please add at least one step or upload a PDF", Toast.LENGTH_SHORT).show()
            }
        }

        btnModeGuide.setOnClickListener {
            guideSection.visibility = View.VISIBLE
            pdfSection.visibility = View.GONE
        }

        btnModePdf.setOnClickListener {
            guideSection.visibility = View.GONE
            pdfSection.visibility = View.VISIBLE
        }

        btnUploadPdf.setOnClickListener { selectPdf() }

        return view
    }

    private fun AddStep() {
        val etStepTitle1 = etStepTitle.text.toString().trim()
        val etStepDescription1 = etStepDescription.text.toString().trim()

        if (etStepTitle1.isNotEmpty() && etStepDescription1.isNotEmpty()) {
            stepList.add(Step(etStepTitle1, etStepDescription1))
            stepAdapter.notifyDataSetChanged()
            etStepTitle.text.clear()
            etStepDescription.text.clear()
        } else {
            Toast.makeText(requireContext(), "Please enter step details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveGuideToFirestore() {
        val guideTitle = etGuideTitle.text.toString().trim()
        val guideType = "StepByStep"

        if (guideTitle.isNotEmpty() && stepList.isNotEmpty()) {
            val guide = Guide(knowledgeGuideId = "", title = guideTitle, steps = stepList, guideType = "StepByStep")

            val db = FirebaseFirestore.getInstance()
            db.collection("knowledge_guide").add(guide).addOnSuccessListener { documentReference ->
                val guideId = documentReference.id
                documentReference.update("knowledgeGuideId", guideId).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Guide saved successfully!", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update guide ID", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save guide", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Please enter a guide title and at least one step", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectPdf() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), PDF_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PDF_PICK_CODE && resultCode == Activity.RESULT_OK) {
            pdfUri = data?.data
            if (pdfUri != null) {
                val pdfName = getFileName(pdfUri!!)
                tvPdfFileName.text = pdfName  // Update the TextView with the file name
                Toast.makeText(requireContext(), "PDF Selected: $pdfName", Toast.LENGTH_SHORT).show()
            } else {
                tvPdfFileName.text = "No PDF selected"
            }
        }
    }

    // Function to get file name from Uri
    private fun getFileName(uri: Uri): String {
        var name = "Unknown"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    companion object {
        private const val PDF_PICK_CODE = 1001 // Fixes the unresolved reference error
    }

    private fun saveGuidePDFToFirestore() {
        val guideTitle = etGuideTitle.text.toString().trim()

        if (guideTitle.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            val guideData = hashMapOf(
                "title" to guideTitle,
                "pdfUrl" to "",
                "guideType" to "PDF"
            )

            db.collection("knowledge_guide").add(guideData)
                .addOnSuccessListener { documentReference ->
                    val guideId = documentReference.id
                    documentReference.update("knowledgeGuideId", guideId)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Guide title saved!", Toast.LENGTH_SHORT).show()

                            // Upload PDF if available
                            if (pdfUri != null) {
                                uploadPdfToStorage(guideId, guideTitle)
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to update guide ID", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to save guide", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Please enter a guide title", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadPdfToStorage(guideId: String, guideTitle: String) {
        if (pdfUri != null) {
            val storageRef: StorageReference = FirebaseStorage.getInstance().reference
            val pdfRef = storageRef.child("pdfs/${guideTitle}.pdf") // Save with title as filename

            pdfRef.putFile(pdfUri!!)
                .addOnSuccessListener {
                    pdfRef.downloadUrl.addOnSuccessListener { uri ->
                        savePdfUrlToFirestore(guideId, guideTitle, uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to upload PDF", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun savePdfUrlToFirestore(guideId: String, guideTitle: String, pdfUrl: String) {
        val db = FirebaseFirestore.getInstance()
        val updateData = mapOf(
            "title" to guideTitle,
            "pdfUrl" to pdfUrl
        )

        db.collection("knowledge_guide").document(guideId).update(updateData)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "PDF uploaded successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                if (isAdded) { //
                    Toast.makeText(requireContext(), "Failed to save PDF URL", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).showBottomNavigationBar()
    }
}
