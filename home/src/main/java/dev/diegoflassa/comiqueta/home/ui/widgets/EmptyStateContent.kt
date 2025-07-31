package dev.diegoflassa.comiqueta.home.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.home.ui.home.HomeIntent


@Composable
fun EmptyStateContent(
    modifier: Modifier = Modifier,
    onIntent: ((HomeIntent) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp.scaled()),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.empty_state_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp.scaled()))
            Text(
                text = stringResource(R.string.empty_state_message),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp.scaled()))
            ExtendedFloatingActionButton(
                onClick = { onIntent?.invoke(HomeIntent.AddFolderClicked) },
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_folder_button)
                    )
                },
                text = { Text(stringResource(R.string.add_folder_button)) })
        }
    }
}