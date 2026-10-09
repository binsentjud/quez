
package com.example.quez

import android.content.Context
import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QuizScreen(
    pdfUri: Uri,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val model = remember {
        Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).generativeModel("gemini-3.5-flash-lite")
    }

    var isProcessing by remember {
        mutableStateOf(true)
    }

    var statusMessage by remember {
        mutableStateOf("Preparing your quiz...")
    }

    var generatedQuestions by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    LaunchedEffect(pdfUri) {
        isProcessing = true
        generatedQuestions = ""
        errorMessage = ""

        try {
            statusMessage = "Reading your PDF..."

            val extractedText = extractPdfText(
                context = context,
                uri = pdfUri
            )

            if (extractedText.isBlank()) {
                error("No readable text was found in this PDF.")
            }

            statusMessage = "Generating your quiz questions..."

            val prompt = """
                Create exactly 10 multiple-choice questions based
                on the following study material.

                Requirements:
                - Each question must have four choices: A, B, C, and D.
                - Include plausible and distinct answer choices.
                - Use only information supported by the study material.
                - Do not reveal the correct answers.
                - Do not include explanations or a study summary.
                - Use plain text without Markdown formatting.
                - Format each question clearly with its four choices.

                Study material:
                $extractedText
            """.trimIndent()

            val response = model.generateContent(prompt)

            generatedQuestions = response.text
                ?.takeIf { it.isNotBlank() }
                ?: error("The AI did not return any questions.")

            statusMessage = "Your questions are ready!"

        } catch (e: Exception) {
            errorMessage = e.localizedMessage
                ?: "Something went wrong while preparing your quiz."
        } finally {
            isProcessing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Quiz",
            color = Color.White
        )

        if (isProcessing) {
            CircularProgressIndicator()

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = statusMessage,
                color = Color.White
            )

            Text(
                text = "Please wait while we prepare your questions.",
                color = Color(0xFFA0A0A0)
            )

        } else if (errorMessage.isNotBlank()) {
            Text(
                text = "Unable to prepare your quiz.",
                color = Color.White
            )

            Text(
                text = errorMessage,
                color = Color(0xFFFF8A80)
            )

            Button(onClick = onBack) {
                Text("Back to Review")
            }

        } else {
            Text(
                text = statusMessage,
                color = Color(0xFFA0A0A0)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = generatedQuestions,
                    color = Color.White
                )
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Review")
            }
        }
    }
}

private suspend fun extractPdfText(
    context: Context,
    uri: Uri
): String = withContext(Dispatchers.IO) {
    val inputStream = context.contentResolver
        .openInputStream(uri)
        ?: error("Unable to open the selected PDF.")

    inputStream.use { stream ->
        PDDocument.load(stream).use { document ->
            PDFTextStripper().getText(document)
        }
    }
}