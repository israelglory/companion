package com.example.companion

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter

@SuppressLint("LocalContextResourcesRead")
@Composable
fun BakingScreen(
    bakingViewModel: BakingViewModel = viewModel()
) {
    // selectedImage is a simple saved Int state (index of selected image)

    val placeholderPrompt = stringResource(R.string.prompt_placeholder)
    val placeholderResult = stringResource(R.string.results_placeholder)

    var prompt by rememberSaveable { mutableStateOf(placeholderPrompt) }
    var result by rememberSaveable { mutableStateOf(placeholderResult) }

    val uiState by bakingViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // For picking an image from gallery
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.baking_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        // Content area (use weight to let this grow)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState is UiState.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Update result text & color depending on state
                val textColor = when (uiState) {
                    is UiState.Error -> {
                        result = (uiState as UiState.Error).errorMessage
                        MaterialTheme.colorScheme.error
                    }
                    is UiState.Success -> {
                        result = (uiState as UiState.Success).outputText
                        MaterialTheme.colorScheme.onSurface
                    }
                    else -> MaterialTheme.colorScheme.onSurface
                }

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = result,
                        textAlign = TextAlign.Start,
                        color = textColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Image picker + preview
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Pick Image")
            }

            selectedImageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(16.dp)
                        .requiredSize(200.dp)
                )
            }
        }

        // Prompt input + action button
        Row(
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = prompt,
                label = { Text(stringResource(R.string.label_prompt)) },
                onValueChange = { prompt = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            )

            Button(
                onClick = {
                    if (selectedImageUri != null) {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(context.contentResolver,
                                selectedImageUri!!
                            )
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            @Suppress("DEPRECATION")
                            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, selectedImageUri)
                        }
                        bakingViewModel.sendPrompt(bitmap, prompt)
                    }
                },
                enabled = prompt.isNotEmpty()
            ) {
                Text(text = stringResource(R.string.action_go))
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun BakingScreenPreview() {
    // Note: preview will not have real ViewModel or resources available; this preview is just to show layout.
    // Provide a fake ViewModel if you want a live preview in Studio.
    // For now, call the composable without a real VM to avoid preview errors.
    // BakingScreen() // uncomment if you provide a preview-friendly BakingViewModel
}
