package com.example.lawassist

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lawassist.database.LawDatabase
import com.example.lawassist.model.ChatMessage
import com.example.lawassist.repository.LawRepository
import com.example.lawassist.service.FirestoreService
import com.example.lawassist.service.TextToSpeechService
import com.example.lawassist.settings.SettingsManager
import com.example.lawassist.ui.theme.LawAssistTheme
import com.example.lawassist.ui.theme.initializeTheme
import com.example.lawassist.ui.theme.WelcomeScreen
import com.example.lawassist.ui.theme.LoginScreen
import com.example.lawassist.ui.theme.RegisterScreen
import com.example.lawassist.ui.theme.SettingsScreen
import com.example.lawassist.viewmodel.LawViewModel
import com.example.lawassist.viewmodel.LawViewModelFactory
import com.example.lawassist.viewmodel.VoiceViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Extension function to find activity from context
fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

// Format timestamp to a readable format
fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(date)
}

class MainActivity : ComponentActivity() {

    private val lawRepository: LawRepository by lazy {
        val lawDao = LawDatabase.getDatabase(applicationContext).lawDao()
        LawRepository(lawDao)
    }

    private val lawViewModel: LawViewModel by viewModels {
        LawViewModelFactory(lawRepository)
    }

    private val voiceViewModel: VoiceViewModel by viewModels()

    private val userInputState = mutableStateOf("")

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val firestoreService: FirestoreService by lazy { FirestoreService() }

