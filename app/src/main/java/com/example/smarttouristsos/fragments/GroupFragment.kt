package com.example.smarttouristsos.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarttouristsos.GroupChatActivity
import com.example.smarttouristsos.R
import com.example.smarttouristsos.adapters.GroupAdapter
import com.example.smarttouristsos.models.Group
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GroupFragment : Fragment() {

    private lateinit var groupRecyclerView: RecyclerView
    private lateinit var groupAdapter: GroupAdapter
    private lateinit var fabAddGroup: FloatingActionButton

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupRecyclerView = view.findViewById(R.id.groupsRecyclerView)
        fabAddGroup = view.findViewById(R.id.fabAddGroup)

        setupRecyclerView()
        fetchGroupsFromFirestore()

        fabAddGroup.setOnClickListener {
            showAddOrJoinDialog()
        }
    }

    private fun setupRecyclerView() {
        // --- THIS IS THE MODIFIED PART ---
        // We now pass two blocks of code (lambdas) to our adapter constructor
        groupAdapter = GroupAdapter(
            mutableListOf(),
            // First Lambda: This is what happens on a SHORT CLICK
            onItemClicked = { group ->
                // Open the chat activity
                val intent = Intent(requireContext(), GroupChatActivity::class.java).apply {
                    putExtra("GROUP_ID", group.id)
                    putExtra("GROUP_NAME", group.name)
                }
                startActivity(intent)
            },
            // Second Lambda: This is what happens on a LONG PRESS
            onItemLongClicked = { group ->
                // Copy the Group ID to the clipboard
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Group ID", group.id)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Group ID for '${group.name}' copied!", Toast.LENGTH_LONG).show()
            }
        )

        groupRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }
    }

    private fun fetchGroupsFromFirestore() {
        val currentUser = auth.currentUser ?: return

        db.collection("groups")
            .whereArrayContains("members", currentUser.uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("GroupFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val newGroups = mutableListOf<Group>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val group = doc.toObject(Group::class.java).apply { id = doc.id }
                        newGroups.add(group)
                    }
                }
                groupAdapter.updateGroups(newGroups)
            }
    }

    // --- All functions below this line are unchanged ---
    private fun showAddOrJoinDialog() {
        val options = arrayOf("Create a new group", "Join an existing group")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Group")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showCreateGroupDialog()
                    1 -> showJoinGroupDialog()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showCreateGroupDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Create a New Group")
        val input = EditText(context).apply { hint = "Enter group name" }
        builder.setView(input)
        builder.setPositiveButton("Create") { _, _ ->
            val groupName = input.text.toString().trim()
            if (groupName.isNotEmpty()) createNewGroup(groupName)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showJoinGroupDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Join an Existing Group")
        val input = EditText(context).apply { hint = "Enter Group ID" }
        builder.setView(input)
        builder.setPositiveButton("Join") { _, _ ->
            val groupId = input.text.toString().trim()
            if (groupId.isNotEmpty()) joinGroupWithId(groupId)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun createNewGroup(name: String) {
        val currentUser = auth.currentUser ?: return
        val newGroup = Group(name = name, members = listOf(currentUser.uid))
        db.collection("groups").add(newGroup)
            .addOnSuccessListener { Toast.makeText(context, "Group '$name' created!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Log.w("GroupFragment", "Error creating group", e) }
    }

    private fun joinGroupWithId(groupId: String) {
        val currentUser = auth.currentUser ?: return
        val groupRef = db.collection("groups").document(groupId)
        groupRef.update("members", FieldValue.arrayUnion(currentUser.uid))
            .addOnSuccessListener {
                Toast.makeText(context, "Successfully joined group!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error joining group. Check the ID and try again.", Toast.LENGTH_LONG).show()
            }
    }
}