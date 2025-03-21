package com.gentestrana.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentestrana.R
import com.gentestrana.components.ProfilePhotoCarousel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePicturesScreen(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
//    Log.d("ProfilePicturesScreen", "DEBUG - Numero immagini: ${imageUrls.size}")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.photos)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Azione "Indietro"
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // Qui chiamiamo il ProfilePhotoCarousel
            ProfilePhotoCarousel(
                imageUrls = imageUrls,
                modifier = Modifier
                    .weight(1f)
            )
        }
    }
}
