package com.pixelpal.app.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing

/**
 * Themed snackbar host matching the warm brown/gold palette.
 * Use in Scaffold's `snackbarHost` parameter.
 */
@Composable
fun PixelPalSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(horizontal = Spacing.screenHorizontal),
        snackbar = { data -> PixelPalSnackbar(data) }
    )
}

@Composable
private fun PixelPalSnackbar(data: SnackbarData) {
    Snackbar(
        modifier = Modifier.padding(bottom = Spacing.sm),
        shape = RoundedCornerShape(Radius.medium),
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        actionContentColor = MaterialTheme.colorScheme.primary,
        action = data.visuals.actionLabel?.let { actionLabel ->
            {
                TextButton(onClick = { data.performAction() }) {
                    Text(
                        text = actionLabel,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    ) {
        Text(
            text = data.visuals.message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
