package com.example.terramaster

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Locale

class SchedulesAdapter(
    private var schedules: List<Schedules>, private val context: Context
) : RecyclerView.Adapter<SchedulesAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val stepProgressLayout: LinearLayout = itemView.findViewById(R.id.stepProgressLayout)
        val stepProgressLayoutProcessor: LinearLayout = itemView.findViewById(R.id.stepProgressLayoutProcessor)
        val startDateTime: TextView = itemView.findViewById(R.id.startDateTime)
        val step1: View = itemView.findViewById(R.id.step1)
        val step2: View = itemView.findViewById(R.id.step2)
        val step3: View = itemView.findViewById(R.id.step3)
        val step4: View = itemView.findViewById(R.id.step4)
        val step5: View = itemView.findViewById(R.id.step5)

        val step1Processor: View = itemView.findViewById(R.id.step1Processor)
        val step2Processor: View = itemView.findViewById(R.id.step2Processor)
        val step3Processor: View = itemView.findViewById(R.id.step3Processor)
        val step4Processor: View = itemView.findViewById(R.id.step4Processor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.schedules_item, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = schedules[position]

        updateStepColor(schedule.documentStatus, holder, position)
        updateStepColorProcessor(schedule.documentStatus, holder, position)

        holder.userName.text = schedule.userName

        if (!schedule.profileImageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(schedule.profileImageUrl)
                .into(holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_pic)
        }

        holder.startDateTime.text = "Start: ${formatTimestamp(schedule.startDateTime)}"
        Log.d("SchedulesAdapter", "User: ${schedule.userName}, isProcessor: ${schedule.isProcessor}")
        // Handle progress layout visibility based on user type
        if (schedule.isProcessor) {
            holder.stepProgressLayout.visibility = View.GONE
            holder.stepProgressLayoutProcessor.visibility = View.VISIBLE
        } else {
            holder.stepProgressLayout.visibility = View.VISIBLE
            holder.stepProgressLayoutProcessor.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = schedules.size

    private fun formatTimestamp(timestamp: Timestamp?): String {
        return timestamp?.toDate()?.let {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(it)
        } ?: "N/A"
    }

    private fun updateStepColor(status: String, holder: SchedulesAdapter.ScheduleViewHolder, position: Int) {
        val defaultColor = ContextCompat.getColor(context, R.color.DarkYellow)
        val activeColor = ContextCompat.getColor(context, R.color.YellowGreen)

        // Reset all to default color first
        holder.step1.setBackgroundColor(defaultColor)
        holder.step2.setBackgroundColor(defaultColor)
        holder.step3.setBackgroundColor(defaultColor)
        holder.step4.setBackgroundColor(defaultColor)
        holder.step5.setBackgroundColor(defaultColor)

        // Set progress based on current status
        when (status) {
            "Prepare Blueprint" -> {
                holder.step1.setBackgroundColor(activeColor)
            }

            "Submit Blueprint" -> {
                holder.step1.setBackgroundColor(activeColor)
                holder.step2.setBackgroundColor(activeColor)
            }

            "Follow-up Approval" -> {
                holder.step1.setBackgroundColor(activeColor)
                holder.step2.setBackgroundColor(activeColor)
                holder.step3.setBackgroundColor(activeColor)
            }

            "Ready to Claim" -> {
                holder.step1.setBackgroundColor(activeColor)
                holder.step2.setBackgroundColor(activeColor)
                holder.step3.setBackgroundColor(activeColor)
                holder.step4.setBackgroundColor(activeColor)
            }

            "Completed" -> {
                // Update all steps to active color
                holder.step1.setBackgroundColor(activeColor)
                holder.step2.setBackgroundColor(activeColor)
                holder.step3.setBackgroundColor(activeColor)
                holder.step4.setBackgroundColor(activeColor)
                holder.step5.setBackgroundColor(activeColor)
            }
        }
    }
    private fun updateStepColorProcessor(status: String, holder: SchedulesAdapter.ScheduleViewHolder, position: Int) {
        val defaultColor = ContextCompat.getColor(context, R.color.DarkYellow)
        val activeColor = ContextCompat.getColor(context, R.color.YellowGreen)

        // Reset all to default color first
        holder.step1Processor.setBackgroundColor(defaultColor)
        holder.step2Processor.setBackgroundColor(defaultColor)
        holder.step3Processor.setBackgroundColor(defaultColor)
        holder.step4Processor.setBackgroundColor(defaultColor)

        // Set progress based on current status
        when (status) {
            "Prepare the Tax Declaration" -> {
                holder.step1Processor.setBackgroundColor(activeColor)
            }

            "Approval Department Head" -> {
                holder.step1Processor.setBackgroundColor(activeColor)
                holder.step2Processor.setBackgroundColor(activeColor)
            }

            "Ready to Claim" -> {
                holder.step1Processor.setBackgroundColor(activeColor)
                holder.step2Processor.setBackgroundColor(activeColor)
                holder.step3Processor.setBackgroundColor(activeColor)
            }

            "Completed" -> {
                // Update all steps to active color
                holder.step1Processor.setBackgroundColor(activeColor)
                holder.step2Processor.setBackgroundColor(activeColor)
                holder.step3Processor.setBackgroundColor(activeColor)
                holder.step4Processor.setBackgroundColor(activeColor)


            }
        }
    }

    fun updateData(newSchedules: List<Schedules>) {
        schedules = newSchedules
        notifyDataSetChanged()
    }

}


