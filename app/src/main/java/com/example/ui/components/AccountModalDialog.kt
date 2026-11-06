package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KurdPlayBluePrimary
import com.example.ui.theme.KurdPlaySurface

@Composable
fun AccountModalDialog(
    initialUnlocked: Boolean = true,
    onDismiss: () -> Unit,
    onLoginSubscriber: (username: String, password: String) -> Boolean,
    onSaveXtream: ((name: String, host: String, user: String, pass: String, plan: String) -> Unit)? = null,
    onSaveM3u: ((name: String, url: String, plan: String) -> Unit)? = null
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        containerColor = KurdPlaySurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "داخڵبوونی ئەکاونتی VIP / VIP Login",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_close_btn")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "تکایە یوزەرنەیم و پاسۆردی ئەکاونتەکەت بنووسە بۆ چالاککردنی VIP:",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )

                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = null
                    },
                    label = { Text("ناوی بەکارهێنەر / Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = KurdPlayBluePrimary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vip_user_input"),
                    singleLine = true
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("تێپەڕەوشە / Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = KurdPlayBluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vip_pass_input"),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = Color.Red, fontSize = 11.sp)
                }

                // Device limit info notice
                Surface(
                    color = Color(0xFF00B4DB).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00B4DB), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تەنها ۱ ئامێر دەتوانێت بەکاری بهێنێت (1 Device Max Limit)",
                            color = Color(0xFF00B4DB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            errorMessage = "❌ تکایە هەردوو خانەی یوزەرنەیم و تێپەڕەوشە پڕبکەرەوە!"
                        } else {
                            val success = onLoginSubscriber(username.trim(), password.trim())
                            if (!success) {
                                errorMessage = "❌ ناوی بەکارهێنەر یان تێپەڕەوشە هەڵەیە! یوزەر لە داتابەیس نەدۆزرایەوە."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("vip_login_btn")
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("چوونەژوورەوەی VIP / Login", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    )
}
