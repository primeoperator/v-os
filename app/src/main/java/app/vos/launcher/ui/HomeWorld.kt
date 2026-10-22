package app.vos.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.vos.launcher.model.AppItem
import app.vos.launcher.model.LayoutState
import app.vos.launcher.model.PlacedApp
import app.vos.launcher.model.Room
import app.vos.launcher.model.SceneMetrics
import app.vos.launcher.ui.theme.LocalVos
import app.vos.launcher.ui.theme.roomTheme
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val TWO_PI = 6.2831855f

/** Игровая 2D-сцена: объекты-приложения в выбранной комнате. */
@Composable
fun HomeWorld(
    room: Room,
    apps: List<AppItem>,
    layout: LayoutState,
    editMode: Boolean,
    accent: Color,
    feedback: Feedback,
    onLaunch: (AppItem) -> Unit,
    onEnterEdit: () -> Unit,
    onMove: (key: String, x: Float, yDp: Float) -> Unit,
    onChoose: (AppItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = room,
        modifier = modifier,
        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) },
        label = "room-switch",
    ) { target ->
        val placed = remember(apps, layout, target) {
            apps.mapNotNull { app ->
                layout.placements[app.key]
                    ?.takeIf { it.room == target.id }
                    ?.let { PlacedApp(app, it) }
            }.sortedBy { it.placement.yDp }
        }
        RoomScene(
            room = target,
            placed = placed,
            editMode = editMode,
            accent = accent,
            feedback = feedback,
            onLaunch = onLaunch,
            onEnterEdit = onEnterEdit,
            onMove = onMove,
            onChoose = onChoose,
        )
    }
}

@Composable
private fun RoomScene(
    room: Room,
    placed: List<PlacedApp>,
    editMode: Boolean,
    accent: Color,
    feedback: Feedback,
    onLaunch: (AppItem) -> Unit,
    onEnterEdit: () -> Unit,
    onMove: (String, Float, Float) -> Unit,
    onChoose: (AppItem) -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "idle")
    val phase = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "phase",
    )
    val scrollState = rememberScrollState()
    val currentFeedback by rememberUpdatedState(feedback)
    val currentEnterEdit by rememberUpdatedState(onEnterEdit)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sceneWidthPx = constraints.maxWidth.toFloat()
        val lowest = placed.maxOfOrNull { it.placement.yDp } ?: 0f
        val contentHeight = maxHeight.coerceAtLeast((lowest + SceneMetrics.OBJECT_HEIGHT_DP + 150f).dp)

        Box(
            Modifier
                .fillMaxSize()
                // Долгое нажатие на пустое место тоже включает режим редактирования
                .pointerInput(editMode) {
                    detectTapGestures(
                        onLongPress = {
                            if (!editMode) {
                                currentFeedback.pick()
                                currentEnterEdit()
                            }
                        },
                    )
                }
                .verticalScroll(scrollState),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(contentHeight),
            ) {
                placed.forEachIndexed { index, item ->
                    key(item.app.key) {
                        SceneObject(
                            item = item,
                            index = index,
                            sceneWidthPx = sceneWidthPx,
                            editMode = editMode,
                            accent = accent,
                            phase = phase,
                            feedback = feedback,
                            onLaunch = onLaunch,
                            onEnterEdit = onEnterEdit,
                            onMove = onMove,
                            onChoose = onChoose,
                        )
                    }
                }
            }
        }

        if (placed.isEmpty()) EmptyRoom(room, accent)
    }
}

@Composable
private fun EmptyRoom(room: Room, accent: Color) {
    val vos = LocalVos.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🪴", fontSize = 52.sp)
        Text(
            text = "В комнате «${room.title}» пока пусто",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = vos.textStrong,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Открой другую комнату, включи «Редактировать», нажми на приложение и выбери эту комнату.",
            fontSize = 15.sp,
            color = vos.textSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Box(
            Modifier
                .padding(top = 16.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent),
        )
    }
}

