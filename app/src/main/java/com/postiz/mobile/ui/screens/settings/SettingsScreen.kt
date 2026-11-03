package com.postiz.mobile.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postiz.mobile.BuildConfig
import com.postiz.mobile.data.local.ThemeMode
import com.postiz.mobile.ui.components.SbCard
import com.postiz.mobile.ui.components.SectionHeader

private const val SUPPORT_EMAIL = "support@shackyapps.in"

/** "More" tab: who is signed in, appearance, API key, help and legal links, app version, sign out. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scheme = MaterialTheme.colorScheme

    var revealKey by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    val webBase = BuildConfig.LOCKED_SERVER_URL.ifBlank { "https://${session.serverUrl}" }

    fun openInBrowser(url: String) {
        runCatching { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url)) }
    }

    fun email(subject: String, body: String = "") {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, "Write to $SUPPORT_EMAIL", Toast.LENGTH_LONG).show() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("More", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))

        // ---- Profile
        SbCard {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                val initial = (session.name.ifBlank { session.email }).take(1).uppercase().ifBlank { "S" }
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, style = MaterialTheme.typography.titleLarge, color = scheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        session.name.ifBlank { session.email.ifBlank { "SocioBird account" } },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (session.email.isNotBlank()) session.email else "Signed in with an API key",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        session.serverUrl.removePrefix("https://").ifBlank { "–" },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- Appearance
        Column {
            SectionHeader("Appearance")
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                options.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        label = { Text(label) }
                    )
                }
            }
        }

        // ---- API key
        Column {
            SectionHeader("Account")
            Spacer(Modifier.height(4.dp))
            SbCard {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Key,
                        title = "API key",
                        subtitle = if (revealKey) session.apiToken.ifBlank { "–" } else maskToken(session.apiToken),
                        trailing = {
                            Row {
                                IconButton(onClick = { revealKey = !revealKey }) {
                                    Icon(
                                        if (revealKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (revealKey) "Hide API key" else "Show API key"
                                    )
                                }
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(session.apiToken))
                                    Toast.makeText(context, "API key copied", Toast.LENGTH_SHORT).show()
                                }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy API key") }
                            }
                        }
                    )
                    HorizontalDivider(color = scheme.outlineVariant)
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        title = "Open SocioBird on the web",
                        subtitle = "Billing, team and channel setup",
                        onClick = { openInBrowser(webBase) }
                    )
                }
            }
        }

        // ---- Help and legal
        Column {
            SectionHeader("Help and legal")
            Spacer(Modifier.height(4.dp))
            SbCard {
                Column {
                    SettingsRow(Icons.AutoMirrored.Filled.HelpOutline, "Help and support", SUPPORT_EMAIL, onClick = { email("SocioBird app help") })
                    HorizontalDivider(color = scheme.outlineVariant)
                    SettingsRow(Icons.Default.PrivacyTip, "Privacy policy", onClick = { openInBrowser("$webBase/privacy") })
                    HorizontalDivider(color = scheme.outlineVariant)
                    SettingsRow(Icons.Default.Description, "Terms of service", onClick = { openInBrowser("$webBase/terms") })
                    HorizontalDivider(color = scheme.outlineVariant)
                    SettingsRow(
                        Icons.Default.DeleteOutline,
                        "Request account deletion",
                        "We delete your account and data on request",
                        onClick = {
                            email(
                                "Delete my SocioBird account",
                                "Please delete my SocioBird account and data.\nAccount email: ${session.email.ifBlank { "(please add it)" }}"
                            )
                        }
                    )
                }
            }
        }

        // ---- About
        SbCard {
            SettingsRow(Icons.Default.Info, "SocioBird for Android", "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        }

        OutlinedButton(
            onClick = { confirmSignOut = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = scheme.error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign out", color = scheme.error)
        }
        Spacer(Modifier.height(12.dp))
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need to sign in again to use SocioBird on this phone. Your posts and channels stay on your account.") },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; viewModel.disconnect() }) { Text("Sign out", color = scheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        trailingContent = trailing
    )
}

private fun maskToken(token: String): String =
    if (token.length <= 8) "••••••••" else token.take(4) + "••••••••" + token.takeLast(4)
