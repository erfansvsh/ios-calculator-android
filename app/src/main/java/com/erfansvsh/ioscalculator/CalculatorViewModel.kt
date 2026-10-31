package com.erfansvsh.ioscalculator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.*

class CalculatorViewModel : ViewModel() {

    enum class InputState {
        TYPING,                 // User is typing digits or decimal point
        OPERATOR_JUST_PRESSED,  // An operator (+, -, *, /) was just clicked
        OPERAND_READY           // An operand was produced by constant (pi, e), function (sqrt, sin), or equals
    }

    private val _displayValue = MutableStateFlow("0")
    val displayValue: StateFlow<String> = _displayValue.asStateFlow()

    private val _activeOperatorHighlight = MutableStateFlow<String?>(null)
    val activeOperator: StateFlow<String?> = _activeOperatorHighlight.asStateFlow()

    private val _isRadMode = MutableStateFlow(false)
    val isRadMode: StateFlow<Boolean> = _isRadMode.asStateFlow()

    private val _is2nd = MutableStateFlow(false)
    val is2nd: StateFlow<Boolean> = _is2nd.asStateFlow()

    // Internal calculation engine state
    private var inputState: InputState = InputState.TYPING
    private var currentValue: Double = 0.0

    // Two-level operator stack for algebraic operator precedence (+/- vs */powers)
    private var pendingLowOp: Pair<Double, String>? = null   // +, -
    private var pendingHighOp: Pair<Double, String>? = null  // *, /, powers, roots, log, EE

    // Parenthesis stack for nested expressions
    private val parenStack = ArrayDeque<Pair<Pair<Double, String>?, Pair<Double, String>?>>()

    // Repeated equals operation: (operator, rightOperand)
    private var lastEqualsOp: Pair<String, Double>? = null

    private var hasError: Boolean = false
    private var memory: Double = 0.0

    private val symbols = DecimalFormatSymbols(Locale.US)
    private val integerFormat = DecimalFormat("#,###", symbols)

    /**
     * Cleans floating point errors (e.g., 0.1 + 0.2 = 0.3, tan(45°) = 1.0)
     * while maintaining full double precision for transcendental values like pi.
     */
    private fun sanitize(num: Double): Double {
        if (num.isNaN() || num.isInfinite()) return num
        if (abs(num) < 1e-13) return 0.0
        val roundInt = round(num)
        if (abs(num - roundInt) < 1e-11) {
            return roundInt
        }
        return num
    }

    /**
     * Formats the displayed number exactly like Apple iOS Calculator:
     * - Portrait: Up to 9 significant digits, grouped with commas, scientific notation if >= 1e9 or < 1e-8.
     * - Landscape: Up to 16 significant digits, grouped with commas, scientific notation if >= 1e16 or < 1e-8.
     */
    fun getFormattedDisplay(isLandscape: Boolean): String {
        if (hasError || _displayValue.value == "Error") return "Error"

        // During manual typing, show exact digits with commas in the integer portion
        if (inputState == InputState.TYPING) {
            return formatNumberWithCommas(_displayValue.value)
        }

        val num = currentValue
        if (num.isNaN() || num.isInfinite()) return "Error"
        if (num == 0.0) {
            return if (_displayValue.value.startsWith("-")) "-0" else "0"
        }

        val absNum = abs(num)
        val maxDigits = if (isLandscape) 16 else 9
        val maxLimit = if (isLandscape) 1e16 else 1e9
        val minLimit = 1e-8

        // Scientific notation when number genuinely exceeds screen bounds
        if (absNum >= maxLimit || absNum < minLimit) {
            val expFormat = if (isLandscape) "%.10e" else "%.5e"
            val expStr = String.format(Locale.US, expFormat, num)
            return expStr.replace("+", "").replace("e0", "e").replace("e-0", "e-")
        }

        return try {
            val mc = MathContext(maxDigits, RoundingMode.HALF_UP)
            val bd = BigDecimal(num.toString(), mc)
            val plain = bd.stripTrailingZeros().toPlainString()
            formatNumberWithCommas(plain)
        } catch (e: Exception) {
            formatNumberWithCommas(_displayValue.value)
        }
    }

