package com.gentestrana.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentestrana.R
import com.gentestrana.components.GenericLoadingScreen
import com.gentestrana.components.ProfileCompletionFields
import com.gentestrana.users.UserRepository
import com.gentestrana.utils.missingRequiredProfileFields
import kotlinx.coroutines.launch

/**
 * Schermata di completamento obbligatorio del profilo.
 *
 * Mostrata quando l'utente e autenticato ma il profilo non ha ancora
 * bio, almeno un topic e data di nascita. Copre:
 * - primo accesso con Google (il documento viene creato vuoto)
 * - account recuperati da createMissingUserDocument()
 * - utenti registrati prima dell'introduzione di questo obbligo
 *
 * Non e' possibile saltarla: il NavGraph riporta qui finche' il profilo non e' completo.
 */
@Composable
fun CompleteProfileScreen(
    initialBio: String,
    initialTopics: List<String>,
    initialBirthTimestamp: Long,
    onProfileCompleted: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }

    var bio by remember { mutableStateOf(initialBio) }
    var topics by remember { mutableStateOf(initialTopics) }
    var birthTimestamp by remember { mutableStateOf(initialBirthTimestamp) }
    var isLoading by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }

    val missingFields = missingRequiredProfileFields(bio, topics, birthTimestamp)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_complete_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.profile_complete_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            ProfileCompletionFields(
                context = context,
                bio = bio,
                onBioChange = { bio = it },
                topics = topics,
                onTopicsChange = { topics = it },
                birthTimestamp = birthTimestamp,
                onBirthTimestampChange = { birthTimestamp = it },
                missingFields = if (showValidationErrors) missingFields else emptyList()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (missingFields.isNotEmpty()) {
                        showValidationErrors = true
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.profile_complete_required_error)
                            )
                        }
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        userRepository.updateRequiredProfileFields(
                            bio = bio.trim(),
                            topics = topics,
                            rawBirthTimestamp = birthTimestamp,
                            onSuccess = {
                                isLoading = false
                                onProfileCompleted()
                            },
                            onFailure = { error ->
                                isLoading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        error ?: context.getString(R.string.profile_save_error)
                                    )
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    GenericLoadingScreen(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.profile_complete_save_button))
                }
            }
        }
    }
}