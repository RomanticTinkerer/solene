package com.solene.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared M3 top app bar for Solene.
 *
 * Uses LargeTopAppBar for prominent scroll-collapsing effect
 * on screens that support it, and standard CenterAlignedTopAppBar elsewhere.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoleneTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )
}
