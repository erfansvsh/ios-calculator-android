package com.erfansvsh.ioscalculator

import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.*

// =========================================================================
// BUTTON ANIMATION TIMING CONFIGURATION (TUNABLE HERE)
// =========================================================================
// Attack: touch-down response in milliseconds (default: 80ms)
const val BUTTON_PRESS_DOWN_MS = 80

// Release: soft, breath-like lingering color decay in milliseconds (default: 480ms)
// Increase (e.g. 520, 560) for a slower fade, or decrease (e.g. 400, 360) for faster.
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

/**
 * Loads Apple San Francisco (SF Pro) font from assets,
 * falling back gracefully to FontFamily.SansSerif if the asset is missing.
 */
@Composable
fun rememberSfFont(assetName: String): FontFamily {
    val context = LocalContext.current
    return remember(assetName) {
        try {
            val typeface = Typeface.createFromAsset(context.assets, "fonts/$assetName")
            FontFamily(typeface)
        } catch (e: Exception) {
            FontFamily.SansSerif
        }
    }
}

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
 * - Immediate touch-down (BUTTON_PRESS_DOWN_MS = 80ms)
 * - Soft, slower lingering breath-like fade out (BUTTON_RELEASE_FADE_MS = 480ms) on release
 * - Exactly unified across ALL buttons: numbers, scientific, AC, +/-, %, and operators
 * - No Android ripple (indication = null) so colors never flash or conflict
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

    // Always prioritize isPressed so EVERY button gives the identical tactile touch feedback
    val targetBg = when {
        isPressed -> pressedBg
        isActive -> activeBg
        else -> defaultBg
    }

    val targetFg = when {
        isActive -> activeFg
        else -> defaultFg
    }

    // Authentic Apple breath-like color transition:
    // Identical timing and easing across all button types
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

    Box(
        modifier = modifier
            .clip(shape)
            .background(animatedBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disables Android ripple completely so all buttons share the identical iOS breath animation
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(animatedFg)
    }
}

/**
 * Pixel-perfect Apple iOS Calculator '+/-' glyph vector
 * Extracted directly from real iPhone image (public/assets/aistudio/plus-slash-minus_1f60594b.png).
 * Unified across both portrait and landscape modes.
 */
@Composable
fun IosPlusMinusGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height) / 2f
        val strokeSlash = s * 0.074f
        val strokeBars = s * 0.060f

        // Central diagonal slash from bottom-left to top-right
        drawLine(
            color = color,
            start = Offset(center.x - 0.235f * s, center.y + 0.295f * s),
            end = Offset(center.x + 0.155f * s, center.y - 0.255f * s),
            strokeWidth = strokeSlash,
            cap = StrokeCap.Square
        )

        // Bold '+' sign (top-left)
        val plusCx = center.x - 0.255f * s
        val plusCy = center.y - 0.128f * s
        val plusHalf = (s * 0.215f) / 2f
        drawLine(
            color = color,
            start = Offset(plusCx - plusHalf, plusCy),
            end = Offset(plusCx + plusHalf, plusCy),
            strokeWidth = strokeBars,
            cap = StrokeCap.Square
        )
        drawLine(
            color = color,
            start = Offset(plusCx, plusCy - plusHalf),
            end = Offset(plusCx, plusCy + plusHalf),
            strokeWidth = strokeBars,
            cap = StrokeCap.Square
        )

        // Bold '−' sign (bottom-right)
        val minusCx = center.x + 0.155f * s
        val minusCy = center.y + 0.161f * s
        val minusHalf = (s * 0.201f) / 2f
        drawLine(
            color = color,
            start = Offset(minusCx - minusHalf, minusCy),
            end = Offset(minusCx + minusHalf, minusCy),
            strokeWidth = strokeBars,
            cap = StrokeCap.Square
        )
    }
}

/**
 * Pixel-perfect Apple iOS Calculator '%' glyph vector
 * Extracted directly from real iPhone image (public/assets/aistudio/percent_1d923e38.png).
 * Unified across both portrait and landscape modes.
 */
