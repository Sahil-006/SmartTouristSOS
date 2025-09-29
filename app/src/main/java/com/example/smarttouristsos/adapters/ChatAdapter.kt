package com.example.smarttouristsos.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarttouristsos.R
import com.example.smarttouristsos.models.Message
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ChatAdapter(
    private val messageList: MutableList<Message>
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private val currentUserId = Firebase.auth.currentUser?.uid
    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageTextView)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        // If the message's sender ID matches the current user's ID, it's a "sent" message
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        // Inflate the correct layout based on the view type
        val layoutId = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.messageText.text = message.text
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    fun addMessage(message: Message) {
        messageList.add(message)
        notifyItemInserted(messageList.size - 1)
    }

    fun clearMessages() {
        messageList.clear()
        notifyDataSetChanged()
    }
}
