package dev.diegoflassa.comiqueta.home.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.bottomAppBarSelectedIcon
import dev.diegoflassa.comiqueta.core.theme.bottomAppBarSelectedText
import dev.diegoflassa.comiqueta.core.theme.bottomAppBarUnselectedIcon
import dev.diegoflassa.comiqueta.core.theme.bottomAppBarUnselectedText
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.home.ui.enums.BottomNavItems


@Composable
fun BottomNavItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    label: String,
    type: BottomNavItems = BottomNavItems.UNKNOWN,
    isSelected: Boolean,
    onClick: ((BottomNavItems) -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = { onClick?.invoke(type) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(ComiquetaTheme.dimen.bottomAppBarIconSize.scaled()),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) ComiquetaTheme.colorScheme.bottomAppBarSelectedIcon else ComiquetaTheme.colorScheme.bottomAppBarUnselectedIcon
        )
        Text(
            text = label,
            color = if (isSelected) ComiquetaTheme.colorScheme.bottomAppBarSelectedText else ComiquetaTheme.colorScheme.bottomAppBarUnselectedText,
            style = ComiquetaTheme.typography.bottomAppBarText
        )
    }
}