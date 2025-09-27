package com.example.smarttouristsos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GroupFragment : Fragment() {

    // A placeholder list of group members for testing
    private val groupMembers = listOf(
        GroupMember("Sahil Kumar", "Online"),
        GroupMember("Friend 1", "Offline"),
        GroupMember("Friend 2", "Online")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvGroupMembers: RecyclerView = view.findViewById(R.id.rvGroupMembers)
        val tvEmptyGroup: TextView = view.findViewById(R.id.tvEmptyGroup)
        val fabAddGroup: FloatingActionButton = view.findViewById(R.id.fabAddGroup)

        // Set up the RecyclerView
        rvGroupMembers.layoutManager = LinearLayoutManager(requireContext())
        val adapter = GroupAdapter(groupMembers)
        rvGroupMembers.adapter = adapter

        // Show or hide the empty text based on the list
        if (groupMembers.isEmpty()) {
            tvEmptyGroup.visibility = View.VISIBLE
            rvGroupMembers.visibility = View.GONE
        } else {
            tvEmptyGroup.visibility = View.GONE
            rvGroupMembers.visibility = View.VISIBLE
        }

        // --- UPDATED LOGIC FOR THE FLOATING BUTTON ---
        fabAddGroup.setOnClickListener {
            showCreateJoinGroupDialog()
        }
    }

    private fun showCreateJoinGroupDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_group, null)
        val etUsername: EditText = dialogView.findViewById(R.id.etUsername)
        val btnJoin: Button = dialogView.findViewById(R.id.btnJoin)
        val btnCreate: Button = dialogView.findViewById(R.id.btnCreate)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnJoin.setOnClickListener {
            val username = etUsername.text.toString()
            if (username.isNotEmpty()) {
                // In the future, this will send a request to the backend
                Toast.makeText(requireContext(), "Joining group with $username...", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }

        btnCreate.setOnClickListener {
            val username = etUsername.text.toString()
            if (username.isNotEmpty()) {
                // In the future, this will send a request to the backend
                Toast.makeText(requireContext(), "Creating group with $username...", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
}

// --- Helper classes for the RecyclerView list (UNCHANGED) ---
data class GroupMember(val name: String, val status: String)
class GroupAdapter(private val members: List<GroupMember>) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return GroupViewHolder(view)
    }
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val member = members[position]
        holder.text1.text = member.name
        holder.text2.text = "Status: ${member.status}"
    }
    override fun getItemCount() = members.size
    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text1: TextView = itemView.findViewById(android.R.id.text1)
        val text2: TextView = itemView.findViewById(android.R.id.text2)
    }
}

