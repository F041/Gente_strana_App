package com.gentestrana.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gentestrana.R
import com.gentestrana.components.VideoPlayer
import androidx.compose.ui.platform.LocalConfiguration
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.pager.PagerState


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(navController: NavHostController,
                     context: Context = LocalContext.current,) {
    val pagerState = rememberPagerState(pageCount = {4})
    var isNextPageEnabled by remember { mutableStateOf(false) }


    Column(modifier = Modifier
        .fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            userScrollEnabled = !(pagerState.currentPage == 2 && !isNextPageEnabled)
        ) { page ->
            OnboardingPage(page = page,
                navController = navController,
                context = context,
                pagerState = pagerState,
                onNextPageEnabledChanged = { enabled ->
                    isNextPageEnabled = enabled
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            for (i in 0 until pagerState.pageCount) {
                // Itera sul numero di pagine
                val color = if (i == pagerState.currentPage) {
                    MaterialTheme.colorScheme.primary
                // Colore "attivo" per la pagina corrente
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) // Colore "inattivo" per le altre pagine
                }
                Box(
                    modifier = Modifier
                        .padding(2.dp) // Spazio tra i pallini
                        .clip(CircleShape) // Forma a cerchio
                        .background(color) // Colore del pallino
                        .size(12.dp) // Dimensione del pallino
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPage(page: Int,
                   navController: NavHostController,
                   context: Context = LocalContext.current,
                   pagerState: PagerState,
                   onNextPageEnabledChanged: (Boolean) -> Unit
) {


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page) {
            0 -> {
                Text(
                    text = stringResource(R.string.onboarding_screen1_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    painter = painterResource(id = R.drawable.onboarding_screen1), // <== Assicurati che "onboarding_screen1" corrisponda al NOME del tuo file immagine (senza estensione .png o .jpg)
                    contentDescription = "community screen",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.70f)
                        .padding(bottom = 16.dp)
                // Spazio tra immagine e titolo
                )

                Text(
                    text = stringResource(R.string.onboarding_screen1_description),
                    textAlign = TextAlign.Center
                )
            }
            1 -> {
                Text(
                    text = stringResource(R.string.onboarding_screen2_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box( // Box to fix background
                    modifier = Modifier
                ) {
                    VideoPlayer()
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_screen2_description),
                    textAlign = TextAlign.Center
                )
            }
            // Removable if you fork this for another kind of social:
            // this page checks the presence of some kind of neurodiversity
            2 -> {
                OnboardingAssessmentPageContent(pagerState = pagerState,
                    onNextPageEnabledChanged = onNextPageEnabledChanged,context = context)
            }

            3 -> {
                Text(
                    text = stringResource(R.string.onboarding_screen4_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_screen4_description),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp)) // Spazio extra prima del bottone

                Button(
                    onClick = {
                        val sharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit().putBoolean("onboarding_completed", true).apply()

                        val auth = FirebaseAuth.getInstance()
                        if (auth.currentUser != null) {
                            // User is logged in - go to mainTabs
                            navController.navigate("main") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        } else {
                            // User isn't logged in - go to auth flow
                            navController.navigate("auth") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(stringResource(R.string.onboarding_button_start))
                }
            }
        }
    }
}