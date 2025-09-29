package com.example.smarttouristsos.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarttouristsos.R
import com.example.smarttouristsos.models.Group

// --- MODIFIED: The constructor now accepts two different click actions ---
class GroupAdapter(
    private var groups: MutableList<Group>,
    private val onItemClicked: (Group) -> Unit,
    private val onItemLongClicked: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupNameTextView: TextView = itemView.findViewById(R.id.groupNameTextView)
        private val memberCountTextView: TextView = itemView.findViewById(R.id.memberCountTextView)

        fun bind(group: Group) {
            groupNameTextView.text = group.name
            val count = group.memberCount
            memberCountTextView.text = itemView.context.resources.getQuantityString(R.plurals.member_count, count, count)

            // --- MODIFIED: We now have two separate listeners ---

            // 1. A standard click listener for opening the chat
            itemView.setOnClickListener {
                onItemClicked(group)
            }

            // 2. A long click listener for copying the ID
            itemView.setOnLongClickListener {
                onItemLongClicked(group)
                true // Returning true tells the system we've handled the event
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount(): Int {
        return groups.size
    }

    fun updateGroups(newGroups: List<Group>) {
        groups.clear()
        groups.addAll(newGroups)
        notifyDataSetChanged()
    }
}