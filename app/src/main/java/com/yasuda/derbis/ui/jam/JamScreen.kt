@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.yasuda.derbis.ui.jam

import com.yasuda.derbis.ui.components.DerbisBackButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.feature.jam.DerbisJamRelay
import com.yasuda.derbis.feature.jam.JamConnectionState
import com.yasuda.derbis.feature.jam.JamFailure
import com.yasuda.derbis.feature.jam.JamGuestPermission
import com.yasuda.derbis.feature.jam.JamParticipant
import com.yasuda.derbis.feature.jam.JamPendingParticipant
import com.yasuda.derbis.feature.jam.JamSessionState
import com.yasuda.derbis.feature.jam.JamSessionCode
import com.yasuda.derbis.feature.jam.JamTrack
import com.yasuda.derbis.feature.jam.JamTransportMode
import com.yasuda.derbis.feature.jam.JamUiState
import com.yasuda.derbis.ui.components.ModernListenTogetherVector
import com.yasuda.derbis.ui.components.ModernCopyLinkVector
import com.yasuda.derbis.ui.components.ModernShareSparkleVector
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast

private val JamPermissionOptions = listOf(
    JamGuestPermission.HostOnly,
    JamGuestPermission.AddSongs,
    JamGuestPermission.Collaborative
)

