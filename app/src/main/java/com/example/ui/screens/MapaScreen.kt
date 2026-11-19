package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Patio
import com.example.domain.model.PatioKind
import com.example.domain.model.PatioTotals
import com.example.domain.model.Slot
import com.example.domain.model.SlotType
import com.example.domain.model.StreetColumnData
import com.example.domain.model.UserProfile
import com.example.domain.rules.MapaRules
import com.example.ui.HighlightedSlot
import com.example.ui.components.SlotEditorTarget
import com.example.ui.components.formatBr
import com.example.ui.theme.BarracaoAccent
import com.example.ui.theme.BarracaoAccentContainer
import com.example.ui.theme.BarracaoBg
import com.example.ui.theme.BarracaoBorder
import com.example.ui.theme.BarracaoHeader
import com.example.ui.theme.BarracaoNotebookLine
import com.example.ui.theme.BarracaoOnAccentContainer
import com.example.ui.theme.BarracaoRoofDark
import com.example.ui.theme.BarracaoSurface
import com.example.ui.theme.CottonInk
import com.example.ui.theme.CottonMutedInk
import com.example.ui.theme.CottonOutline
import com.example.ui.theme.CottonPaperBg
import com.example.ui.theme.CottonSoftOutline
import com.example.ui.theme.CottonSurface
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ForestPrimary
import com.example.ui.theme.HighlightGold
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.NotebookLineGray
import com.example.ui.theme.RepeatBadgeBg
import com.example.ui.theme.RepeatBadgeBorder
import com.example.ui.theme.RepeatBadgeInk
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VacantBorder
import com.example.ui.theme.VacantFill
import com.example.ui.theme.VacantText
import kotlin.math.roundToInt

