package com.example.terramaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class SurveyorRecommendationAdapter(private val surveyors: List<Recommendation>, private val fragmentActivity: FragmentActivity,  private val onItemClick: (String) -> Unit) :
    RecyclerView.Adapter<SurveyorRecommendationAdapter.SurveyorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SurveyorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return SurveyorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SurveyorViewHolder, position: Int) {
        val surveyor = surveyors[position]
        holder.nameTextView.text = "${surveyor.firstName} ${surveyor.lastName}"
        holder.userTypeTextView.text = surveyor.userType
        holder.itemView.setOnClickListener {
            onItemClick(surveyor.userId) // Pass userId when clicked
        }
        holder.addressTextView.text = surveyor.address
        holder.addressTextView.setOnClickListener {
            val fragment = FragmentMap()

            // Pass latitude & longitude to the fragment
            val args = Bundle().apply {
                putDouble("latitude", surveyor.surveyorLat)
                putDouble("longitude", surveyor.surveyorLon)
            }
            fragment.arguments = args

            // Replace current fragment with MapFragment
            fragmentActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment) // Make sure R.id.fragment_container exists in your activity layout
                .addToBackStack(null) // Enables back navigation
                .commit()
        }

        Glide.with(holder.profileImageView.context)
            .load(surveyor.profileImage)
            .placeholder(R.drawable.profile) // Default image if none provided
            .into(holder.profileImageView)
    }

    override fun getItemCount(): Int = surveyors.size

    class SurveyorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val userTypeTextView: TextView = view.findViewById(R.id.userTypeTextView)
        val addressTextView: TextView = view.findViewById(R.id.addressTextView)
    }
}
