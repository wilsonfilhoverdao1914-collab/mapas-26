package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.Patio
import com.example.domain.model.PatioKind
import com.example.domain.model.PatioTotals
import com.example.ui.components.formatBr
import com.example.ui.theme.BarracaoAccent
import com.example.ui.theme.BarracaoAccentContainer
import com.example.ui.theme.BarracaoOnAccentContainer

@Composable
fun TotaisScreen(
  patioTotals: List<PatioTotals>,
  grandTotalBlocks: Int,
  grandTotalBales: Int,
  onOpenPatio: (Patio) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
      Text(
        text = "Totais por Pátio e Geral",
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = "Resumo individual de cada pátio/barracão e soma geral abaixo",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (patioTotals.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Nenhum pátio ou barracão cadastrado para totalizar.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      return
    }

    val patiosCount = patioTotals.count { it.patio.kind == PatioKind.PATIO }
    val barracoesCount = patioTotals.count { it.patio.kind == PatioKind.BARRACAO }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(patioTotals, key = { it.patio.id }) { item ->
        val isBarracao = item.patio.kind == PatioKind.BARRACAO
        Surface(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.medium)
              .clickable { onOpenPatio(item.patio) }
              .testTag("totals_patio_card_${item.patio.id}"),
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
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
                  contentDescription = item.patio.kind.label,
                  tint =
                    if (isBarracao) BarracaoOnAccentContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.patio.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${item.patio.kind.label} · ${item.patio.streetRangeLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "${item.totalBales.formatBr()} fardos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isBarracao) BarracaoAccent else MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "${item.blockCount.formatBr()} blocos",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Abaixo de todos: Total Geral
      item(key = "grand_total_card") {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.fillMaxWidth().testTag("grand_total_card"),
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Assessment,
                  contentDescription = null,
                  modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "TOTAL GERAL",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
              Text(
                text = "$patiosCount pátios · $barracoesCount barracões",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
              )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(
                  text = "Blocos mapeados",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
                Text(
                  text = grandTotalBlocks.formatBr(),
                  style = MaterialTheme.typography.headlineMedium,
                  fontWeight = FontWeight.Bold,
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "Total de fardos",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
                Text(
                  text = grandTotalBales.formatBr(),
                  style = MaterialTheme.typography.headlineMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  }
}
