package com.example.domain.rules

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class CalcOp(val symbol: String) {
  ADD("+"),
  SUBTRACT("−"),
  MULTIPLY("×"),
  DIVIDE("÷"),
}

data class CalculatorState(
  val expressionDisplay: String = "",
  val mainDisplay: String = "0",
  val storedOperand: BigDecimal? = null,
  val pendingOp: CalcOp? = null,
  val currentInput: String = "0",
  val isWaitingForNextOperand: Boolean = false,
  val justEvaluated: Boolean = false,
  val isError: Boolean = false,
)

object CalculatorEngine {
  private val mathCtx = MathContext(12, RoundingMode.HALF_UP)

  fun pressDigit(state: CalculatorState, digit: Int): CalculatorState {
    require(digit in 0..9)
    if (state.isError) {
      return CalculatorState(
        mainDisplay = digit.toString(),
        currentInput = digit.toString(),
      )
    }
    if (state.justEvaluated) {
      return CalculatorState(
        mainDisplay = digit.toString(),
        currentInput = digit.toString(),
      )
    }
    if (state.isWaitingForNextOperand) {
      val next = digit.toString()
      return state.copy(
        currentInput = next,
        mainDisplay = next,
        isWaitingForNextOperand = false,
      )
    }
    val next =
      when {
        state.currentInput == "0" -> digit.toString()
        state.currentInput == "-0" -> "-$digit"
        state.currentInput.replace("-", "").replace(".", "").length >= 12 -> state.currentInput
        else -> state.currentInput + digit
      }
    return state.copy(
      currentInput = next,
      mainDisplay = toPortugueseDisplay(next),
    )
  }

  fun pressDecimal(state: CalculatorState): CalculatorState {
    if (state.isError || state.justEvaluated) {
      return CalculatorState(
        mainDisplay = "0,",
        currentInput = "0.",
      )
    }
    if (state.isWaitingForNextOperand) {
      return state.copy(
        currentInput = "0.",
        mainDisplay = "0,",
        isWaitingForNextOperand = false,
      )
    }
    if (state.currentInput.contains('.')) return state
    val next = "${state.currentInput}."
    return state.copy(
      currentInput = next,
      mainDisplay = toPortugueseDisplay(next),
    )
  }

  fun pressBackspace(state: CalculatorState): CalculatorState {
    if (state.isError) return CalculatorState()
    if (state.justEvaluated || state.isWaitingForNextOperand) return state
    val trimmed = state.currentInput.dropLast(1)
    val next =
      when {
        trimmed.isEmpty() || trimmed == "-" -> "0"
        else -> trimmed
      }
    return state.copy(
      currentInput = next,
      mainDisplay = toPortugueseDisplay(next),
    )
  }

  fun pressClear(): CalculatorState = CalculatorState()

  fun pressNegate(state: CalculatorState): CalculatorState {
    if (state.isError) return CalculatorState()
    val value = parseInput(state.currentInput)
    if (value.compareTo(BigDecimal.ZERO) == 0) return state
    val toggled =
      if (state.currentInput.startsWith("-")) state.currentInput.drop(1)
      else "-${state.currentInput}"
    return state.copy(
      currentInput = toggled,
      mainDisplay = toPortugueseDisplay(toggled),
      justEvaluated = false,
    )
  }

  fun pressPercent(state: CalculatorState): CalculatorState {
    if (state.isError) return CalculatorState()
    val currentVal = parseInput(state.currentInput)
    val hundred = BigDecimal(100)
    val result =
      if (
        state.storedOperand != null &&
          (state.pendingOp == CalcOp.ADD || state.pendingOp == CalcOp.SUBTRACT)
      ) {
        state.storedOperand.multiply(currentVal, mathCtx).divide(hundred, mathCtx)
      } else {
        currentVal.divide(hundred, mathCtx)
      }
    val rawStr = formatRaw(result)
    return state.copy(
      currentInput = rawStr,
      mainDisplay = formatPortuguese(result),
      isWaitingForNextOperand = false,
      justEvaluated = false,
    )
  }

  fun pressOperator(state: CalculatorState, op: CalcOp): CalculatorState {
    if (state.isError) return CalculatorState()
    val currentVal = parseInput(state.currentInput)

    if (state.storedOperand != null && state.pendingOp != null && !state.isWaitingForNextOperand && !state.justEvaluated) {
      val computed = evaluate(state.storedOperand, currentVal, state.pendingOp)
        ?: return CalculatorState(
          expressionDisplay = "${formatPortuguese(state.storedOperand)} ${state.pendingOp.symbol} ${formatPortuguese(currentVal)} =",
          mainDisplay = "Erro",
          isError = true,
        )
      val rawStr = formatRaw(computed)
      return CalculatorState(
        expressionDisplay = "${formatPortuguese(computed)} ${op.symbol}",
        mainDisplay = formatPortuguese(computed),
        storedOperand = computed,
        pendingOp = op,
        currentInput = rawStr,
        isWaitingForNextOperand = true,
        justEvaluated = false,
      )
    }

    val left = if (state.justEvaluated) parseInput(state.currentInput) else currentVal
    return state.copy(
      expressionDisplay = "${formatPortuguese(left)} ${op.symbol}",
      storedOperand = left,
      pendingOp = op,
      isWaitingForNextOperand = true,
      justEvaluated = false,
    )
  }

  fun pressEquals(state: CalculatorState): CalculatorState {
    if (state.isError) return state
    val op = state.pendingOp ?: return state.copy(
      expressionDisplay = "${toPortugueseDisplay(state.currentInput)} =",
      justEvaluated = true,
    )
    val left = state.storedOperand ?: return state
    val right = parseInput(state.currentInput)
    val expr = "${formatPortuguese(left)} ${op.symbol} ${formatPortuguese(right)} ="
    val computed =
      evaluate(left, right, op)
        ?: return CalculatorState(
          expressionDisplay = expr,
          mainDisplay = "Erro",
          isError = true,
        )
    val rawStr = formatRaw(computed)
    return CalculatorState(
      expressionDisplay = expr,
      mainDisplay = formatPortuguese(computed),
      storedOperand = computed,
      pendingOp = null,
      currentInput = rawStr,
      isWaitingForNextOperand = false,
      justEvaluated = true,
    )
  }

  private fun evaluate(left: BigDecimal, right: BigDecimal, op: CalcOp): BigDecimal? {
    return when (op) {
      CalcOp.ADD -> left.add(right, mathCtx)
      CalcOp.SUBTRACT -> left.subtract(right, mathCtx)
      CalcOp.MULTIPLY -> left.multiply(right, mathCtx)
      CalcOp.DIVIDE -> {
        if (right.compareTo(BigDecimal.ZERO) == 0) null
        else left.divide(right, 8, RoundingMode.HALF_UP)
      }
    }
  }

  private fun parseInput(raw: String): BigDecimal {
    val sanitized = raw.trim().replace(',', '.')
    if (sanitized.isEmpty() || sanitized == "-" || sanitized == "." || sanitized == "-.") {
      return BigDecimal.ZERO
    }
    return sanitized.toBigDecimalOrNull() ?: BigDecimal.ZERO
  }

  private fun formatRaw(value: BigDecimal): String {
    val stripped =
      if (value.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
      else value.stripTrailingZeros()
    val plain = stripped.toPlainString()
    return if (plain == "-0") "0" else plain
  }

  private fun formatPortuguese(value: BigDecimal): String {
    return formatRaw(value).replace('.', ',')
  }

  private fun toPortugueseDisplay(raw: String): String {
    return raw.replace('.', ',')
  }
}