@Composable
internal fun DerbisJamOverlay(
    jam: JamUiState,
    displayName: String,
    currentTrack: Track? = null,
    onDisplayNameChange: (String) -> Unit,
    onCreate: (JamTransportMode, JamGuestPermission, Boolean) -> Unit,
    onJoin: (String) -> Unit,
    onLeave: () -> Unit,
    onEnd: () -> Unit,
    onPermissionChange: (JamGuestPermission) -> Unit,
    onApprovalRequiredChange: (Boolean) -> Unit,
    onLockedChange: (Boolean) -> Unit,
    onApproveParticipant: (String) -> Unit,
    onRejectParticipant: (String) -> Unit,
    onRemoveParticipant: (String, Boolean) -> Unit,
    onClearBans: () -> Unit,
    onShare: (String) -> Unit,
    onDismissFailure: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val remoteJamConfigured = remember { DerbisJamRelay.isConfigured }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var pendingTransportModeId by rememberSaveable { mutableStateOf(JamTransportMode.Lan.id) }
    var pendingPermissionId by rememberSaveable { mutableStateOf(JamGuestPermission.AddSongs.id) }
    var pendingApproval by rememberSaveable { mutableStateOf(true) }
    val selectedTransportMode = JamTransportMode.fromId(pendingTransportModeId)
    val selectedPermission = JamGuestPermission.fromId(pendingPermissionId)
    val session = jam.session
    val participants = session?.participants.orEmpty()
    val contributions = remember(session?.queue, participants) {
        jamContributions(session?.queue.orEmpty(), participants)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        JamAmbientBackdrop()
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(contentType = "jam-header") {
                JamHeader(
                    title = strings.jamTitle,
                    subtitle = if (remoteJamConfigured) {
                        "Listen together on local Wi-Fi or through the remote room relay."
                    } else {
                        strings.jamSubtitle
                    },
                    onClose = onClose
                )
            }

            if (!jam.isActive && currentTrack != null) {
                item(contentType = "jam-current-track") {
                    JamCurrentTrackCard(track = currentTrack, strings = strings)
                }
            }

            jam.failure?.let { failure ->
                item(contentType = "jam-failure") {
                    JamFailureBanner(
                        message = jamFailureText(failure, strings),
                        dismissLabel = strings.close,
                        onDismiss = onDismissFailure
                    )
                }
            }

            if (!jam.isActive) {
                item(contentType = "jam-identity") {
                    JamCard {
                        JamCardTitle(strings.jamDisplayName)
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = onDisplayNameChange,
                            singleLine = true,
                            label = { Text(strings.jamDisplayName) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item(contentType = "jam-create") {
                    JamCard {
                        JamCardTitle(strings.jamCreate)
                        JamConnectionModePicker(
                            selected = selectedTransportMode,
                            serverAvailable = remoteJamConfigured,
                            onSelect = { pendingTransportModeId = it.id }
                        )
                        JamCardTitle(strings.jamPermissions)
                        JamPermissionPicker(
                            selected = selectedPermission,
                            strings = strings,
                            onSelect = { pendingPermissionId = it.id }
                        )
                        JamSwitchRow(
                            title = strings.jamApprovalRequired,
                            subtitle = strings.jamApprovalRequiredSubtitle,
                            checked = pendingApproval,
                            onCheckedChange = { pendingApproval = it }
                        )
                        JamPrimaryButton(
                            label = "Start ${jamTransportLabel(selectedTransportMode)}",
                            onClick = { onCreate(selectedTransportMode, selectedPermission, pendingApproval) }
                        )
                    }
                }
                item(contentType = "jam-join") {
                    JamCard {
                        JamCardTitle(strings.jamJoin)
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { entered ->
                                joinCode = buildString {
                                    entered.forEach { character ->
                                        character.uppercaseChar()
                                            .takeIf { it in 'A'..'Z' }
                                            ?.let(::append)
                                    }
                                }.take(JamSessionCode.ENCODED_LENGTH)
                            },
                            singleLine = true,
                            label = { Text(strings.jamCodeHint) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        JamPrimaryButton(
                            label = "Join Listen Together",
                            enabled = joinCode.length == JamSessionCode.ENCODED_LENGTH,
                            onClick = { onJoin(joinCode) }
                        )
                        Text(
                            if (remoteJamConfigured) {
                                "Paste a Listen Together session code. Derbis detects the room type automatically; everyone syncs playback from their music source."
                            } else {
                                "Paste a LAN Jam code. Remote Listen Together is unavailable until this build is connected to the Derbis Jam relay."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                item(contentType = "jam-hero") {
                    val trackInfo = currentTrack?.let { "🎶 ${it.title} — ${it.artist}\n" }.orEmpty()
                    val invitePayload = "🎧 Listen Together on Derbis Jam!\n${trackInfo}Session Code: ${jam.code}\nJoin: https://derbis-web.vercel.app/jam/${jam.code}"
                    JamSessionHero(
                        jam = jam,
                        strings = strings,
                        currentTrack = currentTrack,
                        onShare = { onShare(invitePayload) }
                    )
                }

                if (jam.isHost) {
                    if (jam.pending.isNotEmpty()) {
                        item(contentType = "jam-pending-header") {
                            JamSectionLabel("${strings.jamPendingRequests} · ${jam.pending.size}")
                        }
                        items(
                            items = jam.pending,
                            key = { "pending-${it.participantId}" },
                            contentType = { "jam-pending" }
                        ) { request ->
                            JamPendingRow(
                                request = request,
                                strings = strings,
                                onApprove = { onApproveParticipant(request.participantId) },
                                onReject = { onRejectParticipant(request.participantId) }
                            )
                        }
                    }

                    item(contentType = "jam-moderation") {
                        JamCard {
                            JamCardTitle(strings.jamHostControls)
                            JamSwitchRow(
                                title = strings.jamApprovalRequired,
                                subtitle = strings.jamApprovalRequiredSubtitle,
                                checked = jam.requireApproval,
                                onCheckedChange = onApprovalRequiredChange
                            )
                            JamSwitchRow(
                                title = strings.jamLockSession,
                                subtitle = strings.jamLockSessionSubtitle,
                                checked = jam.locked,
                                onCheckedChange = onLockedChange
                            )
                            if (jam.bannedCount > 0) {
                                JamInlineAction(
                                    icon = Icons.Rounded.Block,
                                    label = "${strings.jamBannedGuests} · ${jam.bannedCount}",
                                    actionLabel = strings.jamClearBans,
                                    onClick = onClearBans
                                )
                            }
                        }
                    }

                    item(contentType = "jam-permissions") {
                        JamCard {
                            JamCardTitle(strings.jamPermissions)
                            JamPermissionPicker(
                                selected = jam.permission,
                                strings = strings,
                                onSelect = onPermissionChange
                            )
                        }
                    }
                }

                item(contentType = "jam-participants-header") {
                    JamSectionLabel("${strings.jamParticipants} · ${participants.size}")
                }

                if (participants.isEmpty()) {
                    item(contentType = "jam-participants-empty") {
                        JamEmptyRow(strings.jamNoParticipants)
                    }
                } else {
                    items(
                        items = participants,
                        key = { it.id },
                        contentType = { "jam-participant" }
                    ) { participant ->
                        JamParticipantRow(
                            participant = participant,
                            strings = strings,
                            isSelf = participant.id == jam.selfParticipantId,
                            contributions = contributions[participant.id] ?: 0,
                            canModerate = jam.isHost && !participant.isHost,
                            onKick = { onRemoveParticipant(participant.id, false) },
                            onBan = { onRemoveParticipant(participant.id, true) }
                        )
                    }
                }

                item(contentType = "jam-exit") {
                    Spacer(modifier = Modifier.height(4.dp))
                    JamSecondaryButton(
                        label = if (jam.isHost) strings.jamEnd else strings.jamLeave,
                        onClick = { if (jam.isHost) onEnd() else onLeave() }
                    )
                }
            }
        }
    }
}

@Composable
private fun JamAmbientBackdrop() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 64.dp, y = 72.dp)
                .size(210.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    MaterialShapes.Cookie9Sided.toShape()
                )
                .clearAndSetSemantics { }
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-72).dp)
                .size(160.dp)
                .background(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                    MaterialShapes.Clover4Leaf.toShape()
                )
                .clearAndSetSemantics { }
        )
    }
}

@Composable
private fun JamHeader(title: String, subtitle: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DerbisBackButton(onClick = onClose, contentDescription = LocalDerbisStrings.current.back, size = 52.dp)
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        ModernListenTogetherVector,
                        contentDescription = "Listen Together",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(9999.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        ModernListenTogetherVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        "LISTEN TOGETHER",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    )
                }
            }

            Text(
                title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun JamCurrentTrackCard(track: Track, strings: DerbisStrings) {
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    strings.jamListeningNow.uppercase(),
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    track.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun JamSessionHero(
    jam: JamUiState,
    strings: DerbisStrings,
    currentTrack: Track? = null,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val connected = jam.connection == JamConnectionState.Connected
    val statusColor by animateColorAsState(
        targetValue = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "jam-status"
    )
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(statusColor, CircleShape)
                        .clearAndSetSemantics { }
                )
                Text(
                    if (jam.isHost) strings.jamRoleHost else strings.jamRoleGuest,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(ModernListenTogetherVector, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text("SYNC", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                jam.transportMode?.let { transportMode ->
                    JamBadge(label = jamTransportLabel(transportMode))
                }
                if (jam.isHost && jam.locked) {
                    JamBadge(label = strings.jamLocked, icon = Icons.Rounded.Lock)
                }
            }
            Text(
                jamConnectionText(jam.connection, strings),
                color = statusColor,
                style = MaterialTheme.typography.labelLarge
            )

            if (currentTrack != null) {
                JamCurrentTrackCard(track = currentTrack, strings = strings)
            }

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.34f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.jamParticipants, style = MaterialTheme.typography.labelLarge)
                    Text(
                        (jam.session?.participants?.size ?: 0).toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (jam.isHost && jam.code.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.46f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialShapes.Cookie4Sided.toShape()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(strings.jamSessionCode, style = MaterialTheme.typography.labelMedium)
                        Text(
                            jam.code,
                            style = MaterialTheme.typography.headlineLarge,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(jam.code))
                                    Toast.makeText(context, "Session code copied!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(ModernCopyLinkVector, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                JamPrimaryButton(
                    label = "Invite to Listen Together",
                    icon = Icons.Rounded.IosShare,
                    onClick = onShare
                )
            }
        }
    }
}

@Composable
private fun JamPendingRow(
    request: JamPendingParticipant,
    strings: DerbisStrings,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JamAvatar(name = request.name.ifBlank { strings.jamRoleGuest }, accent = MaterialTheme.colorScheme.secondary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.name.ifBlank { strings.jamRoleGuest },
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    strings.jamPendingRequests,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onReject,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = strings.jamReject)
            }
            IconButton(
                onClick = onApprove,
                colors = IconButtonDefaults.filledTonalIconButtonColors(),
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = strings.jamApprove)
            }
        }
    }
}

