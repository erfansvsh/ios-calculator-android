package com.erfansvsh.calculator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.*

class CalculatorViewModel : ViewModel() {

    private val _displayValue = MutableStateFlow("0")
    val displayValue: StateFlow<String> = _displayValue.asStateFlow()

    private val _activeOperator = MutableStateFlow<String?>(null)
    val activeOperator: StateFlow<String?> = _activeOperator.asStateFlow()

    private var previousValue: Double? = null
    private var waitingForOperand: Boolean = false
    private var lastOperation: Pair<String, Double>? = null
    private var hasError: Boolean = false

    // Scientific states
    private val _isRad = MutableStateFlow(true)
    val isRad: StateFlow<Boolean> = _isRad.asStateFlow()

    private val _is2nd = MutableStateFlow(false)
    val is2nd: StateFlow<Boolean> = _is2nd.asStateFlow()

    private var memory: Double = 0.0

    private val symbols = DecimalFormatSymbols(Locale.US)
    private val integerFormat = DecimalFormat("#,###", symbols)

    fun getFormattedDisplay(): String {
        val raw = _displayValue.value
        if (raw == "Error") return "Error"

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
            return
        }

        if (waitingForOperand) {
            _displayValue.value = digit
            waitingForOperand = false
            _activeOperator.value = null
        } else {
            val current = _displayValue.value
            if (current == "0") {
                _displayValue.value = digit
            } else if (current == "-0") {
                _displayValue.value = "-$digit"
            } else {
                val digitsOnly = current.replace(Regex("[.,-]"), "")
                if (digitsOnly.length < 9) {
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
            return
        }

        if (waitingForOperand) {
            _displayValue.value = "0."
            waitingForOperand = false
            _activeOperator.value = null
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
        val current = _displayValue.value.toDoubleOrNull() ?: return
        val op = _activeOperator.value

        val res = if (op != null && previousValue != null) {
            if (op == "+" || op == "−") {
                previousValue!! * (current / 100.0)
            } else {
                current / 100.0
            }
        } else {
            current / 100.0
        }
        _displayValue.value = formatResult(res)
    }

    fun onClear() {
        if (_displayValue.value != "0" || hasError) {
            _displayValue.value = "0"
            hasError = false
        } else {
            _displayValue.value = "0"
            previousValue = null
            _activeOperator.value = null
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

        if (_activeOperator.value != null && waitingForOperand) {
            _activeOperator.value = op
            return
        }

        val currentNum = _displayValue.value.toDoubleOrNull() ?: 0.0

        if (_activeOperator.value != null && previousValue != null) {
            val result = calculate(_activeOperator.value!!, previousValue!!, currentNum)
            if (result.isNaN() || result.isInfinite()) {
                setError()
                return
            }
            val formatted = formatResult(result)
            _displayValue.value = formatted
            previousValue = result
            lastOperation = Pair(_activeOperator.value!!, currentNum)
        } else {
            previousValue = currentNum
        }

        _activeOperator.value = op
        waitingForOperand = true
    }

    fun onEquals() {
        if (hasError) return

        val currentNum = _displayValue.value.toDoubleOrNull() ?: 0.0

        if (_activeOperator.value != null && previousValue != null) {
            val result = calculate(_activeOperator.value!!, previousValue!!, currentNum)
            if (result.isNaN() || result.isInfinite()) {
                setError()
                return
            }
            lastOperation = Pair(_activeOperator.value!!, currentNum)
            _displayValue.value = formatResult(result)
            previousValue = null
            _activeOperator.value = null
            waitingForOperand = true
        } else if (lastOperation != null) {
            val (op, operand) = lastOperation!!
            val result = calculate(op, currentNum, operand)
            if (result.isNaN() || result.isInfinite()) {
                setError()
                return
            }
            _displayValue.value = formatResult(result)
            waitingForOperand = true
        }
    }

    private fun calculate(op: String, a: Double, b: Double): Double {
        return when (op) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            "÷" -> if (b == 0.0) Double.NaN else a / b
            else -> b
        }
    }

    private fun formatResult(num: Double): String {
        if (num.isNaN() || num.isInfinite()) return "Error"
        if (num == 0.0) return "0"

        // Round to 9 significant figures
        val df = DecimalFormat("#.########", symbols)
        val formatted = df.format(num)
        return if (formatted.length > 9) {
            String.format(Locale.US, "%.4e", num).replace("+", "")
        } else {
            formatted
        }
    }

    private fun setError() {
        hasError = true
        _displayValue.value = "Error"
        previousValue = null
        _activeOperator.value = null
        waitingForOperand = false
    }

    fun toggleRad() {
        _isRad.value = !_isRad.value
    }

    fun toggle2nd() {
        _is2nd.value = !_is2nd.value
    }

    fun onScientific(fn: String) {
        val curr = _displayValue.value.toDoubleOrNull() ?: return
        val rad = _isRad.value
        val sec = _is2nd.value

        val res = when (fn) {
            "x²" -> curr.pow(2)
            "x³" -> curr.pow(3)
            "10ˣ" -> 10.0.pow(curr)
            "eˣ" -> exp(curr)
            "1/x" -> if (curr == 0.0) Double.NaN else 1.0 / curr
            "²√x" -> if (curr < 0) Double.NaN else sqrt(curr)
            "³√x" -> cbrt(curr)
            "ln" -> if (curr <= 0) Double.NaN else ln(curr)
            "log₁₀" -> if (curr <= 0) Double.NaN else log10(curr)
            "x!" -> factorial(curr)
            "sin" -> if (sec) asin(curr) else sin(if (rad) curr else Math.toRadians(curr))
            "cos" -> if (sec) acos(curr) else cos(if (rad) curr else Math.toRadians(curr))
            "tan" -> if (sec) atan(curr) else tan(if (rad) curr else Math.toRadians(curr))
            "π" -> Math.PI
            "e" -> Math.E
            "Rand" -> Math.random()
            else -> curr
        }

        if (res.isNaN() || res.isInfinite()) {
            setError()
        } else {
            _displayValue.value = formatResult(res)
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
}
