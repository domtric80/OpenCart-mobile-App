package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OpenCartDanger
import com.example.ui.theme.OpenCartDangerContainer
import com.example.ui.theme.OpenCartSuccess
import com.example.ui.theme.OpenCartSuccessContainer
import com.example.ui.theme.OpenCartWarning
import com.example.ui.theme.OpenCartWarningContainer

@Composable
fun OrderStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor) = when (status) {
        "In attesa" -> Triple(OpenCartWarningContainer, Color(0xFF92400E), OpenCartWarning)
        "In lavorazione" -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), Color(0xFF3B82F6))
        "Confermato" -> Triple(Color(0xFFE0E7FF), Color(0xFF3730A3), Color(0xFF6366F1))
        "Spedito" -> Triple(Color(0xFFEDE9FE), Color(0xFF5B21B6), Color(0xFF8B5CF6))
        "Consegnato" -> Triple(OpenCartSuccessContainer, Color(0xFF065F46), OpenCartSuccess)
        "Annullato" -> Triple(OpenCartDangerContainer, Color(0xFF991B1B), OpenCartDanger)
        "Rimborsato" -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Color(0xFF64748B))
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF334155), Color(0xFF64748B))
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 5.dp)
        )
    }
}

@Composable
fun StockBadge(
    quantity: Int,
    minQuantity: Int = 5,
    modifier: Modifier = Modifier
) {
    val triple: Triple<Color, Color, String> = when {
        quantity <= 0 -> Triple(OpenCartDangerContainer, OpenCartDanger, "Esaurito (0)")
        quantity <= minQuantity -> Triple(OpenCartWarningContainer, Color(0xFF92400E), "Scorte Basse ($quantity pz)")
        else -> Triple(OpenCartSuccessContainer, Color(0xFF065F46), "Disponibile ($quantity pz)")
    }
    val bgColor = triple.first
    val textColor = triple.second
    val label = triple.third

    Text(
        text = label,
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

@Composable
fun CustomerGroupBadge(
    group: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (group) {
        "VIP" -> Pair(Color(0xFFFEF3C7), Color(0xFF92400E))
        "B2B", "Wholesale" -> Pair(Color(0xFFEDE9FE), Color(0xFF5B21B6))
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Text(
        text = group,
        color = textColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
