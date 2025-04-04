package com.example.terramaster

import android.graphics.Bitmap

data class PDFPageData(
    val bitmap: Bitmap? = null,  // Bitmap for the PDF page
    val signatureUrl: String? = null // URL for the signature (optional)
)