@Composable
fun MapaScreen(
  patio: Patio?,
  patioTotals: PatioTotals?,
  streetColumns: List<StreetColumnData>,
  blockOccurrences: Map<String, MapaRules.BlockOccurrenceSummary>,
  usersById: Map<String, UserProfile> = emptyMap(),
  canMergeBlocks: Boolean,
  highlightedSlot: HighlightedSlot?,
  isLoading: Boolean,
  onOpenSlotEditor: (SlotEditorTarget) -> Unit,
  onOpenCorrectBlockDialog: (MapaRules.SlotAlertState, Patio) -> Unit,
  onOpenMergeDialog: () -> Unit,
  onCreateFirstPatio: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isLoading) {
    Box(modifier = modifier.fillMaxSize().background(CottonPaperBg), contentAlignment = Alignment.Center) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Carregando mapa...",
          style = MaterialTheme.typography.bodyMedium,
          color = CottonMutedInk,
        )
      }
    }
    return
  }

  if (patio == null) {
    EmptyMapaState(
      onCreateFirstPatio = onCreateFirstPatio,
      modifier = modifier.background(CottonPaperBg),
    )
    return
  }

  val isBarracao = patio.kind == PatioKind.BARRACAO
  val rowState = rememberLazyListState()
  var zoomScale by rememberSaveable { mutableFloatStateOf(1f) }

  LaunchedEffect(highlightedSlot, patio.id, streetColumns) {
    if (highlightedSlot != null && highlightedSlot.patioId == patio.id) {
      val streetIndex = streetColumns.indexOfFirst {
        it.street.equals(highlightedSlot.street, ignoreCase = true)
      }
      if (streetIndex >= 0) {
        rowState.animateScrollToItem(streetIndex)
      }
    }
  }

  val canvasBg = if (isBarracao) BarracaoBg else CottonPaperBg
  val notebookLineColor = if (isBarracao) BarracaoNotebookLine else NotebookLineGray

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(canvasBg)
        .drawBehind {
          // Linhas de caderno cinza claro no fundo de papel
          val lineSpacing = (28f * zoomScale).dp.toPx()
          var y = lineSpacing
          while (y < size.height) {
            drawLine(
              color = notebookLineColor.copy(alpha = 0.75f),
              start = Offset(0f, y),
              end = Offset(size.width, y),
              strokeWidth = 1.dp.toPx(),
            )
            y += lineSpacing
          }
        },
  ) {
    if (isBarracao) {
      BarracaoRoofHeader(
        patio = patio,
        totals = patioTotals,
        zoomScale = zoomScale,
        canMergeBlocks = canMergeBlocks,
        onZoomOut = { zoomScale = (zoomScale - 0.15f).coerceIn(0.75f, 1.45f) },
        onZoomIn = { zoomScale = (zoomScale + 0.15f).coerceIn(0.75f, 1.45f) },
        onOpenMergeDialog = onOpenMergeDialog,
      )
    } else {
      PatioContextBanner(
        patio = patio,
        totals = patioTotals,
        zoomScale = zoomScale,
        canMergeBlocks = canMergeBlocks,
        onZoomOut = { zoomScale = (zoomScale - 0.15f).coerceIn(0.75f, 1.45f) },
        onZoomIn = { zoomScale = (zoomScale + 0.15f).coerceIn(0.75f, 1.45f) },
        onOpenMergeDialog = onOpenMergeDialog,
      )
    }

    BoxWithConstraints(
      modifier =
        Modifier
          .fillMaxSize()
          .pointerInput(patio.id) {
            // Zoom com dois dedos sem bloquear a rolagem com um dedo
            awaitEachGesture {
              awaitFirstDown(requireUnconsumed = false)
              do {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } >= 2) {
                  val zoomFactor = event.calculateZoom()
                  if (zoomFactor != 1f) {
                    zoomScale = (zoomScale * zoomFactor).coerceIn(0.75f, 1.45f)
                    event.changes.forEach { it.consume() }
                  }
                }
              } while (event.changes.any { it.pressed })
            }
          },
    ) {
      val availableWidth = maxWidth
      if (isBarracao) {
        // Barracão: somente 2 colunas grandes (A e B) ocupando a largura da tela sem rolagem horizontal
        val horizontalPadding = 10.dp
        val gap = 8.dp
        val columnWidth = ((availableWidth - (horizontalPadding * 2) - gap) / 2).coerceAtLeast(150.dp)
        Row(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(horizontal = horizontalPadding, vertical = 10.dp)
              .testTag("barracao_two_columns"),
          horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
          streetColumns.take(2).forEach { streetData ->
            StreetColumnCard(
              patio = patio,
              streetData = streetData,
              blockOccurrences = blockOccurrences,
              usersById = usersById,
              isBarracao = true,
              zoomScale = zoomScale,
              columnWidth = columnWidth,
              highlightedIdx =
                if (
                  highlightedSlot?.patioId == patio.id &&
                    highlightedSlot.street.equals(streetData.street, ignoreCase = true)
                ) {
                  highlightedSlot.idx
                } else {
                  null
                },
              onSlotClick = { slot, alert, isLast ->
                if (alert != null && alert.isRedAlert) {
                  onOpenCorrectBlockDialog(alert, patio)
                } else {
                  onOpenSlotEditor(
                    SlotEditorTarget(
                      patio = patio,
                      street = streetData.street,
                      idx = slot.idx,
                      existingSlot = slot,
                      canDeletePosition = isLast,
                    )
                  )
                }
              },
              onAddBlockClick = {
                val target =
                  MapaRules.resolveTargetForNewBlock(
                    streetSlots = streetData.slots,
                    maxPositions = streetData.maxPositions,
                  )
                if (target is MapaRules.AddBlockTarget.SlotIndex) {
                  onOpenSlotEditor(
                    SlotEditorTarget(
                      patio = patio,
                      street = streetData.street,
                      idx = target.idx,
                      existingSlot = null,
                      canDeletePosition = false,
                      availableVacantIndices = target.availableVacantIndices,
                      isReusingVacant = target.isReusingVacant,
                    )
                  )
                }
              },
              modifier = Modifier.weight(1f),
            )
          }
        }
      } else {
        val baseWidth = if (availableWidth >= 600.dp) 216.dp else 186.dp
        val columnWidth: Dp = (baseWidth * zoomScale).coerceIn(142.dp, 280.dp)

        LazyRow(
          state = rowState,
          modifier = Modifier.fillMaxSize().testTag("streets_horizontal_list"),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(streetColumns, key = { "${patio.id}_${it.street}" }) { streetData ->
            StreetColumnCard(
              patio = patio,
              streetData = streetData,
              blockOccurrences = blockOccurrences,
              usersById = usersById,
              isBarracao = false,
              zoomScale = zoomScale,
              columnWidth = columnWidth,
              highlightedIdx =
                if (
                  highlightedSlot?.patioId == patio.id &&
                    highlightedSlot.street.equals(streetData.street, ignoreCase = true)
                ) {
                  highlightedSlot.idx
                } else {
                  null
                },
              onSlotClick = { slot, alert, isLast ->
                if (alert != null && alert.isRedAlert) {
                  onOpenCorrectBlockDialog(alert, patio)
                } else {
                  onOpenSlotEditor(
                    SlotEditorTarget(
                      patio = patio,
                      street = streetData.street,
                      idx = slot.idx,
                      existingSlot = slot,
                      canDeletePosition = isLast,
                    )
                  )
                }
              },
              onAddBlockClick = {
                val target =
                  MapaRules.resolveTargetForNewBlock(
                    streetSlots = streetData.slots,
                    maxPositions = streetData.maxPositions,
                  )
                if (target is MapaRules.AddBlockTarget.SlotIndex) {
                  onOpenSlotEditor(
                    SlotEditorTarget(
                      patio = patio,
                      street = streetData.street,
                      idx = target.idx,
                      existingSlot = null,
                      canDeletePosition = false,
                      availableVacantIndices = target.availableVacantIndices,
                      isReusingVacant = target.isReusingVacant,
                    )
                  )
                }
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PatioContextBanner(
  patio: Patio,
  totals: PatioTotals?,
  zoomScale: Float,
  canMergeBlocks: Boolean,
  onZoomOut: () -> Unit,
  onZoomIn: () -> Unit,
  onOpenMergeDialog: () -> Unit,
) {
  Surface(
    color = CottonSurface.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, CottonSoftOutline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Default.WbSunny,
            contentDescription = patio.kind.label,
            tint = ForestPrimary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${patio.kind.label} · ${patio.streetRangeLabel} · ${(totals?.blockCount ?: 0).formatBr()} bl · ${(totals?.totalBales ?: 0).formatBr()} f",
            fontFamily = IbmPlexMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CottonInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        ZoomControlsPill(
          zoomScale = zoomScale,
          isBarracao = false,
          onZoomOut = onZoomOut,
          onZoomIn = onZoomIn,
        )
      }

      if (canMergeBlocks) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = ForestPrimary,
            modifier =
              Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenMergeDialog)
                .testTag("banner_merge_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Default.Merge,
                contentDescription = "Juntar fardos",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Juntar fardos",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BarracaoRoofHeader(
  patio: Patio,
  totals: PatioTotals?,
  zoomScale: Float,
  canMergeBlocks: Boolean,
  onZoomOut: () -> Unit,
  onZoomIn: () -> Unit,
  onOpenMergeDialog: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    // Telhado angular desenhado (visual de barracão coberto)
    Box(
      modifier = Modifier.fillMaxWidth().height(20.dp).background(BarracaoBg),
      contentAlignment = Alignment.BottomCenter,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val roofPath =
          Path().apply {
            moveTo(0f, h)
            lineTo(w * 0.5f, 2.dp.toPx())
            lineTo(w, h)
            close()
          }
        drawPath(path = roofPath, color = BarracaoRoofDark)
        drawLine(
          color = Color.White.copy(alpha = 0.35f),
          start = Offset(0f, h - 1.dp.toPx()),
          end = Offset(w * 0.5f, 3.dp.toPx()),
          strokeWidth = 2.dp.toPx(),
        )
        drawLine(
          color = Color.White.copy(alpha = 0.35f),
          start = Offset(w * 0.5f, 3.dp.toPx()),
          end = Offset(w, h - 1.dp.toPx()),
          strokeWidth = 2.dp.toPx(),
        )
      }
    }

    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(BarracaoHeader)
          .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Default.Warehouse,
              contentDescription = patio.kind.label,
              tint = Color.White,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Barracão · A e B · ${(totals?.blockCount ?: 0).formatBr()} bl · ${(totals?.totalBales ?: 0).formatBr()} f",
              fontFamily = IbmPlexMonoFontFamily,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          ZoomControlsPill(
            zoomScale = zoomScale,
            isBarracao = true,
            onZoomOut = onZoomOut,
            onZoomIn = onZoomIn,
          )
        }

        if (canMergeBlocks) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color.White.copy(alpha = 0.18f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
              modifier =
                Modifier
                  .heightIn(min = 44.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .clickable(onClick = onOpenMergeDialog)
                  .testTag("banner_merge_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Default.Merge,
                  contentDescription = "Juntar fardos",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Juntar fardos",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ZoomControlsPill(
  zoomScale: Float,
  isBarracao: Boolean,
  onZoomOut: () -> Unit,
  onZoomIn: () -> Unit,
) {
  val containerColor =
    if (isBarracao) Color.White.copy(alpha = 0.15f)
    else CottonPaperBg
  val borderColor =
    if (isBarracao) Color.White.copy(alpha = 0.35f)
    else CottonOutline
  val ink = if (isBarracao) Color.White else CottonInk

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier
            .size(width = 44.dp, height = 44.dp)
            .clickable(enabled = zoomScale > 0.76f, onClick = onZoomOut)
            .testTag("zoom_out_button"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Remove,
          contentDescription = "Diminuir zoom",
          tint = ink.copy(alpha = if (zoomScale > 0.76f) 1f else 0.4f),
          modifier = Modifier.size(18.dp),
        )
      }
      Text(
        text = "${(zoomScale * 100f).roundToInt()}%",
        fontFamily = IbmPlexMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = ink,
        modifier = Modifier.padding(horizontal = 2.dp),
      )
      Box(
        modifier =
          Modifier
            .size(width = 44.dp, height = 44.dp)
            .clickable(enabled = zoomScale < 1.44f, onClick = onZoomIn)
            .testTag("zoom_in_button"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Aumentar zoom",
          tint = ink.copy(alpha = if (zoomScale < 1.44f) 1f else 0.4f),
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun StreetColumnCard(
  patio: Patio,
  streetData: StreetColumnData,
  blockOccurrences: Map<String, MapaRules.BlockOccurrenceSummary>,
  usersById: Map<String, UserProfile>,
  isBarracao: Boolean,
  zoomScale: Float,
  columnWidth: Dp,
  highlightedIdx: Int?,
  onSlotClick: (Slot, MapaRules.SlotAlertState?, isLast: Boolean) -> Unit,
  onAddBlockClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val columnListState = rememberLazyListState()

  LaunchedEffect(highlightedIdx) {
    if (highlightedIdx != null) {
      val posIndex = streetData.slots.indexOfFirst { it.idx == highlightedIdx }
      if (posIndex >= 0) {
        columnListState.animateScrollToItem(posIndex)
      }
    }
  }

  val cardBg = if (isBarracao) BarracaoSurface else CottonSurface.copy(alpha = 0.92f)
  val borderColor = if (isBarracao) BarracaoBorder else CottonOutline
  val headerBg = if (isBarracao) BarracaoAccentContainer else CottonPaperBg
  val headerInk = if (isBarracao) BarracaoOnAccentContainer else CottonInk

  Surface(
    modifier =
      modifier
        .width(columnWidth)
        .fillMaxHeight()
        .testTag("street_column_${streetData.street}"),
    shape = RoundedCornerShape(8.dp),
    color = cardBg,
    border = BorderStroke(1.2.dp, borderColor),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Cabeçalho de rua com letra grande num quadrado de borda grossa
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(headerBg)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          modifier = Modifier.size((38f * zoomScale.coerceIn(0.85f, 1.2f)).dp),
          shape = RoundedCornerShape(4.dp),
          color = if (isBarracao) Color.White else CottonSurface,
          border = BorderStroke(2.5.dp, if (isBarracao) BarracaoRoofDark else CottonOutline),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = streetData.street,
              fontFamily = IbmPlexMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = (20f * zoomScale.coerceIn(0.85f, 1.2f)).sp,
              color = headerInk,
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${streetData.blockCount.formatBr()} bl · ${streetData.totalBales.formatBr()} f",
            fontFamily = IbmPlexMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (11.5f * zoomScale.coerceIn(0.85f, 1.15f)).sp,
            color = headerInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Pos. ${streetData.slots.size}/${streetData.maxPositions}",
            fontFamily = IbmPlexMonoFontFamily,
            fontSize = 10.sp,
            color = headerInk.copy(alpha = 0.78f),
            maxLines = 1,
          )
        }
      }

      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(borderColor),
      )

      LazyColumn(
        state = columnListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        itemsIndexed(
          items = streetData.slots,
          key = { _, slot -> "${patio.id}_${streetData.street}_${slot.idx}" },
        ) { index, slot ->
          val isLast = index == streetData.slots.lastIndex
          val isHighlighted = highlightedIdx == slot.idx
          val alert = MapaRules.alertForSlot(slot, blockOccurrences)
          SlotLedgerRow(
            slot = slot,
            alert = alert,
            usersById = usersById,
            isBarracao = isBarracao,
            zoomScale = zoomScale,
            isHighlighted = isHighlighted,
            onClick = { onSlotClick(slot, alert, isLast) },
          )
        }

        item(key = "add_block_${streetData.street}") {
          Spacer(modifier = Modifier.height(2.dp))
          Button(
            onClick = onAddBlockClick,
            enabled = streetData.canAddMore,
            shape = RoundedCornerShape(8.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (isBarracao) BarracaoAccent else ForestPrimary,
              ),
            modifier =
              Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("add_block_button_${streetData.street}"),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
          ) {
            if (streetData.canAddMore) {
              Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text =
                  if (streetData.hasVacancy) "+ Bloco (vaga ${streetData.nextIdx + 1})"
                  else "+ Bloco",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            } else {
              Text(
                text = "Rua cheia (${streetData.maxPositions})",
                style = MaterialTheme.typography.labelSmall,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SlotLedgerRow(
  slot: Slot,
  alert: MapaRules.SlotAlertState?,
  usersById: Map<String, UserProfile>,
  isBarracao: Boolean,
  zoomScale: Float,
  isHighlighted: Boolean,
  onClick: () -> Unit,
) {
  val authorName =
    slot.updatedByUid?.let { uid ->
      MapaRules.resolveAuthorName(uid, usersById, fallbackName = slot.updatedByName)
    } ?: slot.updatedByName?.takeIf { it.isNotBlank() }
  val timeStr = MapaRules.formatHourMinute(slot.updatedAt)
  val attributionText =
    when {
      authorName != null && timeStr.isNotEmpty() -> "editado por $authorName, $timeStr"
      authorName != null -> "editado por $authorName"
      else -> null
    }

  val highlightColor by
    animateColorAsState(
      targetValue =
        if (isHighlighted) HighlightGold.copy(alpha = 0.42f)
        else Color.Transparent,
      label = "slotHighlight",
    )

  val baseMinHeight = if (isBarracao) 64f else 48f
  val scaledMinHeight = (baseMinHeight * zoomScale).coerceIn(44f, 92f).dp

  if (slot.type == SlotType.VAGA) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = scaledMinHeight)
          .clip(RoundedCornerShape(4.dp))
          .background(if (isHighlighted) highlightColor else VacantFill)
          .drawBehind {
            val stroke =
              Stroke(
                width = if (isHighlighted) 2.2.dp.toPx() else 1.4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f),
              )
            drawRoundRect(
              color = if (isHighlighted) HighlightGold else VacantBorder,
              style = stroke,
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
          }
          .clickable(onClick = onClick)
          .padding(horizontal = 6.dp, vertical = 6.dp)
          .testTag("slot_row_${slot.street}_${slot.idx}"),
      contentAlignment = Alignment.Center,
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = slot.positionNumber.toString().padStart(2, '0'),
            fontFamily = IbmPlexMonoFontFamily,
            fontSize = (10.5f * zoomScale.coerceIn(0.85f, 1.2f)).sp,
            color = VacantText,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "vaga",
            fontFamily = IbmPlexMonoFontFamily,
            fontSize = (12.5f * zoomScale.coerceIn(0.85f, 1.2f)).sp,
            color = VacantText,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
          )
          if (!slot.note.isNullOrBlank()) {
            Icon(
              Icons.Default.Description,
              contentDescription = "Possui observação",
              modifier = Modifier.size(13.dp),
              tint = VacantText,
            )
          }
        }
        if (attributionText != null) {
          Text(
            text = if (slot.hasPendingWrites) "$attributionText · pendente" else attributionText,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = VacantText.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
          )
        }
      }
    }
  } else {
    val isRedAlert = alert?.isRedAlert == true
    val isOverSingle = MapaRules.isOverBaleLimit(slot.bales)
    val repeatCount = alert?.occurrenceSummary?.locationCount ?: 1

    val cardBg =
      when {
        isHighlighted -> HighlightGold.copy(alpha = 0.42f)
        isRedAlert -> ErrorContainerLight
        else -> CottonSurface
      }
    val borderColor =
      when {
        isHighlighted -> HighlightGold
        isRedAlert -> ErrorRed
        else -> CottonOutline
      }

    val ratio = MapaRules.balesProgressRatio(slot.bales)
    val barColor = if (isOverSingle || isRedAlert) ErrorRed else SuccessGreen

    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = scaledMinHeight)
          .clip(RoundedCornerShape(4.dp))
          .clickable(onClick = onClick)
          .testTag("slot_row_${slot.street}_${slot.idx}"),
    ) {
      Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = scaledMinHeight),
        shape = RoundedCornerShape(4.dp),
        color = cardBg,
        border = BorderStroke(if (isHighlighted || isRedAlert) 1.8.dp else 1.dp, borderColor),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Célula larga (nº do bloco) + célula estreita (fardos) com divisória fina preta
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier =
                Modifier
                  .weight(1.45f)
                  .padding(horizontal = 6.dp, vertical = if (isBarracao) 10.dp else 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = slot.positionNumber.toString().padStart(2, '0'),
                fontFamily = IbmPlexMonoFontFamily,
                fontSize = 10.sp,
                color = if (isRedAlert) ErrorRed else CottonMutedInk,
                modifier = Modifier.width(18.dp),
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = slot.number.orEmpty(),
                fontFamily = IbmPlexMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (15f * zoomScale.coerceIn(0.85f, 1.25f)).sp,
                color = if (isRedAlert) ErrorRed else CottonInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
              )
              if (slot.checked) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Bloco conferido",
                  modifier = Modifier.size(15.dp),
                  tint = SuccessGreen,
                )
              }
              if (!slot.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                  imageVector = Icons.Default.Description,
                  contentDescription = "Possui observação",
                  modifier = Modifier.size(13.dp),
                  tint = ForestPrimary,
                )
              }
            }

            // Divisória vertical fina preta entre célula de número e célula de fardos
            Box(
              modifier =
                Modifier
                  .fillMaxHeight()
                  .width(1.dp)
                  .background(borderColor),
            )

            // Célula estreita com os fardos
            Box(
              modifier =
                Modifier
                  .weight(0.75f)
                  .fillMaxHeight()
                  .background(
                    when {
                      isRedAlert -> ErrorRed.copy(alpha = 0.14f)
                      isBarracao -> BarracaoAccentContainer.copy(alpha = 0.45f)
                      else -> CottonPaperBg.copy(alpha = 0.65f)
                    }
                  )
                  .padding(horizontal = 4.dp, vertical = if (isBarracao) 10.dp else 6.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = slot.bales.formatBr(),
                fontFamily = IbmPlexMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (13f * zoomScale.coerceIn(0.85f, 1.25f)).sp,
                color = if (isRedAlert) ErrorRed else CottonInk,
                textAlign = TextAlign.Center,
                maxLines = 1,
              )
            }
          }

          val shortWarning = alert?.shortMapWarning()
          if (shortWarning != null) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(12.dp),
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text =
                  if (alert?.isPositionOverLimit == true) "passa ${MapaRules.excessBales(slot.bales)}"
                  else shortWarning,
                fontFamily = IbmPlexMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ErrorRed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          if (attributionText != null) {
            Text(
              text = if (slot.hasPendingWrites) "$attributionText · pendente" else attributionText,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              color = CottonMutedInk,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 3.dp),
            )
          }

          // Barra de fardos fina embaixo
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.Black.copy(alpha = 0.08f)),
          ) {
            Box(
              modifier =
                Modifier
                  .fillMaxWidth(ratio)
                  .fillMaxHeight()
                  .background(barColor),
            )
          }
        }
      }

      // Selo "x2" (ou "xN") no canto quando bloco está repetido em mais de um lugar
      if (repeatCount > 1) {
        Surface(
          shape = RoundedCornerShape(bottomStart = 6.dp, topEnd = 4.dp),
          color = RepeatBadgeBg,
          border = BorderStroke(1.dp, RepeatBadgeBorder),
          modifier = Modifier.align(Alignment.TopEnd).testTag("repeat_badge_${slot.street}_${slot.idx}"),
        ) {
          Text(
            text = "x$repeatCount",
            fontFamily = IbmPlexMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = RepeatBadgeInk,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyMapaState(
  onCreateFirstPatio: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize().padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = CottonSurface,
      border = BorderStroke(1.dp, CottonOutline),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(64.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              Icons.Default.GridOn,
              contentDescription = null,
              modifier = Modifier.size(34.dp),
              tint = ForestPrimary,
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Nenhum pátio ou barracão",
          style = MaterialTheme.typography.titleLarge,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text =
            "Crie um pátio a céu aberto (ruas A–Z) ou um barracão (ruas A e B) para começar a mapear os blocos de algodão.",
          style = MaterialTheme.typography.bodyMedium,
          color = CottonMutedInk,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = onCreateFirstPatio,
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("empty_create_patio_button"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Default.Add, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Criar Pátio ou Barracão")
        }
      }
    }
  }
}
