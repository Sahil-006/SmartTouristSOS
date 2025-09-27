package com.example.smarttouristsos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast

class FeedbackFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSubmitFeedback: Button = view.findViewById(R.id.btnSubmitFeedback)
        btnSubmitFeedback.setOnClickListener {
            // In the future, this will send the feedback to the backend.
            Toast.makeText(requireContext(), "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
        }
    }
}
