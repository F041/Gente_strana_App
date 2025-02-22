package com.gentestrana.utils

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.users.User

/**
 * Composable function to fetch user data from Firestore.
 *
 * It uses `produceState` to handle the asynchronous data fetching and updates the state
 * when the data is loaded or if there's an error.
 *
 * @return State<User?>: A Compose State that holds the User object when loaded, or null if loading or error.
 */
@Composable
fun rememberUserData(): State<User?> {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid // Get current user UID
    val firestore = Firebase.firestore

    return produceState<User?>(initialValue = null) {
        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    value = document.toObject(User::class.java)
                }
                .addOnFailureListener { e ->
                    Log.e("UserDataFetcher", "Error fetching user data", e)
                    value = null // Set value to null in case of error as well
                }
        } else {
            Log.w("UserDataFetcher", "No current user UID found.")
            value = null // No user logged in, return null
        }
    }
}