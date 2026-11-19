package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.rules.CalcOp
import com.example.domain.rules.CalculatorEngine
import com.example.domain.rules.CalculatorState
import com.example.ui.theme.CottonInk
import com.example.ui.theme.CottonMutedInk
import com.example.ui.theme.CottonOutline
import com.example.ui.theme.CottonPaperBg
import com.example.ui.theme.CottonSoftOutline
import com.example.ui.theme.CottonSurface
import com.example.ui.theme.CottonSurfaceVariant
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ForestPrimary
import com.example.ui.theme.ForestPrimaryContainer
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.OnForestPrimaryContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var calcState by remember { mutableStateOf(CalculatorState()) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = CottonPaperBg,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 16.dp)
          .padding(bottom = 20.dp)
          .testTag("calculator_sheet"),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Calculadora",
            style = MaterialTheme.typography.titleLarge,
            color = CottonInk,
          )
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("calculator_close_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Fechar calculadora",
              tint = CottonInk,
            )
          }
        }

        // Visor de papel quadriculado: expressão pequena em cima e número grande embaixo
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = CottonSurface,
          border = BorderStroke(1.5.dp, CottonOutline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.End,
          ) {
            Text(
              text = calcState.expressionDisplay.ifEmpty { " " },
              fontFamily = IbmPlexMonoFontFamily,
              fontSize = 14.sp,
              color = CottonMutedInk,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.End,
              modifier = Modifier.fillMaxWidth().testTag("calculator_expression"),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = calcState.mainDisplay,
              fontFamily = IbmPlexMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 34.sp,
              lineHeight = 40.sp,
              color = if (calcState.isError) ErrorRed else CottonInk,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.End,
              modifier = Modifier.fillMaxWidth().testTag("calculator_display"),
            )
          }
        }

        // Teclado 4 colunas: C, apagar, %, ÷ | 7 8 9 × | 4 5 6 − | 1 2 3 + | ±, 0, ',', =
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CalcKeyButton(
              label = "C",
              bgColor = ErrorContainerLight,
              contentColor = ErrorRed,
              onClick = { calcState = CalculatorEngine.pressClear() },
              modifier = Modifier.weight(1f).testTag("calc_btn_C"),
            )
            CalcBackspaceButton(
              onClick = { calcState = CalculatorEngine.pressBackspace(calcState) },
              modifier = Modifier.weight(1f).testTag("calc_btn_backspace"),
            )
            CalcKeyButton(
              label = "%",
              bgColor = CottonSurfaceVariant,
              contentColor = CottonInk,
              onClick = { calcState = CalculatorEngine.pressPercent(calcState) },
              modifier = Modifier.weight(1f).testTag("calc_btn_percent"),
            )
            CalcKeyButton(
              label = "÷",
              bgColor = ForestPrimaryContainer,
              contentColor = OnForestPrimaryContainer,
              onClick = { calcState = CalculatorEngine.pressOperator(calcState, CalcOp.DIVIDE) },
              modifier = Modifier.weight(1f).testTag("calc_btn_divide"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(7, 8, 9).forEach { digit ->
              CalcDigitButton(
                digit = digit,
                onClick = { calcState = CalculatorEngine.pressDigit(calcState, digit) },
                modifier = Modifier.weight(1f),
              )
            }
            CalcKeyButton(
              label = "×",
              bgColor = ForestPrimaryContainer,
              contentColor = OnForestPrimaryContainer,
              onClick = { calcState = CalculatorEngine.pressOperator(calcState, CalcOp.MULTIPLY) },
              modifier = Modifier.weight(1f).testTag("calc_btn_multiply"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(4, 5, 6).forEach { digit ->
              CalcDigitButton(
                digit = digit,
                onClick = { calcState = CalculatorEngine.pressDigit(calcState, digit) },
                modifier = Modifier.weight(1f),
              )
            }
            CalcKeyButton(
              label = "−",
              bgColor = ForestPrimaryContainer,
              contentColor = OnForestPrimaryContainer,
              onClick = { calcState = CalculatorEngine.pressOperator(calcState, CalcOp.SUBTRACT) },
              modifier = Modifier.weight(1f).testTag("calc_btn_subtract"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(1, 2, 3).forEach { digit ->
              CalcDigitButton(
                digit = digit,
                onClick = { calcState = CalculatorEngine.pressDigit(calcState, digit) },
                modifier = Modifier.weight(1f),
              )
            }
            CalcKeyButton(
              label = "+",
              bgColor = ForestPrimaryContainer,
              contentColor = OnForestPrimaryContainer,
              onClick = { calcState = CalculatorEngine.pressOperator(calcState, CalcOp.ADD) },
              modifier = Modifier.weight(1f).testTag("calc_btn_add"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CalcKeyButton(
              label = "±",
              bgColor = CottonSurfaceVariant,
              contentColor = CottonInk,
              onClick = { calcState = CalculatorEngine.pressNegate(calcState) },
              modifier = Modifier.weight(1f).testTag("calc_btn_negate"),
            )
            CalcDigitButton(
              digit = 0,
              onClick = { calcState = CalculatorEngine.pressDigit(calcState, 0) },
              modifier = Modifier.weight(1f),
            )
            CalcKeyButton(
              label = ",",
              bgColor = CottonSurface,
              contentColor = CottonInk,
              onClick = { calcState = CalculatorEngine.pressDecimal(calcState) },
              modifier = Modifier.weight(1f).testTag("calc_btn_comma"),
            )
            CalcKeyButton(
              label = "=",
              bgColor = ForestPrimary,
              contentColor = Color.White,
              borderColor = ForestPrimary,
              onClick = { calcState = CalculatorEngine.pressEquals(calcState) },
              modifier = Modifier.weight(1f).testTag("calc_btn_equals"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CalcDigitButton(
  digit: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  CalcKeyButton(
    label = digit.toString(),
    bgColor = CottonSurface,
    contentColor = CottonInk,
    onClick = onClick,
    modifier = modifier.testTag("calc_btn_$digit"),
  )
}

@Composable
private fun CalcKeyButton(
  label: String,
  bgColor: Color,
  contentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  borderColor: Color = CottonSoftOutline,
) {
  Surface(
    modifier =
      modifier
        .heightIn(min = 56.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(10.dp),
    color = bgColor,
    border = BorderStroke(1.dp, borderColor),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        fontFamily = IbmPlexMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = contentColor,
      )
    }
  }
}

@Composable
private fun CalcBackspaceButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .heightIn(min = 56.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(10.dp),
    color = CottonSurfaceVariant,
    border = BorderStroke(1.dp, CottonSoftOutline),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Backspace,
        contentDescription = "Apagar último dígito",
        tint = CottonInk,
      )
    }
  }
}