@Composable
private fun SceneObject(
    item: PlacedApp,
    index: Int,
    sceneWidthPx: Float,
    editMode: Boolean,
    accent: Color,
    phase: State<Float>,
    feedback: Feedback,
    onLaunch: (AppItem) -> Unit,
    onEnterEdit: () -> Unit,
    onMove: (String, Float, Float) -> Unit,
    onChoose: (AppItem) -> Unit,
) {
    val vos = LocalVos.current
    val dens = LocalDensity.current
    val objectWidthPx = with(dens) { SceneMetrics.OBJECT_WIDTH_DP.dp.toPx() }
    val rangePx = (sceneWidthPx - objectWidthPx).coerceAtLeast(1f)
    val maxYPx = with(dens) { SceneMetrics.MAX_Y_DP.dp.toPx() }
    val tilePx = with(dens) { SceneMetrics.TILE_DP.dp.toPx() }
    val bobPx = with(dens) { 4.dp.toPx() }
    val placement = item.placement

    // Положение в пикселях сцены. Меняется при перетаскивании без перекомпоновки.
    val pos = remember(placement.x, placement.yDp, sceneWidthPx) {
        mutableStateOf(
            Offset(
                x = placement.x * rangePx,
                y = with(dens) { placement.yDp.dp.toPx() },
            ),
        )
    }
    var dragging by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }

    // Плавное появление
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(18) * 28L)
        appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
    }

    val pressScale by animateFloatAsState(
        targetValue = if (dragging) 1.1f else if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "press-scale",
    )
    val seed = remember(item.app.key) { (item.app.key.hashCode() and 0xFF) / 255f }

    val fb by rememberUpdatedState(feedback)
    val launch by rememberUpdatedState(onLaunch)
    val enterEdit by rememberUpdatedState(onEnterEdit)
    val move by rememberUpdatedState(onMove)
    val choose by rememberUpdatedState(onChoose)
    val app = item.app

    val tileShape = RoundedCornerShape(22.dp)

    Box(
        Modifier
            .offset { IntOffset(pos.value.x.roundToInt(), pos.value.y.roundToInt()) }
            .zIndex(if (dragging) 10f else 0f)
            .width(SceneMetrics.OBJECT_WIDTH_DP.dp)
            .graphicsLayer {
                val a = appear.value
                alpha = a.coerceIn(0f, 1f)
                val s = (0.55f + 0.45f * a) * pressScale
                scaleX = s
                scaleY = s
                rotationZ = if (editMode && !dragging) {
                    sin((phase.value * 9f + seed) * TWO_PI) * 2.2f
                } else {
                    0f
                }
            }
            .semantics {
                contentDescription = app.label
                role = Role.Button
            }
            .pointerInput(editMode, pos, rangePx) {
                if (editMode) {
                    val commit = {
                        val x = pos.value.x.coerceIn(0f, rangePx)
                        val y = pos.value.y.coerceIn(0f, maxYPx)
                        pos.value = Offset(x, y)
                        fb.drop()
                        move(app.key, x / rangePx, y / this.density)
                    }
                    detectDragGestures(
                        onDragStart = {
                            dragging = true
                            fb.pick()
                        },
                        onDragEnd = {
                            dragging = false
                            commit()
                        },
                        onDragCancel = {
                            dragging = false
                            commit()
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            pos.value = pos.value + amount
                        },
                    )
                } else {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                        onTap = {
                            fb.tap()
                            launch(app)
                        },
                        onLongPress = {
                            fb.pick()
                            enterEdit()
                        },
                    )
                }
            }
            .pointerInput(editMode) {
                if (editMode) {
                    detectTapGestures(
                        onTap = {
                            fb.tap()
                            choose(app)
                        },
                    )
                }
            },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Мягкая тень на полу: сжимается, когда объект «взлетает»
                    val up = (sin((phase.value + seed) * TWO_PI) + 1f) / 2f
                    val k = 1f - 0.14f * up
                    val sw = 54.dp.toPx() * k
                    val sh = 12.dp.toPx() * k
                    drawOval(
                        color = Color.Black.copy(alpha = 0.17f - 0.05f * up),
                        topLeft = Offset((size.width - sw) / 2f, tilePx - sh * 0.35f),
                        size = Size(sw, sh),
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(SceneMetrics.TILE_DP.dp)
                    .graphicsLayer {
                        val up = (sin((phase.value + seed) * TWO_PI) + 1f) / 2f
                        translationY = if (dragging) 0f else -up * bobPx
                    }
                    .shadow(
                        elevation = if (dragging) 18.dp else 8.dp,
                        shape = tileShape,
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.35f),
                    )
                    .clip(tileShape)
                    .background(vos.tileBg)
                    .border(
                        width = if (editMode) 2.dp else 1.dp,
                        color = if (editMode) accent else vos.tileBorder,
                        shape = tileShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val icon = app.icon
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(5.dp),
                        filterQuality = FilterQuality.Medium,
                    )
                } else {
                    Text(
                        text = app.label.take(1).uppercase(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                    )
                }
            }
            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = vos.textStrong,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(vos.labelBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
