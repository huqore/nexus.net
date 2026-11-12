package net.nexus.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import net.nexus.app.BuildConfig
import net.nexus.app.MainViewModel
import net.nexus.app.R

@Composable
fun MandatoryTelegramAuthScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    var showWebLoginDialog by remember { mutableStateOf(false) }

    val style = NX.style
    // Liquid Glass keeps the shared glass backdrop visible; M3 uses a flat surface (no neon glow).
    Box(Modifier.fillMaxSize().then(if (style == net.nexus.app.data.DesignStyle.LIQUID_GLASS) Modifier else Modifier.background(NX.bg))) {
        // Ambient neon purple glow background
        if (style != net.nexus.app.data.DesignStyle.MATERIAL3) Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.35f)
            val radius = size.width * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NX.accent.copy(alpha = 0.22f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("NEXUS", fontSize = 42.sp, fontWeight = FontWeight.Black, color = NX.text)
                    Text(".net", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = NX.accentLight)
                }
                Spacer(Modifier.height(4.dp))
                Text("VPN", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NX.muted, letterSpacing = 8.sp)
            }

            Spacer(Modifier.height(28.dp))

            // Feature Highlights Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NX.card)
                    .nxBorder(RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FeatureRow(
                    icon = Icons.Filled.CheckCircle,
                    iconTint = NX.green,
                    text = stringResource(R.string.mandatory_auth_feature_sub)
                )
                FeatureRow(
                    icon = Icons.Filled.Security,
                    iconTint = NX.accentLight,
                    text = stringResource(R.string.mandatory_auth_feature_security)
                )
                FeatureRow(
                    icon = Icons.Filled.Bolt,
                    iconTint = NX.yellow,
                    text = stringResource(R.string.mandatory_auth_feature_speed)
                )
            }

            Spacer(Modifier.height(28.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { vm.startTelegramAuth() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NX.accent,
                        contentColor = NX.onAccent
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.tg_auth_btn_login),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                OutlinedButton(
                    onClick = { showWebLoginDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NX.text
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NX.cardBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        tint = NX.accentLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Войти через сайт",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                TextButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NEXUSnet_robot")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            ctx.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NX.muted2)
                ) {
                    Text(
                        text = stringResource(R.string.tg_auth_btn_open_bot),
                        fontSize = 14.sp
                    )
                }

                TextButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://nexus-net.xyz")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            ctx.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NX.muted2)
                ) {
                    Text(
                        text = "Открыть сайт",
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "NEXUS.net v${BuildConfig.VERSION_NAME}",
                    fontSize = 12.sp,
                    color = NX.muted,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (showWebLoginDialog) {
            WebsiteLoginDialog(
                vm = vm,
                onDismiss = { showWebLoginDialog = false }
            )
        }
    }
}

@Composable
fun WebsiteLoginDialog(
    vm: MainViewModel,
    onDismiss: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoggingIn by vm.isWebLoggingIn.collectAsState()
    val loginError by vm.webLoginError.collectAsState()

    Dialog(
        onDismissRequest = { if (!isLoggingIn) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isLoggingIn,
            dismissOnClickOutside = !isLoggingIn,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .nxDialogSurface(RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(NX.accentDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        tint = NX.accentLight,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Вход через сайт",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NX.text
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Личный кабинет nexus-net.xyz",
                    fontSize = 13.sp,
                    color = NX.muted2,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Логин или Telegram") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NX.accentLight,
                        unfocusedBorderColor = NX.cardBorder,
                        focusedLabelColor = NX.accentLight,
                        unfocusedLabelColor = NX.muted2,
                        focusedTextColor = NX.text,
                        unfocusedTextColor = NX.text,
                        cursorColor = NX.accentLight
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = NX.muted2
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NX.accentLight,
                        unfocusedBorderColor = NX.cardBorder,
                        focusedLabelColor = NX.accentLight,
                        unfocusedLabelColor = NX.muted2,
                        focusedTextColor = NX.text,
                        unfocusedTextColor = NX.text,
                        cursorColor = NX.accentLight
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!loginError.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = loginError ?: "",
                        color = NX.red,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoggingIn,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Отмена", color = NX.muted2, fontSize = 15.sp)
                    }

                    Button(
                        onClick = {
                            vm.loginWithWebsite(identifier, password) {
                                onDismiss()
                            }
                        },
                        enabled = !isLoggingIn && identifier.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NX.accent,
                            contentColor = NX.onAccent,
                            disabledContainerColor = NX.accent.copy(alpha = 0.4f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NX.onAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Войти", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, iconTint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 13.5.sp,
            color = NX.text,
            fontWeight = FontWeight.Medium
        )
    }
}
