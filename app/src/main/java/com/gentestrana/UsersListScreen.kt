package com.gentestrana

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun UsersListScreen() {
    // Use produceState to asynchronously fetch the users list from Firestore.
    val usersState = produceState<List<User>>(initialValue = emptyList()) {
        Firebase.firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                value = result.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)
                }
            }
            .addOnFailureListener {
                // You can handle errors here (for example, log the error or show a message).
            }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(usersState.value) { user ->
            UserItem(user)
        }
    }
}

@Composable
fun UserItem(user: User) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        // Display the profile picture
        Image(
            painter = rememberAsyncImagePainter(user.profilePicUrl.trim()),
            contentDescription = "Profile Picture of ${user.username}",
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        // Display user details: username, bio, and description
        Column {
            Text(text = user.username, style = MaterialTheme.typography.titleMedium)
            Text(text = user.bio, style = MaterialTheme.typography.bodyMedium)
            Text(text = user.description, style = MaterialTheme.typography.bodySmall)
            Text(text = "URL: ${user.profilePicUrl}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
