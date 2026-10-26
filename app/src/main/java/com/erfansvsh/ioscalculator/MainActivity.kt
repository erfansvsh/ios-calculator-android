package com.erfansvsh.ioscalculator

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

// iOS Colors
val ColorBlack = Color(0xFF000000)
val ColorDarkGray = Color(0xFF333333)
val ColorDarkGrayPressed = Color(0xFF737373)
val ColorLightGray = Color(0xFFA5A5A5)
val ColorLightGrayPressed = Color(0xFFD9D9D9)
val ColorOrange = Color(0xFFFF9F0A)
val ColorOrangePressed = Color(0xFFFCC781)
val ColorWhite = Color(0xFFFFFFFF)
val ColorSciGray = Color(0xFF212121)
val ColorSciGrayPressed = Color(0xFF424242)

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

@Composable
fun PortraitCalculator(vm: CalculatorViewModel) {
    val displayValue by vm.displayValue.collectAsState()
    val activeOperator by vm.activeOperator.collectAsState()
    val haptic = LocalHapticFeedback.current

    val clearLabel = if (displayValue != "0") "C" else "AC"
    val formattedDisplay = vm.getFormattedDisplay(isLandscape = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Display with swipe to delete
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (kotlin.math.abs(dragAmount) > 20) {
                            vm.onBackspace()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                },
            contentAlignment = Alignment.BottomEnd
        ) {
            val fontSize = when {
                formattedDisplay.length <= 6 -> 94.sp
                formattedDisplay.length == 7 -> 80.sp
                formattedDisplay.length == 8 -> 68.sp
                formattedDisplay.length == 9 -> 56.sp
                else -> 44.sp
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

        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IosCalcButton(
                label = { Text(text = clearLabel, fontSize = 32.sp, fontWeight = FontWeight.Normal) },
                defaultBg = ColorLightGray,
                pressedBg = ColorLightGrayPressed,
                defaultFg = ColorBlack,
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onClear()
            }

            IosCalcButton(
                label = { PlusMinusIcon(color = ColorBlack) },
                defaultBg = ColorLightGray,
                pressedBg = ColorLightGrayPressed,
                defaultFg = ColorBlack,
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onToggleSign()
            }

            IosCalcButton(
                label = { Text(text = "%", fontSize = 32.sp, fontWeight = FontWeight.Normal) },
                defaultBg = ColorLightGray,
                pressedBg = ColorLightGrayPressed,
                defaultFg = ColorBlack,
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onPercent()
            }

            IosOperatorButton(
                label = "÷",
                isActive = activeOperator == "÷",
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onOperator("÷")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("7", "8", "9").forEach { digit ->
                IosCalcButton(
                    label = { Text(text = digit, fontSize = 40.sp, fontWeight = FontWeight.Normal) },
                    defaultBg = ColorDarkGray,
                    pressedBg = ColorDarkGrayPressed,
                    defaultFg = ColorWhite,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onDigit(digit)
                }
            }
            IosOperatorButton(
                label = "×",
                isActive = activeOperator == "×",
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onOperator("×")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("4", "5", "6").forEach { digit ->
                IosCalcButton(
                    label = { Text(text = digit, fontSize = 40.sp, fontWeight = FontWeight.Normal) },
                    defaultBg = ColorDarkGray,
                    pressedBg = ColorDarkGrayPressed,
                    defaultFg = ColorWhite,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onDigit(digit)
                }
            }
            IosOperatorButton(
                label = "−",
                isActive = activeOperator == "−",
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onOperator("−")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("1", "2", "3").forEach { digit ->
                IosCalcButton(
                    label = { Text(text = digit, fontSize = 40.sp, fontWeight = FontWeight.Normal) },
                    defaultBg = ColorDarkGray,
                    pressedBg = ColorDarkGrayPressed,
                    defaultFg = ColorWhite,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onDigit(digit)
                }
            }
            IosOperatorButton(
                label = "+",
                isActive = activeOperator == "+",
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onOperator("+")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 5: 0 (double pill), ., =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pill zero
            IosPillZeroButton(modifier = Modifier.weight(2f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDigit("0")
            }

            IosCalcButton(
                label = { Text(text = ".", fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-4).dp)) },
                defaultBg = ColorDarkGray,
                pressedBg = ColorDarkGrayPressed,
                defaultFg = ColorWhite,
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDecimal()
            }

            IosCalcButton(
                label = { Text(text = "=", fontSize = 42.sp, fontWeight = FontWeight.Normal) },
                defaultBg = ColorOrange,
                pressedBg = ColorOrangePressed,
                defaultFg = ColorWhite,
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onEquals()
            }
        }
    }
}

@Composable
fun PlusMinusIcon(
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp
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
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = (-4).dp, x = 1.dp)
            )
            Text(
                text = "/",
                color = color,
                fontSize = fontSize,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(horizontal = 0.5.dp)
            )
            Text(
                text = "−",
                color = color,
                fontSize = fontSize * 0.7f,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = 4.dp, x = (-1).dp)
            )
        }
    }
}

