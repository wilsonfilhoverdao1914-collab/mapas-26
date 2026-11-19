package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.Patio
import com.example.domain.model.PatioKind
import com.example.domain.model.PatioTotals
import com.example.ui.theme.BarracaoAccent
import com.example.ui.theme.BarracaoAccentContainer
import com.example.ui.theme.BarracaoOnAccentContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatioSelectorSheet(
  patioTotals: List<PatioTotals>,
  selectedPatioId: String?,
  onSelectPatio: (Patio) -> Unit,
  onCreateClick: () -> Unit,
  onRenameClick: (Patio) -> Unit,
  onDeleteClick: (Patio) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
          .padding(horizontal = 16.dp)
          .padding(bottom = 20.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Selecionar Pátio ou Barracão",
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = "Toque para abrir o mapa ou gerencie os locais",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = {
            onDismiss()
            onCreateClick()
          },
          modifier = Modifier.heightIn(min = 48.dp).testTag("sheet_create_patio_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Novo")
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (patioTotals.isEmpty()) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              Icons.Default.GridView,
              contentDescription = null,
              modifier = Modifier.size(36.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Nenhum pátio ou barracão cadastrado.",
              style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Toque em \"Novo\" para criar o primeiro local.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 8.dp),
        ) {
          items(patioTotals, key = { it.patio.id }) { item ->
            val patio = item.patio
            val isSelected = patio.id == selectedPatioId
            val isBarracao = patio.kind == PatioKind.BARRACAO

            Surface(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(MaterialTheme.shapes.medium)
                  .clickable {
                    onSelectPatio(patio)
                    onDismiss()
                  }
                  .testTag("selector_patio_item_${patio.id}"),
              shape = MaterialTheme.shapes.medium,
              color =
                if (isSelected) {
                  if (isBarracao) BarracaoAccentContainer.copy(alpha = 0.55f)
                  else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
              border =
                BorderStroke(
                  width = if (isSelected) 2.dp else 1.dp,
                  color =
                    if (isSelected) {
                      if (isBarracao) BarracaoAccent else MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    },
                ),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color =
                    if (isBarracao) BarracaoAccentContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(44.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector =
                        if (isBarracao) Icons.Default.Warehouse else Icons.Default.WbSunny,
                      contentDescription = patio.kind.label,
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
                      text = patio.name,
                      style = MaterialTheme.typography.titleMedium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isSelected) {
                      Spacer(modifier = Modifier.width(6.dp))
                      Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selecionado",
                        modifier = Modifier.size(18.dp),
                        tint = if (isBarracao) BarracaoAccent else MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                  Text(
                    text = "${patio.kind.label} · ${patio.streetRangeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = "${item.blockCount.formatBr()} blocos · ${item.totalBales.formatBr()} fardos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }

                IconButton(
                  onClick = { onRenameClick(patio) },
                  modifier = Modifier.size(48.dp).testTag("selector_rename_${patio.id}"),
                ) {
                  Icon(
                    Icons.Default.Edit,
                    contentDescription = "Renomear ${patio.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                IconButton(
                  onClick = { onDeleteClick(patio) },
                  modifier = Modifier.size(48.dp).testTag("selector_delete_${patio.id}"),
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir ${patio.name}",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PatioFormDialog(
  editingPatio: Patio?,
  onConfirm: (name: String, kind: PatioKind, lastLetter: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isEdit = editingPatio != null
  var name by remember(editingPatio) { mutableStateOf(editingPatio?.name.orEmpty()) }
  var kind by remember(editingPatio) { mutableStateOf(editingPatio?.kind ?: PatioKind.PATIO) }
  var lastLetter by remember(editingPatio) {
    mutableStateOf(
      if (editingPatio?.kind == PatioKind.BARRACAO) "B"
      else editingPatio?.normalizedLast?.toString() ?: "M"
    )
  }

  val isValid = name.trim().isNotEmpty()
  val allLetters = remember { ('A'..'Z').map { it.toString() } }

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
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = if (isEdit) "Editar ${editingPatio?.kind?.label}" else "Novo Pátio ou Barracão",
        style = MaterialTheme.typography.titleLarge,
      )

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nome do local") },
        placeholder = { Text(if (kind == PatioKind.PATIO) "Ex.: Pátio Sul" else "Ex.: Barracão 1") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("patio_name_input"),
      )

      if (!isEdit) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Tipo de local",
            style = MaterialTheme.typography.titleSmall,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            KindOptionCard(
              label = "Pátio",
              subtitle = "Ruas A–Z\nAté 50/rua",
              icon = Icons.Default.WbSunny,
              selected = kind == PatioKind.PATIO,
              onClick = { kind = PatioKind.PATIO },
              modifier = Modifier.weight(1f).testTag("kind_patio_option"),
            )
            KindOptionCard(
              label = "Barracão",
              subtitle = "2 ruas (A e B)\nAté 100/rua",
              icon = Icons.Default.Warehouse,
              selected = kind == PatioKind.BARRACAO,
              onClick = { kind = PatioKind.BARRACAO },
              modifier = Modifier.weight(1f).testTag("kind_barracao_option"),
            )
          }
        }
      }

      if (kind == PatioKind.PATIO) {
        val selectedChar = lastLetter.firstOrNull() ?: 'M'
        val streetCount = (selectedChar - 'A') + 1
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Faixa de ruas: A até $lastLetter",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = "$streetCount ruas",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            allLetters.forEach { letter ->
              val isChosen = letter == lastLetter
              val isWithinRange = letter.first() <= selectedChar
              Surface(
                modifier =
                  Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { lastLetter = letter }
                    .testTag("street_range_letter_$letter"),
                shape = RoundedCornerShape(8.dp),
                color =
                  when {
                    isChosen -> MaterialTheme.colorScheme.primary
                    isWithinRange -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                  },
                border =
                  if (isChosen) null
                  else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = letter,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                    color =
                      when {
                        isChosen -> MaterialTheme.colorScheme.onPrimary
                        isWithinRange -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                      },
                  )
                }
              }
            }
          }
        }
      } else {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = BarracaoAccentContainer.copy(alpha = 0.6f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Barracão possui sempre 2 ruas fixas (A e B), com até 100 posições em cada rua.",
            style = MaterialTheme.typography.bodySmall,
            color = BarracaoOnAccentContainer,
            modifier = Modifier.padding(12.dp),
          )
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
          onClick = {
            onConfirm(
              name.trim(),
              kind,
              if (kind == PatioKind.BARRACAO) "B" else lastLetter,
            )
          },
          enabled = isValid,
          modifier = Modifier.weight(1.5f).heightIn(min = 48.dp).testTag("patio_form_confirm"),
        ) {
          Text(if (isEdit) "Salvar" else "Criar")
        }
      }
    }
  }
}

@Composable
private fun KindOptionCard(
  label: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
    shape = MaterialTheme.shapes.medium,
    color =
      if (selected) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    border =
      BorderStroke(
        width = if (selected) 2.dp else 1.dp,
        color =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
      ),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.Start,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color =
          if (selected) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun DeletePatioConfirmDialog(
  patio: Patio,
  blockCount: Int,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Excluir ${patio.kind.label.lowercase()}?") },
    text = {
      val extra =
        if (blockCount > 0) {
          " Todos os $blockCount blocos e posições deste local também serão removidos."
        } else {
          ""
        }
      Text("Tem certeza que deseja excluir \"${patio.name}\"?$extra")
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
        modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_delete_patio_button"),
      ) {
        Text("Excluir")
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Text("Cancelar")
      }
    },
  )
}
