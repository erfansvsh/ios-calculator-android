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

        // Scientific notation only when number exceeds display limits
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
        _displayValue.value = res.toString()
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
            _displayValue.value = result.toString()
            previousValue = result
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
            lastOperation = Pair(pendingOperator!!, currentNum)
            _displayValue.value = result.toString()
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
            _displayValue.value = result.toString()
            waitingForOperand = true
        }
    }

    private fun calculate(op: String, a: Double, b: Double): Double {
        return when (op) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            "÷" -> if (b == 0.0) Double.NaN else a / b
            "xʸ" -> a.pow(b)
            "yˣ" -> b.pow(a)
            "ʸ√x" -> if (b == 0.0) Double.NaN else a.pow(1.0 / b)
            "log_y" -> if (a <= 0.0 || b <= 0.0 || b == 1.0) Double.NaN else log(a, b)
            "EE" -> a * 10.0.pow(b)
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

    fun onScientific(fn: String) {
        val curr = _displayValue.value.toDoubleOrNull() ?: return
        val isRad = _isRadMode.value
        val sec = _is2nd.value

        // Binary operators
        when (fn) {
            "xʸ", "yˣ", "ʸ√x", "log_y", "EE" -> {
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
        }

        // Unary functions
        val res = when (fn) {
            "x²" -> curr.pow(2)
            "x³" -> curr.pow(3)
            "10ˣ" -> 10.0.pow(curr)
            "2ˣ" -> 2.0.pow(curr)
            "eˣ" -> exp(curr)
            "1/x" -> if (curr == 0.0) Double.NaN else 1.0 / curr
            "²√x" -> if (curr < 0) Double.NaN else sqrt(curr)
            "³√x" -> cbrt(curr)
            "ln" -> if (curr <= 0) Double.NaN else ln(curr)
            "log₁₀" -> if (curr <= 0) Double.NaN else log10(curr)
            "log₂" -> if (curr <= 0) Double.NaN else log2(curr)
            "x!" -> factorial(curr)
            "sin" -> if (sec) {
                val radVal = asin(curr)
                if (isRad) radVal else Math.toDegrees(radVal)
            } else {
                sin(if (isRad) curr else Math.toRadians(curr))
            }
            "cos" -> if (sec) {
                val radVal = acos(curr)
                if (isRad) radVal else Math.toDegrees(radVal)
            } else {
                cos(if (isRad) curr else Math.toRadians(curr))
            }
            "tan" -> if (sec) {
                val radVal = atan(curr)
                if (isRad) radVal else Math.toDegrees(radVal)
            } else {
                tan(if (isRad) curr else Math.toRadians(curr))
            }
            "sinh" -> if (sec) asinh(curr) else sinh(curr)
            "cosh" -> if (sec) acosh(curr) else cosh(curr)
            "tanh" -> if (sec) atanh(curr) else tanh(curr)
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
        if (n < 0 || n != floor(n) || n > 170) return Double.NaN
        var res = 1.0
        for (i in 2..n.toInt()) {
            res *= i
        }
        return res
    }

    private fun asinh(x: Double): Double = ln(x + sqrt(x * x + 1.0))
    private fun acosh(x: Double): Double = if (x < 1.0) Double.NaN else ln(x + sqrt(x * x - 1.0))
    private fun atanh(x: Double): Double = if (abs(x) >= 1.0) Double.NaN else 0.5 * ln((1.0 + x) / (1.0 - x))
}
