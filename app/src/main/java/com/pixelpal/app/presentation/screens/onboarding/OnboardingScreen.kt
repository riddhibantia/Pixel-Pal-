package com.pixelpal.app.presentation.screens.onboarding

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.overlay.OverlayService
import com.pixelpal.app.presentation.components.AppTextField
import com.pixelpal.app.presentation.components.PetRenderer
import com.pixelpal.app.presentation.components.PrimaryButton
import com.pixelpal.app.presentation.components.SecondaryButton
import com.pixelpal.app.presentation.theme.Spacing
import com.pixelpal.app.util.PermissionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity

    val petName by viewModel.petName.collectAsState()
    val selectedPetType by viewModel.selectedPetType.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> PageWelcome()
                1 -> PageChoosePet(selectedPetType)
                2 -> PageNamePet(petName, onNameChange = { viewModel.updatePetName(it) })
                3 -> PageEnableOverlay(petName = petName, activity = activity)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                SecondaryButton(
                    text = "Back",
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (pagerState.currentPage < 3) {
                PrimaryButton(
                    text = "Next",
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = if (pagerState.currentPage > 0) Modifier.weight(1f) else Modifier.fillMaxWidth()
                )
            } else {
                PrimaryButton(
                    text = "Let's Go!",
                    onClick = {
                        if (PermissionHelper.canDrawOverlays(context)) {
                            OverlayService.start(context)
                        }
                        viewModel.saveAndComplete(onOnboardingComplete)
                    }
                )
            }
        }
    }
}

@Composable
private fun PageWelcome() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PetRenderer(petType = "cat", animationState = AnimationState.IDLE, size = 180.dp)
        Spacer(modifier = Modifier.height(Spacing.xl))
        Text(
            text = "Welcome to PixelPal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Your tiny companion is waiting to meet you.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun PageChoosePet(selectedType: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose Your Companion",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        PetRenderer(petType = selectedType.ifBlank { "cat" }, animationState = AnimationState.HAPPY, size = 180.dp)
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Cat (Unlocked)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = "Dog, Bunny, Fox, and Axolotl unlock as your bond grows!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PageNamePet(petName: String, onNameChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PetRenderer(petType = "cat", animationState = AnimationState.EXCITED, size = 150.dp)
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = "Name Your Companion",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        AppTextField(
            value = petName,
            onValueChange = { if (it.length <= 20) onNameChange(it) },
            label = "Companion Name",
            modifier = Modifier.fillMaxWidth(0.85f)
        )
    }
}

@Composable
private fun PageEnableOverlay(petName: String, activity: Activity?) {
    val context = LocalContext.current
    val hasPermission = PermissionHelper.canDrawOverlays(context)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Let $petName Join You!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "$petName lives on your phone screen as a tiny pixel pet overlay.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        PrimaryButton(
            text = if (hasPermission) "Permission Granted" else "Enable Screen Overlay",
            modifier = Modifier.fillMaxWidth(0.85f),
            onClick = {
                activity?.let { PermissionHelper.requestOverlayPermission(it) }
            }
        )
    }
}