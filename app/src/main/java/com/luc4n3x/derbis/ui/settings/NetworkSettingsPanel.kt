package com.luc4n3x.derbis.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.derbis.data.network.byedpi.ByeDpiSupervisor
import com.luc4n3x.derbis.domain.DerbisDnsMode
import com.luc4n3x.derbis.domain.DerbisNetworkSettings
import com.luc4n3x.derbis.domain.DerbisNetworkSettingsError
import com.luc4n3x.derbis.domain.DerbisNetworkTestOutcome
import com.luc4n3x.derbis.domain.DerbisProxyMode
import com.luc4n3x.derbis.ui.i18n.DerbisStrings
import com.luc4n3x.derbis.ui.i18n.LocalDerbisStrings
import com.luc4n3x.derbis.ui.theme.DerbisCyan
import com.luc4n3x.derbis.ui.theme.DerbisMuted
import com.luc4n3x.derbis.ui.theme.DerbisOnAccent
import com.luc4n3x.derbis.ui.theme.DerbisPanel
import com.luc4n3x.derbis.ui.theme.DerbisText

private val DnsModes = listOf(
    DerbisDnsMode.System,
    DerbisDnsMode.Cloudflare,
    DerbisDnsMode.Google,
    DerbisDnsMode.AdGuard,
    DerbisDnsMode.Quad9,
    DerbisDnsMode.Custom
)

private val ProxyModes = listOf(
    DerbisProxyMode.Disabled,
    DerbisProxyMode.Http,
    DerbisProxyMode.Socks
)

