package com.example.terramaster

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NotificationAdapter(
    private val context: Context,
    private val notifications: MutableList<Notification>,
    private val onClickListener: OnNotificationClickListener
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    interface OnNotificationClickListener {
        fun onNotificationClick(notification: Notification)
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notificationMessageTextView: TextView = itemView.findViewById(R.id.notificationMessageTextView)
        val notificationTimestampTextView: TextView = itemView.findViewById(R.id.notificationTimestampTextView)
        val senderProfileImageView: ImageView = itemView.findViewById(R.id.senderProfileImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        holder.notificationMessageTextView.text = notification.message
        holder.notificationTimestampTextView.text = notification.timestamp?.toDate()?.toString() ?: "N/A"

        // Load the sender's profile picture (if available)
        if (!notification.profilePicture.isNullOrEmpty()) {
            Glide.with(context)
                .load(notification.profilePicture)
                .placeholder(R.drawable.profile)
                .into(holder.senderProfileImageView)
        } else {
            holder.senderProfileImageView.setImageResource(R.drawable.profile)
        }

        // Handle item click based on the notification type
        holder.itemView.setOnClickListener {
            onClickListener.onNotificationClick(notification)
        }
    }

    override fun getItemCount(): Int = notifications.size
}