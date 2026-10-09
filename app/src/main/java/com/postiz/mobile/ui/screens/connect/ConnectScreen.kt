package com.postiz.mobile.ui.screens.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ConnectScreen(viewModel: ConnectViewModel = hiltViewModel()) {
    val state = viewModel.uiState
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val hairline = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.padding(top = 56.dp, start = 28.dp, end = 28.dp)) {
            Text("CONNECT", style = MaterialTheme.typography.labelLarge, color = accent)
            Text("Your server", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Self-hosted or Postiz Cloud — either works.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted
            )
        }

        Column(modifier = Modifier.padding(top = 40.dp, start = 28.dp, end = 28.dp)) {

            EditorialField(
                label = "SERVER URL",
                value = state.serverUrl,
                onValueChange = viewModel::onServerUrlChange,
                placeholder = "postiz.mycompany.com",
                keyboardType = KeyboardType.Uri,
                ink = ink,
                muted = muted,
                hairline = hairline
            )

            Spacer(Modifier.height(28.dp))

            var showToken by remember { mutableStateOf(false) }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("API KEY", style = MaterialTheme.typography.labelLarge, color = muted)
                    Text(
                        if (showToken) "Hide" else "Reveal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                        modifier = Modifier.clickable { showToken = !showToken }
                    )
                }
                Spacer(Modifier.height(10.dp))
                EditorialField(
                    label = null,
                    value = state.apiToken,
                    onValueChange = viewModel::onTokenChange,
                    placeholder = "Paste from Settings → Developer",
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    ink = ink,
                    muted = muted,
                    hairline = hairline
                )
            }

            Spacer(Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.onCloudToggle(!state.isCloud) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("This is Postiz Cloud", style = MaterialTheme.typography.bodyMedium, color = ink)
                EditorialSwitch(checked = state.isCloud, accent = accent, track = hairline)
            }
        }

        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp, start = 28.dp, end = 28.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Need an API key? Open your Sociobird web app → Settings → Developer.",
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .background(accent, RoundedCornerShape(999.dp))
                .clickable(enabled = !state.isLoading, onClick = viewModel::connect)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    "Connect",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun EditorialField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    ink: Color,
    muted: Color,
    hairline: Color,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = muted)
            Spacer(Modifier.height(10.dp))
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = muted.copy(alpha = 0.6f)
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = ink,
                unfocusedIndicatorColor = hairline,
                cursorColor = ink
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EditorialSwitch(checked: Boolean, accent: Color, track: Color) {
    Row(
        modifier = Modifier
            .size(width = 42.dp, height = 24.dp)
            .background(if (checked) accent else track, CircleShape)
            .padding(3.dp),
        horizontalArrangement = if (checked) Arrangement.End else Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .size(18.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape)
        ) {}
    }
}