@Composable
internal fun NetworkSettingsPanel(
    settings: DerbisNetworkSettings,
    proxyPasswordSet: Boolean,
    testing: Boolean,
    testOutcome: DerbisNetworkTestOutcome?,
    errors: List<DerbisNetworkSettingsError>,
    onApply: (DerbisNetworkSettings, String?) -> Unit,
    onTest: (DerbisNetworkSettings, String?) -> Unit
) {
    val strings = LocalDerbisStrings.current
    var dnsMode by rememberSaveable(settings.dnsMode) { mutableStateOf(settings.dnsMode.id) }
    var customDohUrl by rememberSaveable(settings.customDohUrl) { mutableStateOf(settings.customDohUrl) }
    var proxyMode by rememberSaveable(settings.proxyMode) { mutableStateOf(settings.proxyMode.id) }
    var proxyHost by rememberSaveable(settings.proxyHost) { mutableStateOf(settings.proxyHost) }
    var proxyPort by rememberSaveable(settings.proxyPort) { mutableStateOf(settings.proxyPort.toString()) }
    var proxyAuth by rememberSaveable(settings.proxyAuthenticationEnabled) {
        mutableStateOf(settings.proxyAuthenticationEnabled)
    }
    var proxyUsername by rememberSaveable(settings.proxyUsername) { mutableStateOf(settings.proxyUsername) }
    var proxyPassword by remember { mutableStateOf("") }
    var bypassStreams by rememberSaveable(settings.bypassProxyForStreams) {
        mutableStateOf(settings.bypassProxyForStreams)
    }
    var byeDpiEnabled by rememberSaveable(settings.byeDpiEnabled) {
        mutableStateOf(settings.byeDpiEnabled)
    }
    var youtubeRegionProfileEnabled by rememberSaveable(settings.youtubeRegionProfileEnabled) {
        mutableStateOf(settings.youtubeRegionProfileEnabled)
    }

    val edited = DerbisNetworkSettings(
        dnsMode = DerbisDnsMode.fromId(dnsMode),
        customDohUrl = customDohUrl,
        proxyMode = DerbisProxyMode.fromId(proxyMode),
        proxyHost = proxyHost,
        proxyPort = proxyPort.toIntOrNull() ?: 0,
        proxyUsername = proxyUsername,
        proxyAuthenticationEnabled = proxyAuth,
        bypassProxyForStreams = bypassStreams,
        byeDpiEnabled = byeDpiEnabled,
        youtubeRegionProfileEnabled = youtubeRegionProfileEnabled
    )
    val passwordArgument: String? = when {
        proxyPassword.isNotEmpty() -> proxyPassword
        !proxyAuth -> ""
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DnsSettingsCard(
            dnsMode = dnsMode,
            customDohUrl = customDohUrl,
            errors = errors,
            strings = strings,
            onDnsModeChange = { dnsMode = it },
            onCustomDohUrlChange = { customDohUrl = it }
        )

        ProxySettingsCard(
            proxyMode = proxyMode,
            proxyHost = proxyHost,
            proxyPort = proxyPort,
            proxyAuth = proxyAuth,
            proxyUsername = proxyUsername,
            proxyPassword = proxyPassword,
            proxyPasswordSet = proxyPasswordSet,
            bypassStreams = bypassStreams,
            errors = errors,
            strings = strings,
            onProxyModeChange = { proxyMode = it },
            onProxyHostChange = { proxyHost = it },
            onProxyPortChange = { proxyPort = it },
            onProxyAuthChange = { proxyAuth = it },
            onProxyUsernameChange = { proxyUsername = it },
            onProxyPasswordChange = { proxyPassword = it },
            onBypassStreamsChange = { bypassStreams = it }
        )

        RestrictedCompatibilityCard(
            byeDpiEnabled = byeDpiEnabled,
            onByeDpiChange = { byeDpiEnabled = it },
            youtubeRegionProfileEnabled = youtubeRegionProfileEnabled,
            onYoutubeRegionProfileChange = { youtubeRegionProfileEnabled = it },
            strings = strings
        )

        ValidationErrorsCard(errors = errors, strings = strings)

        testOutcome?.let { outcome ->
            NetworkCard {
                Text(
                    networkTestText(outcome, strings),
                    color = if (outcome == DerbisNetworkTestOutcome.Success) DerbisCyan else DerbisText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = { onApply(edited, passwordArgument) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DerbisCyan, contentColor = DerbisOnAccent)
        ) {
            Text(strings.save, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { onTest(edited, proxyPassword.takeIf { it.isNotEmpty() }) },
            enabled = !testing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DerbisPanel, contentColor = DerbisText)
        ) {
            Text(if (testing) strings.checking else strings.networkTest, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DnsSettingsCard(
    dnsMode: String,
    customDohUrl: String,
    errors: List<DerbisNetworkSettingsError>,
    strings: DerbisStrings,
    onDnsModeChange: (String) -> Unit,
    onCustomDohUrlChange: (String) -> Unit
) {
    NetworkCard {
        Text(strings.networkDns, color = DerbisText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(strings.networkSubtitle, color = DerbisMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        DnsModes.forEach { mode ->
            NetworkOptionRow(
                label = dnsModeLabel(mode, strings),
                selected = mode.id == dnsMode,
                onClick = { onDnsModeChange(mode.id) }
            )
        }
        if (DerbisDnsMode.fromId(dnsMode) == DerbisDnsMode.Custom) {
            OutlinedTextField(
                value = customDohUrl,
                onValueChange = { onCustomDohUrlChange(it.take(DerbisNetworkSettings.MAX_URL_LENGTH)) },
                singleLine = true,
                label = { Text(strings.networkCustomDohUrl) },
                isError = errors.any { it.name.startsWith("CustomDohUrl") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProxySettingsCard(
    proxyMode: String,
    proxyHost: String,
    proxyPort: String,
    proxyAuth: Boolean,
    proxyUsername: String,
    proxyPassword: String,
    proxyPasswordSet: Boolean,
    bypassStreams: Boolean,
    errors: List<DerbisNetworkSettingsError>,
    strings: DerbisStrings,
    onProxyModeChange: (String) -> Unit,
    onProxyHostChange: (String) -> Unit,
    onProxyPortChange: (String) -> Unit,
    onProxyAuthChange: (Boolean) -> Unit,
    onProxyUsernameChange: (String) -> Unit,
    onProxyPasswordChange: (String) -> Unit,
    onBypassStreamsChange: (Boolean) -> Unit
) {
    NetworkCard {
        Text(strings.networkProxy, color = DerbisText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        ProxyModes.forEach { mode ->
            NetworkOptionRow(
                label = proxyModeLabel(mode, strings),
                selected = mode.id == proxyMode,
                onClick = { onProxyModeChange(mode.id) }
            )
        }
        if (DerbisProxyMode.fromId(proxyMode) != DerbisProxyMode.Disabled) {
            OutlinedTextField(
                value = proxyHost,
                onValueChange = { onProxyHostChange(it.take(DerbisNetworkSettings.MAX_HOST_LENGTH)) },
                singleLine = true,
                label = { Text(strings.networkProxyHost) },
                isError = errors.contains(DerbisNetworkSettingsError.ProxyHostMissing) ||
                    errors.contains(DerbisNetworkSettingsError.ProxyHostInvalid),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = proxyPort,
                onValueChange = { value -> onProxyPortChange(value.filter(Char::isDigit).take(5)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(strings.networkProxyPort) },
                isError = errors.contains(DerbisNetworkSettingsError.ProxyPortOutOfRange),
                modifier = Modifier.fillMaxWidth()
            )
            NetworkToggleRow(
                label = strings.networkProxyAuthentication,
                checked = proxyAuth,
                onCheckedChange = onProxyAuthChange
            )
            if (proxyAuth) {
                OutlinedTextField(
                    value = proxyUsername,
                    onValueChange = { onProxyUsernameChange(it.take(DerbisNetworkSettings.MAX_CREDENTIAL_LENGTH)) },
                    singleLine = true,
                    label = { Text(strings.networkProxyUsername) },
                    isError = errors.contains(DerbisNetworkSettingsError.ProxyUsernameMissing),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proxyPassword,
                    onValueChange = { onProxyPasswordChange(it.take(DerbisNetworkSettings.MAX_CREDENTIAL_LENGTH)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    label = { Text(strings.networkProxyPassword) },
                    isError = errors.contains(DerbisNetworkSettingsError.ProxyPasswordMissing),
                    modifier = Modifier.fillMaxWidth()
                )
                if (proxyPasswordSet && proxyPassword.isEmpty()) {
                    Text(strings.saved, color = DerbisMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            NetworkToggleRow(
                label = strings.networkBypassStreams,
                checked = bypassStreams,
                onCheckedChange = onBypassStreamsChange,
                subtitle = strings.networkBypassStreamsSubtitle
            )
        }
    }
}

@Composable
private fun RestrictedCompatibilityCard(
    byeDpiEnabled: Boolean,
    onByeDpiChange: (Boolean) -> Unit,
    youtubeRegionProfileEnabled: Boolean,
    onYoutubeRegionProfileChange: (Boolean) -> Unit,
    strings: DerbisStrings
) {
    NetworkCard {
        Text(strings.networkRestrictedCompatibility, color = DerbisText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(strings.networkRestrictedCompatibilitySubtitle, color = DerbisMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        val byeDpiAvailable = remember { ByeDpiSupervisor.isAvailable() }
        NetworkToggleRow(
            label = strings.networkByeDpi,
            checked = byeDpiEnabled,
            onCheckedChange = { enabled -> if (byeDpiAvailable || !enabled) onByeDpiChange(enabled) },
            subtitle = strings.networkByeDpiSubtitle,
            enabled = byeDpiAvailable || byeDpiEnabled
        )
        NetworkToggleRow(
            label = strings.networkYoutubeRegionProfile,
            checked = youtubeRegionProfileEnabled,
            onCheckedChange = onYoutubeRegionProfileChange,
            subtitle = strings.networkYoutubeRegionProfileSubtitle
        )
    }
}

@Composable
private fun ValidationErrorsCard(errors: List<DerbisNetworkSettingsError>, strings: DerbisStrings) {
    if (errors.isEmpty()) return
    NetworkCard {
        errors.distinct().forEach { error ->
            Text(
                networkErrorText(error, strings),
                color = DerbisText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun NetworkCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DerbisPanel)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun NetworkOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
    ) {
        Text(
            label,
            color = if (selected) DerbisCyan else DerbisMuted,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NetworkToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (enabled) DerbisText else DerbisMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(it, color = DerbisMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

private fun dnsModeLabel(mode: DerbisDnsMode, strings: DerbisStrings): String = when (mode) {
    DerbisDnsMode.System -> strings.networkDnsSystem
    DerbisDnsMode.Cloudflare -> "Cloudflare"
    DerbisDnsMode.Google -> "Google"
    DerbisDnsMode.AdGuard -> "AdGuard"
    DerbisDnsMode.Quad9 -> "Quad9"
    DerbisDnsMode.Custom -> strings.networkDnsCustom
}

private fun proxyModeLabel(mode: DerbisProxyMode, strings: DerbisStrings): String = when (mode) {
    DerbisProxyMode.Disabled -> strings.networkProxyDisabled
    DerbisProxyMode.Http -> "HTTP"
    DerbisProxyMode.Socks -> "SOCKS"
}

private fun networkErrorText(error: DerbisNetworkSettingsError, strings: DerbisStrings): String = when (error) {
    DerbisNetworkSettingsError.ProxyHostMissing,
    DerbisNetworkSettingsError.ProxyHostInvalid -> strings.networkErrorProxyHost
    DerbisNetworkSettingsError.ProxyPortOutOfRange -> strings.networkErrorProxyPort
    DerbisNetworkSettingsError.ProxyUsernameMissing,
    DerbisNetworkSettingsError.ProxyPasswordMissing -> strings.networkErrorProxyCredentials
    DerbisNetworkSettingsError.CustomDohUrlMissing,
    DerbisNetworkSettingsError.CustomDohUrlNotHttps,
    DerbisNetworkSettingsError.CustomDohUrlInvalid -> strings.networkErrorDohUrl
}

private fun networkTestText(outcome: DerbisNetworkTestOutcome, strings: DerbisStrings): String = when (outcome) {
    DerbisNetworkTestOutcome.Success -> strings.networkTestSuccess
    DerbisNetworkTestOutcome.InvalidConfiguration -> strings.networkTestInvalid
    DerbisNetworkTestOutcome.DnsResolutionFailed -> strings.networkTestDnsFailed
    DerbisNetworkTestOutcome.ProxyAuthenticationFailed -> strings.networkTestProxyAuthFailed
    DerbisNetworkTestOutcome.Timeout -> strings.networkTestTimeout
    DerbisNetworkTestOutcome.ConnectionRefused -> strings.networkTestRefused
    DerbisNetworkTestOutcome.TlsFailure -> strings.networkTestTls
    DerbisNetworkTestOutcome.UnknownError -> strings.networkTestUnknown
}
