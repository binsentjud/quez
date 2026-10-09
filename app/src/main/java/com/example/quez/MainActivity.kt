
package com.example.quez

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView

import com.example.quez.ui.theme.QuEZTheme
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the XML dashboard and bottom navigation bar
        setContentView(R.layout.activity_main)

        // Initialize PDFBox
        PDFBoxResourceLoader.init(applicationContext)

        // Configure Firebase App Check for development
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

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
                showScreen(screen)
            }
        }

        // Display Review when the app starts
        showScreen("Review")

        // Enable immersive fullscreen mode
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }
}