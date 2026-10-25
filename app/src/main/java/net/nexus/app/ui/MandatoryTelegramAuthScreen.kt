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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.nexus.app.BuildConfig
import net.nexus.app.MainViewModel
import net.nexus.app.R

@Composable
fun MandatoryTelegramAuthScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(Modifier.fillMaxSize().background(NX.bg)) {
        // Ambient neon purple glow background
        Canvas(Modifier.fillMaxSize()) {
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

            Spacer(Modifier.height(24.dp))

            // Hero icon with pulsing circle
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(NX.accent.copy(alpha = 0.12f))
                )
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NX.accent, NX.accentDim)
                            )
                        )
                        .border(1.5.dp, NX.accentLight.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Title & Description
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.mandatory_auth_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NX.text
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.mandatory_auth_subtitle),
                    fontSize = 14.sp,
                    color = NX.muted2,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Feature Highlights Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NX.card)
                    .border(1.dp, NX.cardBorder, RoundedCornerShape(20.dp))
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

            Spacer(Modifier.height(30.dp))

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
                        contentColor = Color.White
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

                Text(
                    text = "NEXUS.net v${BuildConfig.VERSION_NAME}",
                    fontSize = 12.sp,
                    color = NX.muted,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
