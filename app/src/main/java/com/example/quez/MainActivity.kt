package com.example.quez

import android.net.Uri
import android.os.Bundle

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Button
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import com.example.quez.ui.theme.QuEZTheme

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.runtime.rememberCoroutineScope

import kotlinx.coroutines.launch

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PDFBoxResourceLoader.init(applicationContext)

        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        setContent {
            QuEZTheme {
                PDFUploader()
            }
        }
    }
}

@Composable
fun PDFUploader() {

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val model = Firebase.ai(
        backend = GenerativeBackend.googleAI()
    ).generativeModel("gemini-3.5-flash-lite")

    var selectedPdfUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var extractedText by remember {
        mutableStateOf("")
    }

    var isExtracting by remember {
        mutableStateOf(false)
    }

    var summaryText by remember {
        mutableStateOf("")
    }

    var isSummarizing by remember {
        mutableStateOf(false)
    }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {
            selectedPdfUri = uri
            extractedText = ""
            isExtracting = true
        }
    }

    // Extract PDF text whenever a PDF is selected
    LaunchedEffect(selectedPdfUri) {

        val uri = selectedPdfUri

        if (uri != null) {

            extractedText = try {

                withContext(Dispatchers.IO) {

                    val inputStream =
                        context.contentResolver.openInputStream(uri)

                    inputStream.use { stream ->

                        val document = PDDocument.load(stream)

                        val stripper = PDFTextStripper()

                        val text = stripper.getText(document)

                        document.close()

                        text
                    }
                }

            } catch (e: Exception) {

                "Error extracting PDF text:\n${e.message}"

            }

            isExtracting = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(
            onClick = {
                pdfPicker.launch(arrayOf("application/pdf"))
            }
        ) {
            Text("Upload PDF")
        }

        if (selectedPdfUri != null) {

            if (isExtracting) {

                Text(
                    text = "Extracting PDF text...",
                    modifier = Modifier.padding(top = 16.dp)
                )

            } else {

                Text(
                    text = "Extracted Text:",
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .align(Alignment.Start)
                )

                // Text area
                Text(
                    text = extractedText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp)
                        .verticalScroll(rememberScrollState())
                )

                Button(
                    onClick = {

                        isSummarizing = true
                        summaryText = ""

                        scope.launch {

                            try {

                                val prompt = """
                    You are a study-material processor.

                    Clean the extracted PDF text internally, then generate 10 multiple-choice questions based ONLY on the cleaned material.
                    
                    Requirements:
                    
                    - Remove irrelevant, repeated, administrative, and navigation information.
                    - Keep important concepts, definitions, formulas, examples, procedures, dates, names, and technical terms.
                    - Do not invent information.
                    - Do not show or output the cleaned summary.
                    - Use plain text only. No Markdown or LaTeX.
                    - Due to the screen size of phones, keep choices short but concise.
                    
                    Format the output exactly as:
                    
                    1. Question?
                    A. Choice A
                    B. Choice B
                    C. Choice C
                    D. Choice D
                    2. Question?
                    A. Choice A
                    B. Choice B
                    C. Choice C
                    D. Choice D
                    
                    Continue until question 10.
                    
                    Do not include answers, explanations, summaries, headings, or bullets. Output only the 10 questions and their choices.

                    $extractedText
                """.trimIndent()

                                val response = model.generateContent(prompt)

                                summaryText = response.text ?: "No summary was generated."

                            } catch (e: Exception) {

                                summaryText = "Error generating summary:\n${e.message}"

                            }

                            isSummarizing = false
                        }
                    }
                ) {
                    Text("Generate Questions")
                }

                if (isSummarizing) {

                    Text(
                        text = "Generating questions...",
                        modifier = Modifier.padding(top = 16.dp)
                    )

                } else if (summaryText.isNotEmpty()) {

                    Text(
                        text = "Questions:",
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .align(Alignment.Start)
                    )

                    Text(
                        text = summaryText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 8.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }

                // Upload another PDF button
                Button(
                    onClick = {
                        pdfPicker.launch(arrayOf("application/pdf"))
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Upload Another PDF")
                }
            }
        }
    }
}