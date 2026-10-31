package com.erfansvsh.ioscalculator

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.*

// =========================================================================
// BUTTON ANIMATION TIMING CONFIGURATION (TUNABLE HERE)
// =========================================================================
// Attack: instantaneous touch-down response in milliseconds (default: 80ms)
const val BUTTON_PRESS_DOWN_MS = 80

// Release: soft, breath-like lingering color decay in milliseconds (default: 480ms)
// Increase (e.g., 500, 520, 560) for an even slower fade, or decrease (e.g., 400, 350) for faster.
const val BUTTON_RELEASE_FADE_MS = 480

// =========================================================================
// EXACT iOS CALCULATOR COLORS (EXTRACTED FROM REAL IPHONE)
// =========================================================================
val ColorBlack = Color(0xFF000000)
val ColorWhite = Color(0xFFFFFFFF)

// Number buttons
val ColorDarkGray = Color(0xFF333333)
val ColorDarkGrayPressed = Color(0xFF737373)

// The three top buttons: AC, +/-, %
val ColorLightGray = Color(0xFFA6A6A6)
val ColorLightGrayPressed = Color(0xFFD9D9D9)

// The five operator buttons: ÷, ×, −, +, =
val ColorOrange = Color(0xFFFF9F0A)
val ColorOrangePressed = Color(0xFFFCC781)

// Scientific math buttons
val ColorSciGray = Color(0xFF212121)
val ColorSciGrayPressed = Color(0xFF4D4D4D)

// The 'mr' button while activated: #808080 with white text
val ColorMrActiveBg = Color(0xFF808080)
val ColorMrActiveFg = Color(0xFFFFFFFF)

// Other active scientific toggle buttons (e.g., 2nd)
val ColorSciActiveBg = Color(0xFF808080)
val ColorSciActiveFg = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure the app is NOT displayed fullscreen - status bar and navigation bar visible like a regular app
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
        insetsController.isAppearanceLightStatusBars = false // Crisp white system icons (clock, signal, battery) on black
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        setContent {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = ColorBlack
            ) {
                if (isLandscape) {
                    LandscapeCalculator(viewModel)
                } else {
                    PortraitCalculator(viewModel)
                }
            }
        }
    }
}

/**
 * Universal iOS button component providing authentic Apple breath-like animation:
 * Immediate touch-down (BUTTON_PRESS_DOWN_MS), followed by a soft, slower lingering breath-like fade out (BUTTON_RELEASE_FADE_MS) on release.
 */
@Composable
fun UniversalIosButton(
    modifier: Modifier = Modifier,
    defaultBg: Color,
    pressedBg: Color,
    defaultFg: Color,
    isActive: Boolean = false,
    activeBg: Color = ColorWhite,
    activeFg: Color = ColorOrange,
    shape: Shape = CircleShape,
    onClick: () -> Unit,
    content: @Composable (fg: Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetBg = when {
        isActive -> activeBg
        isPressed -> pressedBg
        else -> defaultBg
    }

    val targetFg = when {
        isActive -> activeFg
        else -> defaultFg
    }

    // Authentic Apple breath-like color transition:
    // Immediate soft touch-down, followed by an elegant, slower fade out on finger lift
    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = if (isPressed) {
            tween(durationMillis = BUTTON_PRESS_DOWN_MS, easing = LinearEasing)
        } else {
            tween(durationMillis = BUTTON_RELEASE_FADE_MS, easing = FastOutSlowInEasing)
        },
        label = "iosBtnBg"
    )

    val animatedFg by animateColorAsState(
        targetValue = targetFg,
        animationSpec = if (isPressed) {
            tween(durationMillis = BUTTON_PRESS_DOWN_MS, easing = LinearEasing)
        } else {
            tween(durationMillis = BUTTON_RELEASE_FADE_MS, easing = FastOutSlowInEasing)
        },
        label = "iosBtnFg"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier,
        shape = shape,
        color = animatedBg
    ) {
        Box(contentAlignment = Alignment.Center) {
            content(animatedFg)
        }
    }
}

