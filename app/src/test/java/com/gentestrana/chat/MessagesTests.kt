package com.gentestrana.chat

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.*

class MessagesLocalUnitTests {

    private lateinit var chatRepository: ChatRepository
    private val mockFirestore = Mockito.mock(FirebaseFirestore::class.java)
    private val mockAuth = Mockito.mock(FirebaseAuth::class.java)

    @Before
    fun setup() {
        // Configura l'utente mock
        val mockFirebaseUser = Mockito.mock(FirebaseUser::class.java).apply {
            whenever(uid).thenReturn("testUser")
        }
        whenever(mockAuth.currentUser).thenReturn(mockFirebaseUser)

        chatRepository = ChatRepository(mockFirestore, mockAuth)
    }

    @Test
    fun `markMessagesAsDelivered should update only SENT messages from others`() {
        runBlocking {
            // Configurazione
            val currentUserId = "testUser" // Deve corrispondere al mock
            val chatId = "chat123"

            // 1. Crea messaggi di test
            val messages = listOf(
                createMockMessage("msg1", "SENT", "otherUser"), // Aggiornabile
                createMockMessage("msg2", "SENT", currentUserId), // Ignorato (mittente corrente)
                createMockMessage("msg3", "DELIVERED", "otherUser") // Stato errato
            )

            // 2. Configura i mock
            val (queryMock, batchMock) = configureFirestoreMocks(
                chatId = chatId,
                expectedStatus = "SENT",
                mockMessages = messages
            )

            // 3. Esegui
            chatRepository.markMessagesAsDelivered(chatId, currentUserId)

            // 4. Verifica
            verify(queryMock).whereEqualTo("status", "SENT")
            verify(queryMock).whereNotEqualTo("sender", currentUserId)

            // Verifica che solo il messaggio 1 sia stato aggiornato
            verify(batchMock, times(1)).update(any(), eq("status"), eq("DELIVERED"))
        }
    }


    @Test
    fun `markMessagesAsRead should update only DELIVERED messages from others`() {
        runBlocking {
            val currentUserId = "testUser"
            val chatId = "chat456"

            val messages = listOf(
                createMockMessage("msg1", "DELIVERED", "otherUser"), // Aggiornabile
                createMockMessage("msg2", "DELIVERED", currentUserId), // Ignorato
                createMockMessage("msg3", "SENT", "otherUser") // Stato errato
            )

            val (queryMock, batchMock) = configureFirestoreMocks(
                chatId = chatId,
                expectedStatus = "DELIVERED",
                mockMessages = messages
            )

            chatRepository.markMessagesAsRead(chatId)

            verify(queryMock).whereEqualTo("status", "DELIVERED")
            verify(queryMock).whereNotEqualTo("sender", currentUserId)

            // Verifica l'update esatto
            verify(batchMock, times(1)).update(any(), eq("status"), eq("READ"))
        }
    }

    private fun configureFirestoreMocks(
        chatId: String,
        expectedStatus: String,
        mockMessages: List<Map<String, Any>>
    ): Pair<Query, WriteBatch> {
        // 1. Mock dei documenti
        val documents = mockMessages.map { mockDocument(it) }

        // 2. Mock QuerySnapshot
        val querySnapshot = mock<QuerySnapshot>().apply {
            whenever(this.documents).thenReturn(documents)
        }
        val task = Tasks.forResult(querySnapshot)

        // 3. Mock Query
        val queryMock = mock<Query>().apply {
            whenever(whereEqualTo("status", expectedStatus)).thenReturn(this)
            whenever(whereNotEqualTo("sender", any())).thenReturn(this)
            whenever(get()).thenReturn(task)
        }

        // 4. Mock Collection
        val collectionMock = mock<CollectionReference>().apply {
            whenever(whereEqualTo("status", expectedStatus)).thenReturn(queryMock)
        }
        whenever(mockFirestore.collection("chats/$chatId/messages")).thenReturn(collectionMock)

        // 5. Mock Batch
        val batchMock = mock<WriteBatch>()
        whenever(mockFirestore.batch()).thenReturn(batchMock)

        return Pair(queryMock, batchMock)
    }

    private fun createMockMessage(
        id: String,
        status: String,
        sender: String
    ): Map<String, Any> {
        return mapOf(
            "id" to id,
            "sender" to sender,
            "status" to status,
            "timestamp" to Timestamp.now(),
            "message" to "Test message"
        )
    }

    private fun mockDocument(data: Map<String, Any>): DocumentSnapshot {
        return mock<DocumentSnapshot>().apply {
            whenever(getString(any())).thenAnswer { invocation ->
                data[invocation.arguments[0] as String].toString()
            }
            whenever(get("status")).thenReturn(data["status"])
            whenever(reference).thenReturn(mock())
        }
    }
}