@Composable
fun IosPercentGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height) / 2f
        val strokeSlash = s * 0.074f
        val strokeRing = s * 0.060f

        // Central diagonal slash from bottom-left to top-right
        drawLine(
            color = color,
            start = Offset(center.x - 0.235f * s, center.y + 0.289f * s),
            end = Offset(center.x + 0.181f * s, center.y - 0.268f * s),
            strokeWidth = strokeSlash,
            cap = StrokeCap.Square
        )

        // Top ring (oval: rx = 0.121s, ry = 0.141s)
        val topCx = center.x - 0.208f * s
        val topCy = center.y - 0.134f * s
        val topRx = s * 0.121f
        val topRy = s * 0.141f
        drawOval(
            color = color,
            topLeft = Offset(topCx - topRx, topCy - topRy),
            size = Size(topRx * 2f, topRy * 2f),
            style = Stroke(width = strokeRing)
        )

        // Bottom ring (oval: rx = 0.114s, ry = 0.134s)
        val botCx = center.x + 0.188f * s
        val botCy = center.y + 0.148f * s
        val botRx = s * 0.114f
        val botRy = s * 0.134f
        drawOval(
            color = color,
            topLeft = Offset(botCx - botRx, botCy - botRy),
            size = Size(botRx * 2f, botRy * 2f),
            style = Stroke(width = strokeRing)
        )
    }
}

/**
 * Decimal point dot exactly positioned like iPhone Calculator
 * (downward by +14.4% of button diameter, matching zero-dot-equal_447dbf0a.png).
 */
@Composable
fun IosDecimalDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height) / 2f
        val dotRadius = s * 0.060f
        val dotCenterY = center.y + s * 0.288f
        drawCircle(
            color = color,
            radius = dotRadius,
            center = Offset(center.x, dotCenterY)
        )
    }
}

