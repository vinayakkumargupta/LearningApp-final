package com.univ.learningapp.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.univ.learningapp.data.model.User
import com.univ.learningapp.data.model.UserRole
import com.univ.learningapp.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val storage: FirebaseStorage? by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    override val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            val fbUser = firebaseAuth.currentUser
            if (fbUser != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    fetchAndEmitUserProfile(fbUser.uid, fbUser.email ?: "", fbUser.displayName ?: "")
                }
            } else {
                _currentUserFlow.value = null
            }
        }
    }

    override suspend fun login(email: String, pass: String): Result<User> {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be empty."))
        }

        val firebaseInstance = auth
        if (firebaseInstance != null) {
            return try {
                val authResult = firebaseInstance.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                val fbUser = authResult.user ?: return Result.failure(Exception("Authentication failed."))
                val user = fetchAndEmitUserProfile(fbUser.uid, cleanEmail, fbUser.displayName ?: "")
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(Exception(e.localizedMessage ?: "Login failed. Please check your credentials."))
            }
        } else {
            val role = determineRoleFromEmailAndName(cleanEmail, "")
            val user = User(id = "usr_mock", name = cleanEmail.substringBefore("@"), email = cleanEmail, role = role)
            _currentUserFlow.value = user
            return Result.success(user)
        }
    }

    override suspend fun signup(name: String, email: String, pass: String, requestAdminAccess: Boolean): Result<User> {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || cleanPass.isBlank()) {
            return Result.failure(IllegalArgumentException("All fields are required."))
        }

        val firebaseInstance = auth
        if (firebaseInstance != null) {
            return try {
                val authResult = firebaseInstance.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
                val fbUser = authResult.user ?: return Result.failure(Exception("Signup failed."))

                val role = determineRoleFromEmailAndName(cleanEmail, cleanName, requestAdminAccess)

                val user = User(
                    id = fbUser.uid,
                    name = cleanName,
                    email = cleanEmail,
                    role = role
                )

                firestore?.collection("users")?.document(fbUser.uid)?.set(
                    hashMapOf(
                        "id" to fbUser.uid,
                        "name" to cleanName,
                        "email" to cleanEmail,
                        "role" to role.name,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                )?.await()

                _currentUserFlow.value = user
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(Exception(e.localizedMessage ?: "Sign up failed."))
            }
        } else {
            val role = determineRoleFromEmailAndName(cleanEmail, cleanName, requestAdminAccess)
            val user = User(id = "usr_mock", name = cleanName, email = cleanEmail, role = role)
            _currentUserFlow.value = user
            return Result.success(user)
        }
    }

    override suspend fun resetPassword(email: String): Result<String> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        val firebaseInstance = auth
        if (firebaseInstance != null) {
            return try {
                firebaseInstance.sendPasswordResetEmail(cleanEmail).await()
                Result.success("Password reset email sent to $cleanEmail.")
            } catch (e: Exception) {
                Result.failure(Exception(e.localizedMessage ?: "Failed to send reset email."))
            }
        }
        return Result.success("Password reset instructions sent to $cleanEmail.")
    }

    override suspend fun logout() {
        try {
            auth?.signOut()
        } catch (_: Exception) {}
        _currentUserFlow.value = null
    }

    override fun getCurrentUser(): User? {
        return _currentUserFlow.value
    }

    override suspend fun getAllUsers(): Result<List<User>> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))
        // Only the SUPER_ADMIN is allowed to see other members' email addresses.
        // Regular ADMINs can manage roles but must not see anyone's email.
        val viewerIsSuperAdmin = _currentUserFlow.value?.role == UserRole.SUPER_ADMIN
        return try {
            val snapshot = db.collection("users").get().await()
            val usersList = snapshot.documents.mapNotNull { doc ->
                val uid = doc.id
                val email = doc.getString("email") ?: ""
                val name = doc.getString("name") ?: email.substringBefore("@").ifBlank { "User" }
                val roleStr = doc.getString("role") ?: "MEMBER"
                var role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.MEMBER)

                if (email.equals("vinayak678admin@learning.com", ignoreCase = true)) {
                    role = UserRole.SUPER_ADMIN
                }
                User(
                    id = uid,
                    name = name,
                    email = if (viewerIsSuperAdmin) email else "",
                    role = role,
                    phoneNumber = if (viewerIsSuperAdmin) doc.getString("phoneNumber") ?: "" else "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    instagramHandle = doc.getString("instagramHandle") ?: "",
                    twitterHandle = doc.getString("twitterHandle") ?: "",
                    linkedinHandle = doc.getString("linkedinHandle") ?: ""
                )
            }
            Result.success(usersList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserRole(userId: String, newRole: UserRole): Result<Boolean> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))
        return try {
            db.collection("users").document(userId)
                .update("role", newRole.name).await()

            // If updating current logged in user, update local flow
            val current = _currentUserFlow.value
            if (current != null && current.id == userId) {
                _currentUserFlow.value = current.copy(role = newRole)
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        phoneNumber: String,
        instagramHandle: String,
        twitterHandle: String,
        linkedinHandle: String
    ): Result<Boolean> {
        val current = _currentUserFlow.value ?: return Result.failure(Exception("Not logged in."))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))
        return try {
            db.collection("users").document(current.id).set(
                mapOf(
                    "phoneNumber" to phoneNumber.trim(),
                    "instagramHandle" to instagramHandle.trim(),
                    "twitterHandle" to twitterHandle.trim(),
                    "linkedinHandle" to linkedinHandle.trim()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            _currentUserFlow.value = current.copy(
                phoneNumber = phoneNumber.trim(),
                instagramHandle = instagramHandle.trim(),
                twitterHandle = twitterHandle.trim(),
                linkedinHandle = linkedinHandle.trim()
            )
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfilePhoto(photoUri: Uri): Result<String> {
        val current = _currentUserFlow.value ?: return Result.failure(Exception("Not logged in."))
        val bucket = storage ?: return Result.failure(Exception("Storage not initialized."))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))
        return try {
            val ref = bucket.reference.child("profile_photos/${current.id}.jpg")
            ref.putFile(photoUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            db.collection("users").document(current.id)
                .set(mapOf("photoUrl" to downloadUrl), com.google.firebase.firestore.SetOptions.merge())
                .await()

            _currentUserFlow.value = current.copy(photoUrl = downloadUrl)
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchAndEmitUserProfile(uid: String, email: String, fallbackName: String): User {
        var role = determineRoleFromEmailAndName(email, fallbackName)
        var userName = fallbackName.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
        var phoneNumber = ""
        var photoUrl = ""
        var instagramHandle = ""
        var twitterHandle = ""
        var linkedinHandle = ""

        try {
            val doc = firestore?.collection("users")?.document(uid)?.get()?.await()
            if (doc != null && doc.exists()) {
                val fetchedName = doc.getString("name")
                if (!fetchedName.isNullOrBlank()) {
                    userName = fetchedName
                }
                val roleStr = doc.getString("role")
                if (!roleStr.isNullOrBlank()) {
                    val parsedRole = runCatching { UserRole.valueOf(roleStr) }.getOrNull()
                    if (parsedRole != null) {
                        role = parsedRole
                    }
                }
                phoneNumber = doc.getString("phoneNumber") ?: ""
                photoUrl = doc.getString("photoUrl") ?: ""
                instagramHandle = doc.getString("instagramHandle") ?: ""
                twitterHandle = doc.getString("twitterHandle") ?: ""
                linkedinHandle = doc.getString("linkedinHandle") ?: ""
            }
        } catch (_: Exception) { }

        // Override Super Admin for owner accounts
        if (email.equals("vinayak678admin@learning.com", ignoreCase = true)) {
            if (role != UserRole.SUPER_ADMIN) {
                role = UserRole.SUPER_ADMIN
                // Update Firestore to ensure Security Rules recognize them as SUPER_ADMIN
                try {
                    firestore?.collection("users")?.document(uid)?.update("role", role.name)?.await()
                } catch (e: Exception) {}
            }
        }

        val user = User(
            id = uid,
            name = userName,
            email = email,
            role = role,
            phoneNumber = phoneNumber,
            photoUrl = photoUrl,
            instagramHandle = instagramHandle,
            twitterHandle = twitterHandle,
            linkedinHandle = linkedinHandle
        )
        _currentUserFlow.value = user
        return user
    }

    private fun determineRoleFromEmailAndName(email: String, name: String, requestAdminAccess: Boolean = false): UserRole {
        return when {
            email.equals("vinayak678admin@learning.com", ignoreCase = true) -> UserRole.SUPER_ADMIN
            requestAdminAccess -> UserRole.PENDING_ADMIN
            else -> UserRole.MEMBER
        }
    }
}
