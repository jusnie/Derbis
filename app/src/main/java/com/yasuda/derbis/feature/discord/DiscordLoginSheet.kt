package com.yasuda.derbis.feature.discord

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import com.yasuda.derbis.ui.icons.CheckCircle
import com.yasuda.derbis.ui.icons.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.NetworkCheck
import com.yasuda.derbis.ui.icons.Refresh
import com.yasuda.derbis.ui.icons.Visibility
import com.yasuda.derbis.ui.icons.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

/**
 * Discord Login & Authentication Sheet (OuterTune pattern).
 *
 * Offers two seamless login options:
 *  1. Normal Login: Integrated Web login with automated token extraction.
 *  2. Token Login: Direct Discord user token entry with instant connection test.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginSheet(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Normal Login, 1: Token Login

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF101115),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discord Login",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color.White,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Normal Login", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Token Login", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> DiscordWebLoginContent(
                    onTokenFound = { token ->
                        DerbisDiscordRichPresence.loginWithToken(context, token) { result ->
                            if (result.success) {
                                onSuccess()
                            }
                        }
                    }
                )
                1 -> DiscordTokenLoginContent(
                    onSuccess = onSuccess
                )
            }
        }
    }
}

/**
 * Normal Login: Embedded web browser that extracts token upon login.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DiscordWebLoginContent(
    onTokenFound: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0C))
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    CookieManager.getInstance().setAcceptCookie(true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            // Intercept token from localStorage after login
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    try {
                                        let token = window.localStorage.getItem('token');
                                        if (token) return token.replace(/"/g, '');
                                    } catch(e) {}
                                    return '';
                                })();
                                """.trimIndent()
                            ) { result ->
                                val cleanToken = result?.trim('"', ' ', '\n').orEmpty()
                                if (cleanToken.isNotBlank() && cleanToken != "null") {
                                    onTokenFound(cleanToken)
                                }
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            // Detect authorization header from Discord API calls
                            val authHeader = request?.requestHeaders?.get("authorization")
                                ?: request?.requestHeaders?.get("Authorization")
                            if (!authHeader.isNullOrBlank() && !authHeader.startsWith("Bearer", ignoreCase = true)) {
                                post { onTokenFound(authHeader.trim()) }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    loadUrl("https://discord.com/login")
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Token Login: Direct user token input with instant connection verification.
 */
@Composable
private fun DiscordTokenLoginContent(
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var tokenInput by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<DiscordDiagnosticResult?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Paste your Discord user token to connect immediately without opening a browser.",
            fontSize = 13.sp,
            color = Color.LightGray,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = tokenInput,
            onValueChange = {
                tokenInput = it
                testResult = null
            },
            placeholder = { Text("Discord User Token", color = Color.Gray, fontSize = 14.sp) },
            singleLine = true,
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (tokenInput.isNotBlank() && !isChecking) {
                    isChecking = true
                    DerbisDiscordRichPresence.loginWithToken(context, tokenInput) { res ->
                        isChecking = false
                        testResult = res
                        if (res.success) onSuccess()
                    }
                }
            }),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(
                            if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = "Toggle token visibility",
                            tint = Color.LightGray
                        )
                    }
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                        if (clip.isNotBlank()) {
                            tokenInput = clip.trim()
                            testResult = null
                        }
                    }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste token", tint = Color.LightGray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color(0xFF2C2E38),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedContainerColor = Color(0xFF16181F),
                unfocusedContainerColor = Color(0xFF16181F)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Connect & Verify Button
        Button(
            onClick = {
                isChecking = true
                DerbisDiscordRichPresence.loginWithToken(context, tokenInput) { res ->
                    isChecking = false
                    testResult = res
                    if (res.success) onSuccess()
                }
            },
            enabled = tokenInput.isNotBlank() && !isChecking,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF242630),
                disabledContentColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    color = Color.Black,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verifying Connection...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Discord", fontWeight = FontWeight.Bold)
            }
        }

        // Diagnostic Result Box
        AnimatedVisibility(
            visible = testResult != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            testResult?.let { res ->
                val bg = if (res.success) Color(0xFF0F291E) else Color(0xFF2E1215)
                val border = if (res.success) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                val icon = if (res.success) Icons.Rounded.CheckCircle else Icons.Rounded.Error
                val iconTint = if (res.success) Color(0xFF4CAF50) else Color(0xFFEF5350)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (res.success) "Connection Verified" else "Connection Failed",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        if (res.latencyMs > 0L) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${res.latencyMs} ms",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = res.message,
                        fontSize = 13.sp,
                        color = Color(0xFFE0E0E0),
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
