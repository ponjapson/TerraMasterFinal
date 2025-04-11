import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.example.terramaster.FragmentJobs
import com.example.terramaster.MainActivity
import com.example.terramaster.R
import com.example.terramaster.RequestTabFragment
import com.github.gcacace.signaturepad.views.SignaturePad
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
  /*  private lateinit var signaturePad: SignaturePad
    private lateinit var saveButton: Button
    private lateinit var clearButton: Button*/
   // private lateinit var signatureImageView: ImageView


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

       /* signaturePad= view.findViewById(R.id.signature_pad)
        saveButton = view.findViewById(R.id.btnSaveSignature)
        clearButton = view.findViewById(R.id.btnClearSignature)*/

      /*  bookingId?.let {
            checkForPdfSignature(it) { isSignature ->
                if (isSignature && userType == "Processor") {
                    signaturePad.visibility = View.INVISIBLE
                    saveButton.visibility = View.INVISIBLE
                    clearButton.visibility = View.INVISIBLE
                } else if(!isSignature && userType == "Processor") {
                    signaturePad.visibility = View.VISIBLE
                    saveButton.visibility = View.VISIBLE
                    clearButton.visibility = View.VISIBLE
                }
            }
        }
*/


        if (!pdfUrl.isNullOrEmpty()) {
            Log.d("PDF", "Loading PDF directly from URL: $pdfUrl")
            /*if(userType == "Processor") {
                signaturePad.visibility = View.VISIBLE
                saveButton.visibility = View.VISIBLE
                clearButton.visibility = View.VISIBLE
            }else{
                signaturePad.visibility = View.INVISIBLE
                saveButton.visibility = View.INVISIBLE
                clearButton.visibility = View.INVISIBLE
            }*/
            GlobalScope.launch(Dispatchers.Main) {
                downloadPdfFromFirebase(pdfUrl)
            }
        } else if (!guideId.isNullOrEmpty()) {
            Log.d("PDF", "Fetching PDF URL using guideId: $guideId")
           /* signaturePad.visibility = View.INVISIBLE
            saveButton.visibility = View.INVISIBLE
            clearButton.visibility = View.INVISIBLE*/
            fetchPdfUrlFromFirestore(guideId)
        } else {
            Log.e("PDF", "No valid PDF source provided")
            progressBar.visibility = View.GONE
        }

       /* clearButton.setOnClickListener {
            signaturePad.clear()
            //signatureImageView.setImageBitmap(null)
        }

        saveButton.setOnClickListener {
            val signatureBitmap = signaturePad.signatureBitmap
            if (signatureBitmap != null) {
                // Compress and upload the signature in background thread
                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        val compressedData = compressBitmap(signatureBitmap)
                        // Upload to Firebase after compression
                        uploadSignatureToFirebase(compressedData, bookingId!!)
                    } catch (e: Exception) {
                        Log.e("Signature", "Error compressing signature", e)
                    }
                }
            }
        }
*/

        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        return view
    }

    private suspend fun compressBitmap(bitmap: Bitmap): ByteArray {
        return withContext(Dispatchers.IO) {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        }
    }
    private suspend fun uploadSignatureToFirebase(compressedData: ByteArray, bookingId: String) {
        val storageReference = FirebaseStorage.getInstance().reference
        val signatureRef = storageReference.child("signatures/processed_signature.png")

        withContext(Dispatchers.IO) {
            try {
                val uploadTask = signatureRef.putBytes(compressedData)
                uploadTask.await()

                // Retrieve the URL of the uploaded signature
                val downloadUrl = signatureRef.downloadUrl.await()
                Log.d("Signature", "Signature uploaded successfully. Download URL: $downloadUrl")

                // Update the booking with signature URL
                updateBookingWithSignature(downloadUrl.toString(), bookingId)

            } catch (e: Exception) {
                Log.e("Firebase", "Error uploading signature", e)
            }
        }
    }

    // Update booking with the signature URL in Firestore
    private fun updateBookingWithSignature(signatureUrl: String, bookingId: String) {
        val db = FirebaseFirestore.getInstance()

        // Assuming 'bookingId' is available and it's used to identify the booking document
        val bookingRef = db.collection("bookings").document(bookingId)

        // Create a mutable map to store the updates
        val updates = mutableMapOf<String, Any>(
            "signatureUrl" to signatureUrl,
            "signatureTimestamp" to System.currentTimeMillis(), // Optionally add a timestamp
            "status" to "verified",
            "stage" to "ongoing"
        )

        // Explicitly cast to the required type
        val mutableUpdates: MutableMap<String, Any> = updates

        // Perform the update
        bookingRef.update(mutableUpdates)
            .addOnSuccessListener {
                Log.d("Booking", "Booking updated with signature URL: $signatureUrl")

                // Navigate to RequestTabFragment if the update is successful
                navigateToRequestTabFragment()
            }
            .addOnFailureListener { e ->
                Log.e("Booking", "Error updating booking", e)
            }
    }
    fun checkForPdfSignature(bookingId: String, callback: (Boolean) -> Unit) {
        val bookingRef = FirebaseFirestore.getInstance().collection("bookings").document(bookingId)

        bookingRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val signatureUrl = document.getString("signatureUrl")
                    // Return true if signatureUrl is not null or empty
                    callback(!signatureUrl.isNullOrEmpty())
                } else {
                    callback(false) // Booking doesn't exist
                }
            }
            .addOnFailureListener { e ->
                Log.e("SignatureCheck", "Failed to check signature: ${e.message}")
                callback(false) // Error occurred
            }
    }

    // Method to navigate to RequestTabFragment
    private fun navigateToRequestTabFragment() {
        val fragment = FragmentJobs().apply {
            arguments = Bundle().apply {
                putInt("selectedTab", 1)  // Pass tab index (0 for RequestTab)
            }
        }

        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()


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
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
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
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}

