package com.example.quez

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import com.example.quez.ui.theme.QuEZTheme
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {

    companion object {
        private const val CHANNEL_ID = "coming_soon_channel_v2"
        private const val NOTIFICATION_ID = 1
    }

    // Request notification permission
    private val requestNotificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(
                    this,
                    "Notification permission granted",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Notification permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission
        askNotificationPermission()

        // Disable forced system-bar contrast on Android 10 and later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        // Configure fullscreen window for Android 13
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.setBackgroundColor(Color.WHITE)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Load the XML layout
        setContentView(R.layout.activity_main)

        // Initialize PDFBox
        PDFBoxResourceLoader.init(applicationContext)

        // Configure Firebase App Check for development
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // Create notification channel
        createNotificationChannel()

        // Find the main content area and bottom navigation
        val mainContent = findViewById<FrameLayout>(R.id.mainContent)
        val bottomNavigation = findViewById<View>(R.id.bottomNavigation)

        // Retrieve the navigation items
        val navReview = findViewById<TextView>(R.id.navReview)
        val navStats = findViewById<TextView>(R.id.navStats)
        val navSettings = findViewById<TextView>(R.id.navSettings)
        val navUser = findViewById<TextView>(R.id.navUser)

        val navItems: List<Pair<String, TextView>> = listOf(
            "Review" to navReview,
            "Stats" to navStats,
            "Settings" to navSettings,
            "User" to navUser
        )

        // Display the selected screen
        fun showScreen(
            screen: String,
            pdfUri: Uri? = null
        ) {
            // Hide bottom navigation during the quiz
            bottomNavigation.visibility =
                if (screen == "Quiz") View.GONE else View.VISIBLE

            // Keep Review selected while the quiz is open
            val selectedScreen =
                if (screen == "Quiz") "Review" else screen

            // Update the selected navigation item's state
            navItems.forEach { (name, item) ->
                item.isSelected = name == selectedScreen
            }

            // Clear the previous screen
            mainContent.removeAllViews()

            val composeView = ComposeView(this)

            mainContent.addView(
                composeView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            composeView.setContent {
                QuEZTheme {
                    when (screen) {
                        "Review" -> ReviewScreen(
                            onStartQuiz = { selectedUri ->
                                showScreen("Quiz", selectedUri)
                            }
                        )

                        "Quiz" -> {
                            if (pdfUri != null) {
                                QuizScreen(
                                    pdfUri = pdfUri,
                                    onBack = {
                                        showScreen("Review")
                                    }
                                )
                            } else {
                                Text("Please select a PDF first.")
                            }
                        }

                        else -> Text("Coming Soon")
                    }
                }
            }
        }

        // Connect each navigation item to its screen
        navItems.forEach { (screen, item) ->
            item.setOnClickListener {

                // Display one Toast message
                Toast.makeText(
                    this,
                    "$screen pressed",
                    Toast.LENGTH_SHORT
                ).show()

                if (screen != "Review") {
                    showComingSoonNotification()
                } else {
                    showScreen("Review")
                }
            }
        }

        // Display Review when the app starts
        showScreen("Review")

        // Enable immersive fullscreen after the initial screen is created
        window.decorView.post {
            enableImmersiveMode()
        }
    }

    // Request notification permission
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    // Create notification channel
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Coming Soon Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for unavailable options"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // Display Coming Soon notification
    private fun showComingSoonNotification() {

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("QuEZ")
            .setContentText("That option is coming soon")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    // Enable immersive fullscreen mode
    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(
            window,
            window.decorView
        )

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
