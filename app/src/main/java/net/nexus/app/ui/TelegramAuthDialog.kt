package net.nexus.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.nexus.app.MainViewModel
import net.nexus.app.R
import net.nexus.app.auth.TelegramAuthState

@Composable
fun TelegramAuthDialog(state: TelegramAuthState, vm: MainViewModel) {
    if (state is TelegramAuthState.Idle) return

    val ctx = LocalContext.current

    LaunchedEffect(state) {
        if (state is TelegramAuthState.WaitingConfirmation) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.botUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            }
        }
    }

    Dialog(
        onDismissRequest = { vm.dismissTelegramAuthDialog() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(NX.card)
                .border(1.dp, NX.cardBorder, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state) {
                    is TelegramAuthState.CreatingSession -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NX.accentDim),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = NX.accentLight,
                                strokeWidth = 3.dp
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.tg_auth_dialog_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NX.text
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Подготовка защищенной сессии входа…",
                            fontSize = 14.sp,
                            color = NX.muted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        TextButton(
                            onClick = { vm.dismissTelegramAuthDialog() },
                            colors = ButtonDefaults.textButtonColors(contentColor = NX.muted)
                        ) {
                            Text(stringResource(R.string.update_btn_cancel))
                        }
                    }

                    is TelegramAuthState.WaitingConfirmation -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NX.accentDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = NX.accentLight,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = stringResource(R.string.tg_auth_dialog_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NX.text
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.tg_auth_dialog_desc),
                            fontSize = 14.sp,
                            color = NX.muted2,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(20.dp))

                        // Open bot button
                        Button(
                            onClick = {
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.botUrl)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    ctx.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NX.accent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.tg_auth_btn_open_bot),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = NX.accentLight,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.tg_auth_waiting),
                                fontSize = 13.sp,
                                color = NX.muted
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = { vm.dismissTelegramAuthDialog() },
                            colors = ButtonDefaults.textButtonColors(contentColor = NX.muted)
                        ) {
                            Text(stringResource(R.string.update_btn_cancel))
                        }
                    }

                    is TelegramAuthState.Success -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NX.green.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = NX.green,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = "Успешный вход!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NX.text
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.user.formattedUsername,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NX.accentLight
                        )
                        Spacer(Modifier.height(6.dp))
                        if (state.serverCount > 0) {
                            Text(
                                text = "Подписка активна • Загружено серверов: ${state.serverCount}",
                                fontSize = 14.sp,
                                color = NX.green,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.tg_auth_no_subscription),
                                fontSize = 13.sp,
                                color = NX.yellow,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(22.dp))
                        Button(
                            onClick = { vm.dismissTelegramAuthDialog() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NX.accent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Отлично", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    is TelegramAuthState.Error -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NX.red.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = NX.red,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = "Ошибка авторизации",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NX.text
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = NX.muted2,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(22.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { vm.dismissTelegramAuthDialog() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Закрыть", color = NX.muted2)
                            }
                            Button(
                                onClick = { vm.startTelegramAuth() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NX.accent),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Повторить", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    TelegramAuthState.Idle -> Unit
                }
            }
        }
    }
}