    private fun formatNumberWithCommas(raw: String): String {
        val isNegative = raw.startsWith("-")
        val clean = if (isNegative) raw.substring(1) else raw
        val parts = clean.split(".")
        val intPart = parts[0]
        val formattedInt = try {
            if (intPart.isEmpty()) "0" else integerFormat.format(intPart.toLong())
        } catch (e: Exception) {
            intPart
        }
        val prefix = if (isNegative) "-" else ""
        return if (parts.size > 1) {
            "$prefix$formattedInt.${parts[1]}"
        } else if (raw.endsWith(".")) {
            "$prefix$formattedInt."
        } else {
            "$prefix$formattedInt"
        }
    }

    fun onDigit(digit: String) {
        if (hasError) resetAll()

        if (inputState == InputState.OPERATOR_JUST_PRESSED || inputState == InputState.OPERAND_READY) {
            _displayValue.value = digit
            currentValue = digit.toDoubleOrNull() ?: 0.0
            inputState = InputState.TYPING
            _activeOperatorHighlight.value = null
        } else {
            val current = _displayValue.value
            if (current == "0") {
                _displayValue.value = digit
            } else if (current == "-0") {
                _displayValue.value = "-$digit"
            } else {
                val digitsOnly = current.replace(Regex("[.,-]"), "")
                if (digitsOnly.length < 16) {
                    _displayValue.value = current + digit
                }
            }
            currentValue = _displayValue.value.toDoubleOrNull() ?: 0.0
            _activeOperatorHighlight.value = null
        }
    }

    fun onDecimal() {
        if (hasError) resetAll()

        if (inputState == InputState.OPERATOR_JUST_PRESSED || inputState == InputState.OPERAND_READY) {
            _displayValue.value = "0."
            currentValue = 0.0
            inputState = InputState.TYPING
            _activeOperatorHighlight.value = null
        } else {
            if (!_displayValue.value.contains(".")) {
                _displayValue.value = _displayValue.value + "."
            }
            _activeOperatorHighlight.value = null
        }
    }

    fun onToggleSign() {
        if (hasError) return
        val current = _displayValue.value
        if (current == "0") {
            _displayValue.value = "-0"
        } else if (current == "-0") {
            _displayValue.value = "0"
        } else {
            _displayValue.value = if (current.startsWith("-")) current.substring(1) else "-$current"
            currentValue = _displayValue.value.toDoubleOrNull() ?: 0.0
        }
    }

    fun onPercent() {
        if (hasError) return
        val res = if (pendingLowOp != null && pendingHighOp == null) {
            val (valLow, opLow) = pendingLowOp!!
            if (opLow == "+" || opLow == "−") {
                valLow * (currentValue / 100.0)
            } else {
                currentValue / 100.0
            }
        } else {
            currentValue / 100.0
        }
        currentValue = sanitize(res)
        _displayValue.value = currentValue.toString()
        inputState = InputState.OPERAND_READY
    }

    fun onClear() {
        if (_displayValue.value != "0" || hasError) {
            _displayValue.value = "0"
            currentValue = 0.0
            hasError = false
            inputState = InputState.TYPING
        } else {
            resetAll()
        }
    }

    fun onBackspace() {
        if (hasError || inputState != InputState.TYPING) return
        val current = _displayValue.value
        if (current.length > 1) {
            if (current.length == 2 && current.startsWith("-")) {
                _displayValue.value = "0"
            } else {
                _displayValue.value = current.dropLast(1)
            }
        } else {
            _displayValue.value = "0"
        }
        currentValue = _displayValue.value.toDoubleOrNull() ?: 0.0
    }

    private fun isHighPrecedence(op: String): Boolean {
        return op in listOf("×", "÷", "xʸ", "yˣ", "ʸ√x", "log_y", "logᵧ", "EE")
    }

