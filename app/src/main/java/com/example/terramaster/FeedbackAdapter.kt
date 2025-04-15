package com.example.terramaster

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FeedbackAdapter(private val feedbackList: List<Feedback>) : RecyclerView.Adapter<FeedbackAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageProfile: ImageView = view.findViewById(R.id.imageProfile)
        val textUsername: TextView = view.findViewById(R.id.textUsername)
        val textComment: TextView = view.findViewById(R.id.textComment)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.feedback_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feedback = feedbackList[position]

        Log.e("Name", feedback.first_name)
        Log.e("Name", feedback.last_name)
        // Set name
        holder.textUsername.text = "${feedback.first_name} ${feedback.last_name}"

        // Set comment and rating
        holder.textComment.text = feedback.feedback
        holder.ratingBar.rating = feedback.rating

        // Load profile picture using Glide
        Glide.with(holder.imageProfile.context)
            .load(feedback.profile_picture)
            .placeholder(R.drawable.profilefree) // Default image
            .into(holder.imageProfile)
    }

    override fun getItemCount() = feedbackList.size
}
