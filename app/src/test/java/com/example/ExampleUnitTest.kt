package com.example

import com.example.domain.model.HistoryEntry
import com.example.domain.model.Patio
import com.example.domain.model.PatioKind
import com.example.domain.model.Slot
import com.example.domain.model.SlotType
import com.example.domain.model.SyncStateKind
import com.example.domain.model.UserProfile
import com.example.domain.rules.CalcOp
import com.example.domain.rules.CalculatorEngine
import com.example.domain.rules.CalculatorState
import com.example.domain.rules.MapaRules
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun patio_streets_follow_range_and_limits() {
    val patio = Patio(id = "1", name = "Pátio Sul", kind = PatioKind.PATIO, last = "M")
    assertEquals(('A'..'M').map { it.toString() }, patio.streets)
    assertEquals(13, patio.streets.size)
    assertEquals(50, MapaRules.maxPositionsFor(patio.kind))
    assertEquals("Ruas A–M", patio.streetRangeLabel)
  }

  @Test
  fun barracao_always_has_two_streets_and_100_positions() {
    val barracao = Patio(id = "2", name = "Barracão 1", kind = PatioKind.BARRACAO, last = "Z")
    assertEquals(listOf("A", "B"), barracao.streets)
    assertEquals(100, MapaRules.maxPositionsFor(barracao.kind))
    assertEquals("Ruas A e B", barracao.streetRangeLabel)
  }

  @Test
  fun rule1_add_block_occupies_first_or_chosen_vaga_else_end_respecting_max() {
    val streetSlots =
      listOf(
        Slot("p1", "A", 0, SlotType.BLOCO, "100", 120),
        Slot("p1", "A", 1, SlotType.VAGA),
        Slot("p1", "A", 2, SlotType.BLOCO, "101", 100),
        Slot("p1", "A", 3, SlotType.VAGA),
      )

    val firstVaga = MapaRules.resolveTargetForNewBlock(streetSlots, MapaRules.MAX_POSITIONS_PATIO)
    assertEquals(MapaRules.AddBlockTarget.SlotIndex(1, true, listOf(1, 3)), firstVaga)

    val chosenVaga =
      MapaRules.resolveTargetForNewBlock(
        streetSlots,
        MapaRules.MAX_POSITIONS_PATIO,
        preferredVacantIdx = 3,
      )
    assertEquals(MapaRules.AddBlockTarget.SlotIndex(3, true, listOf(1, 3)), chosenVaga)

    val fullNoVaga =
      (0 until 50).map { Slot("p1", "A", it, SlotType.BLOCO, "${200 + it}", 100) }
    assertEquals(
      MapaRules.AddBlockTarget.StreetFull,
      MapaRules.resolveTargetForNewBlock(fullNoVaga, MapaRules.MAX_POSITIONS_PATIO),
    )
  }

  @Test
  fun rule2_and_rule3_detect_single_and_multi_location_excess() {
    val p1 = Patio("p1", "Pátio 1", PatioKind.PATIO, "D")
    val p2 = Patio("p2", "Pátio 2", PatioKind.PATIO, "D")
    val slots =
      listOf(
        Slot("p1", "A", 0, SlotType.BLOCO, "815", 152),
        Slot("p1", "B", 0, SlotType.BLOCO, "589", 96),
        Slot("p2", "A", 0, SlotType.BLOCO, "589", 60),
      )

    val index = MapaRules.buildBlockOccurrencesIndex(listOf(p1, p2), slots)

    val alert815 = MapaRules.alertForSlot(slots[0], index)!!
    assertTrue(alert815.isRedAlert)
    assertEquals(12, alert815.positionExcess)
    assertEquals("passa 12", alert815.shortMapWarning())

    val alert589 = MapaRules.alertForSlot(slots[1], index)!!
    assertTrue(alert589.isMultiLocation)
    assertEquals(156, alert589.totalAcrossLocations)
    assertEquals(16, alert589.totalExcessAcrossLocations)
    assertTrue(alert589.isRedAlert)
  }

  @Test
  fun rule4_load_bales_partial_full_and_reject_excess() {
    val partial = MapaRules.validateLoadedBales(currentBales = 140, loadedBales = 50)
    assertEquals(MapaRules.LoadResult.Valid(remainingBales = 90, becomesVacant = false), partial)

    val full = MapaRules.validateLoadedBales(currentBales = 90, loadedBales = 90)
    assertEquals(MapaRules.LoadResult.Valid(remainingBales = 0, becomesVacant = true), full)

    val invalid = MapaRules.validateLoadedBales(currentBales = 90, loadedBales = 91)
    assertTrue(invalid is MapaRules.LoadResult.Invalid)
  }

  @Test
  fun rule5_merge_preview_sums_and_flags_over_140() {
    val p1 = Patio("p1", "Pátio 1", PatioKind.PATIO, "C")
    val dest = MapaRules.BlockLocationEntry(p1, Slot("p1", "A", 0, SlotType.BLOCO, "504", 50))
    val src1 = MapaRules.BlockLocationEntry(p1, Slot("p1", "A", 1, SlotType.BLOCO, "579", 20))
    val previewOk = MapaRules.previewMerge(dest, listOf(src1))
    assertEquals(70, previewOk.resultingBales)
    assertFalse(previewOk.isOverLimit)

    val src2 = MapaRules.BlockLocationEntry(p1, Slot("p1", "B", 0, SlotType.BLOCO, "600", 80))
    val previewOver = MapaRules.previewMerge(dest, listOf(src1, src2))
    assertEquals(150, previewOver.resultingBales)
    assertTrue(previewOver.isOverLimit)
    assertEquals(10, previewOver.excessOverLimit)
  }

  @Test
  fun rule9_and_rule10_patio_totals_isolated_and_progress_ratio_clamped() {
    val p1 = Patio("p1", "Pátio 1", PatioKind.PATIO, "B")
    val p2 = Patio("p2", "Pátio 2", PatioKind.PATIO, "B")
    val slots =
      listOf(
        Slot("p1", "A", 0, SlotType.BLOCO, "589", 96),
        Slot("p2", "A", 0, SlotType.BLOCO, "589", 44),
      )

    val totals = MapaRules.calculatePatioTotals(listOf(p1, p2), slots)
    assertEquals(96, totals[0].totalBales)
    assertEquals(44, totals[1].totalBales)
    assertEquals(140, totals.sumOf { it.totalBales })

    assertEquals(0.5f, MapaRules.balesProgressRatio(70), 0.001f)
    assertEquals(1.0f, MapaRules.balesProgressRatio(180), 0.001f)
  }

  @Test
  fun history_formatting_day_grouping_and_200_limit() {
    val addedText =
      MapaRules.formatAddedBlockHistory(
        actorName = "João",
        blockNumber = "815",
        bales = 140,
        patioName = "Pátio 1",
        street = "B",
        positionNumber = 7,
      )
    assertEquals("João adicionou o bloco 815 (140 fardos) no Pátio 1, rua B, posição 7", addedText)

    val p1 = Patio("p1", "Pátio 1", PatioKind.PATIO, "B")
    val src = MapaRules.BlockLocationEntry(p1, Slot("p1", "A", 1, SlotType.BLOCO, "579", 20))
    val mergeText = MapaRules.formatMergeHistory("João", "504", listOf(src), 70)
    assertEquals("João juntou 579 (20) no 504: agora 70 fardos", mergeText)

    val tz = TimeZone.getTimeZone("America/Sao_Paulo")
    val cal = Calendar.getInstance(tz).apply { set(2026, Calendar.NOVEMBER, 19, 14, 0, 0) }
    val todayMs = cal.timeInMillis
    val yesterdayMs = todayMs - 86_400_000L
    val olderMs = todayMs - 3 * 86_400_000L

    val entries =
      (1..220).map { idx ->
        val ts =
          when {
            idx <= 10 -> todayMs + idx
            idx <= 20 -> yesterdayMs + idx
            else -> olderMs - idx
          }
        HistoryEntry(id = "h$idx", ts = ts, userId = "u1", text = "Evento $idx", patioName = "Pátio 1")
      }
    val groups = MapaRules.groupHistoryByDay(entries, nowMillis = todayMs, timeZone = tz)
    assertEquals(200, groups.sumOf { it.entries.size })
    assertEquals("Hoje", groups[0].dayLabel)
    assertEquals("Ontem", groups[1].dayLabel)
    assertEquals("16/11/2026", groups[2].dayLabel)
    assertEquals("h10", groups[0].entries.first().id)

    val users = mapOf("u1" to UserProfile("u1", "Maria", "EQ1"))
    assertEquals("Maria", MapaRules.resolveAuthorName("u1", users))
    assertEquals("Operador", MapaRules.resolveAuthorName("u2", users))
  }

  @Test
  fun sync_status_and_remote_conflict_detection() {
    val onlineSynced = MapaRules.computeSyncStatus(isOnline = true, pendingWritesCount = 0)
    assertEquals(SyncStateKind.ONLINE_SYNCED, onlineSynced.kind)
    assertEquals("Online, sincronizado", onlineSynced.statusLabel)

    val syncing = MapaRules.computeSyncStatus(isOnline = true, pendingWritesCount = 2)
    assertEquals(SyncStateKind.SYNCING, syncing.kind)
    assertEquals("Sincronizando...", syncing.statusLabel)

    val offlineOne = MapaRules.computeSyncStatus(isOnline = false, pendingWritesCount = 1)
    assertEquals(SyncStateKind.OFFLINE, offlineOne.kind)
    assertEquals("Sem internet · 1 alteração para enviar", offlineOne.statusLabel)

    val offlineThree = MapaRules.computeSyncStatus(isOnline = false, pendingWritesCount = 3)
    assertEquals("Sem internet · 3 alterações para enviar", offlineThree.statusLabel)

    val opened = Slot("p1", "A", 0, SlotType.BLOCO, "815", 140, updatedAt = 1_000L, updatedByUid = "u1", updatedByName = "João")
    val sameLocal = opened.copy(bales = 120, updatedAt = 2_000L, updatedByUid = "u1")
    assertNull(MapaRules.detectRemoteSlotConflict(opened, sameLocal, currentUserId = "u1"))

    val remoteChanged = opened.copy(bales = 90, updatedAt = 3_000L, updatedByUid = "u2", updatedByName = "Maria")
    val conflict = MapaRules.detectRemoteSlotConflict(opened, remoteChanged, currentUserId = "u1")
    assertNotNull(conflict)
    assertEquals("Maria", conflict?.remoteUpdatedBy)
    assertEquals(90, conflict?.remoteBales)
  }

  @Test
  fun conferencia_aggregates_unique_blocks_requires_all_checked_filters_by_prefix_and_invalidates_edits() {
    val p1 = Patio("p1", "Pátio 1", PatioKind.PATIO, "D")
    val p2 = Patio("p2", "Barracão 1", PatioKind.BARRACAO, "B")
    val slots =
      listOf(
        // Bloco 589 em 2 lugares, apenas 1 marcado como conferido -> NÃO conta como conferido
        Slot("p1", "A", 0, SlotType.BLOCO, "589", 96, checked = true),
        Slot("p2", "A", 0, SlotType.BLOCO, "589", 44, checked = false),
        // Bloco 815 conferido
        Slot("p1", "B", 0, SlotType.BLOCO, "815", 140, checked = true),
        // Bloco 820 não conferido
        Slot("p2", "B", 0, SlotType.BLOCO, "820", 100, checked = false),
      )

    val summary = MapaRules.buildConferenceSummary(listOf(p1, p2), slots)
    assertEquals(3, summary.totalBlocks)
    assertEquals(1, summary.checkedBlocks)
    assertEquals(33, summary.percent)

    val item589 = summary.allItems.first { it.displayNumber == "589" }
    assertEquals(140, item589.totalBales)
    assertEquals(2, item589.locationCount)
    assertFalse(item589.isChecked)

    // Filtro por aba Faltam / Conferidos e busca por prefixo numérico ("8")
    val prefix8All =
      MapaRules.filterConferenceItems(summary.allItems, MapaRules.ConferenceFilter.TODOS, "8")
    assertEquals(listOf("815", "820"), prefix8All.map { it.displayNumber })

    val faltam8 =
      MapaRules.filterConferenceItems(summary.allItems, MapaRules.ConferenceFilter.FALTAM, "8")
    assertEquals(listOf("820"), faltam8.map { it.displayNumber })

    val conferidos =
      MapaRules.filterConferenceItems(summary.allItems, MapaRules.ConferenceFilter.CONFERIDOS, "")
    assertEquals(listOf("815"), conferidos.map { it.displayNumber })

    // Regra de invalidação do visto:
    // - Alterar número ou fardos remove o visto
    // - Alterar apenas observação mantém o visto
    val checkedSlot = Slot("p1", "B", 0, SlotType.BLOCO, "815", 140, note = "antiga", checked = true)
    assertFalse(MapaRules.shouldInvalidateCheckedOnEdit(checkedSlot, SlotType.BLOCO, "815", 140))
    assertTrue(MapaRules.shouldInvalidateCheckedOnEdit(checkedSlot, SlotType.BLOCO, "816", 140))
    assertTrue(MapaRules.shouldInvalidateCheckedOnEdit(checkedSlot, SlotType.BLOCO, "815", 130))
    assertTrue(MapaRules.shouldInvalidateCheckedOnEdit(checkedSlot, SlotType.VAGA, null, 0))

    assertEquals(
      "Fulano conferiu o bloco 815",
      MapaRules.formatConferenceCheckedHistory("Fulano", "815", checked = true),
    )
  }

  @Test
  fun calculator_engine_arithmetic_percent_negate_comma_and_div_by_zero() {
    var s = CalculatorState()
    s = CalculatorEngine.pressDigit(s, 1)
    s = CalculatorEngine.pressDigit(s, 4)
    s = CalculatorEngine.pressDigit(s, 0)
    s = CalculatorEngine.pressOperator(s, CalcOp.ADD)
    s = CalculatorEngine.pressDigit(s, 5)
    s = CalculatorEngine.pressDigit(s, 0)
    s = CalculatorEngine.pressPercent(s) // 50% de 140 = 70
    assertEquals("70", s.mainDisplay)
    s = CalculatorEngine.pressEquals(s)
    assertEquals("210", s.mainDisplay)
    assertEquals("140 + 70 =", s.expressionDisplay)

    // Vírgula decimal e ±
    s = CalculatorEngine.pressClear()
    s = CalculatorEngine.pressDigit(s, 2)
    s = CalculatorEngine.pressDecimal(s)
    s = CalculatorEngine.pressDigit(s, 5)
    assertEquals("2,5", s.mainDisplay)
    s = CalculatorEngine.pressNegate(s)
    assertEquals("-2,5", s.mainDisplay)
    s = CalculatorEngine.pressOperator(s, CalcOp.MULTIPLY)
    s = CalculatorEngine.pressDigit(s, 4)
    s = CalculatorEngine.pressEquals(s)
    assertEquals("-10", s.mainDisplay)

    // Divisão por zero exibe "Erro"
    s = CalculatorEngine.pressOperator(s, CalcOp.DIVIDE)
    s = CalculatorEngine.pressDigit(s, 0)
    s = CalculatorEngine.pressEquals(s)
    assertTrue(s.isError)
    assertEquals("Erro", s.mainDisplay)
  }
}
