
package com.example.quez

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

import androidx.compose.foundation.layout.size

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.clickable




@Composable
fun ReviewScreen(
    onStartQuiz: (Uri) -> Unit
) {
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onStartQuiz(uri)
        }
    }

    val megapixel = FontFamily(
        Font(R.font.megapixel)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.q_review_upload),
            contentDescription = "Choose a PDF",
            modifier = Modifier
                .size(175.dp)
                .clickable {
                    pdfPicker.launch(arrayOf("application/pdf"))
                },
            contentScale = ContentScale.Fit
        )

        Text(
            text = "Upload PDF",
            color = Color(0xFF434343),
            fontFamily = megapixel,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
    }
}