@Composable
fun PortraitCalculator(vm: CalculatorViewModel) {
    val displayValue by vm.displayValue.collectAsState()
    val activeOperator by vm.activeOperator.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Load authentic Apple SF Pro fonts with fallback
    val sfDisplayThin = rememberSfFont("sf_pro_display_thin.otf")
    val sfDisplayLight = rememberSfFont("sf_pro_display_light.otf")
    val sfTextRegular = rememberSfFont("sf_pro_text_regular.otf")

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
            val (fontSize, displayFont) = when {
                formattedDisplay.length <= 5 -> 100.sp to sfDisplayThin
                formattedDisplay.length == 6 -> 90.sp to sfDisplayThin
                formattedDisplay.length == 7 -> 80.sp to sfDisplayLight
                formattedDisplay.length == 8 -> 70.sp to sfDisplayLight
                formattedDisplay.length == 9 -> 58.sp to sfDisplayLight
                else -> 48.sp to sfDisplayLight
            }

            Text(
                text = formattedDisplay,
                color = ColorWhite,
                fontSize = fontSize,
                fontFamily = displayFont,
                fontWeight = FontWeight.W200,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val spacing = 12.dp
            val buttonSize = (maxWidth - spacing * 3) / 4
            val zeroWidth = buttonSize * 2 + spacing

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // Row 1: AC/C, +/-, %, ÷ (Authentic Apple vector glyphs)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    UniversalIosButton(
                        defaultBg = ColorLightGray,
                        pressedBg = ColorLightGrayPressed,
                        defaultFg = ColorBlack,
                        modifier = Modifier.size(buttonSize),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.onClear()
                        }
                    ) { fg ->
                        Text(text = clearLabel, color = fg, fontFamily = sfTextRegular, fontSize = 38.sp, fontWeight = FontWeight.Normal)
                    }

                    // Vector +/- glyph exactly matching public/assets/aistudio/plus-slash-minus_1f60594b.png
                    UniversalIosButton(
                        defaultBg = ColorLightGray,
                        pressedBg = ColorLightGrayPressed,
                        defaultFg = ColorBlack,
                        modifier = Modifier.size(buttonSize),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.onToggleSign()
                        }
                    ) { fg ->
                        IosPlusMinusGlyph(color = fg, modifier = Modifier.fillMaxSize())
                    }

                    // Vector % glyph exactly matching public/assets/aistudio/percent_1d923e38.png
                    UniversalIosButton(
                        defaultBg = ColorLightGray,
                        pressedBg = ColorLightGrayPressed,
                        defaultFg = ColorBlack,
                        modifier = Modifier.size(buttonSize),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.onPercent()
                        }
                    ) { fg ->
                        IosPercentGlyph(color = fg, modifier = Modifier.fillMaxSize())
                    }

                    UniversalIosButton(
                        defaultBg = ColorOrange,
                        pressedBg = ColorOrangePressed,
                        defaultFg = ColorWhite,
                        activeBg = ColorWhite,
                        activeFg = ColorOrange,
                        isActive = activeOperator == "÷",
                        modifier = Modifier.size(buttonSize),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.onOperator("÷")
                        }
                    ) { fg ->
                        Text(text = "÷", color = fg, fontFamily = sfTextRegular, fontSize = 48.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-3).dp))
                    }
                }

                // Row 2: 7, 8, 9, ×
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    listOf("7", "8", "9").forEach { digit ->
                        UniversalIosButton(
                            defaultBg = ColorDarkGray,
                            pressedBg = ColorDarkGrayPressed,
                            defaultFg = ColorWhite,
                            modifier = Modifier.size(buttonSize),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.onDigit(digit)
                            }
                        ) { fg ->
                            Text(text = digit, color = fg, fontFamily = sfTextRegular, fontSize = 46.sp, fontWeight = FontWeight.Normal)
                        }
                    }

                    UniversalIosButton(
                        defaultBg = ColorOrange,
                        pressedBg = ColorOrangePressed,
                        defaultFg = ColorWhite,
                        activeBg = ColorWhite,
                        activeFg = ColorOrange,
                        isActive = activeOperator == "×",
                        modifier = Modifier.size(buttonSize),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.onOperator("×")
                        }
                    ) { fg ->
                        Text(text = "×", color = fg, fontFamily = sfTextRegular, fontSize = 44.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-2).dp))
                    }
                }

                // Row 3: 4, 5, 6, −
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    listOf("4", "5", "6").forEach { digit ->
                        UniversalIosButton(
                            defaultBg = ColorDarkGray,
                            pressedBg = ColorDarkGrayPressed,
                            defaultFg = ColorWhite,
                            modifier = Modifier.size(buttonSize),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.onDigit(digit)
                            }
                        ) { fg ->
                            Text(text = digit, color = fg, fontFamily = sfTextRegular, fontSize = 46.sp, fontWeight = FontWeight.Normal)
                        }
                    }

                    UniversalIosButton(
                        defaultBg = ColorOrange,
                        pressedBg = ColorOrangePressed,
                        defaultFg = ColorWhite,
                        activeBg = ColorWhite,
                        activeFg = ColorOrange,
                        isActive = activeOperator == "−",
                        modifier = Modifier.size(buttonSize),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.onOperator("−")
                        }
                    ) { fg ->
                        Text(text = "−", color = fg, fontFamily = sfTextRegular, fontSize = 48.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-2).dp))
                    }
                }

                // Row 4: 1, 2, 3, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    listOf("1", "2", "3").forEach { digit ->
                        UniversalIosButton(
                            defaultBg = ColorDarkGray,
                            pressedBg = ColorDarkGrayPressed,
                            defaultFg = ColorWhite,
                            modifier = Modifier.size(buttonSize),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.onDigit(digit)
                            }
                        ) { fg ->
                            Text(text = digit, color = fg, fontFamily = sfTextRegular, fontSize = 46.sp, fontWeight = FontWeight.Normal)
                        }
                    }

                    UniversalIosButton(
                        defaultBg = ColorOrange,
                        pressedBg = ColorOrangePressed,
                        defaultFg = ColorWhite,
                        activeBg = ColorWhite,
                        activeFg = ColorOrange,
                        isActive = activeOperator == "+",
                        modifier = Modifier.size(buttonSize),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.onOperator("+")
                        }
                    ) { fg ->
                        Text(text = "+", color = fg, fontFamily = sfTextRegular, fontSize = 46.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-2).dp))
                    }
                }

                // Row 5: 0 (spanning 2 columns, exactly aligned vertically with "." and "="), ., =
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    // '0' button: height is mathematically identical to buttonSize, bottom edge perfectly aligned with '.' and '='
                    UniversalIosButton(
                        defaultBg = ColorDarkGray,
                        pressedBg = ColorDarkGrayPressed,
                        defaultFg = ColorWhite,
                        shape = CircleShape,
                        modifier = Modifier
                            .width(zeroWidth)
                            .height(buttonSize),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.onDigit("0")
                        }
                    ) { fg ->
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Slot matching Column 1 width exactly: digit '0' is 100% horizontally aligned with '1'
                            Box(
                                modifier = Modifier
                                    .size(buttonSize),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "0", color = fg, fontFamily = sfTextRegular, fontSize = 46.sp, fontWeight = FontWeight.Normal)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    // Decimal '.' button: dot placed at authentic Apple downward baseline (+14.4% below center)
                    UniversalIosButton(
                        defaultBg = ColorDarkGray,
                        pressedBg = ColorDarkGrayPressed,
                        defaultFg = ColorWhite,
                        modifier = Modifier.size(buttonSize),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.onDecimal()
                        }
                    ) { fg ->
                        IosDecimalDot(color = fg, modifier = Modifier.fillMaxSize())
                    }

                    UniversalIosButton(
                        defaultBg = ColorOrange,
                        pressedBg = ColorOrangePressed,
                        defaultFg = ColorWhite,
                        modifier = Modifier.size(buttonSize),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.onEquals()
                        }
                    ) { fg ->
                        Text(text = "=", color = fg, fontFamily = sfTextRegular, fontSize = 46.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-2).dp))
                    }
                }
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

    // Load authentic Apple SF Pro fonts with fallback
    val sfDisplayThin = rememberSfFont("sf_pro_display_thin.otf")
    val sfTextRegular = rememberSfFont("sf_pro_text_regular.otf")

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
                    fontFamily = sfTextRegular,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 4.dp)
                )
            }
            Text(
                text = formattedDisplay,
                color = ColorWhite,
                fontFamily = sfDisplayThin,
                fontSize = 64.sp,
                fontWeight = FontWeight.W200,
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
                    when (label) {
                        "±" -> IosPlusMinusGlyph(color = fg, modifier = Modifier.fillMaxSize())
                        "%" -> IosPercentGlyph(color = fg, modifier = Modifier.fillMaxSize())
                        else -> {
                            Text(
                                text = label,
                                color = fg,
                                fontFamily = sfTextRegular,
                                fontSize = if (isOp) 34.sp else if (isTopLight) 22.sp else 19.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
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
                        fontFamily = sfTextRegular,
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
                        fontFamily = sfTextRegular,
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
                        fontFamily = sfTextRegular,
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
                Text(text = radButtonLabel, color = fg, fontFamily = sfTextRegular, fontSize = 19.sp, fontWeight = FontWeight.Normal)
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
                    Text(text = displayLabel, color = fg, fontFamily = sfTextRegular, fontSize = 18.sp, fontWeight = FontWeight.Normal)
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
                        Text(text = "0", color = fg, fontFamily = sfTextRegular, fontSize = 30.sp, fontWeight = FontWeight.Normal)
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
                IosDecimalDot(color = fg, modifier = Modifier.fillMaxSize())
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
                Text(text = "=", color = fg, fontFamily = sfTextRegular, fontSize = 34.sp, fontWeight = FontWeight.Normal, modifier = Modifier.offset(y = (-1).dp))
            }
        }
    }
}