@Composable
fun PlusMinusIcon(
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 36.sp,
    isBold: Boolean = true
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "+",
                color = color,
                fontSize = fontSize * 0.65f,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.offset(y = (-5).dp, x = 1.dp)
            )
            Text(
                text = "/",
                color = color,
                fontSize = fontSize * 0.9f,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.offset(y = (-1).dp)
            )
            Text(
                text = "−",
                color = color,
                fontSize = fontSize * 0.75f,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.offset(y = (4).dp, x = (-1).dp)
            )
        }
    }
}

@Composable
fun PortraitCalculator(vm: CalculatorViewModel) {
    val displayValue by vm.displayValue.collectAsState()
    val activeOperator by vm.activeOperator.collectAsState()
    val haptic = LocalHapticFeedback.current

    val clearLabel = if (displayValue != "0") "C" else "AC"
    val formattedDisplay = formatPortraitDisplay(displayValue)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Display with swipe to delete
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (abs(dragAmount) > 20) {
                            vm.onBackspace()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                },
            contentAlignment = Alignment.BottomEnd
        ) {
            val fontSize = when {
                formattedDisplay.length <= 5 -> 100.sp
                formattedDisplay.length == 6 -> 90.sp
                formattedDisplay.length == 7 -> 82.sp
                formattedDisplay.length == 8 -> 70.sp
                formattedDisplay.length == 9 -> 58.sp
                else -> 48.sp
            }

            Text(
                text = formattedDisplay,
                color = ColorWhite,
                fontSize = fontSize,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row 1: AC/C, +/-, %, ÷
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UniversalIosButton(
                defaultBg = ColorLightGray,
                pressedBg = ColorLightGrayPressed,
                defaultFg = ColorBlack,
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onClear()
                }
            ) { fg ->
                Text(text = clearLabel, color = fg, fontSize = 38.sp, fontWeight = FontWeight.Normal)
            }

            // Bold +/- button
            UniversalIosButton(
                defaultBg = ColorLightGray,
                pressedBg = ColorLightGrayPressed,
                defaultFg = ColorBlack,
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onToggleSign()
                }
            ) { fg ->
                PlusMinusIcon(color = fg, fontSize = 36.sp, isBold = true)
            }

            // Bold % button
            UniversalIosButton(
                defaultBg = ColorLightGray,
                pressedBg = ColorLightGrayPressed,
                defaultFg = ColorBlack,
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onPercent()
                }
            ) { fg ->
                Text(text = "%", color = fg, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            }

            UniversalIosButton(
                defaultBg = ColorOrange,
                pressedBg = ColorOrangePressed,
                defaultFg = ColorWhite,
                activeBg = ColorWhite,
                activeFg = ColorOrange,
                isActive = activeOperator == "÷",
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onOperator("÷")
                }
            ) { fg ->
                Text(text = "÷", color = fg, fontSize = 48.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-3).dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: 7, 8, 9, ×
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("7", "8", "9").forEach { digit ->
                UniversalIosButton(
                    defaultBg = ColorDarkGray,
                    pressedBg = ColorDarkGrayPressed,
                    defaultFg = ColorWhite,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.onDigit(digit)
                    }
                ) { fg ->
                    Text(text = digit, color = fg, fontSize = 42.sp, fontWeight = FontWeight.Normal)
                }
            }

            UniversalIosButton(
                defaultBg = ColorOrange,
                pressedBg = ColorOrangePressed,
                defaultFg = ColorWhite,
                activeBg = ColorWhite,
                activeFg = ColorOrange,
                isActive = activeOperator == "×",
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onOperator("×")
                }
            ) { fg ->
                Text(text = "×", color = fg, fontSize = 48.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-3).dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 3: 4, 5, 6, −
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("4", "5", "6").forEach { digit ->
                UniversalIosButton(
                    defaultBg = ColorDarkGray,
                    pressedBg = ColorDarkGrayPressed,
                    defaultFg = ColorWhite,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.onDigit(digit)
                    }
                ) { fg ->
                    Text(text = digit, color = fg, fontSize = 42.sp, fontWeight = FontWeight.Normal)
                }
            }

            UniversalIosButton(
                defaultBg = ColorOrange,
                pressedBg = ColorOrangePressed,
                defaultFg = ColorWhite,
                activeBg = ColorWhite,
                activeFg = ColorOrange,
                isActive = activeOperator == "−",
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onOperator("−")
                }
            ) { fg ->
                Text(text = "−", color = fg, fontSize = 48.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-2).dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 4: 1, 2, 3, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("1", "2", "3").forEach { digit ->
                UniversalIosButton(
                    defaultBg = ColorDarkGray,
                    pressedBg = ColorDarkGrayPressed,
                    defaultFg = ColorWhite,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.onDigit(digit)
                    }
                ) { fg ->
                    Text(text = digit, color = fg, fontSize = 42.sp, fontWeight = FontWeight.Normal)
                }
            }

            UniversalIosButton(
                defaultBg = ColorOrange,
                pressedBg = ColorOrangePressed,
                defaultFg = ColorWhite,
                activeBg = ColorWhite,
                activeFg = ColorOrange,
                isActive = activeOperator == "+",
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onOperator("+")
                }
            ) { fg ->
                Text(text = "+", color = fg, fontSize = 48.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-2).dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 5: 0 (spanning 2 columns, exactly aligned horizontally with "1"), ., =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // '0' button spanning 2 columns: left section matches column 1 width exactly, centering '0' directly under '1'
            UniversalIosButton(
                defaultBg = ColorDarkGray,
                pressedBg = ColorDarkGrayPressed,
                defaultFg = ColorWhite,
                modifier = Modifier
                    .weight(2f)
                    .aspectRatio(2.15f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onDigit("0")
                }
            ) { fg ->
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "0", color = fg, fontSize = 42.sp, fontWeight = FontWeight.Normal)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            UniversalIosButton(
                defaultBg = ColorDarkGray,
                pressedBg = ColorDarkGrayPressed,
                defaultFg = ColorWhite,
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onDecimal()
                }
            ) { fg ->
                Text(text = ".", color = fg, fontSize = 42.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-4).dp))
            }

            UniversalIosButton(
                defaultBg = ColorOrange,
                pressedBg = ColorOrangePressed,
                defaultFg = ColorWhite,
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onEquals()
                }
            ) { fg ->
                Text(text = "=", color = fg, fontSize = 48.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-2).dp))
            }
        }
    }
}

