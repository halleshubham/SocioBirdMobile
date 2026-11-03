package com.postiz.mobile.ui.screens.connect

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postiz.mobile.R

/** Email/password sign-in for the go.sociobird.app edition (mirrors the web app's login page). */
@Composable
fun LoginScreen(onUseApiKey: () -> Unit = {}, viewModel: LoginViewModel = hiltViewModel()) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val hairline = MaterialTheme.colorScheme.outline

    fun openWeb(path: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${viewModel.serverUrl}$path")))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.padding(top = 56.dp, start = 28.dp, end = 28.dp)) {
            Text("SOCIOBIRD", style = MaterialTheme.typography.labelLarge, color = accent)
            Text("Sign in", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Use the same account as ${viewModel.serverUrl.removePrefix("https://")}.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted
            )
        }

        Column(modifier = Modifier.padding(top = 40.dp, start = 28.dp, end = 28.dp)) {
            EditorialField(
                label = "EMAIL",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = "you@example.com",
                keyboardType = KeyboardType.Email,
                ink = ink,
                muted = muted,
                hairline = hairline
            )
            Spacer(Modifier.height(28.dp))

            var showPassword by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PASSWORD", style = MaterialTheme.typography.labelLarge, color = muted)
                Text(
                    if (showPassword) "Hide" else "Show",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    modifier = Modifier.clickable { showPassword = !showPassword }.padding(4.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            EditorialField(
                label = null,
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "Your password",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                ink = ink,
                muted = muted,
                hairline = hairline
            )
            Text(
                "Forgot password?",
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                modifier = Modifier.padding(top = 14.dp).clickable { openWeb("/auth/forgot") }.padding(vertical = 6.dp)
            )
            Text(
                "Prefer an API key? Use it instead",
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                modifier = Modifier.clickable(onClick = onUseApiKey).padding(vertical = 6.dp)
            )
        }

        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp, start = 28.dp, end = 28.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .background(accent, RoundedCornerShape(999.dp))
                .clickable(enabled = !state.isLoading, onClick = viewModel::signIn)
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
                Text("Sign in", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(Modifier.height(14.dp))
        // Google's sign-in button style: white pill, thin grey border, the multicolour G and dark text.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .background(Color.White, RoundedCornerShape(999.dp))
                .border(1.dp, Color(0xFF747775), RoundedCornerShape(999.dp))
                .clickable(enabled = !state.isLoading) {
                    viewModel.startGoogleSignIn { url ->
                        runCatching { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url)) }
                    }
                }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_google_g),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(12.dp))
            Text("Continue with Google", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1F1F1F))
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "New here? Create an account",
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openWeb("/auth") }
                .padding(horizontal = 28.dp, vertical = 8.dp)
        )
        Spacer(Modifier.height(40.dp))
    }
}