/**
 * Format portrait display to strictly enforce iOS 9-digit max display with HALF_UP rounding and commas.
 * For example: π × 7 evaluates to 21.9911486 (9 significant digits) in portrait mode.
 */
fun formatPortraitDisplay(value: String): String {
    if (value == "Error") return "Error"
    if (value.contains("e") || value.contains("E")) return value

    val num = value.toDoubleOrNull() ?: return value
    if (num == 0.0) return if (value.startsWith("-")) "-0" else "0"

    // While user is actively typing a decimal or trailing zeros e.g. "3." or "3.0"
    if (value.endsWith(".") || (value.contains(".") && value.endsWith("0"))) {
        val parts = value.split(".")
        val isNeg = parts[0].startsWith("-")
        val cleanInt = if (isNeg) parts[0].substring(1) else parts[0]
        val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
        val signedInt = if (isNeg) "-$formattedInt" else formattedInt
        return "$signedInt.${parts[1]}"
    }

    return try {
        val absNum = abs(num)
        if (absNum >= 1e9 || (absNum < 1e-8 && absNum > 0.0)) {
            // Numbers exceeding 9 digits switch to scientific notation
            val expStr = String.format(Locale.US, "%.5e", num)
            expStr.replace("+", "").replace("e0", "e").replace("e-0", "e-")
        } else {
            // Exactly 9 significant digits with HALF_UP rounding (iOS portrait precision)
            val mc = MathContext(9, RoundingMode.HALF_UP)
            val bd = BigDecimal(value, mc)
            val plain = bd.stripTrailingZeros().toPlainString()
            val parts = plain.split(".")
            val isNeg = parts[0].startsWith("-")
            val cleanInt = if (isNeg) parts[0].substring(1) else parts[0]
            val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
            val signedInt = if (isNeg) "-$formattedInt" else formattedInt
            if (parts.size > 1) "$signedInt.${parts[1]}" else signedInt
        }
    } catch (e: Exception) {
        val parts = value.split(".")
        val isNeg = parts[0].startsWith("-")
        val cleanInt = if (isNeg) parts[0].substring(1) else parts[0]
        val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
        val signedInt = if (isNeg) "-$formattedInt" else formattedInt
        if (parts.size > 1) "$signedInt.${parts[1]}" else signedInt
    }
}