    fun onOperator(op: String) {
        if (hasError) return

        val isHigh = isHighPrecedence(op)

        // If the user tapped an operator immediately after another operator: switch operator!
        if (inputState == InputState.OPERATOR_JUST_PRESSED) {
            if (isHigh) {
                pendingLowOp = null
                pendingHighOp = Pair(currentValue, op)
            } else {
                pendingHighOp = null
                pendingLowOp = Pair(currentValue, op)
            }
            _activeOperatorHighlight.value = op
            return
        }

        // An operand is present: evaluate algebraic hierarchy
        var curr = currentValue

        if (isHigh) {
            if (pendingHighOp != null) {
                val (valHigh, opHigh) = pendingHighOp!!
                val res = calculate(opHigh, valHigh, curr)
                if (res.isNaN() || res.isInfinite()) {
                    setError()
                    return
                }
                curr = res
                currentValue = curr
                _displayValue.value = curr.toString()
            }
            pendingHighOp = Pair(curr, op)
        } else { // low op (+, −)
            if (pendingHighOp != null) {
                val (valHigh, opHigh) = pendingHighOp!!
                val res = calculate(opHigh, valHigh, curr)
                if (res.isNaN() || res.isInfinite()) {
                    setError()
                    return
                }
                curr = res
                pendingHighOp = null
            }
            if (pendingLowOp != null) {
                val (valLow, opLow) = pendingLowOp!!
                val res = calculate(opLow, valLow, curr)
                if (res.isNaN() || res.isInfinite()) {
                    setError()
                    return
                }
                curr = res
                pendingLowOp = null
            }
            currentValue = curr
            _displayValue.value = curr.toString()
            pendingLowOp = Pair(curr, op)
        }

        inputState = InputState.OPERATOR_JUST_PRESSED
        _activeOperatorHighlight.value = op
    }

    fun onEquals() {
        if (hasError) return

        var curr = currentValue
        val hasPending = pendingHighOp != null || pendingLowOp != null

        if (hasPending) {
            if (pendingHighOp != null) {
                val (valHigh, opHigh) = pendingHighOp!!
                lastEqualsOp = Pair(opHigh, curr)
                curr = calculate(opHigh, valHigh, curr)
                pendingHighOp = null
            }

            if (pendingLowOp != null) {
                val (valLow, opLow) = pendingLowOp!!
                lastEqualsOp = Pair(opLow, curr)
                curr = calculate(opLow, valLow, curr)
                pendingLowOp = null
            }

            if (curr.isNaN() || curr.isInfinite()) {
                setError()
                return
            }

            currentValue = curr
            _displayValue.value = curr.toString()
            _activeOperatorHighlight.value = null
            inputState = InputState.OPERAND_READY
        } else if (lastEqualsOp != null) {
            val (op, operand) = lastEqualsOp!!
            curr = calculate(op, curr, operand)
            if (curr.isNaN() || curr.isInfinite()) {
                setError()
                return
            }
            currentValue = curr
            _displayValue.value = curr.toString()
            _activeOperatorHighlight.value = null
            inputState = InputState.OPERAND_READY
        }
    }

    private fun calculate(op: String, a: Double, b: Double): Double {
        return when (op) {
            "+" -> sanitize(a + b)
            "−" -> sanitize(a - b)
            "×" -> sanitize(a * b)
            "÷" -> if (b == 0.0) Double.NaN else sanitize(a / b)
            "xʸ" -> {
                if (a == 0.0 && b < 0) Double.NaN
                else if (a < 0.0) {
                    if (b == floor(b)) {
                        val intExp = b.toInt()
                        val sign = if (abs(intExp) % 2 == 1) -1.0 else 1.0
                        sanitize(sign * abs(a).pow(b))
                    } else Double.NaN
                } else sanitize(a.pow(b))
            }
            "yˣ" -> {
                if (b == 0.0 && a < 0) Double.NaN
                else if (b < 0.0) {
                    if (a == floor(a)) {
                        val intExp = a.toInt()
                        val sign = if (abs(intExp) % 2 == 1) -1.0 else 1.0
                        sanitize(sign * abs(b).pow(a))
                    } else Double.NaN
                } else sanitize(b.pow(a))
            }
            "ʸ√x" -> {
                if (b == 0.0) Double.NaN
                else if (a < 0.0) {
                    if (b == floor(b) && abs(b.toInt()) % 2 == 1) {
                        -sanitize(abs(a).pow(1.0 / b))
                    } else Double.NaN
                } else sanitize(a.pow(1.0 / b))
            }
            "log_y", "logᵧ" -> {
                if (a <= 0.0 || b <= 0.0 || b == 1.0) Double.NaN
                else sanitize(ln(a) / ln(b))
            }
            "EE" -> sanitize(a * 10.0.pow(b))
            else -> b
        }
    }

