package com.gentestrana.chat

import com.gentestrana.users.User
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito
import com.google.firebase.firestore.QuerySnapshot
import org.mockito.kotlin.mock

/*
 * WHAT THIS TEST CHECKS:
 * This test ensures that when a user tries to start a chat with another person,
 * the app first checks if a chat already exists between them. If it does, the app
 * should reuse the existing chat instead of creating a duplicate.
 *
 * HOW IT WORKS IN SIMPLE TERMS:
 * 1. Setup Fake Scenario:
 *    - Creates imaginary users ("User A" and "User B").
 *    - Pretends they already have an existing chat ("Chat 123").
 *
 * 2. Test Action:
 *    - Simulates User A clicking "Chat with User B".
 *
 * 3. Verification:
 *    - Checks if the app correctly finds and returns "Chat 123" instead of
 *      creating a new one.
 *
 * WHY THIS MATTERS:
 * This prevents users from seeing multiple identical chats with the same person,
 * which could be confusing and lead to fragmented conversations.
 */

class ChatRepositoryTest {

    @Test
    fun `createNewChat should return existing chat ID if participants match`() = runBlocking {
        // [ARRANGE]
        val currentUserId = "user1_uid"
        val otherUserId = "user2_uid"

        // Mock Firestore
        val mockFirestore = mock<FirebaseFirestore>()
        val mockCollection = mock<CollectionReference>()
        val mockQuery = mock<Query>()
        val mockDocument = mock<DocumentSnapshot>()

        // Configura la catena Firestore
        Mockito.`when`(mockFirestore.collection("chats")).thenReturn(mockCollection)
        Mockito.`when`(mockCollection.whereArrayContains("participants", currentUserId)).thenReturn(mockQuery)

        // Configura QuerySnapshot
        val mockQuerySnapshot = mock<QuerySnapshot>().apply {
            Mockito.`when`(documents).thenReturn(listOf(mockDocument))
        }
        val completedTask = Tasks.forResult(mockQuerySnapshot)
        Mockito.`when`(mockQuery.get()).thenReturn(completedTask)

        // Configura DocumentSnapshot
        Mockito.`when`(mockDocument.id).thenReturn("existing_chat_id")
        Mockito.`when`(mockDocument["participants"]).thenReturn(listOf(currentUserId, otherUserId))

        // Mock FirebaseAuth
        val mockAuth = mock<FirebaseAuth>()
        val mockFirebaseUser = mock<FirebaseUser>().apply {
            Mockito.`when`(uid).thenReturn(currentUserId)
        }
        Mockito.`when`(mockAuth.currentUser).thenReturn(mockFirebaseUser)

        // Inietta le dipendenze mock
        val chatRepository = ChatRepository(db = mockFirestore, auth = mockAuth)

        // [ACT]
        val result = chatRepository.createNewChat(User(docId = otherUserId))

        // [ASSERT]
        assert(result == "existing_chat_id")
    }
}