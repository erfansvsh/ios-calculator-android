# 📱 iOS Calculator — Native iOS Calculator for Android

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.05.00-4285F4?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVI%20%2F%20Unidirectional-00C853?style=flat-square)](#-system-architecture--unidirectional-data-flow)
[![Typography](https://img.shields.io/badge/Typography-Apple%20SF%20Pro-000000?style=flat-square&logo=apple&logoColor=white)](#-apple-sf-pro-typography--vector-glyphs)
[![Precision](https://img.shields.io/badge/Math%20Engine-IEEE--754%20Sanitized-FF6D00?style=flat-square)](#-ieee-754-floating-point-precision-sanitizer)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

An architectural and engineering reproduction of Apple’s native **iOS Calculator**, written completely from scratch in 100% modern **Kotlin** using **Jetpack Compose**. 

This repository delivers 1:1 tactile ergonomics, including **Apple SF Pro Display typography**, custom-rendered **vector glyph geometry**, **two-phase 120ms button animation curves**, **swipe-to-delete display gestures**, **haptic feedback**, **density-independent horizontal screen-filling downscaling**, a **two-level algebraic precedence engine with nested parentheses**, complete portrait-to-landscape expansion into a **50-key scientific matrix**, and an **IEEE-754 precision math sanitizer**.

---

## 📸 Visual Showcase & Ergonomics

| Portrait Mode (Standard 19-Key Layout) | Landscape Mode (50-Key Scientific Matrix) |
| :---: | :---: |
| Circular key geometry, left-anchored double-width stadium zero key, optically tailored top functional row (`AC`, `±`, `%`), and dynamic backspace gesture support. | Expands seamlessly into a 10-column matrix featuring trigonometry, powers, logarithms, hyperbolic functions, memory registers, nested parentheses, and RAD/DEG modes with elevated display alignment. |

---

## 💡 Non-Coder Conceptual Introduction

> **How does an Android app actually work?**

Think of an application as an automated, high-efficiency kitchen:

1. **The Dining Room (The UI / View):** This is the visual interface with buttons and display screens. It receives touches from the user (customer) and displays food (data). The UI *never cooks*—it only conveys touch events back to the kitchen and presents prepared meals.
2. **The Chief Chef (The ViewModel):** The ViewModel is a dedicated, stateful engine that stores order tickets, decides mathematical rules, and calculates recipes. If the building rotates (screen rotation from portrait to landscape), the kitchen stays completely untouched so your current calculation and memory state is never lost.
3. **The Conveyor Belt (StateFlow):** Data flows continuously from the ViewModel to the screen on an invisible, real-time conveyor belt called `StateFlow`. When an operator changes state in the kitchen, the screen automatically updates instantly.

---

## 🏗️ System Architecture & Unidirectional Data Flow

The app follows strict **Unidirectional Data Flow (UDF)** / **MVI pattern**. UI components never mutate internal state or compute math directly; touch events dispatch intents to `CalculatorViewModel`, which handles input staging, algebraic precedence, error boundaries, and numerical sanitization before emitting immutable state down to the UI layer.

```
┌────────────────────────────────────────────────────────┐
│                   MainActivity (UI)                   │
│                                                        │
│   ┌────────────────────┐      ┌────────────────────┐   │
│   │ UniversalIosButton │      │   Swipe Display    │   │
│   │ & Vector Glyphs    │      │ (Auto-Downscaling) │   │
│   └─────────┬──────────┘      └─────────▲──────────┘   │
└─────────────┼──────────────────────────┼───────────────┘
              │ User Touch Events        │ StateFlow Updates
              ▼                          │
┌────────────────────────────────────────┴───────────────┐
│                 CalculatorViewModel                    │
│                                                        │
│   ┌────────────────────┐      ┌────────────────────┐   │
│   │ Two-Level Operator │      │  Nested Expression │   │
│   │   Stack (+ vs *)   │      │  Parenthesis Stack │   │
│   └─────────┬──────────┘      └─────────▲──────────┘   │
└─────────────┼──────────────────────────┼───────────────┘
              │ Raw Input Stream         │ Sanitized State
              ▼                          │
┌────────────────────────────────────────┴───────────────┘
│              Pure Math Engine & Sanitizer              │
│                                                        │
│   • IEEE-754 Residual Snapper   • Degree Normalization │
│   • Rounding to 12 Sig Digits   • Repeated Equals ( = )│
│   • Dynamic Locale Formatting   • Exponential Fallback │
└────────────────────────────────────────────────────────┘
```

---

## 🧠 ViewModel State Management & Core Architecture

The application's core logic is encapsulated within `CalculatorViewModel.kt`. It provides read-only `StateFlow` streams exposed to the UI layer while keeping mutable internal registers and operator stacks private.

### Key State Properties & Operator Stacks

```kotlin
class CalculatorViewModel : ViewModel() {
    enum class InputState {
        TYPING,                 // User is typing digits or decimal point
        OPERATOR_JUST_PRESSED,  // An operator (+, -, *, /) was just clicked
        OPERAND_READY           // An operand was produced by constant (pi, e), function, or equals
    }

    // Encapsulated State Flow Streams
    private val _displayValue = MutableStateFlow("0")
    val displayValue: StateFlow<String> = _displayValue.asStateFlow()

    private val _activeOperatorHighlight = MutableStateFlow<String?>(null)
    val activeOperator: StateFlow<String?> = _activeOperatorHighlight.asStateFlow()

    private val _isRadMode = MutableStateFlow(false)
    val isRadMode: StateFlow<Boolean> = _isRadMode.asStateFlow()

    private val _is2nd = MutableStateFlow(false)
    val is2nd: StateFlow<Boolean> = _is2nd.asStateFlow()

    // Two-level operator stack for algebraic operator precedence (+/- vs */powers)
    private var pendingLowOp: Pair<Double, String>? = null   // +, -
    private var pendingHighOp: Pair<Double, String>? = null  // *, /, powers, roots, log, EE

    // Parenthesis stack for nested expressions
    private val parenStack = ArrayDeque<Pair<Pair<Double, String>?, Pair<Double, String>?>>()

    // Repeated equals operation: (operator, rightOperand)
    private var lastEqualsOp: Pair<String, Double>? = null
    private var memory: Double = 0.0
}
```

---

## 🧮 Math Engine, Formatting & Precision Sanitizer

### Algebraic Operator Precedence & Repeated Equals

The calculation engine mirrors Apple's two-level algebraic operator evaluation hierarchy:
- High-precedence operations (`×`, `÷`, roots, powers, logs) bind tighter than low-precedence operations (`+`, `−`).
- Pressing `=` consecutively repeats the previous binary operation with the cached right-hand operand.
- Full nested parentheses evaluate sub-expressions independently, unwinding onto the operator stack upon closing.

### Floating-Point Sanitizer

Binary hardware calculations inherent to floating-point representation introduce arithmetic anomalies such as:
$$\text{0.1} + \text{0.2} = \text{0.30000000000000004}$$
$$\cos(90^\circ) = 6.123233995736766 \times 10^{-17}$$

To eliminate these artifacts without truncating transcendental constants ($\pi, e$), calculations pass through a dedicated sanitization pass:

```kotlin
private fun sanitize(num: Double): Double {
    if (num.isNaN() || num.isInfinite()) return num
    if (abs(num) < 1e-13) return 0.0
    val roundInt = round(num)
    if (abs(num - roundInt) < 1e-11) {
        return roundInt
    }
    return num
}
```

### Exact Degree Trigonometric Normalization

In Degree mode, angular inputs are normalized into $[0, 360^\circ)$ and resolved via cardinal lookup boundaries:

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

### Adaptive Display Formatting

Output strings are formatted adaptively based on device orientation:
- **Portrait Mode:** Formats up to 9 significant digits with thousands comma separators; switches to lowercase scientific notation (`8e9`) when values equal or exceed $10^9$ or are smaller than $10^{-8}$.
- **Landscape Mode:** Formats up to 16 significant digits with thousands comma separators; switches to scientific notation for values equal or exceeding $10^{16}$ or smaller than $10^{-8}$.

---

## 🎨 Typography, Keypad Mechanics & Visual Architecture

### Apple SF Pro Typography & Vector Glyphs

The user interface uses native **Apple SF Pro Display** fonts (`sfDisplayThin`, `sfDisplayLight`, `sfDisplayRegular`) for exact typographic matching:
- **Dynamic Plus/Minus Glyph (`IosPlusMinusGlyph`):** The $\pm$ symbol is drawn directly on a Compose `Canvas` with native slash angles and line proportions to eliminate platform-dependent Unicode rendering discrepancies.
- **Optically Tailored Top Row:** In portrait mode, the text and glyph sizes on the top functional buttons (`AC`, `±`, `%`) are scaled down by 15% to maintain exact visual balance against numeric digits.

### Dual-Phase Animation Curves (`UniversalIosButton`)

Button interactions utilize asymmetric dual-phase animation curves matching native iOS response profiles:

```kotlin
val animatedBg by animateColorAsState(
    targetValue = targetBg,
    animationSpec = if (isPressed) {
        tween(durationMillis = 50, easing = LinearEasing)
    } else {
        tween(durationMillis = 350, easing = FastOutSlowInEasing)
    },
    label = "iosBtnBg"
)
```

### Screen-Filling Dynamic Downscaling & Gesture Navigation

- **Horizontal Screen-Filling Downscaling:** The display font dynamically scales down based on character count and available horizontal width, filling the screen on large numbers while strictly preventing boundary clipping across all device pixel densities.
- **Landscape Display Elevation:** In landscape mode, output text is aligned with an upward vertical offset to maintain comfortable visual separation from the button matrix.
- **Swipe-to-Delete:** Swiping horizontally across the number output triggers a single backspace alongside a tactile haptic confirmation (`HapticFeedbackType.LongPress`).

---

## 🧪 50-Key Scientific Landscape Matrix

When rotated into landscape orientation, the keypad expands into a **10-column, 50-key scientific matrix**:

| Function Category | Supported Operations |
| :--- | :--- |
| **Parentheses & Logic** | $($, $)$, Two-level algebraic precedence execution |
| **Trigonometry** | $\sin, \cos, \tan, \sin^{-1}, \cos^{-1}, \tan^{-1}$ |
| **Hyperbolic** | $\sinh, \cosh, \tanh, \sinh^{-1}, \cosh^{-1}, \tanh^{-1}$ |
| **Logarithms & Powers** | $e^x, 10^x, y^x, 2^x, \ln, \log_{10}, \log_y, x^2, x^3, \sqrt{x}, \sqrt[3]{x}, \sqrt[y]{x}, \text{EE}$ |
| **Constants & Factory Math**| $\pi, e, x!, 1/x, |x|, \text{Rand}$ |
| **Memory Registers** | $\text{mc}, \text{m+}, \text{m-}, \text{mr}$ |
| **Toggles** | $\text{2nd}$ function modifier key, $\text{Rad} / \text{Deg}$ mode switch |

---

## 🛠️ Build & Installation Guide

### Requirements
- **Android Studio:** Hedgehog | 2023.1.1 or newer
- **Kotlin:** 1.9.0+
- **Minimum SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 (Android 14)
- **Java/JDK:** 17

### Local Compilation

```bash
# Clone Repository
git clone https://github.com/erfansvsh/ios-calculator-android.git
cd ios-calculator-android

# Build Debug APK
./gradlew :app:assembleDebug

# Build Release APK
./gradlew :app:assembleRelease
```

### Automated CI/CD Release Builds (GitHub Actions)

Every push to `main` automatically triggers the release workflow in `.github/workflows/build-apk.yml`.
When `RELEASE_KEYSTORE_BASE64` is configured in GitHub Repository Secrets, the CI pipeline automatically decodes the permanent keystore to produce release-signed APK artifacts ready for distribution.

---

## 📂 Project Structure

```
com.erfansvsh.ios17calculator
 ├── MainActivity.kt               # Jetpack Compose UI layout, SF Pro typography, animations & gestures
 └── CalculatorViewModel.kt        # StateFlow state management, operator stacks & math engine
```

---

## 📜 License

This project is open source and available under the [MIT License](LICENSE).