@Composable
private fun JamParticipantRow(
    participant: JamParticipant,
    strings: DerbisStrings,
    isSelf: Boolean,
    contributions: Int,
    canModerate: Boolean,
    onKick: () -> Unit,
    onBan: () -> Unit
) {
    val role = when {
        participant.isHost -> strings.jamRoleHost
        else -> strings.jamRoleGuest
    }
    val detail = if (contributions > 0) {
        "$role · ${strings.formatTrackCount(contributions)}"
    } else {
        role
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JamAvatar(
                name = participant.name.ifBlank { role },
                accent = if (participant.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        participant.name.ifBlank { role },
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isSelf) JamBadge(label = strings.jamYou)
                }
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canModerate) {
                IconButton(
                    onClick = onKick,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = strings.jamKick)
                }
                IconButton(
                    onClick = onBan,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = strings.jamBan)
                }
            }
        }
    }
}

@Composable
private fun JamAvatar(name: String, accent: Color) {
    val initial = remember(name) {
        name.trim().firstOrNull()?.uppercase() ?: "?"
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(accent.copy(alpha = 0.16f), MaterialShapes.Cookie9Sided.toShape())
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(initial, color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun JamBadge(label: String, icon: ImageVector? = null) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun JamCard(content: @Composable () -> Unit) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun JamCardTitle(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    MaterialShapes.Cookie6Sided.toShape()
                )
        )
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun JamSectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
    )
}

