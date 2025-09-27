package com.example.smarttouristsos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast

class SafePointsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_safe_points, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnActivateAR: Button = view.findViewById(R.id.btnActivateAR)
        btnActivateAR.setOnClickListener {
            // In the future, this will open the camera with AR overlays.
            Toast.makeText(requireContext(), "Activating AR Safe Point Finder...", Toast.LENGTH_SHORT).show()
        }
    }
}
