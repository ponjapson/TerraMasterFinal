package com.example.terramaster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

class ChatAdapter(private val chats: List<Chat>, private val listener: OnClickListener) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    interface OnClickListener {
        fun onItemClick(chat: Chat)
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarImageView: CircleImageView = itemView.findViewById(R.id.avatarImageView)
        private val participantsTextView: TextView = itemView.findViewById(R.id.senderTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(chats[position])
                }
            }
        }

        fun bind(chat: Chat) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val otherParticipant = chat.participants.firstOrNull { it != currentUser?.uid }
            participantsTextView.text = chat.fullName
            contentTextView.text = chat.content

            // Load profile picture using Glide
            Glide.with(itemView.context)
                .load(chat.profilePicture)
                .placeholder(R.drawable.circle_background) // Provide a default avatar image
                .into(avatarImageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item_layout, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int {
        return chats.size
    }
}