@Composable
fun LandscapeCalculator(vm: CalculatorViewModel) {
    val displayValue by vm.displayValue.collectAsState()
    val activeOperator by vm.activeOperator.collectAsState()
    val isRadMode by vm.isRadMode.collectAsState()
    val is2nd by vm.is2nd.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Local memory presence state for 'mr' highlight: active on m+/m-, cleared on mc
    var hasMemory by remember { mutableStateOf(false) }

    val clearLabel = if (displayValue != "0") "C" else "AC"
    val formattedDisplay = formatLandscapeDisplay(displayValue)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Landscape Display with Rad/Deg indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (abs(dragAmount) > 20) {
                            vm.onBackspace()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                },
            contentAlignment = Alignment.BottomEnd
        ) {
            if (isRadMode) {
                Text(
                    text = "Rad",
                    color = ColorLightGray,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 4.dp)
                )
            }
            Text(
                text = formattedDisplay,
                color = ColorWhite,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 10 column grid rows for scientific landscape
        val radButtonLabel = if (isRadMode) "Deg" else "Rad"

        val sciCols = listOf(
            listOf("(", ")", "mc", "m+", "m-", "mr", clearLabel, "±", "%", "÷"),
            listOf(if (is2nd) "2ⁿᵈ" else "2ⁿᵈ", "x²", "x³", "xʸ", if (is2nd) "yˣ" else "eˣ", if (is2nd) "2ˣ" else "10ˣ", "7", "8", "9", "×"),
            listOf("1/x", "²√x", "³√x", "ʸ√x", if (is2nd) "logᵧ" else "ln", if (is2nd) "log₂" else "log₁₀", "4", "5", "6", "−"),
            listOf("x!", if (is2nd) "sin⁻¹" else "sin", if (is2nd) "cos⁻¹" else "cos", if (is2nd) "tan⁻¹" else "tan", "e", "EE", "1", "2", "3", "+"),
            listOf(radButtonLabel, if (is2nd) "sinh⁻¹" else "sinh", if (is2nd) "cosh⁻¹" else "cosh", if (is2nd) "tanh⁻¹" else "tanh", "π", "Rand", "0", ".", "=")
        )

        // Row 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sciCols[0].forEachIndexed { i, label ->
                val isOp = i == 9
                val isTopLight = i in 6..8
                val isMr = label == "mr"
                val isMrActive = isMr && hasMemory
                val isDivActive = isOp && activeOperator == "÷"
                val isActive = isDivActive || isMrActive

                val defaultBg = if (isOp) ColorOrange else if (isTopLight) ColorLightGray else ColorSciGray
                val pressedBg = if (isOp) ColorOrangePressed else if (isTopLight) ColorLightGrayPressed else ColorSciGrayPressed
                val defaultFg = if (isTopLight) ColorBlack else ColorWhite

                // Real iOS: when 'mr' is activated, it displays darker gray #808080 with white text
                val activeBgColor = if (isMr) ColorMrActiveBg else if (isOp) ColorWhite else ColorSciActiveBg
                val activeFgColor = if (isMr) ColorMrActiveFg else if (isOp) ColorOrange else ColorSciActiveFg

                UniversalIosButton(
                    defaultBg = defaultBg,
                    pressedBg = pressedBg,
                    defaultFg = defaultFg,
                    activeBg = activeBgColor,
                    activeFg = activeFgColor,
                    isActive = isActive,
                    modifier = Modifier.weight(1f).height(48.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (label) {
                            "AC", "C" -> vm.onClear()
                            "±" -> vm.onToggleSign()
                            "%" -> vm.onPercent()
                            "÷" -> vm.onOperator("÷")
                            "mc" -> {
                                hasMemory = false
                                vm.onScientific(label)
                            }
                            "m+", "m-" -> {
                                hasMemory = true
                                vm.onScientific(label)
                            }
                            else -> vm.onScientific(label)
                        }
                    }
                ) { fg ->
                    if (label == "±") {
                        PlusMinusIcon(color = fg, fontSize = 24.sp, isBold = true)
                    } else {
                        Text(
                            text = label,
                            color = fg,
                            fontSize = if (isOp) 34.sp else if (isTopLight) 22.sp else 19.sp,
                            fontWeight = if (label == "%" || label == "±") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))

        // Row 2
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sciCols[1].forEachIndexed { i, label ->
                val isDigit = i in 6..8
                val isOp = i == 9
                val isPowerOp = label == "xʸ" || label == "yˣ"
                val is2ndBtn = label == "2ⁿᵈ"
                val isActiveOp = (isOp && activeOperator == "×") || (isPowerOp && activeOperator == label) || (is2ndBtn && is2nd)

                val defaultBg = if (isOp) ColorOrange else if (isDigit) ColorDarkGray else ColorSciGray
                val pressedBg = if (isOp) ColorOrangePressed else if (isDigit) ColorDarkGrayPressed else ColorSciGrayPressed

                val activeBgColor = if (isPowerOp || is2ndBtn) ColorSciActiveBg else ColorWhite
                val activeFgColor = if (isPowerOp || is2ndBtn) ColorSciActiveFg else ColorOrange

                UniversalIosButton(
                    defaultBg = defaultBg,
                    pressedBg = pressedBg,
                    defaultFg = ColorWhite,
                    activeBg = activeBgColor,
                    activeFg = activeFgColor,
                    isActive = isActiveOp,
                    modifier = Modifier.weight(1f).height(48.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when {
                            isDigit -> vm.onDigit(label)
                            isOp -> vm.onOperator("×")
                            label == "2ⁿᵈ" -> vm.toggle2nd()
                            isPowerOp -> vm.onOperator(label)
                            else -> vm.onScientific(label)
                        }
                    }
                ) { fg ->
                    Text(
                        text = label,
                        color = fg,
                        fontSize = if (isOp) 34.sp else if (isDigit) 30.sp else 19.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))

        // Row 3
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sciCols[2].forEachIndexed { i, label ->
                val isDigit = i in 6..8
                val isOp = i == 9
                val isRootOp = label == "ʸ√x" || label == "log_y" || label == "logᵧ"
                val isActiveOp = (isOp && activeOperator == "−") || (isRootOp && activeOperator == label)

                val defaultBg = if (isOp) ColorOrange else if (isDigit) ColorDarkGray else ColorSciGray
                val pressedBg = if (isOp) ColorOrangePressed else if (isDigit) ColorDarkGrayPressed else ColorSciGrayPressed

                val activeBgColor = if (isRootOp) ColorSciActiveBg else ColorWhite
                val activeFgColor = if (isRootOp) ColorSciActiveFg else ColorOrange

                UniversalIosButton(
                    defaultBg = defaultBg,
                    pressedBg = pressedBg,
                    defaultFg = ColorWhite,
                    activeBg = activeBgColor,
                    activeFg = activeFgColor,
                    isActive = isActiveOp,
                    modifier = Modifier.weight(1f).height(48.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when {
                            isDigit -> vm.onDigit(label)
                            isOp -> vm.onOperator("−")
                            isRootOp -> vm.onOperator(label)
                            else -> vm.onScientific(label)
                        }
                    }
                ) { fg ->
                    Text(
                        text = label,
                        color = fg,
                        fontSize = if (isOp) 34.sp else if (isDigit) 30.sp else 19.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))

        // Row 4
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sciCols[3].forEachIndexed { i, label ->
                val isDigit = i in 6..8
                val isOp = i == 9
                val isEEOp = label == "EE"
                val isActiveOp = (isOp && activeOperator == "+") || (isEEOp && activeOperator == "EE")

                val defaultBg = if (isOp) ColorOrange else if (isDigit) ColorDarkGray else ColorSciGray
                val pressedBg = if (isOp) ColorOrangePressed else if (isDigit) ColorDarkGrayPressed else ColorSciGrayPressed

                val activeBgColor = if (isEEOp) ColorSciActiveBg else ColorWhite
                val activeFgColor = if (isEEOp) ColorSciActiveFg else ColorOrange

                UniversalIosButton(
                    defaultBg = defaultBg,
                    pressedBg = pressedBg,
                    defaultFg = ColorWhite,
                    activeBg = activeBgColor,
                    activeFg = activeFgColor,
                    isActive = isActiveOp,
                    modifier = Modifier.weight(1f).height(48.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when {
                            isDigit -> vm.onDigit(label)
                            isOp -> vm.onOperator("+")
                            isEEOp -> vm.onOperator("EE")
                            else -> vm.onScientific(label)
                        }
                    }
                ) { fg ->
                    Text(
                        text = label,
                        color = fg,
                        fontSize = if (isOp) 34.sp else if (isDigit) 30.sp else 19.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))

        // Row 5: 6 sci buttons, 0 (double width aligned directly under "1"), ., =
        // Unified styling: every single button shares height(48.dp) with consistent landscape capsule shape
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Rad/Deg button
            UniversalIosButton(
                defaultBg = ColorSciGray,
                pressedBg = ColorSciGrayPressed,
                defaultFg = ColorWhite,
                modifier = Modifier.weight(1f).height(48.dp),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.toggleRad()
                }
            ) { fg ->
                Text(text = radButtonLabel, color = fg, fontSize = 19.sp, fontWeight = FontWeight.Normal)
            }

            listOf("sinh", "cosh", "tanh", "π", "Rand").forEach { label ->
                val displayLabel = when {
                    is2nd && label == "sinh" -> "sinh⁻¹"
                    is2nd && label == "cosh" -> "cosh⁻¹"
                    is2nd && label == "tanh" -> "tanh⁻¹"
                    else -> label
                }
                UniversalIosButton(
                    defaultBg = ColorSciGray,
                    pressedBg = ColorSciGrayPressed,
                    defaultFg = ColorWhite,
                    modifier = Modifier.weight(1f).height(48.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.onScientific(displayLabel)
                    }
                ) { fg ->
                    Text(text = displayLabel, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Normal)
                }
            }

            // 0 spanning 2 weight: left section matches column 7 width exactly, centering '0' directly under '1'
            UniversalIosButton(
                defaultBg = ColorDarkGray,
                pressedBg = ColorDarkGrayPressed,
                defaultFg = ColorWhite,
                modifier = Modifier.weight(2f).height(48.dp),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onDigit("0")
                }
            ) { fg ->
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "0", color = fg, fontSize = 30.sp, fontWeight = FontWeight.Normal)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Decimal button: Unified with landscape row matching height 48.dp
            UniversalIosButton(
                defaultBg = ColorDarkGray,
                pressedBg = ColorDarkGrayPressed,
                defaultFg = ColorWhite,
                modifier = Modifier.weight(1f).height(48.dp),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onDecimal()
                }
            ) { fg ->
                Text(text = ".", color = fg, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-2).dp))
            }

            // Equals button: Unified with landscape row matching height 48.dp
            UniversalIosButton(
                defaultBg = ColorOrange,
                pressedBg = ColorOrangePressed,
                defaultFg = ColorWhite,
                modifier = Modifier.weight(1f).height(48.dp),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onEquals()
                }
            ) { fg ->
                Text(text = "=", color = fg, fontSize = 34.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-1).dp))
            }
        }
    }
}

