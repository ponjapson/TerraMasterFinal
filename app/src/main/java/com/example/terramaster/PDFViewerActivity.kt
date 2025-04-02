package com.example.terramaster

import android.app.ProgressDialog
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class PDFViewerActivity : AppCompatActivity() {

    private lateinit var pdfView: PDFView
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdfviewer)

        pdfView = findViewById(R.id.pdfView)
        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading PDF...")
            setCancelable(false)
            show()
        }

        val pdfUrl = intent.getStringExtra("pdfUrl")
        if (pdfUrl != null) {
            downloadAndDisplayPDF(pdfUrl)
        } else {
            Log.e("PDF Viewer", "No PDF URL received")
            finish()
        }
    }

    private fun downloadAndDisplayPDF(pdfUrl: String) {
        Thread {
            try {
                val url = URL(pdfUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val inputStream = connection.inputStream
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "temp.pdf")
                val outputStream = FileOutputStream(file)

                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()

                runOnUiThread {
                    progressDialog.dismiss()
                    pdfView.fromFile(file)
                        .enableSwipe(true)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .load()
                }
            } catch (e: Exception) {
                Log.e("PDF Viewer", "Error downloading PDF: ${e.message}")
                runOnUiThread { progressDialog.dismiss() }
            }
        }.start()
    }
}
