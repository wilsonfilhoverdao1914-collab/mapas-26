package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.Patio
import com.example.domain.model.Slot
import com.example.domain.model.SlotType
import com.example.domain.rules.MapaRules
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestAmberContainer
import com.example.ui.theme.OnHarvestAmberContainer
import com.example.ui.theme.SuccessContainerLight
import com.example.ui.theme.SuccessGreen

data class SlotEditorTarget(
  val patio: Patio,
  val street: String,
  val idx: Int,
  val existingSlot: Slot?,
  val canDeletePosition: Boolean,
  val availableVacantIndices: List<Int> = emptyList(),
  val isReusingVacant: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotEditorSheet(
  target: SlotEditorTarget,
  latestRemoteSlot: Slot?,
  currentUserId: String,
  blockOccurrences: Map<String, MapaRules.BlockOccurrenceSummary>,
  onSave: (
    targetIdx: Int,
    type: SlotType,
    number: String?,
    bales: Int,
    note: String?,
    checked: Boolean,
  ) -> Unit,
  onConvertBlockToVacant: () -> Unit,
  onDeletePosition: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onOpenLoadBales: (Slot) -> Unit,
  onOpenMerge: (Slot) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var baselineSlot by remember(target) { mutableStateOf(target.existingSlot) }
  val initial = baselineSlot
  val maxPositions = MapaRules.maxPositionsFor(target.patio.kind)

  var selectedIdx by remember(target) {
    mutableIntStateOf(target.idx)
  }
  var slotType by remember(target) {
    mutableStateOf(
      when {
        initial == null -> SlotType.BLOCO
        initial.type == SlotType.VAGA -> SlotType.BLOCO
        else -> initial.type
      }
    )
  }
  var numberText by remember(target) {
    mutableStateOf(initial?.number.orEmpty())
  }
  var balesText by remember(target) {
    mutableStateOf(if (initial != null && initial.bales > 0) initial.bales.toString() else "")
  }
  var noteText by remember(target) {
    mutableStateOf(initial?.note.orEmpty())
  }
  var isChecked by remember(target) {
    mutableStateOf(initial?.checked ?: false)
  }
  var showDeleteBlockConfirm by remember { mutableStateOf(false) }
  var showDeletePositionConfirm by remember { mutableStateOf(false) }

  val conflictInfo =
    remember(baselineSlot, latestRemoteSlot, currentUserId, selectedIdx) {
      if (selectedIdx == target.idx) {
        MapaRules.detectRemoteSlotConflict(
          openedSnapshot = baselineSlot,
          latestSlot = latestRemoteSlot,
          currentUserId = currentUserId,
        )
      } else {
        null
      }
    }

  val parsedBales = balesText.trim().toIntOrNull() ?: 0
  val isValid =
    when (slotType) {
      SlotType.VAGA -> true
      SlotType.BLOCO ->
        numberText.trim().isNotEmpty() && parsedBales >= MapaRules.MIN_BALES_PER_POSITION
    }

  val positionExcess = if (slotType == SlotType.BLOCO) MapaRules.excessBales(parsedBales) else 0
  val otherLocationsPreview =
    remember(numberText, parsedBales, selectedIdx, slotType, blockOccurrences) {
      if (slotType == SlotType.BLOCO && numberText.trim().isNotEmpty()) {
        MapaRules.previewOtherLocationsForNumber(
          blockNumber = numberText,
          currentPatioId = target.patio.id,
          currentStreet = target.street,
          currentIdx = selectedIdx,
          currentBales = parsedBales,
          occurrencesIndex = blockOccurrences,
        )
      } else {
        null
      }
    }

  val isEditingExistingBlock = initial != null && initial.type == SlotType.BLOCO
  val checkedInvalidatedByEdit =
    isEditingExistingBlock &&
      MapaRules.shouldInvalidateCheckedOnEdit(
        previousSlot = initial,
        newType = slotType,
        newNumber = numberText.trim().takeIf { it.isNotEmpty() },
        newBales = parsedBales,
      )

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
      Column {
        Text(
          text =
            when {
              initial == null -> "Adicionar Bloco"
              initial.type == SlotType.VAGA -> "Ocupar Vaga / Editar"
              else -> "Editar Bloco ${initial.number.orEmpty()}"
            },
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = "${target.patio.name} · Rua ${target.street} · Posição ${selectedIdx + 1}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Aviso de conflito em tempo real se outro celular alterar esta posição durante a edição
      if (conflictInfo != null && latestRemoteSlot != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = HarvestAmberContainer,
          border = BorderStroke(1.5.dp, HarvestAmber),
          modifier = Modifier.fillMaxWidth().testTag("slot_conflict_banner"),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = OnHarvestAmberContainer,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Posição alterada por outra pessoa",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = OnHarvestAmberContainer,
              )
            }
            Text(
              text = conflictInfo.summaryText(),
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = OnHarvestAmberContainer,
            )
            Text(
              text = "Vale a última alteração ao salvar, ou toque abaixo para carregar o valor novo.",
              style = MaterialTheme.typography.labelSmall,
              color = OnHarvestAmberContainer.copy(alpha = 0.85f),
            )
            OutlinedButton(
              onClick = {
                baselineSlot = latestRemoteSlot
                slotType = latestRemoteSlot.type
                numberText = latestRemoteSlot.number.orEmpty()
                balesText =
                  if (latestRemoteSlot.bales > 0) latestRemoteSlot.bales.toString() else ""
                noteText = latestRemoteSlot.note.orEmpty()
                isChecked = latestRemoteSlot.checked
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.heightIn(min = 44.dp).testTag("reload_remote_slot_button"),
            ) {
              Text("Carregar valor novo")
            }
          }
        }
      }

      // Regra 1: Ao clicar em + Bloco quando há vaga(s) na rua, mostra vaga ocupada e permite escolher
      if (initial == null && target.isReusingVacant && target.availableVacantIndices.isNotEmpty()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text =
                  if (target.availableVacantIndices.size == 1) {
                    "Este bloco ocupará a vaga na Posição ${selectedIdx + 1}."
                  } else {
                    "Há ${target.availableVacantIndices.size} vagas na Rua ${target.street}. Posição escolhida: ${selectedIdx + 1}"
                  },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            if (target.availableVacantIndices.size > 1) {
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                target.availableVacantIndices.forEach { vIdx ->
                  FilterChip(
                    selected = selectedIdx == vIdx,
                    onClick = { selectedIdx = vIdx },
                    label = { Text("Vaga ${vIdx + 1}") },
                    modifier = Modifier.testTag("vacant_choice_$vIdx"),
                  )
                }
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SlotTypeChoice(
          label = "Bloco",
          icon = Icons.Default.Inventory2,
          selected = slotType == SlotType.BLOCO,
          onClick = { slotType = SlotType.BLOCO },
          modifier = Modifier.weight(1f).testTag("slot_type_bloco"),
        )
        SlotTypeChoice(
          label = "Posição Vaga",
          icon = Icons.Default.SpaceBar,
          selected = slotType == SlotType.VAGA,
          onClick = { slotType = SlotType.VAGA },
          modifier = Modifier.weight(1f).testTag("slot_type_vaga"),
        )
      }

      if (slotType == SlotType.BLOCO) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = numberText,
            onValueChange = { raw ->
              numberText = raw.take(16)
              if (
                isEditingExistingBlock &&
                  MapaRules.normalizedBlockKey(raw) != MapaRules.normalizedBlockKey(initial?.number)
              ) {
                isChecked = false
              }
            },
            label = { Text("Nº do Bloco") },
            placeholder = { Text("Ex.: 815") },
            singleLine = true,
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Number,
                capitalization = KeyboardCapitalization.Characters,
              ),
            modifier = Modifier.weight(1.25f).testTag("slot_number_input"),
          )

          OutlinedTextField(
            value = balesText,
            onValueChange = { input ->
              val filtered = input.filter { it.isDigit() }.take(6)
              balesText = filtered
              val nextBales = filtered.trim().toIntOrNull() ?: 0
              if (isEditingExistingBlock && nextBales != (initial?.bales ?: 0)) {
                isChecked = false
              }
            },
            label = { Text("Fardos (1–140)") },
            placeholder = { Text("140") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("slot_bales_input"),
          )
        }

        // Regra 2: Aviso ao passar de 140 fardos
        if (positionExcess > 0) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = ErrorContainerLight,
            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  "Bloco passando fardo (passa $positionExcess do limite de 140). Pode salvar, mas a posição ficará vermelha no mapa.",
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed,
                fontWeight = FontWeight.Medium,
              )
            }
          }
        }

        // Regra 3: Aviso "Bloco em N lugares" + listagem das outras localizações + excesso na soma
        if (otherLocationsPreview != null) {
          val totalPlaces = otherLocationsPreview.locations.size + 1
          val isSumOver = otherLocationsPreview.isTotalOverLimit
          Surface(
            shape = RoundedCornerShape(12.dp),
            color =
              if (isSumOver) ErrorContainerLight
              else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
            border =
              BorderStroke(
                1.dp,
                if (isSumOver) ErrorRed.copy(alpha = 0.65f)
                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
              ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "Bloco em $totalPlaces lugares",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color =
                  if (isSumOver) ErrorRed
                  else MaterialTheme.colorScheme.onSecondaryContainer,
              )
              otherLocationsPreview.locations.forEach { other ->
                Text(
                  text =
                    "• ${other.patio.name} · Rua ${other.slot.street} · Pos. ${other.slot.positionNumber} — ${other.slot.bales.formatBr()} fardos",
                  style = MaterialTheme.typography.bodySmall,
                  color =
                    if (isSumOver) ErrorRed
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                )
              }
              Text(
                text =
                  if (isSumOver) {
                    "Soma total: ${otherLocationsPreview.totalBales.formatBr()} fardos — excesso de ${otherLocationsPreview.totalExcess} fardos!"
                  } else {
                    "Soma total nos $totalPlaces lugares: ${otherLocationsPreview.totalBales.formatBr()} fardos."
                  },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color =
                  if (isSumOver) ErrorRed
                  else MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }

        Surface(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { isChecked = !isChecked }
              .testTag("slot_checked_toggle"),
          shape = RoundedCornerShape(12.dp),
          color =
            if (isChecked) SuccessContainerLight.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          border =
            BorderStroke(
              1.dp,
              if (isChecked) SuccessGreen.copy(alpha = 0.6f)
              else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            ),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                Icons.Default.Verified,
                contentDescription = null,
                tint = if (isChecked) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Bloco conferido",
                  style = MaterialTheme.typography.titleSmall,
                )
                Text(
                  text =
                    if (checkedInvalidatedByEdit) {
                      "Alterar nº ou fardos remove o visto da conferência"
                    } else {
                      "Marca este bloco como conferido no pátio"
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Switch(
              checked = isChecked,
              onCheckedChange = { isChecked = it },
              enabled = !checkedInvalidatedByEdit,
            )
          }
        }

        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Observação (opcional)") },
          placeholder = { Text("Ex.: Lote com umidade ou fardos avariados") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth().testTag("slot_note_input"),
        )
      } else {
        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Observação da vaga (opcional)") },
          placeholder = { Text("Ex.: Espaço reservado para descarga") },
          minLines = 2,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("slot_note_input"),
        )
      }

      Button(
        onClick = {
          onSave(
            selectedIdx,
            slotType,
            numberText.trim().takeIf { it.isNotEmpty() },
            parsedBales,
            noteText.trim().takeIf { it.isNotEmpty() },
            isChecked,
          )
        },
        enabled = isValid,
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("save_slot_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (slotType == SlotType.BLOCO) "Salvar Bloco" else "Salvar como Vaga",
          style = MaterialTheme.typography.titleSmall,
        )
      }

      // Ações para posições já existentes (Regras 4, 5 e 7)
      if (initial != null) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        if (isEditingExistingBlock) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = { onOpenLoadBales(initial) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).heightIn(min = 50.dp).testTag("load_bales_button"),
            ) {
              Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Carregado")
            }

            OutlinedButton(
              onClick = { onOpenMerge(initial) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).heightIn(min = 50.dp).testTag("merge_from_slot_button"),
            ) {
              Icon(Icons.Default.Merge, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Juntar")
            }
          }
        }

        // Regra 7: Mover para cima / para baixo
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onMoveUp,
            enabled = selectedIdx > 0,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("move_up_button"),
          ) {
            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mover ↑")
          }

          OutlinedButton(
            onClick = onMoveDown,
            enabled = selectedIdx + 1 < maxPositions,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("move_down_button"),
          ) {
            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mover ↓")
          }
        }

        // Regra 7: Excluir bloco (posição vira VAGA) com confirmação obrigatória
        if (isEditingExistingBlock) {
          OutlinedButton(
            onClick = { showDeleteBlockConfirm = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("delete_block_to_vacant_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Excluir bloco (virar Vaga)")
          }
        } else if (target.canDeletePosition) {
          OutlinedButton(
            onClick = { showDeletePositionConfirm = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("delete_slot_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Remover última vaga da rua")
          }
        }
      }
    }
  }

  if (showDeleteBlockConfirm && initial != null) {
    AlertDialog(
      onDismissRequest = { showDeleteBlockConfirm = false },
      title = { Text("Excluir bloco?") },
      text = {
        Text(
          "Deseja excluir o bloco ${initial.number.orEmpty()} (${initial.bales.formatBr()} fardos)? A posição na Rua ${target.street}, Posição ${selectedIdx + 1} ficará como VAGA."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteBlockConfirm = false
            onConvertBlockToVacant()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
          modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_delete_block_button"),
        ) {
          Text("Excluir bloco")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showDeleteBlockConfirm = false },
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text("Cancelar")
        }
      },
    )
  }

  if (showDeletePositionConfirm) {
    AlertDialog(
      onDismissRequest = { showDeletePositionConfirm = false },
      title = { Text("Remover posição?") },
      text = {
        Text(
          "Deseja remover a última posição vaga (Rua ${target.street}, Posição ${selectedIdx + 1})?"
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeletePositionConfirm = false
            onDeletePosition()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text("Remover")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showDeletePositionConfirm = false },
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text("Cancelar")
        }
      },
    )
  }
}

@Composable
private fun SlotTypeChoice(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color =
      if (selected) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border =
      BorderStroke(
        width = if (selected) 2.dp else 1.dp,
        color =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
      ),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color =
          if (selected) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
