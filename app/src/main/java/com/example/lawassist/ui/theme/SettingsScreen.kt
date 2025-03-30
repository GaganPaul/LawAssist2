package com.example.lawassist.ui.theme

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.lawassist.model.AIModel
import com.example.lawassist.service.FirestoreService
import com.example.lawassist.settings.SettingsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    settingsManager: SettingsManager,
    onDeleteAllChats: () -> Unit = {}
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val firestoreService = FirestoreService()

    // User data state
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userCreatedAt by remember { mutableStateOf(0L) }
    var isUserProfileExpanded by remember { mutableStateOf(false) }

    // Loading state
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Model selection state
    var showModelDropdown by remember { mutableStateOf(false) }
    val selectedModel by settingsManager.selectedModel.collectAsState()

    // Font size state
    val currentFontSize by settingsManager.fontSize.collectAsState()

    // Load user data
    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            userEmail = user.email ?: ""

            // Get additional user data from Firestore
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    isLoading = false

                    if (document != null && document.exists()) {
                        userName = document.getString("name") ?: user.displayName ?: ""

                        // Handle different data types for createdAt field
                        userCreatedAt = when (val createdAtValue = document.get("createdAt")) {
                            is Long -> createdAtValue
                            is Double -> createdAtValue.toLong()
                            is Int -> createdAtValue.toLong()
                            is com.google.firebase.Timestamp -> createdAtValue.toDate().time
                            is String -> try {
                                createdAtValue.toLong()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                            else -> System.currentTimeMillis()
                        }
                    } else {
                        // Handle case where user document doesn't exist
                        userName = user.displayName ?: ""
                        userCreatedAt = System.currentTimeMillis()

                        // Create user document if it doesn't exist
                        val userData = hashMapOf(
                            "name" to userName,
                            "email" to userEmail,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("users").document(user.uid).set(userData)
                            .addOnSuccessListener {
                                android.util.Log.d("SettingsScreen", "User document created successfully")
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("SettingsScreen", "Error creating user document: ${e.message}")
                                loadError = "Failed to create user profile. Please try again."
                            }
                    }
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    loadError = "Failed to load user data. Please try again."
                    android.util.Log.e("SettingsScreen", "Error loading user data: ${e.message}")
                }
        } else {
            isLoading = false
            loadError = "No user is currently logged in."
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top app bar
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (loadError != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = loadError ?: "An unknown error occurred",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // User profile card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // User avatar
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userName.firstOrNull()?.toString() ?: "U",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = userName,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(onClick = { isUserProfileExpanded = !isUserProfileExpanded }) {
                                    Icon(
                                        imageVector = if (isUserProfileExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand user profile"
                                    )
                                }
                            }

                            if (isUserProfileExpanded && userCreatedAt > 0) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Divider()

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Account created: ${formatDate(userCreatedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Theme toggle
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    ThemeToggle()

                    // Font size section
                    Text(
                        text = "Font Size",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Sample Text",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * currentFontSize
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Size: ${String.format("%.1f", currentFontSize)}x",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = {
                                        settingsManager.decreaseFontSize()
                                    },
                                    enabled = currentFontSize > SettingsManager.MIN_FONT_SIZE
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease font size",
                                        tint = if (currentFontSize > SettingsManager.MIN_FONT_SIZE)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }

                                Slider(
                                    value = currentFontSize,
                                    onValueChange = { settingsManager.setFontSize(it) },
                                    valueRange = SettingsManager.MIN_FONT_SIZE..SettingsManager.MAX_FONT_SIZE,
                                    steps = ((SettingsManager.MAX_FONT_SIZE - SettingsManager.MIN_FONT_SIZE) / 0.1f).toInt() - 1,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        settingsManager.increaseFontSize()
                                    },
                                    enabled = currentFontSize < SettingsManager.MAX_FONT_SIZE
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase font size",
                                        tint = if (currentFontSize < SettingsManager.MAX_FONT_SIZE)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }

                    // AI model selection
                    Text(
                        text = "AI Model",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Select the AI model to use for generating responses",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ExposedDropdownMenuBox(
                                expanded = showModelDropdown,
                                onExpandedChange = { showModelDropdown = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedModel,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showModelDropdown)
                                    },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = showModelDropdown,
                                    onDismissRequest = { showModelDropdown = false },
                                    modifier = Modifier.exposedDropdownSize(true)
                                ) {
                                    AIModel.AVAILABLE_MODELS.forEach { model ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = model.id,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = "By ${model.owned_by} • Context: ${model.context_window}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                settingsManager.setSelectedModel(model.id)
                                                showModelDropdown = false
                                            },
                                            contentPadding = PaddingValues(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Data management
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        onDeleteAllChats()
                                        Toast.makeText(context, "All chats deleted", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete all chats",
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text("Delete All Chats")
                            }
                        }
                    }

                    // About the app
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Law Assist is your personal legal assistant for laws, government schemes, and services in India. It provides accurate information and guidance on legal matters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Version",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Version 1.0.0",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Logout button
                    Button(
                        onClick = {
                            auth.signOut()
                            navController.navigate("welcome") {
                                popUpTo(0)
                            }
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Logout")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// Helper function to format user created date
private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return "Unknown"

    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    return formatter.format(date)
}
