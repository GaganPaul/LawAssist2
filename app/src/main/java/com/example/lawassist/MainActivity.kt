package com.example.lawassist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lawassist.database.LawDatabase
import com.example.lawassist.repository.LawRepository
import com.example.lawassist.ui.theme.LawAssistTheme
import com.example.lawassist.ui.theme.LoginScreen
import com.example.lawassist.ui.theme.RegisterScreen
import com.example.lawassist.viewmodel.LawViewModel
import com.example.lawassist.viewmodel.LawViewModelFactory
import com.example.lawassist.viewmodel.VoiceViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

// Message data class to store chat history
data class ChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

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

        // Initialize law data at startup
      //  lawViewModel.getAllLaws()

        setContent {
            LawAssistTheme {
                val navController = rememberNavController()
                LaunchedEffect(Unit) {
                    if (auth.currentUser != null) {
                        navController.navigate("chat") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }

                // Initialize the chat messages list here to be preserved across navigation
                val chatMessages = remember { mutableStateListOf<ChatMessage>() }

                NavigationComponent(
                    navController = navController,
                    userInputState = userInputState,
                    onVoiceInputRequested = { checkAndRequestMicrophonePermission() },
                    onVoiceInputStop = { voiceViewModel.stopVoiceRecognition() },
                    lawViewModel = lawViewModel,
                    voiceViewModel = voiceViewModel,
                    auth = auth,
                    firestore = firestore,
                    chatMessages = chatMessages
                )
            }
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
    chatMessages: MutableList<ChatMessage>
) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    // Reset or refresh viewModel state when logging in
                   // lawViewModel.getAllLaws()
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
                messages = chatMessages
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
    messages: MutableList<ChatMessage>
) {
    val aiResponse by viewModel.aiResponse
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // State for the text field
    var userInput by userInputState

    // Observe voice input status
    val isListening by voiceViewModel.isListening

    // Observe voice input and update the text field
    LaunchedEffect(Unit) {
        voiceViewModel.onTextReceived = { recognizedText ->
            if (recognizedText.isNotEmpty()) {
                userInput = recognizedText

                // Automatically send the message after updating the input
                messages.add(ChatMessage(userInput, true))
                viewModel.queryGroqLlama(userInput)
                userInput = ""

                coroutineScope.launch {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }

    // Add initial greeting message if chat is empty
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(
                ChatMessage(
                    "I'm LawAssist, your AI assistant for laws, government schemes, and services in India.",
                    false
                )
            )
        }
    }

    // Process AI response and scroll to latest message
    LaunchedEffect(aiResponse) {
        if (aiResponse.isNotEmpty() && !messages.any { !it.isFromUser && it.content == aiResponse }) {
            messages.add(ChatMessage(aiResponse, false))
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Law Assist", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    TextButton(
                        onClick = {
                            auth.signOut()
                            // Reset the AI state
                            viewModel.resetResponseState()
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Logout", color = Color.Red)
                    }
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages) { message -> ChatBubble(message) }
                }

                // Show listening indicator when active
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Listening...",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Chat input bar with voice button
            ChatInputBar(
                userInput = userInput,
                onUserInputChange = { userInput = it },
                onMessageSent = {
                    if (userInput.isNotEmpty()) {
                        messages.add(ChatMessage(userInput, true))
                        viewModel.queryGroqLlama(userInput)

                        // Search for relevant laws based on user input
                       // viewModel.searchLaws(userInput)

                        userInput = ""
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                },
                onVoiceInputRequested = {
                    if (isListening) {
                        onVoiceInputStop()
                    } else {
                        onVoiceInputRequested()
                    }
                },
                isListening = isListening
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (message.isFromUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondaryContainer
                )
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Text(
                text = message.content,
                color = if (message.isFromUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ChatInputBar(
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onMessageSent: () -> Unit,
    onVoiceInputRequested: () -> Unit,
    isListening: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = userInput,
                onValueChange = onUserInputChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Type your question...") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onMessageSent()
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                trailingIcon = {
                    Row {
                        // Mic Button with changing icon and color based on listening state
                        IconButton(
                            onClick = { onVoiceInputRequested() }
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                                contentDescription = if (isListening) "Stop Voice Input" else "Voice Input",
                                tint = if (isListening)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        }

                        // Upload Button (sends the message)
                        IconButton(
                            onClick = { onMessageSent() },
                            enabled = userInput.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Upload,
                                contentDescription = "Send Message",
                                tint = if (userInput.isNotEmpty())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    val lawDao = LawDatabase.getDatabase(context).lawDao()
    val lawRepository = LawRepository(lawDao)
    val viewModel = LawViewModel(lawRepository)
    val voiceViewModel = VoiceViewModel()
    val navController = rememberNavController()
    val previewInputState = remember { mutableStateOf("") }
    val previewAuth = FirebaseAuth.getInstance()
    val previewMessages = remember { mutableStateListOf<ChatMessage>() }

    LawAssistTheme {
        ModernChatScreen(
            viewModel = viewModel,
            voiceViewModel = voiceViewModel,
            userInputState = previewInputState,
            onVoiceInputRequested = { /* Preview only */ },
            onVoiceInputStop = { /* Preview only */ },
            navController = navController,
            auth = previewAuth,
            messages = previewMessages
        )
    }
}