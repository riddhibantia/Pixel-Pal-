package com.pixelpal.app.presentation.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pixelpal.app.BuildConfig
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.GroupDivider
import com.pixelpal.app.presentation.components.PetRenderer
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.components.SettingsGroup
import com.pixelpal.app.presentation.components.SettingsRow
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing

/**
 * About PixelPal — companion art, tagline, version (from BuildConfig — never
 * hardcoded), help & feedback, privacy note.
 */
@Composable
fun AboutScreen(
    navController: NavController,
    petType: String = "cat"
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.lg)
    ) {
        AppTopBar(title = "About PixelPal", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PetRenderer(
                petType = petType,
                animationState = AnimationState.WAVE,
                size = 140.dp
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "PixelPal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Your Living Pixel Companion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionHeader(title = "Support")
            SettingsGroup {
                SettingsRow(
                    title = "Help & Feedback",
                    description = "Tell us how PixelPal can be better",
                    icon = Icons.Default.Mail,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_SUBJECT, "PixelPal Feedback")
                        }
                        context.startActivity(Intent.createChooser(intent, "Send feedback"))
                    }
                )
            }

            SectionHeader(title = "Privacy")
            SettingsGroup {
                Surface(
                    shape = RoundedCornerShape(Radius.large),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "PixelPal stores your companion, reminders and preferences " +
                            "locally on your device. Nothing is uploaded anywhere.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = "Made with ${'♥'} and pixels",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
