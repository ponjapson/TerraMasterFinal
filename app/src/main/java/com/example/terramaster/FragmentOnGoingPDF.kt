import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.terramaster.MainActivity
import com.example.terramaster.OnGoingPDFAdapter
import com.example.terramaster.PDFPageData
import com.example.terramaster.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.tasks.await
import java.io.File

class FragmentOnGoingPDF : Fragment() {
    private lateinit var pdfRenderer: PdfRenderer
    private var pageCount = 0
    private lateinit var recyclerView: RecyclerView
    private var pdfAdapter: OnGoingPDFAdapter? = null
    private lateinit var progressBar: ProgressBar
    private var fileDescriptor: ParcelFileDescriptor? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ongoing_pdf, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewPdf)
        progressBar = view.findViewById(R.id.progressBar)

        (requireActivity() as MainActivity).hideBottomNavigationBar()
        progressBar.visibility = View.VISIBLE // Show ProgressBar initially

        pdfAdapter = OnGoingPDFAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = pdfAdapter

        val guideId = arguments?.getString("guideId")
        val pdfUrl = arguments?.getString("pdfUrl")
        val bookingId = arguments?.getString("bookingId")

        Log.d("PDF_FRAGMENT", "Received PDF URL: $pdfUrl, Booking ID: $bookingId")

        if (!pdfUrl.isNullOrEmpty()) {
            Log.d("PDF", "Loading PDF directly from URL: $pdfUrl")
            GlobalScope.launch(Dispatchers.Main) {
                downloadPdfFromFirebase(pdfUrl, bookingId)
            }
        } else if (!guideId.isNullOrEmpty()) {
            Log.d("PDF", "Fetching PDF URL using guideId: $guideId")
            fetchPdfUrlFromFirestore(guideId, bookingId)
        } else {
            Log.e("PDF", "No valid PDF source provided")
            progressBar.visibility = View.GONE
        }

        return view
    }

    private fun fetchPdfUrlFromFirestore(guideId: String, bookingId: String?) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("knowledge_guide").document(guideId)

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val document = docRef.get().await()
                val pdfUrl = document.getString("pdfUrl")

                if (!pdfUrl.isNullOrEmpty()) {
                    Log.d("PDF", "PDF URL fetched: $pdfUrl")
                    downloadPdfFromFirebase(pdfUrl, bookingId)
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

    private suspend fun downloadPdfFromFirebase(pdfUrl: String, bookingId: String?) {
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
        val file = File(requireContext().filesDir, "downloaded_pdf.pdf")

        withContext(Dispatchers.IO) {
            try {
                storageReference.getFile(file).await()
                openPdf(file, bookingId)
            } catch (exception: Exception) {
                Log.e("PDF", "Error downloading PDF: ", exception)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun openPdf(file: File, bookingId: String?) {
        withContext(Dispatchers.IO) {
            try {
                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)
                pageCount = pdfRenderer.pageCount

                // Create a list of PDF pages
                var pdfPages = (0 until pageCount).map { pageIndex ->
                    val bitmap = renderPage(pageIndex)
                    PDFPageData(bitmap)
                }

                // Add the signature page at the end if a signature exists
                val signatureUrl = getSignatureUrlForBooking(bookingId)
                if (!signatureUrl.isNullOrEmpty()) {
                    pdfPages += PDFPageData(signatureUrl = signatureUrl)
                }

                // Update the RecyclerView with the pages and the signature page at the end
                withContext(Dispatchers.Main) {
                    pdfAdapter?.let {
                        it.pages = pdfPages
                        it.notifyDataSetChanged()
                        progressBar.visibility = View.GONE
                    }
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

    private fun getSignatureUrlForBooking(bookingId: String?): String? {
        if (bookingId.isNullOrEmpty()) {
            // Return null if the bookingId is empty or null
            return null
        }

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("bookings").document(bookingId)

        var signatureUrl: String? = null
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val document = docRef.get().await()
                signatureUrl = document.getString("signatureUrl")

                // If no signature URL found, we can handle it accordingly
                if (signatureUrl.isNullOrEmpty()) {
                    Log.e("PDF", "No signature found for bookingId: $bookingId")
                }

                // Proceed to open the PDF once the signature URL is fetched
                openPdfWithSignatureUrl(signatureUrl)

            } catch (e: Exception) {
                Log.e("PDF", "Error getting signature URL: ", e)
            }
        }
        return signatureUrl
    }

    private suspend fun openPdfWithSignatureUrl(signatureUrl: String?) {
        // The openPdf method should be modified to accept and handle the signatureUrl
        // Create a list of PDF pages
        var pdfPages = (0 until pageCount).map { pageIndex ->
            val bitmap = renderPage(pageIndex)
            PDFPageData(bitmap)
        }

        // Add the signature page at the end if a signature exists
        if (!signatureUrl.isNullOrEmpty()) {
            pdfPages += PDFPageData(signatureUrl = signatureUrl)
        }

        // Update the RecyclerView with the pages and the signature page at the end
        withContext(Dispatchers.Main) {
            pdfAdapter?.let {
                it.pages = pdfPages
                it.notifyDataSetChanged()
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::pdfRenderer.isInitialized) {
            pdfRenderer.close()
        }
    }
}
