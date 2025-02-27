// not used
package com.gentestrana.users

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun UserItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Display the user's profile picture
        Image(
            painter = rememberAsyncImagePainter(user.profilePicUrl),
            contentDescription = "Profile picture of ${user.username}",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        // Display the user's username and bio
        Column {
            Text(text = user.username, style = MaterialTheme.typography.bodyLarge)
            Text(text = user.bio, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
