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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.rules.MapaRules
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessContainerLight
import com.example.ui.theme.SuccessGreen

@Composable
fun CorrectBlockAlertDialog(
  alert: MapaRules.SlotAlertState,
  patioName: String,
  onEditClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        tint = ErrorRed,
      )
    },
    title = { Text("Corrigir bloco") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text =
            "Bloco ${alert.slot.number.orEmpty()} · $patioName · Rua ${alert.slot.street} · Posição ${alert.slot.positionNumber}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = ErrorContainerLight,
          border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = alert.correctionDialogMessage(),
            style = MaterialTheme.typography.bodySmall,
            color = ErrorRed,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(10.dp),
          )
        }

        if (alert.isMultiLocation && alert.otherLocations.isNotEmpty()) {
          Text(
            text = "Outras localizações deste bloco:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
          alert.otherLocations.forEach { entry ->
            Text(
              text =
                "• ${entry.patio.name} · Rua ${entry.slot.street} · Pos. ${entry.slot.positionNumber} (${entry.slot.bales.formatBr()} fardos)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onEditClick,
        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
        modifier = Modifier.heightIn(min = 48.dp).testTag("correct_block_edit_button"),
      ) {
        Text("Editar bloco")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Text("Fechar")
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadBalesDialog(
  blockNumber: String,
  locationLabel: String,
  currentBales: Int,
  onConfirmLoad: (loadedBales: Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var loadedText by remember { mutableStateOf("") }
  val parsedLoaded = loadedText.trim().toIntOrNull() ?: 0
  val validation = MapaRules.validateLoadedBales(currentBales, parsedLoaded)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.LocalShipping,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Bloco carregado", style = MaterialTheme.typography.titleLarge)
      }

      Text(
        text = "Bloco $blockNumber · $locationLabel",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "Fardos atuais no bloco: ${currentBales.formatBr()}",
        style = MaterialTheme.typography.bodyMedium,
      )

      OutlinedTextField(
        value = loadedText,
        onValueChange = { input -> loadedText = input.filter { it.isDigit() }.take(6) },
        label = { Text("Fardos carregados") },
        placeholder = { Text("Ex.: 50") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().testTag("loaded_bales_input"),
      )

      if (loadedText.isNotEmpty()) {
        when (validation) {
          is MapaRules.LoadResult.Invalid -> {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = ErrorContainerLight,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = validation.message,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
          is MapaRules.LoadResult.Valid -> {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color =
                if (validation.becomesVacant) MaterialTheme.colorScheme.secondaryContainer
                else SuccessContainerLight,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text =
                  if (validation.becomesVacant) {
                    "Todos os ${currentBales.formatBr()} fardos carregados: o bloco será excluído e a posição virará VAGA no mesmo lugar da rua."
                  } else {
                    "Ficam ${validation.remainingBales.formatBr()} fardos neste bloco."
                  },
                style = MaterialTheme.typography.bodySmall,
                color =
                  if (validation.becomesVacant) MaterialTheme.colorScheme.onSecondaryContainer
                  else SuccessGreen,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) {
          Text("Cancelar")
        }
        Button(
          onClick = { onConfirmLoad(parsedLoaded) },
          enabled = validation is MapaRules.LoadResult.Valid,
          modifier = Modifier.weight(1.5f).heightIn(min = 48.dp).testTag("confirm_load_bales_button"),
        ) {
          Text("Confirmar")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeBlocksDialog(
  allBlocks: List<MapaRules.BlockLocationEntry>,
  initialDestination: MapaRules.BlockLocationEntry?,
  onConfirmMerge: (
    destination: MapaRules.BlockLocationEntry,
    sources: List<MapaRules.BlockLocationEntry>,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var destination by remember(allBlocks, initialDestination) {
    mutableStateOf(
      initialDestination?.let { init ->
        allBlocks.firstOrNull { it.slot.docId == init.slot.docId } ?: init
      } ?: allBlocks.firstOrNull()
    )
  }
  var selectedSourceIds by remember(destination?.slot?.docId) {
    mutableStateOf(emptySet<String>())
  }
  var destinationMenuExpanded by remember { mutableStateOf(false) }
  var showFinalConfirm by remember { mutableStateOf(false) }

  val availableSources =
    remember(allBlocks, destination) {
      allBlocks.filterNot { it.slot.docId == destination?.slot?.docId }
    }
  val selectedSources =
    remember(availableSources, selectedSourceIds) {
      availableSources.filter { it.slot.docId in selectedSourceIds }
    }

  val preview =
    remember(destination, selectedSources) {
      val dest = destination
      if (dest != null && selectedSources.isNotEmpty()) {
        MapaRules.previewMerge(dest, selectedSources)
      } else {
        null
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Merge,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Juntar fardos", style = MaterialTheme.typography.titleLarge)
      }

      if (allBlocks.size < 2) {
        Text(
          text = "É necessário ter pelo menos 2 blocos mapeados para juntar fardos.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Text(
          text = "Bloco de destino (recebe os fardos):",
          style = MaterialTheme.typography.labelLarge,
        )

        Box(modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(
            onClick = { destinationMenuExpanded = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("merge_dest_selector"),
          ) {
            val dest = destination
            Text(
              text =
                if (dest != null) {
                  "Bloco ${dest.slot.number.orEmpty()} (${dest.slot.bales.formatBr()} f) · ${dest.patio.name} · R.${dest.slot.street} P.${dest.slot.positionNumber}"
                } else {
                  "Selecionar destino"
                },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
          }

          DropdownMenu(
            expanded = destinationMenuExpanded,
            onDismissRequest = { destinationMenuExpanded = false },
          ) {
            allBlocks.forEach { entry ->
              DropdownMenuItem(
                text = {
                  Text(
                    "Bloco ${entry.slot.number.orEmpty()} (${entry.slot.bales.formatBr()} f) · ${entry.patio.name} · Rua ${entry.slot.street} Pos. ${entry.slot.positionNumber}"
                  )
                },
                onClick = {
                  destination = entry
                  destinationMenuExpanded = false
                },
              )
            }
          }
        }

        Text(
          text = "Blocos de origem (virarão VAGA):",
          style = MaterialTheme.typography.labelLarge,
        )

        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(availableSources, key = { it.slot.docId }) { entry ->
            val checked = entry.slot.docId in selectedSourceIds
            Surface(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    selectedSourceIds =
                      if (checked) selectedSourceIds - entry.slot.docId
                      else selectedSourceIds + entry.slot.docId
                  }
                  .testTag("merge_source_${entry.slot.docId}"),
              shape = RoundedCornerShape(10.dp),
              color =
                if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              border =
                BorderStroke(
                  1.dp,
                  if (checked) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                ),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(
                  checked = checked,
                  onCheckedChange = { isNowChecked ->
                    selectedSourceIds =
                      if (isNowChecked) selectedSourceIds + entry.slot.docId
                      else selectedSourceIds - entry.slot.docId
                  },
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Bloco ${entry.slot.number.orEmpty()} · ${entry.slot.bales.formatBr()} fardos",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${entry.patio.name} · Rua ${entry.slot.street} · Pos. ${entry.slot.positionNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }

        if (preview != null) {
          HorizontalDivider()
          Surface(
            shape = RoundedCornerShape(12.dp),
            color =
              if (preview.isOverLimit) ErrorContainerLight
              else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            border =
              BorderStroke(
                1.dp,
                if (preview.isOverLimit) ErrorRed.copy(alpha = 0.65f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
              ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "Prévia da junção",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color =
                  if (preview.isOverLimit) ErrorRed
                  else MaterialTheme.colorScheme.onPrimaryContainer,
              )
              val dest = preview.destination
              val sourcesSummary =
                preview.sources.joinToString(" + ") {
                  "${it.slot.number.orEmpty()} (${it.slot.bales})"
                }
              Text(
                text =
                  "Bloco ${dest.slot.number.orEmpty()} (${dest.slot.bales}) + $sourcesSummary = ${preview.resultingBales.formatBr()} fardos",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text =
                  "${preview.sources.size} ${if (preview.sources.size == 1) "posição de origem virará VAGA" else "posições de origem virarão VAGA"}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (preview.isOverLimit) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text =
                    "Aviso: passa ${preview.excessOverLimit} fardos do limite de ${MapaRules.MAX_BALES_PER_POSITION}!",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = ErrorRed,
                )
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) {
          Text("Cancelar")
        }
        Button(
          onClick = { showFinalConfirm = true },
          enabled = preview != null,
          modifier = Modifier.weight(1.5f).heightIn(min = 48.dp).testTag("merge_preview_continue_button"),
        ) {
          Text("Juntar fardos")
        }
      }
    }
  }

  if (showFinalConfirm && preview != null) {
    val dest = preview.destination
    AlertDialog(
      onDismissRequest = { showFinalConfirm = false },
      title = { Text("Confirmar junção de fardos?") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text =
              "O bloco ${dest.slot.number.orEmpty()} (${dest.patio.name} · Rua ${dest.slot.street} · Pos. ${dest.slot.positionNumber}) passará para ${preview.resultingBales.formatBr()} fardos."
          )
          Text(
            text =
              "As ${preview.sources.size} ${if (preview.sources.size == 1) "posição de origem selecionada virará VAGA" else "posições de origem selecionadas virarão VAGA"}."
          )
          if (preview.isOverLimit) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = ErrorContainerLight,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text =
                  "Atenção: este bloco ficará com ${preview.resultingBales} fardos (passa ${preview.excessOverLimit} do limite de 140).",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = ErrorRed,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showFinalConfirm = false
            onConfirmMerge(preview.destination, preview.sources)
          },
          modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_merge_button"),
        ) {
          Text("Confirmar junção")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showFinalConfirm = false },
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text("Voltar")
        }
      },
    )
  }
}
