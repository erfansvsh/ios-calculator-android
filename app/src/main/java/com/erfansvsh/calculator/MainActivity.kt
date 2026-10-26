package com.erfansvsh.calculator

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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

// iOS Colors
val ColorBlack = Color(0xFF000000)
val ColorDarkGray = Color(0xFF333333)
val ColorLightGray = Color(0xFFA5A5A5)
val ColorOrange = Color(0xFFFF9F0A)
val ColorWhite = Color(0xFFFFFFFF)
val ColorSciGray = Color(0xFF212121)

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
    val formattedDisplay = vm.getFormattedDisplay()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
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
                formattedDisplay.length <= 6 -> 84.sp
                formattedDisplay.length == 7 -> 72.sp
                formattedDisplay.length == 8 -> 60.sp
                formattedDisplay.length == 9 -> 50.sp
                else -> 40.sp
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
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircleCalcButton(label = clearLabel, bg = ColorLightGray, fg = ColorBlack, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onClear()
            }
            CircleCalcButton(label = "±", bg = ColorLightGray, fg = ColorBlack, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onToggleSign()
            }
            CircleCalcButton(label = "%", bg = ColorLightGray, fg = ColorBlack, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onPercent()
            }
            OperatorCalcButton(label = "÷", isActive = activeOperator == "÷", modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onOperator("÷")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircleCalcButton(label = "7", bg = ColorDarkGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDigit("7")
            }
            CircleCalcButton(label = "8", bg = ColorDarkGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDigit("8")
            }
            CircleCalcButton(label = "9", bg = ColorDarkGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDigit("9")
            }
            OperatorCalcButton(label = "×", isActive = activeOperator == "×", modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onOperator("×")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Row 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircleCalcButton(label = "4", bg = ColorDarkGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDigit("4")
            }
            CircleCalcButton(label = "5", bg = ColorDarkGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDigit("5")
            }
            CircleCalcButton(label = "6", bg = ColorDarkGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDigit("6")
            }
            OperatorCalcButton(label = "−", isActive = activeOperator == "−", modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onOperator("−")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Row 4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircleCalcButton(label = "1", bg = ColorDarkGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDigit("1")
            }
            CircleCalcButton(label = "2", bg = ColorDarkGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDigit("2")
            }
            CircleCalcButton(label = "3", bg = ColorDarkGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDigit("3")
            }
            OperatorCalcButton(label = "+", isActive = activeOperator == "+", modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onOperator("+")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Row 5: 0 (double pill width), ., =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PillZeroButton(modifier = Modifier.weight(2f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDigit("0")
            }
            CircleCalcButton(label = ".", bg = ColorDarkGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDecimal()
            }
            CircleCalcButton(label = "=", bg = ColorOrange, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onEquals()
            }
        }
    }
}

@Composable
fun CircleCalcButton(
    label: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = CircleShape,
        color = bg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = fg,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun OperatorCalcButton(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isActive) ColorWhite else ColorOrange
    val fg = if (isActive) ColorOrange else ColorWhite

    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = CircleShape,
        color = bg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = fg,
                fontSize = 38.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun PillZeroButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = CircleShape,
        color = ColorDarkGray
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "0",
                color = ColorWhite,
                fontSize = 32.sp,
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
    val formattedDisplay = vm.getFormattedDisplay()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
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
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 4.dp)
                )
            }
            Text(
                text = formattedDisplay,
                color = ColorWhite,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 10 column grid rows for scientific landscape
        val sciCols = listOf(
            listOf("(", ")", "mc", "m+", "m-", "mr", clearLabel, "±", "%", "÷"),
            listOf(if (is2nd) "2ⁿᵈ" else "2ⁿᵈ", "x²", "x³", "xʸ", "eˣ", "10ˣ", "7", "8", "9", "×"),
            listOf("1/x", "²√x", "³√x", "ʸ√x", "ln", "log₁₀", "4", "5", "6", "−"),
            listOf("x!", "sin", "cos", "tan", "e", "EE", "1", "2", "3", "+"),
            listOf(if (isRad) "Rad" else "Deg", "sinh", "cosh", "tanh", "π", "Rand", "0", ".", "=")
        )

        // Row 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sciCols[0].forEachIndexed { i, label ->
                val bg = when (i) {
                    6, 7, 8 -> ColorLightGray
                    9 -> if (activeOperator == "÷") ColorWhite else ColorOrange
                    else -> ColorSciGray
                }
                val fg = when (i) {
                    6, 7, 8 -> ColorBlack
                    9 -> if (activeOperator == "÷") ColorOrange else ColorWhite
                    else -> ColorWhite
                }
                SmallCalcButton(label = label, bg = bg, fg = fg, modifier = Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when (label) {
                        "AC", "C" -> vm.onClear()
                        "±" -> vm.onToggleSign()
                        "%" -> vm.onPercent()
                        "÷" -> vm.onOperator("÷")
                        else -> vm.onScientific(label)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Row 2
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sciCols[1].forEachIndexed { i, label ->
                val bg = when (i) {
                    6, 7, 8 -> ColorDarkGray
                    9 -> if (activeOperator == "×") ColorWhite else ColorOrange
                    else -> ColorSciGray
                }
                val fg = when (i) {
                    9 -> if (activeOperator == "×") ColorOrange else ColorWhite
                    else -> ColorWhite
                }
                SmallCalcButton(label = label, bg = bg, fg = fg, modifier = Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when (label) {
                        "7", "8", "9" -> vm.onDigit(label)
                        "×" -> vm.onOperator("×")
                        "2ⁿᵈ" -> vm.toggle2nd()
                        else -> vm.onScientific(label)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Row 3
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sciCols[2].forEachIndexed { i, label ->
                val bg = when (i) {
                    6, 7, 8 -> ColorDarkGray
                    9 -> if (activeOperator == "−") ColorWhite else ColorOrange
                    else -> ColorSciGray
                }
                val fg = when (i) {
                    9 -> if (activeOperator == "−") ColorOrange else ColorWhite
                    else -> ColorWhite
                }
                SmallCalcButton(label = label, bg = bg, fg = fg, modifier = Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when (label) {
                        "4", "5", "6" -> vm.onDigit(label)
                        "−" -> vm.onOperator("−")
                        else -> vm.onScientific(label)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Row 4
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sciCols[3].forEachIndexed { i, label ->
                val bg = when (i) {
                    6, 7, 8 -> ColorDarkGray
                    9 -> if (activeOperator == "+") ColorWhite else ColorOrange
                    else -> ColorSciGray
                }
                val fg = when (i) {
                    9 -> if (activeOperator == "+") ColorOrange else ColorWhite
                    else -> ColorWhite
                }
                SmallCalcButton(label = label, bg = bg, fg = fg, modifier = Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when (label) {
                        "1", "2", "3" -> vm.onDigit(label)
                        "+" -> vm.onOperator("+")
                        else -> vm.onScientific(label)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Row 5: 6 sci buttons, 0 (double), ., =
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallCalcButton(label = if (isRad) "Rad" else "Deg", bg = ColorSciGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.toggleRad()
            }
            listOf("sinh", "cosh", "tanh", "π", "Rand").forEach { label ->
                SmallCalcButton(label = label, bg = ColorSciGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onScientific(label)
                }
            }
            // 0 spanning 2 weight
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onDigit("0")
                },
                modifier = Modifier
                    .weight(2f)
                    .height(44.dp),
                shape = CircleShape,
                color = ColorDarkGray
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "0", color = ColorWhite, fontSize = 20.sp)
                }
            }
            SmallCalcButton(label = ".", bg = ColorDarkGray, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onDecimal()
            }
            SmallCalcButton(label = "=", bg = ColorOrange, fg = ColorWhite, modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.onEquals()
            }
        }
    }
}

@Composable
fun SmallCalcButton(
    label: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = CircleShape,
        color = bg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = fg,
                fontSize = if (label.length > 3) 12.sp else 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
