package com.entropy.stage

import android.app.role.RoleManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entropy.stage.design.StageGlyph
import com.entropy.stage.designsystem.StagePalette
import com.entropy.stage.onboarding.StageOnboarding
import com.entropy.stage.shell.StageDesktop
import com.entropy.stage.shell.StageShellActions
import com.entropy.stage.shell.StageShellUiState
import com.entropy.stage.shell.StageSurface
import kotlinx.coroutines.delay

@Composable
fun StageApp(
    state: StageShellUiState,
    actions: StageShellActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val homeRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        actions.refreshPlatformState()
    }
    val requestHomeRole = {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        ) {
            homeRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
        } else {
            actions.openSystemDestination(com.entropy.stage.shell.SystemDestination.HOME)
        }
    }

    BackHandler(
        enabled = state.commandCenterOpen || state.controlCenterOpen ||
            state.activeSurface != StageSurface.NONE,
    ) {
        when {
            state.commandCenterOpen -> actions.setCommandCenterOpen(false)
            state.controlCenterOpen -> actions.setControlCenterOpen(false)
            else -> actions.closeSurface()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            !state.preferencesLoaded -> StageBootScreen()
            !state.onboardingComplete -> StageOnboarding(
                profile = state.deviceProfile,
                homeRoleHeld = state.homeRoleHeld,
                homeRoleAvailable = state.homeRoleAvailable,
                reduceMotion = state.reduceMotion,
                onRequestHomeRole = requestHomeRole,
                onFinish = actions::completeOnboarding,
            )

            else -> StageDesktop(
                state = state,
                actions = actions,
                onRequestHomeRole = requestHomeRole,
            )
        }

        AnimatedVisibility(
            visible = state.message != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = state.message.orEmpty(),
                modifier = Modifier
                    .background(StagePalette.Glass, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                color = StagePalette.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(2_400)
            actions.dismissMessage()
        }
    }
}

@Composable
private fun StageBootScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF08090D), Color(0xFF12101A))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        StageGlyph(modifier = Modifier.padding(2.dp).fillMaxSize(0.1f))
    }
}