    fun onScientific(fn: String) {
        if (hasError) resetAll()

        // Binary operations routed to onOperator
        when (fn) {
            "xʸ", "yˣ", "ʸ√x", "log_y", "logᵧ", "EE" -> {
                onOperator(fn)
                return
            }
            "π" -> {
                currentValue = Math.PI
                _displayValue.value = Math.PI.toString()
                inputState = InputState.OPERAND_READY
                _activeOperatorHighlight.value = null
                return
            }
            "e" -> {
                currentValue = Math.E
                _displayValue.value = Math.E.toString()
                inputState = InputState.OPERAND_READY
                _activeOperatorHighlight.value = null
                return
            }
            "Rand" -> {
                val r = Math.random()
                currentValue = r
                _displayValue.value = r.toString()
                inputState = InputState.OPERAND_READY
                _activeOperatorHighlight.value = null
                return
            }
            "(" -> {
                parenStack.addLast(Pair(pendingLowOp, pendingHighOp))
                pendingLowOp = null
                pendingHighOp = null
                inputState = InputState.OPERATOR_JUST_PRESSED
                return
            }
            ")" -> {
                if (parenStack.isEmpty()) return
                var curr = currentValue
                if (pendingHighOp != null) {
                    val (valHigh, opHigh) = pendingHighOp!!
                    curr = calculate(opHigh, valHigh, curr)
                    pendingHighOp = null
                }
                if (pendingLowOp != null) {
                    val (valLow, opLow) = pendingLowOp!!
                    curr = calculate(opLow, valLow, curr)
                    pendingLowOp = null
                }
                val (savedLow, savedHigh) = parenStack.removeLast()
                pendingLowOp = savedLow
                pendingHighOp = savedHigh
                currentValue = curr
                _displayValue.value = curr.toString()
                inputState = InputState.OPERAND_READY
                return
            }
            "mc" -> {
                memory = 0.0
                return
            }
            "m+" -> {
                memory = sanitize(memory + currentValue)
                inputState = InputState.OPERAND_READY
                return
            }
            "m-" -> {
                memory = sanitize(memory - currentValue)
                inputState = InputState.OPERAND_READY
                return
            }
            "mr" -> {
                currentValue = memory
                _displayValue.value = memory.toString()
                inputState = InputState.OPERAND_READY
                _activeOperatorHighlight.value = null
                return
            }
        }

        // Unary functions applied immediately to currentValue
        val curr = currentValue
        val isRad = _isRadMode.value

        val res = when (fn) {
            "x²" -> sanitize(curr * curr)
            "x³" -> sanitize(curr * curr * curr)
            "10ˣ" -> sanitize(10.0.pow(curr))
            "2ˣ" -> sanitize(2.0.pow(curr))
            "eˣ" -> sanitize(exp(curr))
            "1/x" -> if (curr == 0.0) Double.NaN else sanitize(1.0 / curr)
            "²√x" -> if (curr < 0.0) Double.NaN else sanitize(sqrt(curr))
            "³√x" -> sanitize(cbrt(curr))
            "ln" -> if (curr <= 0.0) Double.NaN else sanitize(ln(curr))
            "log₁₀" -> if (curr <= 0.0) Double.NaN else sanitize(log10(curr))
            "log₂" -> if (curr <= 0.0) Double.NaN else sanitize(log2(curr))
            "x!" -> factorial(curr)
            "sin" -> calcSin(curr, isRad)
            "cos" -> calcCos(curr, isRad)
            "tan" -> calcTan(curr, isRad)
            "sin⁻¹" -> calcAsin(curr, isRad)
            "cos⁻¹" -> calcAcos(curr, isRad)
            "tan⁻¹" -> calcAtan(curr, isRad)
            "sinh" -> sanitize(sinh(curr))
            "cosh" -> sanitize(cosh(curr))
            "tanh" -> sanitize(tanh(curr))
            "sinh⁻¹" -> sanitize(asinh(curr))
            "cosh⁻¹" -> if (curr < 1.0) Double.NaN else sanitize(acosh(curr))
            "tanh⁻¹" -> if (abs(curr) >= 1.0) Double.NaN else sanitize(atanh(curr))
            else -> curr
        }

        if (res.isNaN() || res.isInfinite()) {
            setError()
        } else {
            currentValue = res
            _displayValue.value = res.toString()
            inputState = InputState.OPERAND_READY
        }
    }

