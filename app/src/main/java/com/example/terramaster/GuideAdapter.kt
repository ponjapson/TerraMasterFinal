package com.example.terramaster

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.util.logging.Filter

class GuideAdapter(private val context: Context, private var guideList: MutableList<Guide>, private val onGuideClick: (String, String) -> Unit) :
      RecyclerView.Adapter<GuideAdapter.GuideViewHolder>(){


    private var fullList: List<Guide> = guideList.toList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideViewHolder{
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_guide, parent, false)
        return GuideViewHolder(view)
    }


    override fun onBindViewHolder(holder: GuideViewHolder, position: Int) {
        val guide  = guideList[position]
        holder.tvGuideTitle.text = guide.title
        holder.itemView.setOnClickListener {
            onGuideClick(guide.knowledgeGuideId, guide.guideType)
        }
        holder.arrowIcon.setOnClickListener {
            onGuideClick(guide.knowledgeGuideId, guide.guideType)
        }

        holder.itemView.setOnLongClickListener {
            showDeleteDialog(position, guide.knowledgeGuideId)
            true
        }

    }
    override fun getItemCount(): Int = guideList.size

    class GuideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvGuideTitle: TextView = itemView.findViewById(R.id.tvGuideTitle)
        val arrowIcon: ImageButton = itemView.findViewById(R.id.arrowIcon)
    }

    private fun showDeleteDialog(position: Int, guideId: String){
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val deleteButton = dialogView.findViewById<Button>(R.id.btnDelete)

        deleteButton.setOnClickListener {
            showConfirmationDialog(position, guideId, dialog)
        }
        dialog.show()
    }

    private fun showConfirmationDialog(position: Int, guideId: String, parentDialog: AlertDialog){
        val confirmationDialog = AlertDialog.Builder(context)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this guide?")
            .setPositiveButton("Delete"){_,_ ->
                deleteItem(position, guideId)
                parentDialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()

        confirmationDialog.show()
    }

    private fun deleteItem(position: Int, guideId: String){
        val db = FirebaseFirestore.getInstance()

        db.collection("knowledge_guide").document(guideId)
            .delete()
            .addOnSuccessListener {
                guideList.removeAt(position)
                notifyItemRemoved(position)
                Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error deleting item", Toast.LENGTH_SHORT).show()
            }
    }

    fun setData(list: List<Guide>) {
        fullList = list // Update full list
        guideList = list.toMutableList()
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val searchText = query.lowercase(Locale.getDefault()).trim()
        guideList = if (searchText.isEmpty()) {
            fullList.toMutableList() // Restore the full list when search is empty
        } else {
            fullList.filter { it.title.lowercase(Locale.getDefault()).contains(searchText) }.toMutableList()
        }
        notifyDataSetChanged()
    }
}
