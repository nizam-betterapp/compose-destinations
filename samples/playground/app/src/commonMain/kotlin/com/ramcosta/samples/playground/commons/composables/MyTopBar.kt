package com.ramcosta.samples.playground.commons.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.samples.playground.commons.title
import com.ramcosta.samples.playground.ui.screens.NavGraphs

@Composable
fun TopBar(
    destination: DestinationSpec,
    onDrawerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colors.primary)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            IconButton(
                onClick = onDrawerClick
            ) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    tint = Color.White,
                    contentDescription = "menu"
                )
            }

            IconButton(
                onClick = onNavigateUp
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    tint = Color.White,
                    contentDescription = "back"
                )
            }
        }

        Text(
            text = destination.title ?: "",
            modifier = Modifier.align(Alignment.Center),
            color = Color.White
        )

        if (!NavGraphs.settings.destinations.contains(destination)) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    tint = Color.White,
                    contentDescription = "menu"
                )
            }
        }
    }
}