/**
 * Format landscape display to allow up to 16 digits of precision with commas.
 * For example: π × 7 evaluates to 21.99114857512855 (16 significant digits) in landscape mode.
 */
fun formatLandscapeDisplay(value: String): String {
    if (value == "Error") return "Error"
    if (value.contains("e") || value.contains("E")) return value

    val num = value.toDoubleOrNull() ?: return value
    if (num == 0.0) return if (value.startsWith("-")) "-0" else "0"

    if (value.endsWith(".") || (value.contains(".") && value.endsWith("0"))) {
        val parts = value.split(".")
        val isNeg = parts[0].startsWith("-")
        val cleanInt = if (isNeg) parts[0].substring(1) else parts[0]
        val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
        val signedInt = if (isNeg) "-$formattedInt" else formattedInt
        return "$signedInt.${parts[1]}"
    }

    return try {
        val absNum = abs(num)
        if (absNum >= 1e16 || (absNum < 1e-8 && absNum > 0.0)) {
            val expStr = String.format(Locale.US, "%.10e", num)
            expStr.replace("+", "").replace("e0", "e").replace("e-0", "e-")
        } else {
            val mc = MathContext(16, RoundingMode.HALF_UP)
            val bd = BigDecimal(value, mc)
            val plain = bd.stripTrailingZeros().toPlainString()
            val parts = plain.split(".")
            val isNeg = parts[0].startsWith("-")
            val cleanInt = if (isNeg) parts[0].substring(1) else parts[0]
            val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
            val signedInt = if (isNeg) "-$formattedInt" else formattedInt
            if (parts.size > 1) "$signedInt.${parts[1]}" else signedInt
        }
    } catch (e: Exception) {
        val parts = value.split(".")
        val isNeg = parts[0].startsWith("-")
        val cleanInt = if (isNeg) parts[0].substring(1) else parts[0]
        val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
        val signedInt = if (isNeg) "-$formattedInt" else formattedInt
        if (parts.size > 1) "$signedInt.${parts[1]}" else signedInt
    }
}