    private fun factorial(num: Double): Double {
        if (num < 0 || num != floor(num) || num > 170.0) return Double.NaN
        val n = num.toInt()
        if (n == 0 || n == 1) return 1.0
        var acc = 1.0
        for (i in 2..n) {
            acc *= i
        }
        return acc
    }

    private fun calcSin(angle: Double, inRad: Boolean): Double {
        if (inRad) {
            val pi = Math.PI
            val rem = abs(angle % (2 * pi))
            if (rem < 1e-12 || abs(rem - pi) < 1e-12 || abs(rem - 2 * pi) < 1e-12) return 0.0
            return sanitize(sin(angle))
        }
        var norm = angle % 360.0
        if (norm < 0) norm += 360.0
        return when {
            abs(norm) < 1e-7 || abs(norm - 180.0) < 1e-7 || abs(norm - 360.0) < 1e-7 -> 0.0
            abs(norm - 30.0) < 1e-7 || abs(norm - 150.0) < 1e-7 -> 0.5
            abs(norm - 90.0) < 1e-7 -> 1.0
            abs(norm - 210.0) < 1e-7 || abs(norm - 330.0) < 1e-7 -> -0.5
            abs(norm - 270.0) < 1e-7 -> -1.0
            abs(norm - 45.0) < 1e-7 || abs(norm - 135.0) < 1e-7 -> sanitize(sqrt(2.0) / 2.0)
            abs(norm - 225.0) < 1e-7 || abs(norm - 315.0) < 1e-7 -> sanitize(-sqrt(2.0) / 2.0)
            abs(norm - 60.0) < 1e-7 || abs(norm - 120.0) < 1e-7 -> sanitize(sqrt(3.0) / 2.0)
            abs(norm - 240.0) < 1e-7 || abs(norm - 300.0) < 1e-7 -> sanitize(-sqrt(3.0) / 2.0)
            else -> sanitize(sin(Math.toRadians(norm)))
        }
    }

    private fun calcCos(angle: Double, inRad: Boolean): Double {
        if (inRad) {
            val halfPi = Math.PI / 2.0
            val rem = abs(angle % Math.PI)
            if (abs(rem - halfPi) < 1e-12) return 0.0
            return sanitize(cos(angle))
        }
        var norm = angle % 360.0
        if (norm < 0) norm += 360.0
        return when {
            abs(norm) < 1e-7 || abs(norm - 360.0) < 1e-7 -> 1.0
            abs(norm - 60.0) < 1e-7 || abs(norm - 300.0) < 1e-7 -> 0.5
            abs(norm - 90.0) < 1e-7 || abs(norm - 270.0) < 1e-7 -> 0.0
            abs(norm - 120.0) < 1e-7 || abs(norm - 240.0) < 1e-7 -> -0.5
            abs(norm - 180.0) < 1e-7 -> -1.0
            abs(norm - 45.0) < 1e-7 || abs(norm - 315.0) < 1e-7 -> sanitize(sqrt(2.0) / 2.0)
            abs(norm - 135.0) < 1e-7 || abs(norm - 225.0) < 1e-7 -> sanitize(-sqrt(2.0) / 2.0)
            abs(norm - 30.0) < 1e-7 || abs(norm - 330.0) < 1e-7 -> sanitize(sqrt(3.0) / 2.0)
            abs(norm - 150.0) < 1e-7 || abs(norm - 210.0) < 1e-7 -> sanitize(-sqrt(3.0) / 2.0)
            else -> sanitize(cos(Math.toRadians(norm)))
        }
    }

