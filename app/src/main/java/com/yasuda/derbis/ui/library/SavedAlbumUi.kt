package com.yasuda.derbis.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.theme.DerbisGlass
import com.yasuda.derbis.ui.theme.DerbisGlassBorder
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisPink
import com.yasuda.derbis.ui.theme.DerbisText

@Composable
fun BoxScope.SavedAlbumBookmarkOverlay(
    saved: Boolean,
    enabled: Boolean,
    languageCode: String,
    onToggle: () -> Unit,
    container: Color = DerbisGlass,
    border: Color = DerbisGlassBorder,
    idleTint: Color = DerbisText
) {
    val density = LocalDensity.current
    val strings = DerbisStrings.forCode(languageCode)
    val fixedStatusBarInset = with(density) {
        WindowInsets.statusBars.getTop(density).toDp()
    }

    Surface(
        onClick = onToggle,
        enabled = enabled,
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .zIndex(100f)
            .padding(top = fixedStatusBarInset + 8.dp, end = 76.dp)
            .size(48.dp)
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (saved) strings.removeFromFavorites else strings.addToFavorites,
                tint = when {
                    !enabled -> DerbisMuted
                    saved -> DerbisPink
                    else -> idleTint
                },
                modifier = Modifier.size(23.dp)
            )
        }
    }
}
