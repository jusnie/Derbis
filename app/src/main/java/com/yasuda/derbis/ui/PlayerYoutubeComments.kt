@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.yasuda.derbis.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.getValue
import com.yasuda.derbis.ui.theme.*
import com.yasuda.derbis.ui.components.derbisGroupedListShape
import androidx.compose.runtime.key
import com.yasuda.derbis.ui.player.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.yasuda.derbis.domain.YoutubeComment
import com.yasuda.derbis.domain.YoutubeCommentsState
import com.yasuda.derbis.ui.i18n.DerbisStrings
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged




@Composable
internal fun PlayerYoutubeCommentsSheet(
    comments: YoutubeCommentsState,
    primary: Color,
    secondary: Color,
    strings: DerbisStrings,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onToggleReplies: (String) -> Unit,
    onLoadMoreReplies: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val sheetInteraction = remember { MutableInteractionSource() }

    LaunchedEffect(comments.nextToken, comments.loadingMore, comments.items.size, comments.error) {
        if (comments.error != null || comments.nextToken.isBlank()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (
                    comments.nextToken.isNotBlank() &&
                    !comments.loadingMore &&
                    comments.items.isNotEmpty() &&
                    lastVisible >= comments.items.lastIndex - 2
                ) {
                    onLoadMore()
                }
            }
    }

    val pixel = DerbisIsPixel
    val light = DerbisIsLight
    val textColor = if (light) DerbisText else Color.White
    val mutedColor = if (light) DerbisMuted else Color.White.copy(alpha = 0.52f)
    val accentTint = if (pixel) PixelPalette.cyan else if (light) DerbisCyan else primary.playerMix(Color.White, 0.55f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    when {
                        pixel -> PixelPalette.text.copy(alpha = 0.28f)
                        light -> Color.Black.copy(alpha = 0.34f)
                        else -> Color.Black.copy(alpha = 0.62f)
                    }
                )
                .clickable(onClick = onDismiss)
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .fillMaxHeight(0.82f)
                .navigationBarsPadding()
                .clickable(
                    interactionSource = sheetInteraction,
                    indication = null,
                    onClick = {}
                ),
            color = when {
                pixel -> PixelPalette.black
                light -> DerbisInk
                else -> Color(0xFF101114)
            },
            shape = if (pixel) RectangleShape else RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            border = if (pixel) BorderStroke(PixelDesign.border, PixelPalette.text) else null
        ) {
            Column(modifier = Modifier.fillMaxSize().then(if (pixel) Modifier.pixelPaperGrid() else Modifier)) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(if (pixel) RectangleShape else CircleShape)
                        .background(if (pixel) PixelPalette.outline else if (light) DerbisAdaptiveHairline else Color.White.copy(alpha = 0.24f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (pixel) Modifier.background(PixelPalette.panel) else Modifier)
                        .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.totalComments.uppercase(),
                            color = accentTint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp
                        )
                        val commentCount = youtubeCommentCountBadge(comments.countText)
                        Text(
                            text = commentCount.ifBlank { "—" },
                            color = textColor,
                            fontSize = 30.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.8).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PlayerRoundIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = strings.close,
                        size = 38.dp,
                        iconSize = 20.dp,
                        tint = if (light) DerbisText else Color.White.copy(alpha = 0.82f),
                        background = if (pixel) PixelPalette.ink else if (light) DerbisPanel else Color.White.copy(alpha = 0.08f),
                        borderColor = if (pixel) PixelPalette.outline else Color.Transparent,
                        onClick = onDismiss
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (pixel) PixelDesign.border else 1.dp)
                        .then(
                            if (pixel) {
                                Modifier.background(PixelPalette.outline)
                            } else {
                                Modifier.background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            primary.copy(alpha = 0.34f),
                                            secondary.copy(alpha = 0.28f),
                                            Color.Transparent
                                        )
                                    )
                                )
                            }
                        )
                )

                when {
                    comments.loading && comments.items.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = accentTint,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    comments.disabled -> {
                        YoutubeCommentsEmptyState(
                            icon = Icons.Rounded.ChatBubbleOutline,
                            label = "${strings.totalComments}: —",
                            primary = primary
                        )
                    }
                    comments.error != null && comments.items.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                tint = mutedColor,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = if (pixel) PixelPalette.cyan else primary.copy(alpha = 0.18f),
                                border = BorderStroke(
                                    if (pixel) PixelDesign.border else 1.dp,
                                    if (pixel) PixelPalette.text else primary.copy(alpha = 0.34f)
                                ),
                                shape = if (pixel) RectangleShape else CircleShape,
                                modifier = Modifier.pressable(onClick = onRetry)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    val btnFg = if (pixel) PixelPalette.ink else textColor
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        tint = btnFg,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = strings.check,
                                        color = btnFg,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    comments.items.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            YoutubeCommentsEmptyState(
                                icon = Icons.Rounded.ChatBubbleOutline,
                                label = if (comments.nextToken.isBlank()) {
                                    "${strings.totalComments}: 0"
                                } else {
                                    strings.totalComments
                                },
                                primary = primary,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (comments.nextToken.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                TextButton(onClick = onLoadMore) {
                                    Text(
                                        text = strings.more,
                                        color = accentTint,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(if (pixel) 8.dp else 3.dp)
                        ) {
                            itemsIndexed(
                                items = comments.items,
                                key = { _, comment -> comment.id },
                                contentType = { _, _ -> "youtube-comment" }
                            ) { index, comment ->
                                YoutubeCommentCard(
                                    comment = comment,
                                    shape = if (pixel) RectangleShape else derbisGroupedListShape(index, comments.items.size),
                                    primary = primary,
                                    secondary = secondary,
                                    moreLabel = strings.more,
                                    onToggleReplies = { onToggleReplies(comment.id) },
                                    onLoadMoreReplies = { onLoadMoreReplies(comment.id) }
                                )
                            }
                            if (comments.loadingMore) {
                                item(key = "comments-loading-more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.4.dp,
                                            color = accentTint
                                        )
                                    }
                                }
                            } else if (comments.error != null && comments.nextToken.isNotBlank()) {
                                item(key = "comments-retry-more") {
                                    TextButton(
                                        onClick = onRetryLoadMore,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            tint = accentTint,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(7.dp))
                                        Text(
                                            text = strings.check,
                                            color = accentTint,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else if (comments.nextToken.isNotBlank()) {
                                item(key = "comments-more") {
                                    TextButton(
                                        onClick = onLoadMore,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = strings.more,
                                            color = accentTint,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YoutubeCommentsEmptyState(
    icon: ImageVector,
    label: String,
    primary: Color,
    modifier: Modifier = Modifier
) {
    val pixel = DerbisIsPixel
    val light = DerbisIsLight
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = if (pixel) PixelPalette.panel else primary.copy(alpha = 0.10f),
            shape = if (pixel) RectangleShape else CircleShape,
            border = if (pixel) BorderStroke(1.dp, PixelPalette.outline) else null
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (light) DerbisMuted else Color.White.copy(alpha = 0.44f),
                modifier = Modifier.padding(16.dp).size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            color = if (light) DerbisMuted else Color.White.copy(alpha = 0.52f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun YoutubeCommentCard(
    comment: YoutubeComment,
    shape: Shape,
    primary: Color,
    secondary: Color,
    moreLabel: String,
    onToggleReplies: () -> Unit,
    onLoadMoreReplies: () -> Unit
) {
    val pixel = DerbisIsPixel
    val light = DerbisIsLight
    Surface(
        color = when {
            pixel && comment.pinned -> PixelPalette.panel
            pixel -> PixelPalette.ink
            comment.pinned -> primary.copy(alpha = 0.16f)
            light -> DerbisPanel
            else -> Color.White.copy(alpha = 0.06f)
        },
        shape = shape,
        border = if (pixel) BorderStroke(1.dp, PixelPalette.outline) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            YoutubeCommentHeader(comment, primary)

            Text(
                text = comment.text,
                color = if (light) DerbisText else Color.White.copy(alpha = 0.88f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal
            )

            YoutubeCommentActions(comment, secondary, onToggleReplies)

            YoutubeCommentReplies(comment, primary, moreLabel, onToggleReplies, onLoadMoreReplies)
        }
    }
}

@Composable
private fun YoutubeCommentHeader(comment: YoutubeComment, primary: Color) {
    val light = DerbisIsLight
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        YoutubeCommentAvatar(
            url = comment.authorAvatarUrl,
            author = comment.author,
            tint = if (DerbisIsPixel) PixelPalette.cyan else primary,
            modifier = Modifier.size(38.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = comment.author,
                    color = if (light) DerbisText else Color.White.copy(alpha = 0.94f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (comment.verifiedAuthor) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = if (light) DerbisCyan else primary.playerMix(Color.White, 0.44f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                if (comment.pinned) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null,
                        tint = if (light) DerbisMuted else Color.White.copy(alpha = 0.46f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            if (comment.publishedText.isNotBlank()) {
                Text(
                    text = comment.publishedText,
                    color = if (light) DerbisMuted else Color.White.copy(alpha = 0.46f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        if (comment.heartedByUploader) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF5B72),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun YoutubeCommentActions(comment: YoutubeComment, secondary: Color, onToggleReplies: () -> Unit) {
    val pixel = DerbisIsPixel
    val light = DerbisIsLight
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (comment.likeCountText.isNotBlank()) {
            val pillShape = if (pixel) RectangleShape else CircleShape
            Row(
                modifier = Modifier
                    .clip(pillShape)
                    .background(if (pixel) PixelPalette.panel else if (light) DerbisInk else Color.White.copy(alpha = 0.07f))
                    .then(if (pixel) Modifier.border(1.dp, PixelPalette.outline, RectangleShape) else Modifier)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ThumbUp,
                    contentDescription = null,
                    tint = if (light) DerbisMuted else Color.White.copy(alpha = 0.62f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = comment.likeCountText,
                    color = if (light) DerbisText else Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (comment.replyCount > 0 && comment.replyToken.isNotBlank()) {
            val replyPressSource = remember { MutableInteractionSource() }
            val replyPressed by replyPressSource.collectIsPressedAsState()
            val replyCorner by animateDpAsState(
                targetValue = if (replyPressed || comment.repliesExpanded) 10.dp else 18.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "comment-reply-corner"
            )
            val replyShape = if (pixel) RectangleShape else RoundedCornerShape(replyCorner)
            val replyTint = if (pixel) PixelPalette.cyan else if (light) DerbisCyan else secondary.playerMix(Color.White, 0.66f)
            Row(
                modifier = Modifier
                    .clip(replyShape)
                    .background(
                        when {
                            pixel && comment.repliesExpanded -> PixelPalette.panelSoft
                            pixel -> PixelPalette.panel
                            else -> secondary.copy(alpha = if (comment.repliesExpanded) 0.34f else 0.20f)
                        }
                    )
                    .then(if (pixel) Modifier.border(1.dp, PixelPalette.outline, RectangleShape) else Modifier)
                    .clickable(
                        interactionSource = replyPressSource,
                        indication = LocalIndication.current,
                        onClick = onToggleReplies
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Reply,
                    contentDescription = null,
                    tint = replyTint,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = comment.replyCount.toString(),
                    color = replyTint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.YoutubeCommentReplies(
    comment: YoutubeComment,
    primary: Color,
    moreLabel: String,
    onToggleReplies: () -> Unit,
    onLoadMoreReplies: () -> Unit
) {
    val pixel = DerbisIsPixel
    val light = DerbisIsLight
    val accentTint = if (pixel) PixelPalette.cyan else if (light) DerbisCyan else primary.playerMix(Color.White, 0.52f)
    AnimatedVisibility(
        visible = comment.repliesExpanded,
        enter = fadeIn(tween(160)) + slideInVertically(initialOffsetY = { -it / 5 }),
        exit = fadeOut(tween(120))
    ) {
        val boxShape = if (pixel) RectangleShape else RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 2.dp)
                .clip(boxShape)
                .background(if (pixel) PixelPalette.black else if (light) DerbisInk else Color.Black.copy(alpha = 0.22f))
                .then(if (pixel) Modifier.border(1.dp, PixelPalette.outline, RectangleShape) else Modifier)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            comment.replies.forEach { reply ->
                YoutubeCommentReply(reply = reply, primary = primary)
            }
            if (comment.repliesLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = accentTint
                    )
                }
            } else if (comment.repliesError != null && comment.replies.isEmpty()) {
                TextButton(onClick = onToggleReplies, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = accentTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else if (comment.repliesNextToken.isNotBlank()) {
                TextButton(onClick = onLoadMoreReplies, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = moreLabel,
                        color = accentTint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun YoutubeCommentReply(
    reply: YoutubeComment,
    primary: Color
) {
    val light = DerbisIsLight
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        YoutubeCommentAvatar(
            url = reply.authorAvatarUrl,
            author = reply.author,
            tint = if (DerbisIsPixel) PixelPalette.cyan else Color.White,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = reply.author,
                    color = if (light) DerbisText else Color.White.copy(alpha = 0.86f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (reply.verifiedAuthor) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = if (light) DerbisCyan else primary.playerMix(Color.White, 0.44f),
                        modifier = Modifier.size(11.dp)
                    )
                }
                if (reply.heartedByUploader) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF5B72),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = reply.text,
                color = if (light) DerbisText else Color.White.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (reply.publishedText.isNotBlank()) {
                    Text(
                        text = reply.publishedText,
                        color = if (light) DerbisMuted else Color.White.copy(alpha = 0.36f),
                        fontSize = 9.5.sp
                    )
                }
                if (reply.likeCountText.isNotBlank()) {
                    Text(
                        text = "👍 ${reply.likeCountText}",
                        color = if (light) DerbisMuted else Color.White.copy(alpha = 0.36f),
                        fontSize = 9.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun YoutubeCommentAvatar(
    url: String,
    author: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val pixel = DerbisIsPixel
    val light = DerbisIsLight
    Surface(
        modifier = modifier,
        shape = if (pixel) RectangleShape else CircleShape,
        color = if (pixel) PixelPalette.panel else tint.copy(alpha = 0.24f),
        border = if (pixel) BorderStroke(1.dp, PixelPalette.outline) else null
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = author,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = author.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString().orEmpty(),
                    color = if (light) DerbisText else tint.playerMix(Color.White, 0.7f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