    private fun calcTan(angle: Double, inRad: Boolean): Double {
        if (inRad) {
            val halfPi = Math.PI / 2.0
            val rem = abs(angle % Math.PI)
            if (abs(rem - halfPi) < 1e-12) return Double.NaN
            return sanitize(tan(angle))
        }
        var norm = angle % 180.0
        if (norm < 0) norm += 180.0
        return when {
            abs(norm) < 1e-7 || abs(norm - 180.0) < 1e-7 -> 0.0
            abs(norm - 45.0) < 1e-7 -> 1.0
            abs(norm - 135.0) < 1e-7 -> -1.0
            abs(norm - 90.0) < 1e-7 -> Double.NaN
            abs(norm - 30.0) < 1e-7 -> sanitize(1.0 / sqrt(3.0))
            abs(norm - 60.0) < 1e-7 -> sanitize(sqrt(3.0))
            abs(norm - 120.0) < 1e-7 -> sanitize(-sqrt(3.0))
            abs(norm - 150.0) < 1e-7 -> sanitize(-1.0 / sqrt(3.0))
            else -> sanitize(tan(Math.toRadians(norm)))
        }
    }

    private fun calcAsin(v: Double, inRad: Boolean): Double {
        if (v < -1.0 || v > 1.0) return Double.NaN
        val rad = asin(v)
        if (inRad) return sanitize(rad)
        val deg = Math.toDegrees(rad)
        return when {
            abs(v) < 1e-9 -> 0.0
            abs(v - 0.5) < 1e-9 -> 30.0
            abs(v - 1.0) < 1e-9 -> 90.0
            abs(v - (-0.5)) < 1e-9 -> -30.0
            abs(v - (-1.0)) < 1e-9 -> -90.0
            else -> sanitize(deg)
        }
    }

    private fun calcAcos(v: Double, inRad: Boolean): Double {
        if (v < -1.0 || v > 1.0) return Double.NaN
        val rad = acos(v)
        if (inRad) return sanitize(rad)
        val deg = Math.toDegrees(rad)
        return when {
            abs(v - 1.0) < 1e-9 -> 0.0
            abs(v - 0.5) < 1e-9 -> 60.0
            abs(v) < 1e-9 -> 90.0
            abs(v - (-0.5)) < 1e-9 -> 120.0
            abs(v - (-1.0)) < 1e-9 -> 180.0
            else -> sanitize(deg)
        }
    }

    private fun calcAtan(v: Double, inRad: Boolean): Double {
        val rad = atan(v)
        if (inRad) return sanitize(rad)
        val deg = Math.toDegrees(rad)
        return when {
            abs(v) < 1e-9 -> 0.0
            abs(v - 1.0) < 1e-9 -> 45.0
            abs(v - (-1.0)) < 1e-9 -> -45.0
            else -> sanitize(deg)
        }
    }

    private fun asinh(x: Double): Double = ln(x + sqrt(x * x + 1.0))
    private fun acosh(x: Double): Double = ln(x + sqrt(x * x - 1.0))
    private fun atanh(x: Double): Double = 0.5 * ln((1.0 + x) / (1.0 - x))

    fun toggleRad() {
        _isRadMode.value = !_isRadMode.value
    }

    fun toggle2nd() {
        _is2nd.value = !_is2nd.value
    }

    private fun setError() {
        hasError = true
        _displayValue.value = "Error"
        currentValue = Double.NaN
        _activeOperatorHighlight.value = null
        pendingLowOp = null
        pendingHighOp = null
        inputState = InputState.TYPING
    }

    private fun resetAll() {
        _displayValue.value = "0"
        currentValue = 0.0
        inputState = InputState.TYPING
        pendingLowOp = null
        pendingHighOp = null
        parenStack.clear()
        lastEqualsOp = null
        _activeOperatorHighlight.value = null
        hasError = false
    }
}
