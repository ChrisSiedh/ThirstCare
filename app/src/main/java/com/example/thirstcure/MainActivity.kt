package com.example.thirstcure

import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.thirstcure.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //HomeFragment Menü als Start Festlegen
        binding.bottomNavigationView.menu.findItem(R.id.ic_main).isChecked = true

        //BottomNavigationBar Listener
        //Setzt das AnalyticsFragment im Landscape den Rest in Portrait
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.ic_main -> {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    changeFragment(HomeFragment())
                }
                R.id.ic_overviewFull -> {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    changeFragment(OverviewFragment())
                }
                R.id.ic_drinks -> {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    changeFragment(DrinksFragment())
                }
                R.id.ic_analytics -> {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    changeFragment(AnalyticsFragment())
                }
                R.id.ic_settings -> {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    changeFragment(SettingsFragment())
                }
            }
            true
        }
    }


    //Funktion zum wechseln des Fragments
    private fun changeFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainerView, fragment)
        fragmentTransaction.commit()
    }
}