@Composable
private fun JamEmptyRow(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
private fun JamConnectionModePicker(
    selected: JamTransportMode,
    serverAvailable: Boolean,
    onSelect: (JamTransportMode) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.selectableGroup()
    ) {
        JamTransportMode.entries.forEach { mode ->
            val active = mode == selected
            val enabled = mode != JamTransportMode.Server || serverAvailable
            Surface(
                enabled = enabled,
                // Selection is intentionally a blue outline plus check—not a solid blue fill.
                // It keeps the choice readable and stops a selected row from looking like a
                // primary action button.
                color = if (enabled) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.52f)
                },
                contentColor = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                },
                border = if (active) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                shape = MaterialTheme.shapes.large,
                onClick = { onSelect(mode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .semantics {
                        this.role = Role.RadioButton
                        this.selected = active
                    }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            jamTransportLabel(mode),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (active) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        jamTransportDescription(mode, serverAvailable),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun JamPermissionPicker(
    selected: JamGuestPermission,
    strings: DerbisStrings,
    onSelect: (JamGuestPermission) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.selectableGroup()
    ) {
        JamPermissionOptions.forEach { permission ->
            val active = permission == selected
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = if (active) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                shape = MaterialTheme.shapes.large,
                onClick = { onSelect(permission) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .semantics {
                        this.role = Role.RadioButton
                        this.selected = active
                    }
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        jamPermissionLabel(permission, strings),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (active) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JamSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun JamInlineAction(
    icon: ImageVector,
    label: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick, shapes = ButtonDefaults.shapes()) { Text(actionLabel) }
    }
}

@Composable
private fun JamPrimaryButton(
    label: String,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        enabled = enabled,
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun JamSecondaryButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun JamFailureBanner(message: String, dismissLabel: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                start = 20.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = dismissLabel)
            }
        }
    }
}

internal fun jamContributions(
    queue: List<JamTrack>,
    participants: List<JamParticipant>
): Map<String, Int> {
    if (queue.isEmpty() || participants.isEmpty()) return emptyMap()
    val known = participants.mapTo(hashSetOf()) { it.id }
    val counts = mutableMapOf<String, Int>()
    queue.forEach { track ->
        val owner = track.addedBy
        if (owner.isNotBlank() && owner in known) {
            counts[owner] = (counts[owner] ?: 0) + 1
        }
    }
    return counts
}

private fun jamPermissionLabel(permission: JamGuestPermission, strings: DerbisStrings): String = when (permission) {
    JamGuestPermission.HostOnly -> strings.jamPermissionHostOnly
    JamGuestPermission.AddSongs -> strings.jamPermissionAddSongs
    JamGuestPermission.Collaborative -> strings.jamPermissionCollaborative
}

private fun jamTransportLabel(mode: JamTransportMode): String = when (mode) {
    JamTransportMode.Lan -> "Local Listen Together (LAN)"
    JamTransportMode.Server -> "Remote Listen Together (Relay)"
}

private fun jamTransportDescription(mode: JamTransportMode, serverAvailable: Boolean): String = when (mode) {
    JamTransportMode.Lan -> "Synchronize playback with friends on the same Wi-Fi with ultra-low latency."
    JamTransportMode.Server -> if (serverAvailable) {
        "Hosted room relay so friends can listen together from anywhere across the internet."
    } else {
        "Requires a configured Derbis Jam relay server."
    }
}

private fun jamConnectionText(state: JamConnectionState, strings: DerbisStrings): String = when (state) {
    JamConnectionState.Idle,
    JamConnectionState.Disconnected -> strings.jamDisconnected
    JamConnectionState.Connecting -> strings.jamConnecting
    JamConnectionState.AwaitingApproval -> strings.jamAwaitingApproval
    JamConnectionState.Connected -> strings.connected
}

private fun jamFailureText(failure: JamFailure, strings: DerbisStrings): String = when (failure) {
    JamFailure.InvalidCode -> strings.jamInvalidCode
    JamFailure.NotAuthorized -> strings.jamNotAuthorized
    JamFailure.HostEnded -> strings.jamHostEnded
    JamFailure.Rejected -> strings.jamRejected
    JamFailure.Banned -> strings.jamBannedMessage
    JamFailure.SessionLocked -> strings.jamSessionLockedMessage
    JamFailure.SessionFull -> strings.jamSessionFull
    JamFailure.Removed -> strings.jamRemovedMessage
    JamFailure.ConnectionFailed,
    JamFailure.ProtocolError -> strings.jamConnectionFailed
}
