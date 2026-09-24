package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DetoxMint
import com.example.ui.theme.LimitRose

@Composable
fun PinSetupDialog(
    onSavePin: (String) -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Save PIN"
) {
    var pinInput by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Parent 4-Digit PIN",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = "Create a 4-digit PIN to prevent unauthorized changes and protect parental settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it
                    },
                    label = { Text("4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pinConfirm,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) pinConfirm = it
                    },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_confirm_input")
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = LimitRose,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinInput.length != 4) {
                        errorMessage = "PIN must be exactly 4 digits"
                    } else if (pinInput != pinConfirm) {
                        errorMessage = "PINs do not match"
                    } else {
                        onSavePin(pinInput)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DetoxMint, contentColor = Color(0xFF042F2E)),
                modifier = Modifier.testTag("save_parent_pin_btn")
            ) {
                Text(confirmButtonText)
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

@Composable
fun ChangePinDialog(
    onVerifyCurrentPin: (String) -> Boolean,
    onChangePin: (String, String) -> Boolean,
    onDismiss: () -> Unit
) {
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change Parent PIN",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your current PIN, then choose a new 4-digit PIN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                OutlinedTextField(
                    value = currentPinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            currentPinInput = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Current 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_current_pin_input")
                )

                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            newPinInput = it
                            errorMessage = null
                        }
                    },
                    label = { Text("New 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_new_pin_input")
                )

                OutlinedTextField(
                    value = confirmPinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            confirmPinInput = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_confirm_pin_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = LimitRose,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!onVerifyCurrentPin(currentPinInput)) {
                        errorMessage = "Current PIN is incorrect"
                    } else if (newPinInput.length != 4) {
                        errorMessage = "New PIN must be exactly 4 digits"
                    } else if (newPinInput == currentPinInput) {
                        errorMessage = "New PIN must be different from current PIN"
                    } else if (newPinInput != confirmPinInput) {
                        errorMessage = "New PINs do not match"
                    } else {
                        val success = onChangePin(currentPinInput, newPinInput)
                        if (success) {
                            onDismiss()
                        } else {
                            errorMessage = "Failed to update PIN"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DetoxMint, contentColor = Color(0xFF042F2E)),
                modifier = Modifier.testTag("save_changed_pin_btn")
            ) {
                Text("Save New PIN")
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

@Composable
fun RemovePinDialog(
    onRemovePin: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var currentPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Remove Parent PIN?",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your current PIN to remove parent protection. Child Mode will be deactivated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                OutlinedTextField(
                    value = currentPinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            currentPinInput = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Current 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remove_pin_verify_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = LimitRose,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val removed = onRemovePin(currentPinInput)
                    if (removed) {
                        onDismiss()
                    } else {
                        errorMessage = "Incorrect PIN. Cannot remove protection."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LimitRose, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_remove_pin_btn")
            ) {
                Text("Remove Protection")
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
