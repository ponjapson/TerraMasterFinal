import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terramaster.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.tasks.await
import java.io.File

class FragmentDisplayPDF : Fragment() {
    private lateinit var pdfRenderer: PdfRenderer
    private var pageCount = 0
    private lateinit var recyclerView: RecyclerView
    private var pdfAdapter: PDFAdapter? = null
    private lateinit var progressBar: ProgressBar // Reference to ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_display_pdf, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewPdf)
        progressBar = view.findViewById(R.id.progressBar) // Initialize ProgressBar

        // Show the progress bar initially
        progressBar.visibility = View.VISIBLE

        // Get guideId from arguments
        val guideId = arguments?.getString("guideId") ?: return view

        // Initialize the adapter with an empty list
        pdfAdapter = PDFAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = pdfAdapter

        // Fetch the PDF URL from Firestore
        fetchPdfUrlFromFirestore(guideId)

        return view
    }

    private fun fetchPdfUrlFromFirestore(guideId: String) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("knowledge_guide").document(guideId)

        // Use coroutines to avoid blocking the main thread
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val document = docRef.get().await()
                val pdfUrl = document.getString("pdfUrl")

                if (pdfUrl.isNullOrEmpty()) {
                    Log.e("PDF", "No PDF URL found or it is empty for guide $guideId")
                    progressBar.visibility = View.GONE
                } else {
                    Log.d("PDF", "PDF URL fetched: $pdfUrl")
                    // Once the PDF URL is fetched, download and open the PDF
                    downloadPdfFromFirebase(pdfUrl)
                }
            } catch (e: Exception) {
                Log.e("PDF", "Error getting document: ", e)
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun downloadPdfFromFirebase(pdfUrl: String) {
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
        val file = File(requireContext().filesDir, "downloaded_pdf.pdf")

        // Download the file in the background thread
        withContext(Dispatchers.IO) {
            try {
                storageReference.getFile(file).await()
                // Once the file is downloaded, open the PDF
                openPdf(file)
            } catch (exception: Exception) {
                Log.e("PDF", "Error downloading PDF: ", exception)
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun openPdf(file: File) {
        // Open the PDF in the background thread
        withContext(Dispatchers.IO) {
            try {
                val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor)
                pageCount = pdfRenderer.pageCount

                // Generate pages from the PDF
                val pdfPages = (0 until pageCount).map { pageIndex ->
                    renderPage(pageIndex)
                }

                // Update the adapter with the pages
                withContext(Dispatchers.Main) {
                    pdfAdapter?.let {
                        it.pages = pdfPages
                        it.notifyDataSetChanged()
                    }

                    // Hide the ProgressBar once PDF is ready
                    progressBar.visibility = View.GONE
                }
            } catch (exception: Exception) {
                Log.e("PDF", "Error opening PDF: ", exception)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun renderPage(pageIndex: Int): Bitmap {
        val page = pdfRenderer.openPage(pageIndex)
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer.close()
    }
}
