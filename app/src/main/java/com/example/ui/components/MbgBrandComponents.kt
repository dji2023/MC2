package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.StockStatus
import com.example.model.UserRole
import com.example.ui.theme.MbgBlueAccent
import com.example.ui.theme.MbgBlueContainer
import com.example.ui.theme.MbgGreenContainer
import com.example.ui.theme.MbgGreenPrimary
import com.example.ui.theme.MbgOnBlueContainer
import com.example.ui.theme.MbgOnGreenContainer
import com.example.ui.theme.MbgOnRedContainer
import com.example.ui.theme.MbgOnYellowContainer
import com.example.ui.theme.MbgRedAlert
import com.example.ui.theme.MbgRedContainer
import com.example.ui.theme.MbgYellowAccent
import com.example.ui.theme.MbgYellowContainer

@Composable
fun MbgLogoEmblem(
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size * 0.28f),
        color = MbgGreenPrimary,
        shadowElevation = 3.dp,
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f))
    ) {
        Image(
            painter = painterResource(id = R.drawable.mbg_logo_icon_1795617518243),
            contentDescription = "Logo MBG STOCK",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f))
        )
    }
}

@Composable
fun RoleBadge(role: UserRole, modifier: Modifier = Modifier) {
    val (bg, fg) = when (role) {
        UserRole.PROPRIETAIRE -> MbgGreenContainer to MbgOnGreenContainer
        UserRole.GESTIONNAIRE -> MbgBlueContainer to MbgOnBlueContainer
        UserRole.GERANT -> MbgYellowContainer to MbgOnYellowContainer
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            text = role.shortLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StockStatusBadge(status: StockStatus, modifier: Modifier = Modifier) {
    val (bg, fg, dot) = when (status) {
        StockStatus.NORMAL -> Triple(MbgGreenContainer, MbgOnGreenContainer, MbgGreenPrimary)
        StockStatus.ALERTE -> Triple(MbgYellowContainer, MbgOnYellowContainer, MbgYellowAccent)
        StockStatus.RUPTURE -> Triple(MbgRedContainer, MbgOnRedContainer, MbgRedAlert)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(dot, CircleShape)
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color = MbgGreenPrimary,
    containerColor: Color = MbgGreenContainer,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = containerColor,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DataSaverPill(enabled: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (enabled) MbgBlueContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(if (enabled) MbgBlueAccent else Color.Gray, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (enabled) "Sync • Éco-données" else "Sync standard",
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) MbgOnBlueContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
