package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.rules.MapaRules
import com.example.ui.components.formatBr
import com.example.ui.theme.CottonInk
import com.example.ui.theme.CottonMutedInk
import com.example.ui.theme.CottonOutline
import com.example.ui.theme.CottonPaperBg
import com.example.ui.theme.CottonSoftOutline
import com.example.ui.theme.CottonSurface
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ForestPrimary
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.SuccessContainerLight
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

@Composable
fun ConferenciaScreen(
  summary: MapaRules.ConferenceSummary,
  filteredItems: List<MapaRules.ConferenceBlockItem>,
  activeFilter: MapaRules.ConferenceFilter,
  query: String,
  onFilterChange: (MapaRules.ConferenceFilter) -> Unit,
  onQueryChange: (String) -> Unit,
  onToggleBlock: (MapaRules.ConferenceBlockItem) -> Unit,
  onClearConference: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var confirmClearStep by remember { mutableStateOf(false) }

  LaunchedEffect(confirmClearStep) {
    if (confirmClearStep) {
      delay(5_000)
      confirmClearStep = false
    }
  }

  Column(
    modifier = modifier.fillMaxSize().background(CottonPaperBg),
  ) {
    // Cabeçalho com progresso da conferência
    Surface(
      color = CottonSurface,
      border = BorderStroke(1.dp, CottonSoftOutline),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text =
              "${summary.checkedBlocks.formatBr()} de ${summary.totalBlocks.formatBr()} blocos conferidos (${summary.percent}%)",
            fontFamily = IbmPlexMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = CottonInk,
            modifier = Modifier.weight(1f).testTag("conference_progress_text"),
          )

          if (summary.checkedBlocks > 0 || summary.totalBlocks > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            if (confirmClearStep) {
              Button(
                onClick = {
                  confirmClearStep = false
                  onClearConference()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 44.dp).testTag("confirm_clear_conference_button"),
              ) {
                Text(
                  text = "Confirmar limpar?",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White,
                )
              }
            } else {
              OutlinedButton(
                onClick = { confirmClearStep = true },
                enabled = summary.checkedBlocks > 0,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 44.dp).testTag("clear_conference_button"),
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteSweep,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Limpar conferência",
                  style = MaterialTheme.typography.labelMedium,
                  maxLines = 1,
                )
              }
            }
          }
        }

        LinearProgressIndicator(
          progress = {
            if (summary.totalBlocks == 0) 0f
            else (summary.checkedBlocks.toFloat() / summary.totalBlocks.toFloat()).coerceIn(0f, 1f)
          },
          modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
          color = SuccessGreen,
          trackColor = CottonPaperBg,
        )

        // Busca "Procurar bloco" — só números, pelo começo
        OutlinedTextField(
          value = query,
          onValueChange = { input -> onQueryChange(input.filter { it.isDigit() }) },
          label = { Text("Procurar bloco") },
          placeholder = { Text("Digite o início do número...") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Procurar bloco",
              tint = ForestPrimary,
            )
          },
          trailingIcon = {
            if (query.isNotEmpty()) {
              IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
              }
            }
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("conference_search_input"),
        )

        // Filtros: Todos, Faltam, Conferidos
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          MapaRules.ConferenceFilter.entries.forEach { filter ->
            val selected = activeFilter == filter
            val count =
              when (filter) {
                MapaRules.ConferenceFilter.TODOS -> summary.totalBlocks
                MapaRules.ConferenceFilter.FALTAM -> summary.totalBlocks - summary.checkedBlocks
                MapaRules.ConferenceFilter.CONFERIDOS -> summary.checkedBlocks
              }
            FilterChip(
              selected = selected,
              onClick = { onFilterChange(filter) },
              label = {
                Text(
                  text = "${filter.label} ($count)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
              },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor =
                    if (filter == MapaRules.ConferenceFilter.CONFERIDOS) SuccessContainerLight
                    else MaterialTheme.colorScheme.primaryContainer,
                ),
              modifier = Modifier.heightIn(min = 44.dp).testTag("conference_filter_${filter.name.lowercase()}"),
            )
          }
        }
      }
    }

    if (summary.totalBlocks == 0) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = CottonSurface,
          border = BorderStroke(1.dp, CottonSoftOutline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.Default.CheckBox,
              contentDescription = null,
              modifier = Modifier.size(40.dp),
              tint = ForestPrimary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Nenhum bloco para conferir",
              style = MaterialTheme.typography.titleMedium,
              color = CottonInk,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Adicione blocos nos pátios ou barracões para realizar a conferência.",
              style = MaterialTheme.typography.bodySmall,
              color = CottonMutedInk,
            )
          }
        }
      }
    } else if (filteredItems.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Nenhum bloco encontrado para este filtro ou número.",
          style = MaterialTheme.typography.bodyMedium,
          color = CottonMutedInk,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("conference_list"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(filteredItems, key = { it.blockKey }) { item ->
          ConferenceBlockCard(
            item = item,
            onToggle = { onToggleBlock(item) },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConferenceBlockCard(
  item: MapaRules.ConferenceBlockItem,
  onToggle: () -> Unit,
) {
  val bgColor =
    when {
      item.isChecked -> SuccessContainerLight.copy(alpha = 0.72f)
      item.isOverLimit -> ErrorContainerLight.copy(alpha = 0.65f)
      else -> CottonSurface
    }
  val borderColor =
    when {
      item.isChecked -> SuccessGreen
      item.isOverLimit -> ErrorRed
      else -> CottonOutline
    }

  Surface(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onToggle)
        .testTag("conference_item_${item.blockKey}"),
    shape = RoundedCornerShape(8.dp),
    color = bgColor,
    border = BorderStroke(if (item.isChecked) 1.8.dp else 1.dp, borderColor),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.Top,
    ) {
      // Quadrado com visto verde
      Surface(
        modifier = Modifier.padding(top = 2.dp).size(30.dp),
        shape = RoundedCornerShape(6.dp),
        color = if (item.isChecked) SuccessGreen else Color.Transparent,
        border = BorderStroke(2.dp, if (item.isChecked) SuccessGreen else CottonOutline),
      ) {
        Box(contentAlignment = Alignment.Center) {
          if (item.isChecked) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Bloco conferido",
              tint = Color.White,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        val lugaresLabel = if (item.locationCount == 1) "1 lugar" else "${item.locationCount} lugares"
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "${item.displayNumber} · ${item.totalBales.formatBr()} fardos · $lugaresLabel",
            fontFamily = IbmPlexMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (item.isOverLimit && !item.isChecked) ErrorRed else CottonInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          if (item.isOverLimit) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Passa de 140 fardos",
              tint = ErrorRed,
              modifier = Modifier.size(16.dp),
            )
          }
        }

        if (!item.isChecked && item.checkedLocationsCount > 0) {
          Text(
            text = "Parcial: ${item.checkedLocationsCount}/${item.locationCount} posições com visto (toque para marcar todas)",
            style = MaterialTheme.typography.labelSmall,
            color = ForestPrimary,
          )
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          item.locations.forEach { loc ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color =
                if (loc.slot.checked) SuccessGreen.copy(alpha = 0.14f)
                else CottonPaperBg,
              border =
                BorderStroke(
                  1.dp,
                  if (loc.slot.checked) SuccessGreen.copy(alpha = 0.55f) else CottonSoftOutline,
                ),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (loc.slot.checked) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(12.dp),
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                  text = "${loc.patio.name} · R.${loc.slot.street} P.${loc.slot.positionNumber} (${loc.slot.bales.formatBr()})",
                  fontFamily = IbmPlexMonoFontFamily,
                  fontSize = 11.sp,
                  color = CottonInk,
                )
              }
            }
          }
        }
      }
    }
  }
}
