package com.postiz.mobile.ui.screens.connect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postiz.mobile.ui.components.SbCard

/** API-key connect: locked to go.sociobird.app in the Play edition, free server URL in the bring-your-own edition. */
@Composable
fun ConnectScreen(onBack: (() -> Unit)? = null, viewModel: ConnectViewModel = hiltViewModel()) {
    val state = viewModel.uiState
    val focus = LocalFocusManager.current
    var showToken by remember { mutableStateOf(false) }

    AuthScaffold {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("‹ Sign in with email") }
            Spacer(Modifier.height(8.dp))
        }
        AuthHeader(
            title = if (viewModel.serverLocked) "Use an API key" else "Connect your server",
            subtitle = if (viewModel.serverLocked) "Find it on go.sociobird.app under Settings → Developer."
            else "Self-hosted or Postiz Cloud, either works."
        )
        Spacer(Modifier.height(32.dp))

        if (!viewModel.serverLocked) {
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onServerUrlChange,
                label = { Text("Server URL") },
                placeholder = { Text("postiz.mycompany.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = state.apiToken,
            onValueChange = viewModel::onTokenChange,
            label = { Text("API key") },
            placeholder = { Text("Paste from Settings → Developer") },
            singleLine = true,
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.connect() }),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showToken) "Hide API key" else "Show API key"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (!viewModel.serverLocked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onCloudToggle(!state.isCloud) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("This is Postiz Cloud", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = state.isCloud, onCheckedChange = viewModel::onCloudToggle)
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }

        if (state.error != null) {
            SbCard(container = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        PrimaryActionButton("Connect", state.isLoading) { focus.clearFocus(); viewModel.connect() }
    }
}
