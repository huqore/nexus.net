package net.nexus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NexusPingBadge(
    pingMs: Long,
    modifier: Modifier = Modifier
) {
    val indicatorColor = when {
        pingMs <= 0 -> Color(0xFFE57373)
        pingMs < 100 -> Color(0xFF81C784)
        pingMs <= 250 -> Color(0xFFFFD54F)
        else -> Color(0xFFE57373)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF211F26),
        border = BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = "$pingMs ms",
                fontSize = 11.sp,
                color = Color(0xFFCAC4D0)
            )
        }
    }
}
