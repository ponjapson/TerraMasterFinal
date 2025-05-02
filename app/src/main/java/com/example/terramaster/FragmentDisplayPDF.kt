import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terramaster.FragmentBookingManagement
import com.example.terramaster.MainActivity
import com.example.terramaster.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
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
        (requireActivity() as MainActivity).hideBottomNavigationBar()
        progressBar.visibility = View.VISIBLE // Show ProgressBar initially

        pdfAdapter = PDFAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = pdfAdapter

        val guideId = arguments?.getString("guideId")
        val args = arguments
        val pdfUrl = args?.getString("pdfUrl")
        val userType = args?.getString("userType")
        val bookingId = args?.getString("bookingId")
        Log.d("PDF_FRAGMENT", "Received PDF URL: $pdfUrl, User Type: $userType") // Debugging log




        if (!pdfUrl.isNullOrEmpty()) {
            Log.d("PDF", "Loading PDF directly from URL: $pdfUrl")

            GlobalScope.launch(Dispatchers.Main) {
                downloadPdfFromFirebase(pdfUrl)
            }
        } else if (!guideId.isNullOrEmpty()) {
            Log.d("PDF", "Fetching PDF URL using guideId: $guideId")

            fetchPdfUrlFromFirestore(guideId)
        } else {
            Log.e("PDF", "No valid PDF source provided")
            progressBar.visibility = View.GONE
        }

       /* requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )*/
        return view
    }

    private fun fetchPdfUrlFromFirestore(guideId: String) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("knowledge_guide").document(guideId)

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val document = docRef.get().await()
                val pdfUrl = document.getString("pdfUrl")

                if (!pdfUrl.isNullOrEmpty()) {
                    Log.d("PDF", "PDF URL fetched: $pdfUrl")
                    downloadPdfFromFirebase(pdfUrl)
                } else {
                    Log.e("PDF", "No PDF URL found for guideId: $guideId")
                    progressBar.visibility = View.GONE
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

        withContext(Dispatchers.IO) {
            try {
                storageReference.getFile(file).await()
                openPdf(file)
            } catch (exception: Exception) {
                Log.e("PDF", "Error downloading PDF: ", exception)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private var fileDescriptor: ParcelFileDescriptor? = null

    private suspend fun openPdf(file: File) {
        withContext(Dispatchers.IO) {
            try {
                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)
                pageCount = pdfRenderer.pageCount

                val pdfPages = (0 until pageCount).map { pageIndex ->
                    renderPage(pageIndex)
                }

                withContext(Dispatchers.Main) {
                    pdfAdapter?.let {
                        it.pages = pdfPages
                        it.notifyDataSetChanged()
                    }
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

        val pageWidth = page.width
        val pageHeight = page.height

        // Get the target width: (RecyclerView is match_parent, so use screen width or imageView width)
        val displayMetrics = resources.displayMetrics
        val targetWidth = displayMetrics.widthPixels

        // Calculate height proportionally
        val targetHeight = (targetWidth.toFloat() * pageHeight / pageWidth).toInt()

        // Create a scaled bitmap
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::pdfRenderer.isInitialized) {
            pdfRenderer.close()
        }
    }

    override fun onResume() {
        super.onResume()
        /*requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )*/
    }
}

