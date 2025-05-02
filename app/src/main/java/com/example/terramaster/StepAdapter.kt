package com.example.terramaster

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class StepAdapter(
    private val steps: MutableList<Step>,  private val guide: Guide, private val onEditClick: ((Step, String) -> Unit)? = null
) : RecyclerView.Adapter<StepAdapter.StepViewHolder>() {

    class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStepTitle: TextView = itemView.findViewById(R.id.tvStepTitle)
        val tvStepDescription: TextView = itemView.findViewById(R.id.tvStepDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val step = steps[position]
        holder.tvStepTitle.text = step.title
        holder.tvStepDescription.text = step.description
        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = firebaseAuth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid
            val firestore = FirebaseFirestore.getInstance()

            // Reference to the user's document in the 'users' collection
            val userRef = firestore.collection("users").document(userId)

            // Fetch the user data
            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val userType = document.getString("user_type") // Get the userType field

                    // Check if the userType is not "landowner"
                    if (userType != "Landowner") {
                        // Perform the long click action
                        holder.itemView.setOnLongClickListener {
                            showEditDialog(holder.itemView.context, step)
                            true
                        }
                    }
                }
            }
        }

    }

    override fun getItemCount(): Int = steps.size

    // Method to update the steps in the adapter and notify the RecyclerView of the change
    fun updateSteps(newSteps: List<Step>) {
        steps.clear() // Clear existing steps
        steps.addAll(newSteps) // Add new steps
        notifyDataSetChanged() // Notify adapter to refresh the list
    }

    private fun showEditDialog(context: Context, step: Step){
        val editDialog = LayoutInflater.from(context).inflate(R.layout.dialog_edit, null)
        val showDialog = AlertDialog.Builder(context)
            .setView(editDialog)
            .create()

        val editButton: Button = editDialog.findViewById(R.id.btnEdit)

        editButton.setOnClickListener {
            showDialog.dismiss()
            onEditClick?.invoke(step, guide.title) // Use ?.invoke() to avoid null reference issues
        }
        showDialog.show()
    }



}