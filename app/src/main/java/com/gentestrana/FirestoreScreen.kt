package com.gentestrana

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun FirestoreScreen() {
    val context = LocalContext.current
    val db = Firebase.firestore

    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = {
                // Create a sample data document
                val sampleData = hashMapOf(
                    "timestamp" to System.currentTimeMillis(),
                    "message" to "Hello from Firestore!"
                )
                // Write the data to the "logs" collection
                db.collection("logs")
                    .add(sampleData)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(
                            context,
                            "Document added with ID: ${documentReference.id}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Error adding document: $e",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        ) {
            Text("Add Log")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Read all documents from the "logs" collection
                db.collection("logs")
                    .get()
                    .addOnSuccessListener { result ->
                        val logs = result.map { doc -> doc.data.toString() }
                        Toast.makeText(
                            context,
                            "Logs: $logs",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Error getting documents: $e",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        ) {
            Text("Get Logs")
        }
    }
}
