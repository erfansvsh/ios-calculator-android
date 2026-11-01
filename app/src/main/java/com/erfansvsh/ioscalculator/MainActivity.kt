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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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

// The 'mr' button while activated: #808080 with black text
val ColorMrActiveBg = Color(0xFF808080)
val ColorMrActiveFg = ColorBlack
val ColorMrActivePressedBg = Color(0xFFA6A6A6)

// Other active scientific toggle buttons (e.g., 2nd)
val ColorSciActiveBg = Color(0xFF808080)
val ColorSciActiveFg = Color(0xFFFFFFFF)

/**
 * Loads authentic Apple San Francisco (SF Pro) font from assets,
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
    pressedActiveBg: Color? = null,
    shape: Shape = CircleShape,
    onClick: () -> Unit,
    content: @Composable (fg: Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Always prioritize isPressed so EVERY button gives the identical tactile touch feedback
    val targetBg = when {
        isPressed && isActive && pressedActiveBg != null -> pressedActiveBg
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
 * Custom pixel-perfect Apple iOS +/- glyph rendered on Canvas.
 * Matches the slash angle, stroke weight, and placement of '+' and '−'
 * relative to the division slash from the iOS Calculator screenshot.
 *
 * The slash is drawn as a parallelogram whose top and bottom ends are cut
 * horizontally (like the slash in a percent sign '%') instead of perpendicular
 * to the stroke, so both ends have clean horizontal edges.
 */
