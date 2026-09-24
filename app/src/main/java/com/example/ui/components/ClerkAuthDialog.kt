package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.ClerkUserSummary
import com.example.ui.theme.DetoxMint
import com.example.ui.theme.LimitRose

@Composable
fun ClerkAuthDialog(
    isSignedIn: Boolean,
    currentUser: ClerkUserSummary?,
    publishableKey: String,
    onSignIn: (String, String, (Result<ClerkUserSummary>) -> Unit) -> Unit,
    onSignUp: (String, String, (Result<ClerkUserSummary>) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onUpdatePublishableKey: (String) -> Unit,
    onSignInLocal: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (isSignedIn && currentUser != null) {
        // ── Signed In Profile View ───────────────────────────────
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6C47FF).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF6C47FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF8B70FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Parent Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Authenticated via Clerk",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6C47FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(DetoxMint.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser.fullName.take(1).uppercase(),
                                    color = DetoxMint,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentUser.fullName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = currentUser.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = DetoxMint,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active Parent Session",
                                color = DetoxMint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "User ID: ${currentUser.id}",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Key Config Expander
                    var showKeyConfig by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showKeyConfig = !showKeyConfig }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Clerk Publishable Key",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = if (showKeyConfig) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = showKeyConfig) {
                        var customKeyInput by remember { mutableStateOf(publishableKey) }
                        Column {
                            OutlinedTextField(
                                value = customKeyInput,
                                onValueChange = { customKeyInput = it },
                                label = { Text("Publishable Key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6C47FF),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { onUpdatePublishableKey(customKeyInput) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                            ) {
                                Text("Update Key", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DetoxMint, contentColor = Color(0xFF042F2E))
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        onSignOut()
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LimitRose),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LimitRose.copy(alpha = 0.6f))
                ) {
                    Text("Sign Out")
                }
            },
            containerColor = Color(0xFF131926)
        )
    } else {
        // ── Sign In / Sign Up View ───────────────────────────────
        var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sign In, 1 = Sign Up
        var emailInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var showKeyConfig by remember { mutableStateOf(false) }
        var customKeyInput by remember { mutableStateOf(publishableKey) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF6C47FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "C",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Parent Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Powered by Clerk Authentication",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6C47FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Selector: Sign In / Create Account
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFF6C47FF)
                            )
                        },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0; errorMessage = null },
                            text = {
                                Text(
                                    "Sign In",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1; errorMessage = null },
                            text = {
                                Text(
                                    "Create Account",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (selectedTab == 0)
                            "Sign in with your Clerk parent account to sync wellbeing settings."
                        else
                            "Create a parent account to protect and monitor your child's device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    // Email Field
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; errorMessage = null },
                        label = { Text("Email Address") },
                        placeholder = { Text("parent@example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF94A3B8))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clerk_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6C47FF),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; errorMessage = null },
                        label = { Text("Password") },
                        placeholder = { Text(if (selectedTab == 0) "Enter password" else "Min 8 characters") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!isLoading) {
                                    isLoading = true
                                    if (selectedTab == 0) {
                                        onSignIn(emailInput, passwordInput) { res ->
                                            isLoading = false
                                            res.onFailure { errorMessage = it.message }
                                            res.onSuccess { onDismiss() }
                                        }
                                    } else {
                                        onSignUp(emailInput, passwordInput) { res ->
                                            isLoading = false
                                            res.onFailure { errorMessage = it.message }
                                            res.onSuccess { onDismiss() }
                                        }
                                    }
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clerk_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6C47FF),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Error message
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = LimitRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (onSignInLocal != null) {
                        TextButton(
                            onClick = {
                                val email = emailInput.trim().ifBlank { "parent@detoxwall.com" }
                                onSignInLocal(email)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Or continue with Local Parent Account",
                                color = DetoxMint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Expandable Clerk Publishable Key Configuration
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showKeyConfig = !showKeyConfig }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFF6C47FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clerk API Key Settings",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Icon(
                            imageVector = if (showKeyConfig) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = showKeyConfig) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Your Clerk Publishable Key (pk_test_...):",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customKeyInput,
                                onValueChange = { customKeyInput = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6C47FF),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    onUpdatePublishableKey(customKeyInput)
                                    errorMessage = "Clerk Publishable Key updated!"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                            ) {
                                Text("Save Key", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        if (selectedTab == 0) {
                            onSignIn(emailInput, passwordInput) { res ->
                                isLoading = false
                                res.onFailure { errorMessage = it.message }
                                res.onSuccess { onDismiss() }
                            }
                        } else {
                            onSignUp(emailInput, passwordInput) { res ->
                                isLoading = false
                                res.onFailure { errorMessage = it.message }
                                res.onSuccess { onDismiss() }
                            }
                        }
                    },
                    enabled = !isLoading && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C47FF),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("clerk_submit_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (selectedTab == 0) "Sign In" else "Create Account", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF131926)
        )
    }
}
