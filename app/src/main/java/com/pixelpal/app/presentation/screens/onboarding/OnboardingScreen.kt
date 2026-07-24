package com.pixelpal.app.presentation.screens.onboarding

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.overlay.OverlayService
import com.pixelpal.app.presentation.components.PetRenderer
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
            .padding(24.dp),
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
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }

            if (pagerState.currentPage < 3) {
                Button(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) {
                    Text("Next")
                }
            } else {
                Button(onClick = {
                    if (PermissionHelper.canDrawOverlays(context)) {
                        OverlayService.start(context)
                    }
                    viewModel.saveAndComplete(onOnboardingComplete)
                }) {
                    Text("Let's Go!")
                }
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
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Welcome to PixelPal",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your tiny companion is waiting to meet you.",
            fontSize = 16.sp,
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
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        PetRenderer(petType = "cat", animationState = AnimationState.HAPPY, size = 180.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Cat (Unlocked)", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Dog, Bunny, Fox, and Axolotl unlock as your bond grows!",
            fontSize = 14.sp,
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
        PetRenderer(petType = "cat", animationState = AnimationState.CURIOUS, size = 150.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Name Your Companion",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = petName,
            onValueChange = { if (it.length <= 20) onNameChange(it) },
            label = { Text("Companion Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
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
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$petName lives on your phone screen as a tiny pixel pet overlay.",
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            activity?.let { PermissionHelper.requestOverlayPermission(it) }
        }) {
            Text(if (hasPermission) "Permission Granted ✓" else "Enable Screen Overlay")
        }
    }
}
