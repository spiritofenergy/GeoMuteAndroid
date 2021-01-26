package com.kodexgroup.geomuteapp

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.kodexgroup.geomuteapp.utils.DrawerLayoutStatus
import com.kodexgroup.geomuteapp.utils.createNotificationChannel


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MyApplication_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_map, R.id.nav_list, R.id.nav_info, R.id.nav_settings), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                viewModel.setDrawerStatus(DrawerLayoutStatus.DRAWER_SLIDE)
            }

            override fun onDrawerOpened(drawerView: View) {
                viewModel.setDrawerStatus(DrawerLayoutStatus.DRAWER_OPENED)
            }

            override fun onDrawerClosed(drawerView: View) {
                viewModel.setDrawerStatus(DrawerLayoutStatus.DRAWER_CLOSED)
            }

            override fun onDrawerStateChanged(newState: Int) {
                viewModel.setDrawerStatus(DrawerLayoutStatus.DRAWER_CHANGED)
            }

        })

        createNotificationChannel(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(applicationContext)) {
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.setDrawerStatus("")
    }

    override fun onStop() {
        super.onStop()
        viewModel.setDrawerStatus(DrawerLayoutStatus.DRAWER_NULL)
    }

    override fun onSupportNavigateUp() : Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}