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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.UserProfile
import com.example.domain.rules.MapaRules
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestAmberContainer
import com.example.ui.theme.OnHarvestAmberContainer

@Composable
fun HistoricoScreen(
  historyGroups: List<MapaRules.HistoryDayGroup>,
  usersById: Map<String, UserProfile>,
  isLoading: Boolean,
  modifier: Modifier = Modifier,
) {
  if (isLoading && historyGroups.isEmpty()) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  val totalDisplayed = historyGroups.sumOf { it.entries.size }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 680.dp).testTag("history_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item(key = "header") {
        Surface(
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.History,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Histórico de alterações",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  if (totalDisplayed == 0) "Registro permanente das movimentações"
                  else "Mostrando os últimos $totalDisplayed registros (mais novos primeiro)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      if (historyGroups.isEmpty()) {
        item(key = "empty_history") {
          Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Nenhuma alteração registrada ainda",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text =
                  "Quando alguém criar, editar, carregar ou juntar blocos, o registro aparecerá aqui.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      } else {
        historyGroups.forEach { group ->
          item(key = "day_${group.dayKey}") {
            Text(
              text = group.dayLabel,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
          }

          items(
            items = group.entries,
            key = { it.id },
          ) { entry ->
            val authorName = MapaRules.resolveAuthorName(entry.userId, usersById)
            val timeText = MapaRules.formatHourMinute(entry.ts)

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
              modifier = Modifier.fillMaxWidth().testTag("history_item_${entry.id}"),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = entry.text,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    Icon(
                      imageVector = Icons.Default.Person,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                      tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (timeText.isNotEmpty()) "$authorName · $timeText" else authorName,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }

                  if (entry.patioName.isNotBlank()) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Icon(
                          imageVector = Icons.Default.Place,
                          contentDescription = null,
                          modifier = Modifier.size(12.dp),
                          tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                          text = entry.patioName,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                  }

                  if (entry.isPending) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = HarvestAmberContainer,
                      border = BorderStroke(1.dp, HarvestAmber.copy(alpha = 0.5f)),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Icon(
                          imageVector = Icons.Default.CloudUpload,
                          contentDescription = "Aguardando envio",
                          modifier = Modifier.size(12.dp),
                          tint = OnHarvestAmberContainer,
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                          text = "Pendente",
                          style = MaterialTheme.typography.labelSmall,
                          color = OnHarvestAmberContainer,
                          fontWeight = FontWeight.SemiBold,
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
    }
  }
}
