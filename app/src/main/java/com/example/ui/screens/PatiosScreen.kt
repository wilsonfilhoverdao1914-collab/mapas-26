package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.Patio
import com.example.domain.model.PatioKind
import com.example.domain.model.PatioTotals
import com.example.ui.components.formatBr
import com.example.ui.theme.BarracaoAccent
import com.example.ui.theme.BarracaoAccentContainer
import com.example.ui.theme.BarracaoOnAccentContainer

@Composable
fun PatiosScreen(
  patioTotals: List<PatioTotals>,
  selectedPatioId: String?,
  onCreateClick: () -> Unit,
  onSelectAndOpenMap: (Patio) -> Unit,
  onRenameClick: (Patio) -> Unit,
  onDeleteClick: (Patio) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Pátios e Barracões",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "${patioTotals.size} locais cadastrados",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Button(
        onClick = onCreateClick,
        modifier = Modifier.height(50.dp).testTag("patios_create_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Criar")
      }
    }

    if (patioTotals.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Toque em \"Criar\" para adicionar seu primeiro pátio ou barracão.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(patioTotals, key = { it.patio.id }) { item ->
          val patio = item.patio
          val isSelected = patio.id == selectedPatioId
          val isBarracao = patio.kind == PatioKind.BARRACAO

          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border =
              BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color =
                  if (isSelected) {
                    if (isBarracao) BarracaoAccent else MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                  },
              ),
            tonalElevation = if (isSelected) 2.dp else 0.dp,
            modifier = Modifier.fillMaxWidth().testTag("patio_card_${patio.id}"),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color =
                    if (isBarracao) BarracaoAccentContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(48.dp),
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
                      style = MaterialTheme.typography.titleLarge,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isSelected) {
                      Spacer(modifier = Modifier.width(6.dp))
                      Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Ativo no mapa",
                        modifier = Modifier.size(18.dp),
                        tint = if (isBarracao) BarracaoAccent else MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                  Text(
                    text =
                      "${patio.kind.label} · ${patio.streetRangeLabel} · até ${patio.kind.maxPositionsPerStreet}/rua",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "${item.blockCount.formatBr()} blocos",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${item.totalBales.formatBr()} fardos",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isBarracao) BarracaoAccent else MaterialTheme.colorScheme.primary,
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilledTonalButton(
                  onClick = { onSelectAndOpenMap(patio) },
                  modifier = Modifier.weight(1.3f).heightIn(min = 48.dp),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Abrir")
                }

                OutlinedButton(
                  onClick = { onRenameClick(patio) },
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Editar")
                }

                OutlinedButton(
                  onClick = { onDeleteClick(patio) },
                  colors =
                    ButtonDefaults.outlinedButtonColors(
                      contentColor = MaterialTheme.colorScheme.error,
                    ),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                  modifier = Modifier.heightIn(min = 48.dp),
                  shape = RoundedCornerShape(12.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir ${patio.name}",
                    modifier = Modifier.size(20.dp),
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
