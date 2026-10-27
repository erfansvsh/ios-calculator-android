# 📱 Clean Calculator — Native iOS Calculator for Android

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5%2B-4285F4?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVI%20%2F%20Unidirectional-00C853?style=flat-square)](#-system-architecture--unidirectional-data-flow)
[![Precision](https://img.shields.io/badge/Math%20Engine-IEEE--754%20Sanitized-FF6D00?style=flat-square)](#-ieee-754-floating-point-precision-sanitizer)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

An architectural and engineering clone of Apple’s native **iOS Calculator**, written completely from scratch in 100% modern **Kotlin** using **Jetpack Compose**. 

This repository delivers 1:1 tactile ergonomics, including **120ms color animation curves**, **swipe-to-delete display gestures**, **haptic feedback**, dynamic text downscaling, complete portrait-to-landscape layout expansion into a **50-key scientific matrix**, and an **IEEE-754 precision math sanitizer**.

---

## 📸 Visual Showcase & Ergonomics

| Portrait Mode (Standard 19-Key Layout) | Landscape Mode (50-Key Scientific Matrix) |
| :---: | :---: |
| Pad-aligned circular key layout, double-width stadium zero key, and dynamic backspace gesture support. | Expands seamlessly into trigonometry, powers, logarithms, hyperbolic functions, memory registers, and RAD/DEG modes. |

---

## 💡 Non-Coder Conceptual Introduction

> **How does an Android app actually work?**

Think of an application as an automated, high-efficiency kitchen:

1. **The Dining Room (The UI / View):** This is the visual interface with buttons and display screens. It receives touches from the user (customer) and displays food (data). The UI *never cooks*—it only conveys touch events back to the kitchen and presents prepared meals.
2. **The Chief Chef (The ViewModel):** The ViewModel is a dedicated, stateful engine that stores order tickets, decides mathematical rules, and calculates recipes. If the building rotates (screen rotation from portrait to landscape), the kitchen stays completely untouched so your current order state is never lost.
3. **The Conveyor Belt (StateFlow):** Data flows continuously from the ViewModel to the screen on an invisible, real-time conveyor belt called `StateFlow`. When an operator changes state in the kitchen, the screen automatically updates instantly.

---

## 🏗️ System Architecture & Unidirectional Data Flow

The app follows strict **Unidirectional Data Flow (UDF)** / **MVI pattern**. The UI components never mutate internal state or compute math directly; touch events dispatch intents to the `CalculatorViewModel`, which sanitizes arithmetic operations and emits state back down to the UI layer.

```
┌────────────────────────────────────────────────────────┐
│                   MainActivity (UI)                   │
│                                                        │
│   ┌────────────────────┐      ┌────────────────────┐   │
│   │ UniversalIosButton │      │   Swipe Display    │   │
│   └─────────┬──────────┘      └─────────▲──────────┘   │
└─────────────┼──────────────────────────┼───────────────┘
              │ Direct User Actions      │ StateFlow Updates
              ▼                          │
┌────────────────────────────────────────┴───────────────┐
│                 CalculatorViewModel                    │
│                                                        │
│   ┌────────────────────┐      ┌────────────────────┐   │
│   │  MutableStateFlow  │      │ Memory & Operator  │   │
│   │   Backing Streams  │      │     Registers      │   │
│   └─────────┬──────────┘      └─────────▲──────────┘   │
└─────────────┼──────────────────────────┼───────────────┘
              │ Raw Calculation Inputs   │ Sanitized Output
              ▼                          │
┌────────────────────────────────────────┴───────────────┘
│              Pure Math Engine & Sanitizer              │
│                                                        │
│   • IEEE-754 Residual Snapper   • Degree Normalization │
│   • Rounding to 12 Sig Digits   • Error Handling Guard │
└────────────────────────────────────────────────────────┘
```

---

## 🧠 ViewModel State Management & Core Architecture

The application's core logic is encapsulated within `CalculatorViewModel.kt`. It provides read-only `StateFlow` streams exposed to the UI layer while keeping mutable internal registers private.

### Key State Properties

```kotlin
class CalculatorViewModel : ViewModel() {
    // Encapsulated State Flow Streams
    private val _displayValue = MutableStateFlow("0")
    val displayValue: StateFlow<String> = _displayValue.asStateFlow()

    private val _activeOperatorHighlight = MutableStateFlow<String?>(null)
    val activeOperator: StateFlow<String?> = _activeOperatorHighlight.asStateFlow()

    private val _isRadMode = MutableStateFlow(false)
    val isRadMode: StateFlow<Boolean> = _isRadMode.asStateFlow()

    private val _is2nd = MutableStateFlow(false)
    val is2nd: StateFlow<Boolean> = _is2nd.asStateFlow()

    // Internal Calculation Registers
    private var pendingOperator: String? = null
    private var previousValue: Double? = null
    private var waitingForOperand: Boolean = false
    private var lastOperation: Pair<String, Double>? = null
    private var memory: Double = 0.0
}
```

---

## 🧮 IEEE-754 Floating-Point Precision Sanitizer

Binary hardware calculations inherent to floating-point representation introduce arithmetic anomalies such as:
$$\text{0.1} + \text{0.2} = \text{0.30000000000000004}$$
$$\cos(90^\circ) = 6.123233995736766 \times 10^{-17}$$

To eliminate these floating-point artifacts, every single calculation passes through a multi-tier **`sanitize()` pipeline**:

```kotlin
/**
 * Sanitizes floating point anomalies.
 * Snaps microscopic residual values to 0.0, snaps near-integers,
 * and truncates to 12 significant digits.
 */
fun sanitize(num: Double): Double {
    if (num.isNaN() || num.isInfinite()) return num

    // Step 1: Snap microscopic residuals to zero (e.g., cos(90°))
    if (abs(num) < 1e-12) return 0.0

    // Step 2: Snap near-integers (e.g., 4.999999999999 -> 5.0)
    val rounded = round(num)
    if (abs(num - rounded) < 1e-11) return rounded

    // Step 3: Round to 12 significant digits using BigDecimal
    return try {
        BigDecimal(num.toString(), MathContext(12, RoundingMode.HALF_UP)).toDouble()
    } catch (e: Exception) {
        num
    }
}
```

### Exact Degree Trigonometric Tables

To prevent binary floating-point drift in trigonometric functions, inputs in Degree mode are projected into the $[0, 360^\circ)$ range and evaluated via strict constant lookup tables:

```kotlin
private fun calcSin(angle: Double, inRad: Boolean): Double {
    if (inRad) {
        val rem = abs(angle % (2 * PI))
        if (rem < 1e-12 || abs(rem - PI) < 1e-12) return 0.0
        return sanitize(sin(angle))
    }
    var norm = angle % 360.0
    if (norm < 0) norm += 360.0
    return when {
        abs(norm) < 1e-7 || abs(norm - 180.0) < 1e-7 -> 0.0
        abs(norm - 90.0) < 1e-7 -> 1.0
        abs(norm - 270.0) < 1e-7 -> -1.0
        abs(norm - 30.0) < 1e-7 || abs(norm - 150.0) < 1e-7 -> 0.5
        else -> sanitize(sin(Math.toRadians(norm)))
    }
}
```

---

## 🎨 UI & Dynamic Compose Keypad Mechanics

### 120ms Color Animation & Touch States (`UniversalIosButton`)

The native iOS calculator inverts color states dynamically upon interaction. `UniversalIosButton` mirrors this by combining `MutableInteractionSource` press tracking with a `tween(120)` transition curve.

```kotlin
@Composable
fun UniversalIosButton(
    modifier: Modifier = Modifier,
    defaultBg: Color,
    pressedBg: Color,
    defaultFg: Color,
    isActive: Boolean = false,
    activeBg: Color = Color.White,
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
    val targetFg = if (isActive) activeFg else defaultFg
    val animatedBg by animateColorAsState(targetBg, tween(120), label = "bg")

    Box(
        modifier = modifier
            .clip(shape)
            .background(animatedBg)
            .clickable(
                interactionSource = interactionSource, 
                indication = null, 
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(targetFg)
    }
}
```

### Dynamic Display Scaling & Gestures

- **Swipe-to-Delete:** Swiping left or right on the main number output triggers a backspace operation alongside a subtle tactile haptic vibration (`HapticFeedbackType.LongPress`).
- **Dynamic Downscaling:** Typography shrinks dynamically as string length increases to prevent clipping:

```kotlin
val fontSize = when {
    displayStr.length <= 5 -> 100.sp
    displayStr.length == 6 -> 90.sp
    displayStr.length == 7 -> 82.sp
    displayStr.length == 8 -> 70.sp
    else -> 48.sp
}
```

---

## 🧪 50-Key Scientific Landscape Matrix

When the device rotates into landscape orientation, the layout expands into a **10-column, 50-key scientific matrix**:

| Function Category | Supported Operations |
| :--- | :--- |
| **Trigonometry** | $\sin, \cos, \tan, \sin^{-1}, \cos^{-1}, \tan^{-1}$ |
| **Hyperbolic** | $\sinh, \cosh, \tanh, \sinh^{-1}, \cosh^{-1}, \tanh^{-1}$ |
| **Logarithms & Powers** | $e^x, 10^x, y^x, 2^x, \ln, \log_{10}, \log_y, x^2, x^3, \sqrt{x}, \sqrt[3]{x}, \sqrt[y]{x}$ |
| **Constants & Factory Math**| $\pi, e, x!, 1/x, |x|, \text{Rand}$ |
| **Memory Registers** | $\text{mc}, \text{m+}, \text{m-}, \text{mr}$ |
| **Toggles** | $\text{2nd}$ function modifier key, $\text{Rad} / \text{Deg}$ mode switch |

---

## 🛠️ Build & Installation Guide

### Requirements
- **Android Studio:** Jellyfish | 2023.3.1 or newer
- **Kotlin:** 1.9.0+
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

### Steps

1. **Clone Repository:**
   ```bash
   git clone https://github.com/erfansvsh/ios-calculator-android.git
   cd ios-calculator-android
   ```

2. **Open in Android Studio:**
   Launch Android Studio and choose **Open**, selecting the cloned folder.

3. **Build & Run:**
   Connect a physical device or emulator and execute:
   ```bash
   ./gradlew installDebug
   ```

---

## 📂 Project Structure

```
com.erfansvsh.ioscalculator
 ├── MainActivity.kt               # Jetpack Compose UI layout, animations & gestures
 └── CalculatorViewModel.kt        # StateFlow state management & math engine
```

---

## 📜 License

This project is open source and available under the [MIT License](LICENSE).