/**
 * Format portrait display to match iOS 9-digit max display with commas.
 */
fun formatPortraitDisplay(value: String): String {
    if (value == "Error") return "Error"
    if (value.contains("e") || value.contains("E")) return value

    val parts = value.split(".")
    val integerPart = parts[0]
    val isNegative = integerPart.startsWith("-")
    val cleanInt = if (isNegative) integerPart.substring(1) else integerPart

    val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
    val signedInt = if (isNegative) "-$formattedInt" else formattedInt

    return if (parts.size > 1) {
        "$signedInt.${parts[1]}"
    } else if (value.endsWith(".")) {
        "$signedInt."
    } else {
        signedInt
    }
}

/**
 * Format landscape display to allow up to 16 digits of precision with commas.
 */
fun formatLandscapeDisplay(value: String): String {
    if (value == "Error") return "Error"
    if (value.contains("e") || value.contains("E")) return value

    val parts = value.split(".")
    val integerPart = parts[0]
    val isNegative = integerPart.startsWith("-")
    val cleanInt = if (isNegative) integerPart.substring(1) else integerPart

    val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
    val signedInt = if (isNegative) "-$formattedInt" else formattedInt

    return if (parts.size > 1) {
        "$signedInt.${parts[1]}"
    } else if (value.endsWith(".")) {
        "$signedInt."
    } else {
        signedInt
    }
}
