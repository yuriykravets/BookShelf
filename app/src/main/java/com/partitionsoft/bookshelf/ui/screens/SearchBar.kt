package com.partitionsoft.bookshelf.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.bookshelf.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosedAppBar(
    onSearchClicked: () -> Unit,
    onFavoritesClicked: () -> Unit,
    onPremiumClicked: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = { Text(text = stringResource(id = R.string.app_name)) },
        actions = {
            IconButton(onClick = onPremiumClicked) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = stringResource(id = R.string.premium_action)
                )
            }
            IconButton(onClick = onFavoritesClicked) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(id = R.string.favorites_title)
                )
            }
            IconButton(onClick = onSearchClicked) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(id = R.string.search_action)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenedAppBar(
    text: String,
    onTextChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    onSearchClicked: (String) -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = text,
            onValueChange = onTextChange,
            shape = RoundedCornerShape(28.dp),
            placeholder = {
                Text(
                    modifier = Modifier.alpha(0.7f),
                    text = stringResource(id = R.string.search_hint),
                    color = onSurface.copy(alpha = 0.6f)
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = onSurface),
            singleLine = true,
            leadingIcon = {
                IconButton(
                    modifier = Modifier.alpha(0.7f),
                    onClick = onCloseClicked
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = onSurface
                    )
                }
            },
            trailingIcon = {
                Row {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { onTextChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.clear_action),
                                tint = onSurface
                            )
                        }
                    }
                    IconButton(onClick = { onSearchClicked(text) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(id = R.string.search_action),
                            tint = onSurface
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchClicked(text) }),
            colors = TextFieldDefaults.colors(
                focusedTextColor = onSurface,
                unfocusedTextColor = onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedPlaceholderColor = onSurface.copy(alpha = 0.65f),
                unfocusedPlaceholderColor = onSurface.copy(alpha = 0.55f),
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
