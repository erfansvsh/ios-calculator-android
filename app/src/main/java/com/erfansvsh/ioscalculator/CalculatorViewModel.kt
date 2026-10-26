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

    private val _displayValue = MutableStateFlow("0")
    val displayValue: StateFlow<String> = _displayValue.asStateFlow()

    // Controls the visual highlight of the active operator button
    private val _activeOperatorHighlight = MutableStateFlow<String?>(null)
    val activeOperator: StateFlow<String?> = _activeOperatorHighlight.asStateFlow()

    // Persistent calculation state
    private var pendingOperator: String? = null
    private var previousValue: Double? = null
    private var waitingForOperand: Boolean = false
    private var lastOperation: Pair<String, Double>? = null
    private var hasError: Boolean = false

    // Scientific states: Default is DEGREES (isRadMode = false)
    private val _isRadMode = MutableStateFlow(false)
    val isRadMode: StateFlow<Boolean> = _isRadMode.asStateFlow()

    private val _is2nd = MutableStateFlow(false)
    val is2nd: StateFlow<Boolean> = _is2nd.asStateFlow()

    private var memory: Double = 0.0

    private val symbols = DecimalFormatSymbols(Locale.US)
    private val integerFormat = DecimalFormat("#,###", symbols)

    /**
     * Cleans IEEE-754 floating point inaccuracies (e.g., 0.1 + 0.2 = 0.3, tan(45°) = 1.0)
     */
    private fun sanitize(num: Double): Double {
        if (num.isNaN() || num.isInfinite()) return num
        if (abs(num) < 1e-13) return 0.0

        // If it's extremely close to an integer (e.g. 0.9999999999999999, 2.9999999999999996), snap to integer
        val roundInt = round(num)
        if (abs(num - roundInt) < 1e-11) {
            return roundInt
        }

        // Round to 12 significant decimal places to eliminate floating point artifacts
        return try {
            val bd = BigDecimal(num.toString(), MathContext(12, RoundingMode.HALF_UP))
            bd.toDouble()
        } catch (e: Exception) {
            num
        }
    }

    fun getFormattedDisplay(isLandscape: Boolean): String {
        val raw = _displayValue.value
        if (raw == "Error") return "Error"

        // During manual typing with dot or trailing zeros (e.g., "3." or "3.00")
        if (raw.endsWith(".") || (raw.contains(".") && !waitingForOperand && !raw.contains("e") && !raw.contains("E"))) {
            return formatNumberWithCommas(raw)
        }

        val num = raw.toDoubleOrNull() ?: return raw
        if (num == 0.0) {
            return if (raw.startsWith("-")) "-0" else "0"
        }

        val absNum = abs(num)
        val maxDigits = if (isLandscape) 16 else 9
        val maxLimit = if (isLandscape) 1e16 else 1e9
        val minLimit = 1e-8

        // Scientific notation only when number genuinely exceeds display limits
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
            formatNumberWithCommas(raw)
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
        if (hasError) {
            hasError = false
            _displayValue.value = digit
            waitingForOperand = false
            _activeOperatorHighlight.value = null
            return
        }

        if (waitingForOperand) {
            _displayValue.value = digit
            waitingForOperand = false
            // Visual highlight clears on typing, but pendingOperator remains saved!
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
        }
    }

    fun onDecimal() {
        if (hasError) {
            hasError = false
            _displayValue.value = "0."
            waitingForOperand = false
            _activeOperatorHighlight.value = null
            return
        }

        if (waitingForOperand) {
            _displayValue.value = "0."
            waitingForOperand = false
            _activeOperatorHighlight.value = null
        } else {
            if (!_displayValue.value.contains(".")) {
                _displayValue.value = _displayValue.value + "."
            }
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
        }
    }

    fun onPercent() {
        if (hasError) return
        val currentNum = _displayValue.value.toDoubleOrNull() ?: return

        val res = if (pendingOperator != null && previousValue != null) {
            if (pendingOperator == "+" || pendingOperator == "−") {
                previousValue!! * (currentNum / 100.0)
            } else {
                currentNum / 100.0
            }
        } else {
            currentNum / 100.0
        }
        _displayValue.value = sanitize(res).toString()
    }

    fun onClear() {
        if (_displayValue.value != "0" || hasError) {
            _displayValue.value = "0"
            hasError = false
        } else {
            _displayValue.value = "0"
            previousValue = null
            pendingOperator = null
            _activeOperatorHighlight.value = null
            waitingForOperand = false
            lastOperation = null
            hasError = false
        }
    }

    fun onBackspace() {
        if (hasError || waitingForOperand) return
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
    }

    fun onOperator(op: String) {
        if (hasError) return

        val currentNum = _displayValue.value.toDoubleOrNull() ?: 0.0

        if (waitingForOperand) {
            // User changed the operator before typing next number
            pendingOperator = op
            _activeOperatorHighlight.value = op
            return
        }

        if (pendingOperator != null && previousValue != null) {
            // Chain calculation: calculate previous pending operator first
            val result = calculate(pendingOperator!!, previousValue!!, currentNum)
            if (result.isNaN() || result.isInfinite()) {
                setError()
                return
            }
            val sanitized = sanitize(result)
            _displayValue.value = sanitized.toString()
            previousValue = sanitized
            lastOperation = Pair(pendingOperator!!, currentNum)
        } else {
            previousValue = currentNum
        }

        pendingOperator = op
        _activeOperatorHighlight.value = op
        waitingForOperand = true
    }

    fun onEquals() {
        if (hasError) return

        val currentNum = _displayValue.value.toDoubleOrNull() ?: 0.0

        if (pendingOperator != null && previousValue != null) {
            val result = calculate(pendingOperator!!, previousValue!!, currentNum)
            if (result.isNaN() || result.isInfinite()) {
                setError()
                return
            }
            val sanitized = sanitize(result)
            lastOperation = Pair(pendingOperator!!, currentNum)
            _displayValue.value = sanitized.toString()
            previousValue = null
            pendingOperator = null
            _activeOperatorHighlight.value = null
            waitingForOperand = true
        } else if (lastOperation != null) {
            // Repeat last operation on successive taps on equals
            val (op, operand) = lastOperation!!
            val result = calculate(op, currentNum, operand)
            if (result.isNaN() || result.isInfinite()) {
                setError()
                return
            }
            val sanitized = sanitize(result)
            _displayValue.value = sanitized.toString()
            waitingForOperand = true
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
                    // Check if b is integer
                    if (b == floor(b)) {
                        val intExp = b.toInt()
                        val sign = if (abs(intExp) % 2 == 1) -1.0 else 1.0
                        sanitize(sign * abs(a).pow(b))
                    } else Double.NaN
                } else sanitize(a.pow(b))
            }
            "yˣ" -> {
                // b to the power of a
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
                    // Odd root of negative number is valid (e.g. 3rd root of -8 is -2)
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

    private fun setError() {
        hasError = true
        _displayValue.value = "Error"
        previousValue = null
        pendingOperator = null
        _activeOperatorHighlight.value = null
        waitingForOperand = false
    }

    fun toggleRad() {
        _isRadMode.value = !_isRadMode.value
    }

    fun toggle2nd() {
        _is2nd.value = !_is2nd.value
    }

    // Comprehensive Trigonometric Functions with Exact Values in Degrees
    private fun calcSin(angle: Double, inRad: Boolean): Double {
        if (inRad) {
            val pi = Math.PI
            val rem = abs(angle % (2 * pi))
            if (rem < 1e-12 || abs(rem - pi) < 1e-12 || abs(rem - 2 * pi) < 1e-12) return 0.0
            return sanitize(sin(angle))
        }
        // Normalize angle into [0, 360)
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
            abs(norm - 90.0) < 1e-7 -> Double.NaN // Undefined at 90°
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

    fun onScientific(fn: String) {
        val curr = _displayValue.value.toDoubleOrNull() ?: return
        val isRad = _isRadMode.value

        // Binary operators
        when (fn) {
            "xʸ", "yˣ", "ʸ√x", "log_y", "logᵧ", "EE" -> {
                onOperator(fn)
                return
            }
            "π" -> {
                _displayValue.value = Math.PI.toString()
                waitingForOperand = true
                return
            }
            "e" -> {
                _displayValue.value = Math.E.toString()
                waitingForOperand = true
                return
            }
            "Rand" -> {
                _displayValue.value = Math.random().toString()
                waitingForOperand = true
                return
            }
            "mc" -> {
                memory = 0.0
                return
            }
            "m+" -> {
                memory = sanitize(memory + curr)
                waitingForOperand = true
                return
            }
            "m-" -> {
                memory = sanitize(memory - curr)
                waitingForOperand = true
                return
            }
            "mr" -> {
                _displayValue.value = sanitize(memory).toString()
                waitingForOperand = true
                return
            }
        }

        // Unary functions with robust error bounds and sanitization
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
            _displayValue.value = res.toString()
            waitingForOperand = true
        }
    }

    private fun factorial(n: Double): Double {
        if (n < 0.0 || n != floor(n) || n > 170.0) return Double.NaN
        var res = 1.0
        for (i in 2..n.toInt()) {
            res *= i
        }
        return sanitize(res)
    }

    private fun asinh(x: Double): Double = ln(x + sqrt(x * x + 1.0))
    private fun acosh(x: Double): Double = ln(x + sqrt(x * x - 1.0))
    private fun atanh(x: Double): Double = 0.5 * ln((1.0 + x) / (1.0 - x))
}