@Composable
fun IosCalcButton(
    label: @Composable () -> Unit,
    defaultBg: Color,
    pressedBg: Color,
    defaultFg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedBg by animateColorAsState(
        targetValue = if (isPressed) pressedBg else defaultBg,
        animationSpec = tween(durationMillis = 120),
        label = "buttonBg"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.aspectRatio(1f),
        shape = CircleShape,
        color = animatedBg
    ) {
        Box(contentAlignment = Alignment.Center) {
            label()
        }
    }
}

@Composable
fun IosOperatorButton(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetBg = when {
        isActive -> ColorWhite
        isPressed -> ColorOrangePressed
        else -> ColorOrange
    }

    val targetFg = when {
        isActive -> ColorOrange
        else -> ColorWhite
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 120),
        label = "opBg"
    )

    val animatedFg by animateColorAsState(
        targetValue = targetFg,
        animationSpec = tween(durationMillis = 120),
        label = "opFg"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.aspectRatio(1f),
        shape = CircleShape,
        color = animatedBg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = animatedFg,
                fontSize = 44.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.offset(y = (-1).dp)
            )
        }
    }
}

@Composable
fun IosPillZeroButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedBg by animateColorAsState(
        targetValue = if (isPressed) ColorDarkGrayPressed else ColorDarkGray,
        animationSpec = tween(durationMillis = 120),
        label = "zeroBg"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.aspectRatio(2.1f),
        shape = CircleShape,
        color = animatedBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 30.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "0",
                color = ColorWhite,
                fontSize = 40.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun LandscapeCalculator(vm: CalculatorViewModel) {
    val displayValue by vm.displayValue.collectAsState()
    val activeOperator by vm.activeOperator.collectAsState()
    val isRad by vm.isRad.collectAsState()
    val is2nd by vm.is2nd.collectAsState()
    val haptic = LocalHapticFeedback.current

    val clearLabel = if (displayValue != "0") "C" else "AC"
    val formattedDisplay = vm.getFormattedDisplay(isLandscape = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomEnd
        ) {
            if (isRad) {
                Text(
                    text = "Rad",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 4.dp)
                )
            }
            Text(
                text = formattedDisplay,
                color = ColorWhite,
                fontSize = 54.sp,
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
        val sciCols = listOf(
            listOf("(", ")", "mc", "m+", "m-", "mr", clearLabel, "±", "%", "÷"),
            listOf(if (is2nd) "2ⁿᵈ" else "2ⁿᵈ", "x²", "x³", "xʸ", if (is2nd) "yˣ" else "eˣ", if (is2nd) "2ˣ" else "10ˣ", "7", "8", "9", "×"),
            listOf("1/x", "²√x", "³√x", "ʸ√x", if (is2nd) "log_y" else "ln", if (is2nd) "log₂" else "log₁₀", "4", "5", "6", "−"),
            listOf("x!", if (is2nd) "sin⁻¹" else "sin", if (is2nd) "cos⁻¹" else "cos", if (is2nd) "tan⁻¹" else "tan", "e", "EE", "1", "2", "3", "+"),
            listOf(if (isRad) "Rad" else "Deg", if (is2nd) "sinh⁻¹" else "sinh", if (is2nd) "cosh⁻¹" else "cosh", if (is2nd) "tanh⁻¹" else "tanh", "π", "Rand", "0", ".", "=")
        )

        // Row 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sciCols[0].forEachIndexed { i, label ->
                val isOp = i == 9
                val isTopLight = i in 6..8
                val defaultBg = if (isOp) ColorOrange else if (isTopLight) ColorLightGray else ColorSciGray
                val pressedBg = if (isOp) ColorOrangePressed else if (isTopLight) ColorLightGrayPressed else ColorSciGrayPressed
                val defaultFg = if (isTopLight) ColorBlack else ColorWhite

                if (label == "±") {
                    SmallIosButton(
                        label = { PlusMinusIcon(color = ColorBlack, fontSize = 20.sp) },
                        defaultBg = defaultBg,
                        pressedBg = pressedBg,
                        modifier = Modifier.weight(1f)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.onToggleSign()
                    }
                } else {
                    SmallIosButton(
                        label = {
                            Text(
                                text = label,
                                color = if (isOp && activeOperator == "÷") ColorOrange else defaultFg,
                                fontSize = if (isOp) 26.sp else 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        },
                        defaultBg = if (isOp && activeOperator == "÷") ColorWhite else defaultBg,
                        pressedBg = pressedBg,
                        modifier = Modifier.weight(1f)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (label) {
                            "AC", "C" -> vm.onClear()
                            "%" -> vm.onPercent()
                            "÷" -> vm.onOperator("÷")
                            else -> vm.onScientific(label)
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
                val isActiveOp = (isOp && activeOperator == "×") || (isPowerOp && activeOperator == label)

                val defaultBg = if (isOp) ColorOrange else if (isDigit) ColorDarkGray else ColorSciGray
                val pressedBg = if (isOp) ColorOrangePressed else if (isDigit) ColorDarkGrayPressed else ColorSciGrayPressed

                SmallIosButton(
                    label = {
                        Text(
                            text = label,
                            color = if (isActiveOp) ColorOrange else ColorWhite,
                            fontSize = if (isOp || isDigit) 24.sp else 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    defaultBg = if (isActiveOp) ColorWhite else defaultBg,
                    pressedBg = pressedBg,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when {
                        isDigit -> vm.onDigit(label)
                        isOp -> vm.onOperator("×")
                        label == "2ⁿᵈ" -> vm.toggle2nd()
                        isPowerOp -> vm.onOperator(label)
                        else -> vm.onScientific(label)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))

        // Row 3
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sciCols[2].forEachIndexed { i, label ->
                val isDigit = i in 6..8
                val isOp = i == 9
                val isRootOp = label == "ʸ√x" || label == "log_y"
                val isActiveOp = (isOp && activeOperator == "−") || (isRootOp && activeOperator == label)

                val defaultBg = if (isOp) ColorOrange else if (isDigit) ColorDarkGray else ColorSciGray
                val pressedBg = if (isOp) ColorOrangePressed else if (isDigit) ColorDarkGrayPressed else ColorSciGrayPressed

                SmallIosButton(
                    label = {
                        Text(
                            text = label,
                            color = if (isActiveOp) ColorOrange else ColorWhite,
                            fontSize = if (isOp || isDigit) 24.sp else 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    defaultBg = if (isActiveOp) ColorWhite else defaultBg,
                    pressedBg = pressedBg,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when {
                        isDigit -> vm.onDigit(label)
                        isOp -> vm.onOperator("−")
                        isRootOp -> vm.onOperator(label)
                        else -> vm.onScientific(label)
                    }
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

                SmallIosButton(
                    label = {
                        Text(
                            text = label,
                            color = if (isActiveOp) ColorOrange else ColorWhite,
                            fontSize = if (isOp || isDigit) 24.sp else 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    defaultBg = if (isActiveOp) ColorWhite else defaultBg,
                    pressedBg = pressedBg,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when {
                        isDigit -> vm.onDigit(label)
                        isOp -> vm.onOperator("+")
                        isEEOp -> vm.onOperator("EE")
                        else -> vm.onScientific(label)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))

        // Row 5: 6 sci buttons, 0 (double width), ., =
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallIosButton(
                label = { Text(text = if (isRad) "Rad" else "Deg", color = ColorWhite, fontSize = 15.sp) },
                defaultBg = ColorSciGray,
                pressedBg = ColorSciGrayPressed,
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.toggleRad()
            }

            listOf("sinh", "cosh", "tanh", "π", "Rand").forEach { label ->
                val displayLabel = when {
                    is2nd && label == "sinh" -> "sinh⁻¹"
                    is2nd && label == "cosh" -> "cosh⁻¹"
                    is2nd && label == "tanh" -> "tanh⁻¹"
                    else -> label
                }
                SmallIosButton(
                    label = { Text(text = displayLabel, color = ColorWhite, fontSize = 15.sp) },
                    defaultBg = ColorSciGray,
                    pressedBg = ColorSciGrayPressed,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onScientific(displayLabel)
                }
            }

            // 0 spanning 2 weight
            val interactionSource = remember { MutableInteractionSource() }
            val isZeroPressed by interactionSource.collectIsPressedAsState()
            val animatedZeroBg by animateColorAsState(
                targetValue = if (isZeroPressed) ColorDarkGrayPressed else ColorDarkGray,
                animationSpec = tween(120),
                label = "landZeroBg"
            )

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onDigit("0")
                },
                interactionSource = interactionSource,
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp),
                shape = CircleShape,
                color = animatedZeroBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "0", color = ColorWhite, fontSize = 24.sp)
                }
            }

            SmallIosButton(
                label = { Text(text = ".", color = ColorWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                defaultBg = ColorDarkGray,
                pressedBg = ColorDarkGrayPressed,
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDecimal()
            }

            SmallIosButton(
                label = { Text(text = "=", color = ColorWhite, fontSize = 26.sp) },
                defaultBg = ColorOrange,
                pressedBg = ColorOrangePressed,
                modifier = Modifier.weight(1f)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onEquals()
            }
        }
    }
}

@Composable
fun SmallIosButton(
    label: @Composable () -> Unit,
    defaultBg: Color,
    pressedBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedBg by animateColorAsState(
        targetValue = if (isPressed) pressedBg else defaultBg,
        animationSpec = tween(durationMillis = 120),
        label = "smallBg"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        color = animatedBg
    ) {
        Box(contentAlignment = Alignment.Center) {
            label()
        }
    }
}
