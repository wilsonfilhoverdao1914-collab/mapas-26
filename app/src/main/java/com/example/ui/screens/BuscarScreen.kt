package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.PatioKind
import com.example.domain.model.SearchMatch
import com.example.domain.model.SearchResultGroup
import com.example.domain.rules.MapaRules
import com.example.ui.components.formatBr
import com.example.ui.theme.BarracaoAccent
import com.example.ui.theme.BarracaoAccentContainer
import com.example.ui.theme.BarracaoOnAccentContainer
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun BuscarScreen(
  query: String,
  results: SearchResultGroup,
  onQueryChange: (String) -> Unit,
  onMatchClick: (SearchMatch) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "Buscar Bloco",
      style = MaterialTheme.typography.headlineSmall,
    )
    Text(
      text = "Encontre todas as posições e o total de fardos do bloco",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      label = { Text("Número do bloco") },
      placeholder = { Text("Ex.: 815") },
      leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = null)
      },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(
            onClick = { onQueryChange("") },
            modifier = Modifier.size(48.dp),
          ) {
            Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
          }
        }
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().testTag("search_block_input"),
    )

    if (query.trim().isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Digite o número do bloco acima para ver onde ele está armazenado.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
      return
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Card resumo com o total de fardos encontrado
    val summaryExcess =
      if (results.distinctBlockNumbers.size == 1) MapaRules.excessBales(results.totalBales) else 0
    Surface(
      shape = MaterialTheme.shapes.medium,
      color =
        if (summaryExcess > 0) ErrorContainerLight
        else MaterialTheme.colorScheme.primaryContainer,
      border =
        if (summaryExcess > 0) BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)) else null,
      modifier = Modifier.fillMaxWidth().testTag("search_total_card"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              if (results.distinctBlockNumbers.size == 1) {
                "Total do Bloco ${results.distinctBlockNumbers.first()}"
              } else {
                "Total encontrado"
              },
            style = MaterialTheme.typography.labelLarge,
            color =
              if (summaryExcess > 0) ErrorRed
              else MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text =
              buildString {
                append(
                  "${results.matches.size} ${if (results.matches.size == 1) "localização" else "localizações"}"
                )
                if (summaryExcess > 0) append(" · passa $summaryExcess de 140")
              },
            style = MaterialTheme.typography.bodySmall,
            color =
              if (summaryExcess > 0) ErrorRed
              else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
          )
        }
        Text(
          text = "${results.totalBales.formatBr()} fardos",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color =
            if (summaryExcess > 0) ErrorRed
            else MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }

    if (results.distinctBlockNumbers.size > 1) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        results.distinctBlockNumbers.take(10).forEach { blockNum ->
          FilterChip(
            selected = blockNum.equals(query.trim(), ignoreCase = true),
            onClick = { onQueryChange(blockNum) },
            label = { Text("Bloco $blockNum") },
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (results.matches.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Nenhuma posição encontrada para o bloco \"${results.query}\".",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(
          items = results.matches,
          key = { "${it.patio.id}_${it.slot.street}_${it.slot.idx}" },
        ) { match ->
          val isBarracao = match.patio.kind == PatioKind.BARRACAO
          val matchExcess = MapaRules.excessBales(match.slot.bales)
          Surface(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { onMatchClick(match) }
                .testTag("search_result_${match.patio.id}_${match.slot.street}_${match.slot.idx}"),
            shape = MaterialTheme.shapes.medium,
            color =
              if (matchExcess > 0) ErrorContainerLight.copy(alpha = 0.7f)
              else MaterialTheme.colorScheme.surface,
            border =
              BorderStroke(
                1.dp,
                if (matchExcess > 0) ErrorRed.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
              ),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color =
                  if (isBarracao) BarracaoAccentContainer
                  else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(46.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector =
                      if (isBarracao) Icons.Default.Warehouse else Icons.Default.WbSunny,
                    contentDescription = match.patio.kind.label,
                    tint =
                      if (isBarracao) BarracaoOnAccentContainer
                      else MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Bloco ${match.slot.number.orEmpty()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color =
                      if (matchExcess > 0) ErrorRed
                      else MaterialTheme.colorScheme.secondaryContainer,
                  ) {
                    Text(
                      text =
                        if (matchExcess > 0) {
                          "${match.slot.bales.formatBr()} fardos (passa $matchExcess)"
                        } else {
                          "${match.slot.bales.formatBr()} fardos"
                        },
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color =
                        if (matchExcess > 0) androidx.compose.ui.graphics.Color.White
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text =
                    "${match.patio.name} · Rua ${match.slot.street} · Posição ${match.slot.positionNumber}",
                  style = MaterialTheme.typography.bodyMedium,
                  color =
                    if (isBarracao) BarracaoAccent
                    else MaterialTheme.colorScheme.onSurface,
                )
                if (!match.slot.note.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "Obs.: ${match.slot.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Abrir no mapa",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}
