package com.example.lawassist.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun registerUser(email: String, password: String, name: String): Result<String> {
        return try {
            val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = userCredential.user?.uid ?: throw Exception("User ID not found")

            val user = hashMapOf(
                "name" to name,
                "email" to email,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("users").document(userId).set(user).await()
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<String> {
        return try {
            val userCredential = auth.signInWithEmailAndPassword(email, password).await()
            val userId = userCredential.user?.uid ?: throw Exception("User ID not found")
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logoutUser() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}
