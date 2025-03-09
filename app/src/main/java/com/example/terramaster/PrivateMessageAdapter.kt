package com.example.terramaster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrivateMessageAdapter(private val messages: List<PrivateMessageSealed>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val SENT_MESSAGE_VIEW_TYPE = 1
    private val RECEIVED_MESSAGE_VIEW_TYPE = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            SENT_MESSAGE_VIEW_TYPE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_private_message_sender, parent, false)
                SentMessageViewHolder(view)
            }
            RECEIVED_MESSAGE_VIEW_TYPE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_private_message_receiver, parent, false)
                ReceivedMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder.itemViewType) {
            SENT_MESSAGE_VIEW_TYPE -> {
                val sentHolder = holder as SentMessageViewHolder
                sentHolder.bind(message as PrivateMessageSealed.SentMessage)
            }
            RECEIVED_MESSAGE_VIEW_TYPE -> {
                val receivedHolder = holder as ReceivedMessageViewHolder
                receivedHolder.bind(message as PrivateMessageSealed.ReceivedMessage)
            }
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is PrivateMessageSealed.SentMessage -> SENT_MESSAGE_VIEW_TYPE
            is PrivateMessageSealed.ReceivedMessage -> RECEIVED_MESSAGE_VIEW_TYPE
        }
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.textMessage)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestamp)

        fun bind(sentMessage: PrivateMessageSealed.SentMessage) {
            messageTextView.text = sentMessage.message.text
            timestampTextView.text = formatDate(sentMessage.message.timestamp) // Format timestamp
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.textMessageReceiver)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampreceiver)

        fun bind(receivedMessage: PrivateMessageSealed.ReceivedMessage) {
            messageTextView.text = receivedMessage.message.text
            timestampTextView.text = formatDate(receivedMessage.message.timestamp) // Format timestamp
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm", Locale.getDefault())
        val time = sdf.format(Date(timestamp))
        return time
    }
}
