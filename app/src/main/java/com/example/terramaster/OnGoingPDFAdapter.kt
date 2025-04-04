package com.example.terramaster

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.terramaster.R

class OnGoingPDFAdapter(var pages: List<PDFPageData>) : RecyclerView.Adapter<OnGoingPDFAdapter.PdfViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pdf_page, parent, false)
        return PdfViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        val pdfPage = pages[position]

        // Check if it's the last page and has a signature URL
        if (pdfPage.signatureUrl != null && position == pages.size - 1) {
            // This is the last page, display the signature image
            holder.imageView.setImageBitmap(null)  // Clear previous bitmap (if any)
            Glide.with(holder.itemView.context)
                .load(pdfPage.signatureUrl)  // Load signature URL
                .into(holder.imageView)
        } else if (pdfPage.bitmap != null) {
            // Otherwise, display the PDF page bitmap
            holder.imageView.setImageBitmap(pdfPage.bitmap)
        }
    }

    override fun getItemCount(): Int {
        return pages.size
    }

    inner class PdfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.pdfPageImage)

        // No additional binding needed, handled in onBindViewHolder
    }
}
