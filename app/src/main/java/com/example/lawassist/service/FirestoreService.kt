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
            
            // Format userId to match the new schema format: /users/{userId}
            val formattedUserId = if (!userId.startsWith("/users/")) {
                "/users/$userId"
            } else {
                userId
            }
            
            val chatData = hashMapOf(
                "userId" to formattedUserId,
                "text" to message,
                "role" to role,
                "timestamp" to Timestamp.now()
            )
            db.collection("chat_history").add(chatData).await()
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
            // Format userId to match the new schema format: /users/{userId}
            val formattedUserId = if (!userId.startsWith("/users/")) {
                "/users/$userId"
            } else {
                userId
            }
            
            val querySnapshot = db.collection("chat_history")
                .whereEqualTo("userId", formattedUserId)
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

    // Get all unique chat sessions for a user
    suspend fun getChatSessions(userId: String): List<Map<String, Any>> {
        return try {
            // Format userId to match the new schema format: /users/{userId}
            val formattedUserId = if (!userId.startsWith("/users/")) {
                "/users/$userId"
            } else {
                userId
            }
            
            // Get all messages for this user
            val querySnapshot = db.collection("chat_history")
                .whereEqualTo("userId", formattedUserId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
                
            // Group messages by conversation (based on timestamp proximity)
            val messages = querySnapshot.documents.map { doc ->
                val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                val text = doc.getString("text") ?: ""
                val role = doc.getString("role") ?: ""
                
                Triple(timestamp, text, role)
            }
            
            // Group messages into conversations (30 min gap = new conversation)
            val conversations = mutableListOf<MutableList<Triple<Long, String, String>>>()
            var currentConversation = mutableListOf<Triple<Long, String, String>>()
            
            for (i in messages.indices) {
                if (i == 0) {
                    currentConversation.add(messages[i])
                    continue
                }
                
                val timeDiff = messages[i].first - messages[i-1].first
                // If more than 30 minutes between messages, start a new conversation
                if (timeDiff > 30 * 60 * 1000) {
                    conversations.add(currentConversation)
                    currentConversation = mutableListOf()
                }
                
                currentConversation.add(messages[i])
                
                // Add the last conversation
                if (i == messages.size - 1) {
                    conversations.add(currentConversation)
                }
            }
            
            // Create a summary for each conversation
            conversations.mapNotNull { convo ->
                if (convo.isEmpty()) return@mapNotNull null
                
                // Find the first user message to use as the title
                val firstUserMessage = convo.find { it.third == "user" }?.second ?: "New Chat"
                val title = if (firstUserMessage.length > 30) 
                    "${firstUserMessage.take(30)}..." 
                else 
                    firstUserMessage
                
                val timestamp = convo.first().first
                
                mapOf(
                    "title" to title,
                    "timestamp" to timestamp,
                    "messageCount" to convo.size,
                    "preview" to convo.take(2).joinToString(" ") { it.second.take(20) + "..." }
                )
            }
        } catch (e: Exception) {
            println("Error retrieving chat sessions: ${e.message}")
            emptyList()
        }
    }

    // Create a new chat session
    public suspend fun createNewChat(userId: String, initialMessage: String): Boolean {
        return saveChatMessage(userId, initialMessage, "user")
    }

    // Delete chat history for a user
    suspend fun deleteChatHistory(userId: String): Boolean {
        return try {
            // Format userId to match the new schema format: /users/{userId}
            val formattedUserId = if (!userId.startsWith("/users/")) {
                "/users/$userId"
            } else {
                userId
            }
            
            val querySnapshot = db.collection("chat_history")
                .whereEqualTo("userId", formattedUserId)
                .get()
                .await()

            querySnapshot.documents.forEach { it.reference.delete().await() }
            true
        } catch (e: Exception) {
            println("Error deleting chat history: ${e.message}")
            false
        }
    }
    
    // Delete all chats for a specific user
    suspend fun deleteAllChats(userId: String): Boolean {
        return deleteChatHistory(userId)
    }
    
    // Delete all chats
    suspend fun deleteAllChats(): Boolean {
        return try {
            val querySnapshot = db.collection("chat_history")
                .get()
                .await()

            querySnapshot.documents.forEach { it.reference.delete().await() }
            true
        } catch (e: Exception) {
            println("Error deleting all chats: ${e.message}")
            false
        }
    }
    
    // Get user details from Firestore
    suspend fun getUserDetails(userId: String): Map<String, Any>? {
        return try {
            val docSnapshot = db.collection("users").document(userId).get().await()
            
            if (docSnapshot.exists()) {
                docSnapshot.data
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error getting user details: ${e.message}")
            null
        }
    }
}
