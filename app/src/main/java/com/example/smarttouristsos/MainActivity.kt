package com.example.smarttouristsos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Load the default fragment when the app starts
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.navigation_dashboard -> selectedFragment = DashboardFragment()
                R.id.navigation_group -> selectedFragment = GroupFragment()
                R.id.navigation_safepoints -> selectedFragment = SafePointsFragment()
                R.id.navigation_feedback -> selectedFragment = FeedbackFragment()
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment)
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}