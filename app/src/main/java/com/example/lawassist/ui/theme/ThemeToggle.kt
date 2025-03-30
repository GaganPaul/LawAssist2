package com.example.lawassist.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

@Composable
fun ThemeToggle(
    modifier: Modifier = Modifier
) {
    // Get the current theme state
    val isDarkTheme = isDarkMode()
    
    // Remember the state locally to avoid recomposition issues
    var isChecked by remember { mutableStateOf(isDarkTheme) }
    
    // Get the context for theme persistence
    val context = LocalContext.current
    
    // Get the lifecycle owner for activity recreation
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Observe the lifecycle to recreate the activity when the theme changes
    LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            if (isChecked != isDarkMode()) {
                isChecked = isDarkMode()
            }
        }
    }.also { lifecycleOwner.lifecycle.addObserver(it) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable {
                    // Toggle the theme when the entire row is clicked
                    isChecked = !isChecked
                    toggleTheme(context)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isChecked) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = "Theme mode",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = if (isChecked) "Dark Mode" else "Light Mode",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Switch(
                checked = isChecked,
                onCheckedChange = { 
                    isChecked = it
                    toggleTheme(context) 
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                    uncheckedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            )
        }
    }
}
