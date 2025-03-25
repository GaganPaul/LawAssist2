package com.example.lawassist.service
import com.example.lawassist.model.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreService {

    private val db = FirebaseFirestore.getInstance()

    // Save chat message to Firestore with detailed logging
    suspend fun saveChatMessage(userId: String, message: String, role: String): Boolean {
        return try {
            if (userId.isBlank() || message.isBlank() || role.isBlank()) {
                println("Invalid Data: UserId, Message, or Role is empty")
                return false
            }
            println("Saving Message - Role: $role, Text: $message")
            val chatData = hashMapOf(
                "userId" to userId,
                "text" to message,
                "role" to role,
                "timestamp" to Timestamp.now()
            )
            db.collection("chathistory").add(chatData).await()
            println("Message Saved Successfully!")
            true
        } catch (e: Exception) {
            println("Error saving message: ${e.message}")
            false
        }
    }


    // Retrieve chat history with user and AI messages
    suspend fun getChatHistory(userId: String): List<ChatMessage> {
        return try {
            val querySnapshot = db.collection("chathistory")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            querySnapshot.documents.map { doc ->
                ChatMessage(
                    userId = doc.getString("userId") ?: "",
                    role = doc.getString("role") ?: "Unknown",
                    text = doc.getString("text") ?: "",
                    timestamp = (doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L)
                )
            }
        } catch (e: Exception) {
            println("Error retrieving chat history: ${e.message}")
            emptyList()
        }
    }

    // Delete chat history for a user
    suspend fun deleteChatHistory(userId: String): Boolean {
        return try {
            val querySnapshot = db.collection("chathistory")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            querySnapshot.documents.forEach { it.reference.delete().await() }
            true
        } catch (e: Exception) {
            println("Error deleting chat history: ${e.message}")
            false
        }
    }
}