@Composable
fun IosPlusMinusGlyph(
    color: Color,
    glyphHeight: androidx.compose.ui.unit.Dp = 29.0.dp,
    modifier: Modifier = Modifier
) {
    val glyphWidth = glyphHeight * 1.120f
    Canvas(
        modifier = modifier.size(width = glyphWidth, height = glyphHeight)
    ) {
        val H = size.height
        val center = Offset(size.width / 2f, size.height / 2f)

        // 1. Diagonal slash with horizontal-cut ends (like '%'):
        // Perpendicular stroke thickness:
        val strokeSlash = H * 0.088f
        // Centerline endpoints of the slash (bottom-left to top-right):
        val slashStart = Offset(center.x - 0.380f * H, center.y + 0.500f * H)
        val slashEnd   = Offset(center.x + 0.280f * H, center.y - 0.500f * H)

        // Horizontal half-width so the perpendicular thickness equals strokeSlash:
        val slashDx = slashEnd.x - slashStart.x
        val slashDy = slashEnd.y - slashStart.y
        val slashLen = kotlin.math.sqrt(slashDx * slashDx + slashDy * slashDy)
        val slashHalfW = strokeSlash * slashLen / (2f * kotlin.math.abs(slashDy))

        // Parallelogram with horizontal top/bottom edges and slanted sides parallel to the slash:
        val slashPath = Path().apply {
            moveTo(slashStart.x - slashHalfW, slashStart.y)  // bottom-left
            lineTo(slashStart.x + slashHalfW, slashStart.y)  // bottom-right
            lineTo(slashEnd.x   + slashHalfW, slashEnd.y)    // top-right
            lineTo(slashEnd.x   - slashHalfW, slashEnd.y)    // top-left
            close()
        }
        drawPath(path = slashPath, color = color)

        // 2. Stroke thickness of the '+' and '−' bars:
        val strokeBars = H * 0.092f
        val armLength = H * 0.309f

        // 3. Top-left '+' sign:
        // Position of the '+' center relative to canvas center:
        val plusCenter = Offset(center.x - 0.419f * H, center.y - 0.257f * H)
        // Horizontal bar of '+':
        drawLine(
            color = color,
            start = Offset(plusCenter.x - armLength / 2f, plusCenter.y),
            end = Offset(plusCenter.x + armLength / 2f, plusCenter.y),
            strokeWidth = strokeBars,
            cap = StrokeCap.Square
        )
        // Vertical bar of '+':
        drawLine(
            color = color,
            start = Offset(plusCenter.x, plusCenter.y - armLength / 2f),
            end = Offset(plusCenter.x, plusCenter.y + armLength / 2f),
            strokeWidth = strokeBars,
            cap = StrokeCap.Square
        )

        // 4. Bottom-right '−' (minus) sign:
        // Position of the '−' center relative to canvas center:
        val minusCenter = Offset(center.x + 0.313f * H, center.y + 0.260f * H)
        // Horizontal bar of '−':
        drawLine(
            color = color,
            start = Offset(minusCenter.x - armLength / 2f, minusCenter.y),
            end = Offset(minusCenter.x + armLength / 2f, minusCenter.y),
            strokeWidth = strokeBars,
            cap = StrokeCap.Square
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
        // Display with swipe to delete and dynamic iOS font auto-scaling
        BoxWithConstraints(
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
            val density = LocalDensity.current
            val textMeasurer = rememberTextMeasurer()

            // Available horizontal width for the text, leaving 4.dp padding on each side
            val availableWidthPx = with(density) { (maxWidth - 8.dp).toPx() }

            // Apple iOS 4-Stage Portrait Font Scaling:
            // Stage 1 - 1 to 6 digits (e.g. "0" to "888,888"): Largest size (94sp, Thin W200), filling screen horizontally on 888,888
            // Stage 2 - 7 digits (e.g. "8,888,888"): Scales down slightly (82sp, Light W300) to fit
            // Stage 3 - 8 digits (e.g. "88,888,888"): Scales down slightly more (72sp, Light W300) to fit
            // Stage 4 - 9 digits (e.g. "888,888,888" or scientific notation like "8.888889e9" / "8e9"): Scales down to fit 9 digits (62sp, Light W300)
            val (optimalFontSize, optimalFont, optimalWeight) = remember(formattedDisplay, availableWidthPx) {
                // Find maximum fitting size for a given reference text within availableWidthPx
                fun findFittingSp(text: String, startSp: Float, minSp: Float, font: FontFamily, weight: FontWeight): Float {
                    var currentSp = startSp
                    while (currentSp > minSp) {
                        val layout = textMeasurer.measure(
                            text = text,
                            style = TextStyle(fontFamily = font, fontWeight = weight, fontSize = currentSp.sp),
                            maxLines = 1
                        )
                        if (layout.size.width <= availableWidthPx) break
                        currentSp -= 1f
                    }
                    return currentSp
                }

                val stage1Sp = findFittingSp("888,888", 94f, 70f, sfDisplayThin, FontWeight.W200)
                val stage2Sp = findFittingSp("8,888,888", 82f, 58f, sfDisplayLight, FontWeight.W300)
                val stage3Sp = findFittingSp("88,888,888", 72f, 48f, sfDisplayLight, FontWeight.W300)
                val stage4Sp = findFittingSp("888,888,888", 62f, 38f, sfDisplayLight, FontWeight.W300)

                // Count digits (0-9)
                val isExp = formattedDisplay.contains("e") || formattedDisplay.contains("E")
                val digitCount = if (isExp) 9 else formattedDisplay.count { it.isDigit() }

                val (baseSp, font, weight) = when {
                    digitCount <= 6 -> Triple(stage1Sp, sfDisplayThin, FontWeight.W200)
                    digitCount == 7 -> Triple(stage2Sp, sfDisplayLight, FontWeight.W300)
                    digitCount == 8 -> Triple(stage3Sp, sfDisplayLight, FontWeight.W300)
                    else -> Triple(stage4Sp, sfDisplayLight, FontWeight.W300)
                }

                // Guarantee that the actual formatted string (including sign, e-exponent, etc.) does not clip
                var finalSp = baseSp
                var layout = textMeasurer.measure(
                    text = formattedDisplay,
                    style = TextStyle(fontFamily = font, fontWeight = weight, fontSize = finalSp.sp),
                    maxLines = 1
                )
                while (layout.size.width > availableWidthPx && finalSp > 24f) {
                    finalSp -= 1f
                    layout = textMeasurer.measure(
                        text = formattedDisplay,
                        style = TextStyle(fontFamily = font, fontWeight = weight, fontSize = finalSp.sp),
                        maxLines = 1
                    )
                }

                Triple(finalSp.sp, font, weight)
            }

            Text(
                text = formattedDisplay,
                color = ColorWhite,
                fontSize = optimalFontSize,
                fontFamily = optimalFont,
                fontWeight = optimalWeight,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
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
                    // AC/C button: increased to 42.sp to match exact visual height with % and +/-
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
                        Text(text = clearLabel, color = fg, fontFamily = sfTextRegular, fontSize = 42.sp, fontWeight = FontWeight.Normal)
                    }

                    // Vector +/- glyph created directly from SF Pro %: matches exact visual height
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
                        IosPlusMinusGlyph(color = fg, glyphHeight = 29.dp)
                    }

                    // % button: rendered with genuine SF Pro font, matching exact height of AC and +/-
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
                        Text(text = "%", color = fg, fontFamily = sfTextRegular, fontSize = 42.sp, fontWeight = FontWeight.Normal)
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
                                vm.onDigit(digit, isLandscape = false)
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
                                vm.onDigit(digit, isLandscape = false)
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
                                vm.onDigit(digit, isLandscape = false)
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
                            vm.onDigit("0", isLandscape = false)
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

                // Real iOS: when 'mr' is activated, it displays darker gray #808080 with black text
                val activeBgColor = if (isMr) ColorMrActiveBg else if (isOp) ColorWhite else ColorSciActiveBg
                val activeFgColor = if (isMr) ColorMrActiveFg else if (isOp) ColorOrange else ColorSciActiveFg
                val pressedActiveBgColor = if (isMr) ColorMrActivePressedBg else null

                UniversalIosButton(
                    defaultBg = defaultBg,
                    pressedBg = pressedBg,
                    defaultFg = defaultFg,
                    activeBg = activeBgColor,
                    activeFg = activeFgColor,
                    pressedActiveBg = pressedActiveBgColor,
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
                        "±" -> IosPlusMinusGlyph(color = fg, glyphHeight = 16.dp)
                        "%" -> {
                            Text(
                                text = "%",
                                color = fg,
                                fontFamily = sfTextRegular,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        "AC", "C" -> {
                            Text(
                                text = label,
                                color = fg,
                                fontFamily = sfTextRegular,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        else -> {
                            Text(
                                text = label,
                                color = fg,
                                fontFamily = sfTextRegular,
                                fontSize = if (isOp) 34.sp else 19.sp,
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
                            isDigit -> vm.onDigit(label, isLandscape = true)
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
                            isDigit -> vm.onDigit(label, isLandscape = true)
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
                            isDigit -> vm.onDigit(label, isLandscape = true)
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
                    vm.onDigit("0", isLandscape = true)
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
 * Numbers with > 9 digits or magnitude >= 1e9 format as authentic iOS exponential notation (e.g. 8e9 or 8.888889e9).
 */
fun formatPortraitDisplay(value: String): String {
    if (value == "Error") return "Error"
    if (value.contains("e") || value.contains("E")) {
        val expStr = value.replace("+", "").replace("e0", "e").replace("e-0", "e-").replace("E0", "e").replace("E-0", "e-").replace("E", "e")
        val eIndex = expStr.indexOf('e')
        return if (eIndex != -1) {
            val mantissa = expStr.substring(0, eIndex)
            val exponent = expStr.substring(eIndex)
            val cleanMantissa = if (mantissa.contains('.')) mantissa.trimEnd('0').trimEnd('.') else mantissa
            cleanMantissa + exponent
        } else expStr
    }

    val num = value.toDoubleOrNull() ?: return value
    if (num == 0.0) return if (value.startsWith("-")) "-0" else "0"

    val absNum = abs(num)
    val digitsOnly = value.replace(Regex("[^0-9]"), "")

    // Switch to scientific notation if magnitude >= 1e9 or very small, OR if digit count exceeds 9
    if (absNum >= 1e9 || (absNum < 1e-8 && absNum > 0.0) || digitsOnly.length > 9) {
        val expStr = String.format(Locale.US, "%.6e", num)
            .replace("+", "")
            .replace("e0", "e")
            .replace("e-0", "e-")
        val eIndex = expStr.indexOf('e')
        return if (eIndex != -1) {
            val mantissa = expStr.substring(0, eIndex)
            val exponent = expStr.substring(eIndex)
            val cleanMantissa = if (mantissa.contains('.')) mantissa.trimEnd('0').trimEnd('.') else mantissa
            cleanMantissa + exponent
        } else {
            expStr
        }
    }

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
        val mc = MathContext(9, RoundingMode.HALF_UP)
        val bd = BigDecimal(value, mc)
        val plain = bd.stripTrailingZeros().toPlainString()
        val parts = plain.split(".")
        val isNeg = parts[0].startsWith("-")
        val cleanInt = if (isNeg) parts[0].substring(1) else parts[0]
        val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
        val signedInt = if (isNeg) "-$formattedInt" else formattedInt
        if (parts.size > 1) "$signedInt.${parts[1]}" else signedInt
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
    if (value.contains("e") || value.contains("E")) {
        val expStr = value.replace("+", "").replace("e0", "e").replace("e-0", "e-").replace("E0", "e").replace("E-0", "e-").replace("E", "e")
        val eIndex = expStr.indexOf('e')
        return if (eIndex != -1) {
            val mantissa = expStr.substring(0, eIndex)
            val exponent = expStr.substring(eIndex)
            val cleanMantissa = if (mantissa.contains('.')) mantissa.trimEnd('0').trimEnd('.') else mantissa
            cleanMantissa + exponent
        } else expStr
    }

    val num = value.toDoubleOrNull() ?: return value
    if (num == 0.0) return if (value.startsWith("-")) "-0" else "0"

    val absNum = abs(num)
    val digitsOnly = value.replace(Regex("[^0-9]"), "")

    if (absNum >= 1e16 || (absNum < 1e-8 && absNum > 0.0) || digitsOnly.length > 16) {
        val expStr = String.format(Locale.US, "%.10e", num)
            .replace("+", "")
            .replace("e0", "e")
            .replace("e-0", "e-")
        val eIndex = expStr.indexOf('e')
        return if (eIndex != -1) {
            val mantissa = expStr.substring(0, eIndex)
            val exponent = expStr.substring(eIndex)
            val cleanMantissa = if (mantissa.contains('.')) mantissa.trimEnd('0').trimEnd('.') else mantissa
            cleanMantissa + exponent
        } else {
            expStr
        }
    }

    if (value.endsWith(".") || (value.contains(".") && value.endsWith("0"))) {
        val parts = value.split(".")
        val isNeg = parts[0].startsWith("-")
        val cleanInt = if (isNeg) parts[0].substring(1) else parts[0]
        val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
        val signedInt = if (isNeg) "-$formattedInt" else formattedInt
        return "$signedInt.${parts[1]}"
    }

    return try {
        val mc = MathContext(16, RoundingMode.HALF_UP)
        val bd = BigDecimal(value, mc)
        val plain = bd.stripTrailingZeros().toPlainString()
        val parts = plain.split(".")
        val isNeg = parts[0].startsWith("-")
        val cleanInt = if (isNeg) parts[0].substring(1) else parts[0]
        val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
        val signedInt = if (isNeg) "-$formattedInt" else formattedInt
        if (parts.size > 1) "$signedInt.${parts[1]}" else signedInt
    } catch (e: Exception) {
        val parts = value.split(".")
        val isNeg = parts[0].startsWith("-")
        val cleanInt = if (isNeg) parts[0].substring(1) else parts[0]
        val formattedInt = cleanInt.reversed().chunked(3).joinToString(",").reversed()
        val signedInt = if (isNeg) "-$formattedInt" else formattedInt
        if (parts.size > 1) "$signedInt.${parts[1]}" else signedInt
    }
}
