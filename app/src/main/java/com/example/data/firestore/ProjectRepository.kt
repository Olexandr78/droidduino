package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.ai.ChatMessage
import com.example.data.model.Sketch
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "ProjectRepository"

class ProjectRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    /**
     * Saves or updates a project sketch in Firestore under /users/{userId}/projects/{projectId}
     */
    suspend fun saveProject(userId: String, sketch: Sketch) {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        require(sketch.id.isNotBlank()) { "Sketch ID must not be blank" }

        val docRef = db.collection("users").document(userId)
            .collection("projects").document(sketch.id)

        val payload = mapOf(
            "id" to sketch.id,
            "userId" to userId,
            "name" to sketch.name,
            "code" to sketch.code,
            "boardId" to sketch.boardId,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            Log.d(TAG, "Successfully synced sketch ${sketch.name} to Firestore")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving project to Firestore", e)
            throw e
        }
    }

    /**
     * Persists a chat message into the project's chat log file in Firestore
     * /users/{userId}/projects/{projectId}/messages/{messageId}
     */
    suspend fun saveChatMessage(userId: String, projectId: String, chatMessage: ChatMessage) {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        require(projectId.isNotBlank()) { "Project ID must not be blank" }

        val docRef = db.collection("users").document(userId)
            .collection("projects").document(projectId)
            .collection("messages").document(chatMessage.id)

        val payload = mapOf(
            "id" to chatMessage.id,
            "userId" to userId,
            "projectId" to projectId,
            "role" to (if (chatMessage.isUser) "user" else "model"),
            "text" to chatMessage.text,
            "timestamp" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            Log.d(TAG, "Saved chat message ${chatMessage.id} for project $projectId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving chat message to Firestore", e)
            throw e
        }
    }

    /**
     * Observes real-time chat messages for a specific project
     */
    fun observeProjectChat(userId: String, projectId: String): Flow<List<ChatMessage>> {
        if (userId.isBlank() || projectId.isBlank()) return flowOf(emptyList())

        return db.collection("users").document(userId)
            .collection("projects").document(projectId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val role = doc.getString("role") ?: "model"
                    val text = doc.getString("text") ?: ""
                    val ts = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                    ChatMessage(
                        id = id,
                        isUser = (role == "user"),
                        text = text,
                        timestamp = ts
                    )
                }
            }
            .catch { e ->
                Log.e(TAG, "Error observing project chat: ${e.message}", e)
                emit(emptyList())
            }
    }
}