    private lateinit var textToSpeechService: TextToSpeechService
    private lateinit var settingsManager: SettingsManager

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start voice recognition
            voiceViewModel.startVoiceRecognition(this)
        } else {
            // Permission denied, show a message
            Toast.makeText(this, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    // Check and request microphone permission
    fun checkAndRequestMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted, start voice recognition
                voiceViewModel.startVoiceRecognition(this)
            }
            else -> {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize text-to-speech service
        textToSpeechService = TextToSpeechService(this)
        
        // Initialize settings manager
        settingsManager = SettingsManager.getInstance(this)
        
        // Initialize theme
        initializeTheme(this)

        setContent {
            LawAssistTheme {
                val navController = rememberNavController()

                // Initialize the chat messages list here to be preserved across navigation
                val chatMessages = remember { mutableStateListOf<ChatMessage>() }

                // Load chat history when app starts
                LaunchedEffect(Unit) {
                    if (auth.currentUser != null) {
                        loadChatHistory(chatMessages)
                    }
                }

                NavigationComponent(
                    navController = navController,
                    userInputState = userInputState,
                    onVoiceInputRequested = { checkAndRequestMicrophonePermission() },
                    onVoiceInputStop = { voiceViewModel.stopVoiceRecognition() },
                    lawViewModel = lawViewModel,
                    voiceViewModel = voiceViewModel,
                    auth = auth,
                    firestore = firestore,
                    chatMessages = chatMessages,
                    textToSpeechService = textToSpeechService,
                    settingsManager = settingsManager,
                    onDeleteAllChats = { deleteAllChats(chatMessages) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeechService.shutdown()
    }

    // Function to load chat history from Firestore
    private fun loadChatHistory(chatMessages: MutableList<ChatMessage>) {
        val userId = auth.currentUser?.uid ?: return
        
        android.util.Log.d("MainActivity", "Loading chat history for user: $userId")
        
        // Show loading indicator
        val loadingMessage = ChatMessage(
            userId = "/users/$userId",
            role = "assistant",
            text = "Loading chat history...",
            timestamp = System.currentTimeMillis()
        )
        chatMessages.add(loadingMessage)
        
        // Simple query without composite index requirements
        FirebaseFirestore.getInstance().collection("chat_history")
            .whereEqualTo("userId", "/users/$userId")
            .get()
            .addOnSuccessListener { documents ->
                chatMessages.clear() // Clear existing messages before loading
                
                android.util.Log.d("MainActivity", "Found ${documents.size()} chat messages")
                
                val tempMessages = mutableListOf<ChatMessage>()
                
                for (document in documents) {
                    val role = document.getString("role") ?: continue
                    val text = document.getString("text") ?: continue
                    val userId = document.getString("userId") ?: continue
                    val timestamp = document.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                    
                    tempMessages.add(
                        ChatMessage(
                            userId = userId,
                            role = role,
                            text = text,
                            timestamp = timestamp
                        )
                    )
                }
                
                // Sort messages by timestamp in memory instead of in the query
                tempMessages.sortBy { it.timestamp }
                
                // Add all messages to the chat
                chatMessages.addAll(tempMessages)
                
                // Add welcome message if chat is empty
                if (chatMessages.isEmpty()) {
                    chatMessages.add(
                        ChatMessage(
                            userId = "/users/$userId",
                            role = "assistant",
                            text = "I'm LawAssist, your AI assistant for laws, government schemes, and services in India.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                
                android.util.Log.d("MainActivity", "Chat history loaded with ${chatMessages.size} messages")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MainActivity", "Error loading chat history: ${e.message}")
                
                // Remove loading message
                chatMessages.removeAll { it.text == "Loading chat history..." }
                
                // Add welcome message if there was an error
                chatMessages.add(
                    ChatMessage(
                        userId = "/users/$userId",
                        role = "assistant",
                        text = "I'm LawAssist, your AI assistant for laws, government schemes, and services in India.",
                        timestamp = System.currentTimeMillis()
                    )
                )
                
                // Show error toast
                Toast.makeText(
                    this,
                    "Failed to load chat history: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Function to create a new chat - made public
    fun createNewChat(chatMessages: MutableList<ChatMessage>) {
        val userId = auth.currentUser?.uid ?: return

        // Clear existing messages
        chatMessages.clear()

        // Add welcome message
        chatMessages.add(
            ChatMessage(
                userId = "/users/$userId",
                role = "assistant",
                text = "I'm LawAssist, your AI assistant for laws, government schemes, and services in India.",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // Function to delete all chats
    fun deleteAllChats(chatMessages: MutableList<ChatMessage>) {
        val userId = auth.currentUser?.uid ?: return

        // Clear local messages
        chatMessages.clear()

        // Add welcome message
        chatMessages.add(
            ChatMessage(
                userId = "/users/$userId",
                role = "assistant",
                text = "I'm LawAssist, your AI assistant for laws, government schemes, and services in India.",
                timestamp = System.currentTimeMillis()
            )
        )

        // Launch a coroutine to delete from Firestore
        lifecycleScope.launch {
            firestoreService.deleteAllChats("/users/$userId")
        }
    }
}
@Composable
fun NavigationComponent(
    navController: NavHostController,
    userInputState: MutableState<String>,
    onVoiceInputRequested: () -> Unit,
    onVoiceInputStop: () -> Unit,
    lawViewModel: LawViewModel,
    voiceViewModel: VoiceViewModel,
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    chatMessages: MutableList<ChatMessage>,
    textToSpeechService: TextToSpeechService,
    settingsManager: SettingsManager,
    onDeleteAllChats: () -> Unit
) {
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(navController)
        }

        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    // Reset or refresh viewModel state when logging in
                    lawViewModel.checkUserSession()
                }
            )
        }

        composable("register") {
            RegisterScreen(navController)
        }

        composable("chat") {
            ModernChatScreen(
                viewModel = lawViewModel,
                voiceViewModel = voiceViewModel,
                userInputState = userInputState,
                onVoiceInputRequested = onVoiceInputRequested,
                onVoiceInputStop = onVoiceInputStop,
                navController = navController,
                auth = auth,
                messages = chatMessages,
                textToSpeechService = textToSpeechService,
                onNewChat = { (navController.context as MainActivity).createNewChat(chatMessages) },
                onDeleteAllChats = onDeleteAllChats,
                settingsManager = settingsManager
            )
        }

        composable("settings") {
            SettingsScreen(
                settingsManager = settingsManager,
                navController = navController,
                onDeleteAllChats = onDeleteAllChats
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernChatScreen(
    viewModel: LawViewModel,
    voiceViewModel: VoiceViewModel,
    userInputState: MutableState<String>,
    onVoiceInputRequested: () -> Unit,
    onVoiceInputStop: () -> Unit,
    navController: NavHostController,
    auth: FirebaseAuth,
    messages: MutableList<ChatMessage>,
    textToSpeechService: TextToSpeechService,
    onNewChat: () -> Unit,
    onDeleteAllChats: () -> Unit,
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val aiResponse by viewModel.aiResponse
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // State for drawer
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // State for the text field
    var userInput by userInputState

    // Observe voice input status
    val isListening by voiceViewModel.isListening

    // State for text-to-speech
    var isSpeaking by remember { mutableStateOf(false) }

    // State for current chat title
    var currentChatTitle by remember { mutableStateOf("LawAssist") }

    // Get font size from settings
    val currentFontSize by settingsManager.fontSize.collectAsState()

    // State for showing font size controls
    var showFontSizeControls by remember { mutableStateOf(false) }

    // Get selected model from settings
    val selectedModel by settingsManager.selectedModel.collectAsState()

    // Observe voice input results
    LaunchedEffect(voiceViewModel.voiceInputResult.value) {
        val result = voiceViewModel.voiceInputResult.value
        if (result.isNotBlank()) {
            userInput = result
            voiceViewModel.clearVoiceInputResult()
            
            // Automatically send the voice input
            if (result.isNotBlank()) {
                // Add user message to chat
                messages.add(
                    ChatMessage(
                        userId = "/users/${auth.currentUser?.uid}",
                        role = "user",
                        text = result,
                        timestamp = System.currentTimeMillis()
                    )
                )
                
                // Save user message to Firestore
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val chatMessage = hashMapOf(
                        "userId" to "/users/$userId",
                        "role" to "user",
                        "text" to result,
                        "timestamp" to Date()
                    )
                    
                    FirebaseFirestore.getInstance().collection("chat_history")
                        .add(chatMessage)
                        .addOnSuccessListener {
                            android.util.Log.d("MainActivity", "User message saved to Firestore")
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("MainActivity", "Error saving user message: ${e.message}")
                        }
                }
                
                // Show loading indicator
                messages.add(
                    ChatMessage(
                        userId = "/users/${auth.currentUser?.uid}",
                        role = "assistant",
                        text = "Thinking...",
                        timestamp = System.currentTimeMillis()
                    )
                )
                
                // Query AI for response
                coroutineScope.launch {
                    try {
                        val response = viewModel.queryGroqLlama(result, settingsManager.getSelectedModel())
                        
                        // Remove loading message
                        val loadingIndex = messages.indexOfLast { it.text == "Thinking..." }
                        if (loadingIndex != -1) {
                            messages.removeAt(loadingIndex)
                        }
                        
                        // Add AI response to messages
                        messages.add(
                            ChatMessage(
                                userId = "/users/${auth.currentUser?.uid}",
                                role = "assistant",
                                text = response,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        
                        // Save AI response to Firestore
                        if (userId != null) {
                            val aiMessage = hashMapOf(
                                "userId" to "/users/$userId",
                                "role" to "assistant",
                                "text" to response,
                                "timestamp" to Date()
                            )
                            
                            FirebaseFirestore.getInstance().collection("chat_history")
                                .add(aiMessage)
                                .addOnSuccessListener {
                                    android.util.Log.d("MainActivity", "AI response saved to Firestore")
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("MainActivity", "Error saving AI response: ${e.message}")
                                }
                        }
                    } catch (e: Exception) {
                        // Remove loading message
                        val loadingIndex = messages.indexOfLast { it.text == "Thinking..." }
                        if (loadingIndex != -1) {
                            messages.removeAt(loadingIndex)
                        }
                        
                        // Show error message
                        Toast.makeText(
                            context,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Add error message to chat
                        messages.add(
                            ChatMessage(
                                userId = "/users/${auth.currentUser?.uid}",
                                role = "assistant",
                                text = "Sorry, I encountered an error. Please try again later.",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                
                // Clear the input field
                userInput = ""
            }
        }
    }

    // Scroll to bottom when new message is added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Update speaking state
    LaunchedEffect(textToSpeechService.isSpeaking) {
        isSpeaking = textToSpeechService.isSpeaking
    }

    // Simpler approach to handle back button press
    val onBackPressedCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerState.isOpen) {
                    scope.launch {
                        drawerState.close()
                    }
                } else {
                    isEnabled = false
                    context.findActivity()?.onBackPressedDispatcher?.onBackPressed()
                    isEnabled = true
                }
            }
        }
    }

    // Register the callback
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.onBackPressedDispatcher?.addCallback(onBackPressedCallback)
        onDispose {
            onBackPressedCallback.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LawAssist",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * currentFontSize
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    // Font size controls toggle
                    IconButton(onClick = { showFontSizeControls = !showFontSizeControls }) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Font Size"
                        )
                    }

                    // Model selection info
                    Box {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Model Info"
                            )
                        }

                        Text(
                            text = selectedModel.split("-").firstOrNull() ?: "LLaMA",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column {
                // Font size controls
                if (showFontSizeControls) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
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

                            Text(
                                text = "Font Size: ${String.format("%.1f", currentFontSize)}x",
                                style = MaterialTheme.typography.bodyMedium
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
                // Chat input bar
                ChatInputBar(
                    userInput = userInput,
                    onUserInputChange = { userInput = it },
                    onMessageSent = {
                        if (userInput.isNotBlank()) {
                            // Add user message to chat
                            val userMessage = ChatMessage(
                                userId = "/users/${auth.currentUser?.uid}",
                                role = "user",
                                text = userInput,
                                timestamp = System.currentTimeMillis()
                            )
                            messages.add(userMessage)
                            
                            // Save user message to Firestore
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                val chatMessage = hashMapOf(
                                    "userId" to "/users/$userId",
                                    "role" to "user",
                                    "text" to userInput,
                                    "timestamp" to Date()
                                )
                                
                                FirebaseFirestore.getInstance().collection("chat_history")
                                    .add(chatMessage)
                                    .addOnSuccessListener {
                                        android.util.Log.d("MainActivity", "User message saved to Firestore with ID: ${it.id}")
                                    }
                                    .addOnFailureListener { e ->
                                        android.util.Log.e("MainActivity", "Error saving user message: ${e.message}")
                                    }
                            }
                            
                            // Store the user input before clearing it
                            val userInputText = userInput
                            
                            // Clear the input field immediately
                            userInput = ""
                            
                            // Show loading indicator
                            val loadingMessage = ChatMessage(
                                userId = "/users/${auth.currentUser?.uid}",
                                role = "assistant",
                                text = "Thinking...",
                                timestamp = System.currentTimeMillis()
                            )
                            messages.add(loadingMessage)
                            
                            // Query AI for response
                            coroutineScope.launch {
                                try {
                                    val response = viewModel.queryGroqLlama(userInputText, settingsManager.getSelectedModel())
                                    
                                    // Remove loading message
                                    val loadingIndex = messages.indexOfLast { it.text == "Thinking..." }
                                    if (loadingIndex != -1) {
                                        messages.removeAt(loadingIndex)
                                    }
                                    
                                    // Add AI response to messages
                                    val aiMessage = ChatMessage(
                                        userId = "/users/${auth.currentUser?.uid}",
                                        role = "assistant",
                                        text = response,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    messages.add(aiMessage)
                                    
                                    // Save AI response to Firestore
                                    if (userId != null) {
                                        val aiMessageData = hashMapOf(
                                            "userId" to "/users/$userId",
                                            "role" to "assistant",
                                            "text" to response,
                                            "timestamp" to Date()
                                        )
                                        
                                        FirebaseFirestore.getInstance().collection("chat_history")
                                            .add(aiMessageData)
                                            .addOnSuccessListener {
                                                android.util.Log.d("MainActivity", "AI response saved to Firestore with ID: ${it.id}")
                                            }
                                            .addOnFailureListener { e ->
                                                android.util.Log.e("MainActivity", "Error saving AI response: ${e.message}")
                                            }
                                    }
                                } catch (e: Exception) {
                                    // Remove loading message
                                    val loadingIndex = messages.indexOfLast { it.text == "Thinking..." }
                                    if (loadingIndex != -1) {
                                        messages.removeAt(loadingIndex)
                                    }
                                    
                                    // Show error message
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    
                                    // Add error message to chat
                                    messages.add(
                                        ChatMessage(
                                            userId = "/users/${auth.currentUser?.uid}",
                                            role = "assistant",
                                            text = "Sorry, I encountered an error. Please try again later.",
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    
                                    // Log the error
                                    android.util.Log.e("MainActivity", "Error getting AI response: ${e.message}", e)
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onVoiceInputRequested = {
                        if (isListening) {
                            onVoiceInputStop()
                        } else {
                            onVoiceInputRequested()
                        }
                    },
                    isListening = isListening,
                    fontSize = currentFontSize
                )
            }
        }
    ) { innerPadding ->
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerContentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Drawer header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Law Assist",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize * currentFontSize
                            )
                        )
                    }

                    Divider()

                    // New chat button
                    ListItem(
                        headlineContent = {
                            Text(
                                "New Chat",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * currentFontSize
                                )
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat"
                            )
                        },
                        modifier = Modifier.clickable {
                            onNewChat()
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )

                    // Settings button
                    ListItem(
                        headlineContent = {
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * currentFontSize
                                )
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        modifier = Modifier.clickable {
                            navController.navigate("settings")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )

                    Divider()

                    // Logout button
                    ListItem(
                        headlineContent = {
                            Text(
                                "Logout",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * currentFontSize
                                )
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout"
                            )
                        },
                        modifier = Modifier.clickable {
                            auth.signOut()
                            navController.navigate("welcome") {
                                popUpTo(0)
                            }
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(
                        message = message,
                        onSpeakClicked = { text ->
                            if (isSpeaking) {
                                textToSpeechService.stop()
                            } else {
                                textToSpeechService.speak(text)
                            }
                        },
                        isSpeaking = isSpeaking && message.role == "assistant",
                        fontSize = currentFontSize
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeakClicked: (String) -> Unit,
    isSpeaking: Boolean,
    fontSize: Float = 1.0f
) {
    val isUserMessage = message.role == "user"
    val bubbleColor = if (isUserMessage) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = if (isUserMessage) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    val alignment = if (isUserMessage) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUserMessage) 16.dp else 0.dp,
                        bottomEnd = if (isUserMessage) 0.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUserMessage) 16.dp else 0.dp,
                        bottomEnd = if (isUserMessage) 0.dp else 16.dp
                    )
                )
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUserMessage) 16.dp else 0.dp,
                        bottomEnd = if (isUserMessage) 0.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontSize
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = MaterialTheme.typography.bodySmall.fontSize * fontSize
                        ),
                        color = textColor.copy(alpha = 0.7f)
                    )

                    // Only show speak button for assistant messages
                    if (!isUserMessage) {
                        IconButton(
                            onClick = { onSpeakClicked(message.text) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = if (isSpeaking) "Stop Speaking" else "Speak Message",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onMessageSent: () -> Unit,
    onVoiceInputRequested: () -> Unit,
    isListening: Boolean = false,
    fontSize: Float = 1.0f
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text input field
            OutlinedTextField(
                value = userInput,
                onValueChange = onUserInputChange,
                placeholder = {
                    Text(
                        "Type your message...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontSize
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { 
                        if (userInput.isNotBlank()) {
                            onMessageSent()
                        } else {
                            Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
                        }
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                maxLines = 5,
                singleLine = false
            )

            // Voice input button
            IconButton(
                onClick = onVoiceInputRequested,
                modifier = Modifier
                    .padding(4.dp)
                    .size(56.dp) // Increased size
                    .clip(CircleShape)
                    .background(
                        if (isListening) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary // Changed to primary color
                    )
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                    contentDescription = if (isListening) "Stop Voice Input" else "Start Voice Input",
                    tint = if (isListening) MaterialTheme.colorScheme.onError
                           else MaterialTheme.colorScheme.onPrimary // Changed to onPrimary
                )
            }

            // Send button
            IconButton(
                onClick = { 
                    if (userInput.isNotBlank()) {
                        onMessageSent()
                    } else {
                        Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .padding(4.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}