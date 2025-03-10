package com.example.terramaster

import android.app.Activity
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

class FragmentBooking : Fragment() {

    private lateinit var scanButton: Button
    private lateinit var imageView: ImageView

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(data)

                scanningResult?.pages?.let { pages ->
                    if (pages.isNotEmpty()) {
                        val imageUri = pages[0].imageUri
                        displayScannedImage(imageUri)
                    }
                }

                scanningResult?.pdf?.let { pdf ->
                    val pdfUri = pdf.uri
                    val pageCount = pdf.pageCount
                    Toast.makeText(requireContext(), "PDF with $pageCount pages saved", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(requireContext(), "Scanning canceled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_booking, container, false)
        scanButton = view.findViewById(R.id.scanButton)
        imageView = view.findViewById(R.id.imageView)

        scanButton.setOnClickListener {
            launchScanner()
        }

        return view
    }

    private fun launchScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)  // Allow importing from gallery
            .setPageLimit(2)               // Max 2 pages
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        scanner.getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to start scanner: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun displayScannedImage(imageUri: Uri) {
        imageView.setImageURI(imageUri)
    }

    private fun convertToPdf(imageUri: Uri?) {
        if (imageUri == null) return

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        pdfDocument.finishPage(page)

        val file = File(requireContext().getExternalFilesDir(null), "scanned_document.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        Toast.makeText(requireContext(), "PDF saved at: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
