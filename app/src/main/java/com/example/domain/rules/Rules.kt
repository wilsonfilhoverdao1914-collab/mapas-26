package com.example.domain.rules

import com.example.domain.model.HistoryEntry
import com.example.domain.model.Patio
import com.example.domain.model.PatioKind
import com.example.domain.model.PatioTotals
import com.example.domain.model.SearchMatch
import com.example.domain.model.SearchResultGroup
import com.example.domain.model.Slot
import com.example.domain.model.SlotType
import com.example.domain.model.StreetColumnData
import com.example.domain.model.SyncStatus
import com.example.domain.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MapaRules {
  const val MIN_BALES_PER_POSITION = 1
  const val MAX_BALES_PER_POSITION = 140
  const val MAX_POSITIONS_PATIO = 50
  const val MAX_POSITIONS_BARRACAO = 100
  const val MAX_HISTORY_ENTRIES = 200


  fun maxPositionsFor(kind: PatioKind): Int =
    when (kind) {
      PatioKind.PATIO -> MAX_POSITIONS_PATIO
      PatioKind.BARRACAO -> MAX_POSITIONS_BARRACAO
    }

  fun isOverBaleLimit(bales: Int): Boolean = bales > MAX_BALES_PER_POSITION

  fun excessBales(bales: Int): Int = (bales - MAX_BALES_PER_POSITION).coerceAtLeast(0)

  fun balesProgressRatio(bales: Int): Float =
    (bales.toFloat() / MAX_BALES_PER_POSITION.toFloat()).coerceIn(0f, 1f)

  fun normalizedBlockKey(number: String?): String = number?.trim()?.uppercase().orEmpty()

  sealed interface AddBlockTarget {
    data class SlotIndex(
      val idx: Int,
      val isReusingVacant: Boolean,
      val availableVacantIndices: List<Int>,
    ) : AddBlockTarget

    data object StreetFull : AddBlockTarget
  }

  /**
   * Regra 1:
   * Se houver vaga na rua, ocupa EXATAMENTE a vaga escolhida (ou a primeira vaga).
   * Sem vaga, vai para o fim da rua, respeitando o máximo (50 pátio / 100 barracão).
   */
  fun resolveTargetForNewBlock(
    streetSlots: List<Slot>,
    maxPositions: Int,
    preferredVacantIdx: Int? = null,
  ): AddBlockTarget {
    val vacantIndices =
      streetSlots
        .filter { it.type == SlotType.VAGA && it.idx in 0 until maxPositions }
        .map { it.idx }
        .sorted()

    if (preferredVacantIdx != null && preferredVacantIdx in vacantIndices) {
      return AddBlockTarget.SlotIndex(
        idx = preferredVacantIdx,
        isReusingVacant = true,
        availableVacantIndices = vacantIndices,
      )
    }

    if (vacantIndices.isNotEmpty()) {
      return AddBlockTarget.SlotIndex(
        idx = vacantIndices.first(),
        isReusingVacant = true,
        availableVacantIndices = vacantIndices,
      )
    }

    val nextEndIdx = (streetSlots.maxOfOrNull { it.idx } ?: -1) + 1
    return if (nextEndIdx < maxPositions) {
      AddBlockTarget.SlotIndex(
        idx = nextEndIdx,
        isReusingVacant = false,
        availableVacantIndices = emptyList(),
      )
    } else {
      AddBlockTarget.StreetFull
    }
  }

  data class BlockLocationEntry(
    val patio: Patio,
    val slot: Slot,
  )

  data class BlockOccurrenceSummary(
    val blockKey: String,
    val displayNumber: String,
    val locations: List<BlockLocationEntry>,
    val totalBales: Int,
  ) {
    val locationCount: Int
      get() = locations.size

    val isMultiLocation: Boolean
      get() = locationCount > 1

    val totalExcess: Int
      get() = excessBales(totalBales)

    val isTotalOverLimit: Boolean
      get() = totalExcess > 0
  }

  data class SlotAlertState(
    val slot: Slot,
    val positionExcess: Int,
    val occurrenceSummary: BlockOccurrenceSummary?,
  ) {
    val isPositionOverLimit: Boolean
      get() = positionExcess > 0

    val otherLocations: List<BlockLocationEntry>
      get() =
        occurrenceSummary?.locations?.filterNot {
          it.slot.patioId == slot.patioId &&
            it.slot.street.equals(slot.street, ignoreCase = true) &&
            it.slot.idx == slot.idx
        }.orEmpty()

    val isMultiLocation: Boolean
      get() = (occurrenceSummary?.locationCount ?: 0) > 1

    val totalAcrossLocations: Int
      get() = occurrenceSummary?.totalBales ?: slot.bales

    val totalExcessAcrossLocations: Int
      get() = occurrenceSummary?.totalExcess ?: positionExcess

    val isRedAlert: Boolean
      get() = isPositionOverLimit || totalExcessAcrossLocations > 0

    fun shortMapWarning(): String? =
      when {
        isPositionOverLimit -> "passa $positionExcess"
        totalExcessAcrossLocations > 0 -> "soma passa $totalExcessAcrossLocations"
        else -> null
      }

    fun correctionDialogMessage(): String {
      val parts = mutableListOf<String>()
      if (isPositionOverLimit) {
        parts +=
          "Esta posição está com ${slot.bales} fardos — bloco passando fardo (passa $positionExcess do limite de $MAX_BALES_PER_POSITION)."
      }
      if (isMultiLocation && totalExcessAcrossLocations > 0) {
        parts +=
          "O bloco ${slot.number.orEmpty()} aparece em ${occurrenceSummary?.locationCount} lugares somando $totalAcrossLocations fardos (passa $totalExcessAcrossLocations do limite de $MAX_BALES_PER_POSITION)."
      }
      return parts.joinToString("\n\n").ifEmpty {
        "Verifique a quantidade de fardos deste bloco."
      }
    }
  }

  /**
   * Regras 2 e 3:
   * Indexa todos os blocos por número para detectar excesso individual (>140),
   * blocos em múltiplos lugares e excesso na soma de todas as localizações.
   */
  fun buildBlockOccurrencesIndex(
    patios: List<Patio>,
    slots: List<Slot>,
  ): Map<String, BlockOccurrenceSummary> {
    val patioMap = patios.associateBy { it.id }
    val grouped =
      slots
        .asSequence()
        .filter { it.type == SlotType.BLOCO && !it.number.isNullOrBlank() }
        .mapNotNull { slot ->
          val patio = patioMap[slot.patioId] ?: return@mapNotNull null
          BlockLocationEntry(patio = patio, slot = slot)
        }
        .groupBy { normalizedBlockKey(it.slot.number) }

    return grouped.mapValues { (key, entries) ->
      val sortedEntries =
        entries.sortedWith(
          compareBy<BlockLocationEntry> { it.patio.name }
            .thenBy { it.slot.street }
            .thenBy { it.slot.idx }
        )
      BlockOccurrenceSummary(
        blockKey = key,
        displayNumber = sortedEntries.first().slot.number.orEmpty(),
        locations = sortedEntries,
        totalBales = sortedEntries.sumOf { it.slot.bales },
      )
    }
  }

  fun alertForSlot(
    slot: Slot,
    occurrencesIndex: Map<String, BlockOccurrenceSummary>,
  ): SlotAlertState? {
    if (slot.type != SlotType.BLOCO) return null
    val key = normalizedBlockKey(slot.number)
    val summary = if (key.isNotEmpty()) occurrencesIndex[key] else null
    return SlotAlertState(
      slot = slot,
      positionExcess = excessBales(slot.bales),
      occurrenceSummary = summary,
    )
  }

  /**
   * Prévia ao editar ou criar um bloco (Regra 3) para exibir "Bloco em N lugares"
   * considerando outras localizações já salvas com o mesmo número.
   */
  fun previewOtherLocationsForNumber(
    blockNumber: String,
    currentPatioId: String,
    currentStreet: String,
    currentIdx: Int,
    currentBales: Int,
    occurrencesIndex: Map<String, BlockOccurrenceSummary>,
  ): BlockOccurrenceSummary? {
    val key = normalizedBlockKey(blockNumber)
    if (key.isEmpty()) return null
    val existing = occurrencesIndex[key] ?: return null
    val others =
      existing.locations.filterNot {
        it.slot.patioId == currentPatioId &&
          it.slot.street.equals(currentStreet, ignoreCase = true) &&
          it.slot.idx == currentIdx
      }
    if (others.isEmpty()) return null
    return BlockOccurrenceSummary(
      blockKey = key,
      displayNumber = blockNumber.trim(),
      locations = others,
      totalBales = others.sumOf { it.slot.bales } + currentBales.coerceAtLeast(0),
    )
  }

  /**
   * Regra 4: Bloco carregado.
   * Não aceita carregar mais do que existe. Se carregar todos, retorna remainingBales = 0
   * e becomesVacant = true.
   */
  sealed interface LoadResult {
    data class Valid(
      val remainingBales: Int,
      val becomesVacant: Boolean,
    ) : LoadResult

    data class Invalid(val message: String) : LoadResult
  }

  fun validateLoadedBales(currentBales: Int, loadedBales: Int): LoadResult {
    if (currentBales <= 0) {
      return LoadResult.Invalid("Este bloco não possui fardos para carregar.")
    }
    if (loadedBales <= 0) {
      return LoadResult.Invalid("Informe pelo menos 1 fardo carregado.")
    }
    if (loadedBales > currentBales) {
      return LoadResult.Invalid(
        "Não é possível carregar $loadedBales fardos: existem apenas $currentBales neste bloco."
      )
    }
    val remaining = currentBales - loadedBales
    return LoadResult.Valid(
      remainingBales = remaining,
      becomesVacant = remaining == 0,
    )
  }

  /**
   * Regra 5: Juntar fardos.
   * Soma os fardos das origens no destino e avisa se o resultado passar de 140.
   */
  data class MergePreview(
    val destination: BlockLocationEntry,
    val sources: List<BlockLocationEntry>,
    val resultingBales: Int,
    val excessOverLimit: Int,
  ) {
    val isOverLimit: Boolean
      get() = excessOverLimit > 0
  }

  fun previewMerge(
    destination: BlockLocationEntry,
    sources: List<BlockLocationEntry>,
  ): MergePreview {
    val uniqueSources =
      sources.filterNot {
        it.slot.patioId == destination.slot.patioId &&
          it.slot.street.equals(destination.slot.street, ignoreCase = true) &&
          it.slot.idx == destination.slot.idx
      }
    val total = destination.slot.bales + uniqueSources.sumOf { it.slot.bales }
    return MergePreview(
      destination = destination,
      sources = uniqueSources,
      resultingBales = total,
      excessOverLimit = excessBales(total),
    )
  }

  /**
   * Regra 9: Totais por pátio (considera apenas os blocos que estão naquele pátio) e total geral.
   */
  fun calculatePatioTotals(
    patios: List<Patio>,
    slots: List<Slot>,
  ): List<PatioTotals> {
    val slotsByPatio = slots.groupBy { it.patioId }
    return patios.map { patio ->
      val patioSlots = slotsByPatio[patio.id].orEmpty()
      val blocks = patioSlots.filter { it.type == SlotType.BLOCO }
      val vacant = patioSlots.count { it.type == SlotType.VAGA }
      PatioTotals(
        patio = patio,
        blockCount = blocks.size,
        totalBales = blocks.sumOf { it.bales },
        vacantCount = vacant,
      )
    }
  }

  fun buildStreetColumns(
    patio: Patio,
    patioSlots: List<Slot>,
  ): List<StreetColumnData> {
    val maxPositions = maxPositionsFor(patio.kind)
    return patio.streets.map { streetLetter ->
      val streetSlotsMap =
        patioSlots
          .filter {
            it.street.equals(streetLetter, ignoreCase = true) && it.idx in 0 until maxPositions
          }
          .associateBy { it.idx }
      val maxIdx = streetSlotsMap.keys.maxOrNull() ?: -1
      val filledAndGaps =
        if (maxIdx < 0) {
          emptyList()
        } else {
          (0..maxIdx).map { idx ->
            streetSlotsMap[idx]
              ?: Slot(
                patioId = patio.id,
                street = streetLetter,
                idx = idx,
                type = SlotType.VAGA,
              )
          }
        }
      val blockSlots = filledAndGaps.filter { it.type == SlotType.BLOCO }
      StreetColumnData(
        street = streetLetter,
        slots = filledAndGaps,
        blockCount = blockSlots.size,
        totalBales = blockSlots.sumOf { it.bales },
        maxPositions = maxPositions,
      )
    }
  }

  /**
   * Regra 8: Busca pelo número do bloco em todos os pátios/barracões.
   */
  fun searchBlocks(
    query: String,
    patios: List<Patio>,
    slots: List<Slot>,
  ): SearchResultGroup {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) {
      return SearchResultGroup(
        query = "",
        matches = emptyList(),
        totalBales = 0,
        distinctBlockNumbers = emptyList(),
      )
    }
    val patioMap = patios.associateBy { it.id }
    val matches =
      slots
        .asSequence()
        .filter { slot ->
          slot.type == SlotType.BLOCO &&
            !slot.number.isNullOrBlank() &&
            slot.number.contains(trimmedQuery, ignoreCase = true)
        }
        .mapNotNull { slot ->
          val patio = patioMap[slot.patioId] ?: return@mapNotNull null
          SearchMatch(patio = patio, slot = slot)
        }
        .sortedWith(
          compareByDescending<SearchMatch> {
            it.slot.number.equals(trimmedQuery, ignoreCase = true)
          }
            .thenByDescending {
              it.slot.number?.startsWith(trimmedQuery, ignoreCase = true) == true
            }
            .thenBy { it.patio.name }
            .thenBy { it.slot.street }
            .thenBy { it.slot.idx }
        )
        .toList()

    return SearchResultGroup(
      query = trimmedQuery,
      matches = matches,
      totalBales = matches.sumOf { it.slot.bales },
      distinctBlockNumbers = matches.mapNotNull { it.slot.number }.distinct(),
    )
  }

  // --- Histórico, Atribuição e Conflitos ---

  fun formatAddedBlockHistory(
    actorName: String,
    blockNumber: String,
    bales: Int,
    patioName: String,
    street: String,
    positionNumber: Int,
  ): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    return "$cleanActor adicionou o bloco $blockNumber ($bales fardos) no $patioName, rua $street, posição $positionNumber"
  }

  fun formatEditedBlockHistory(
    actorName: String,
    blockNumber: String,
    bales: Int,
    patioName: String,
    street: String,
    positionNumber: Int,
  ): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    return "$cleanActor editou o bloco $blockNumber ($bales fardos) no $patioName, rua $street, posição $positionNumber"
  }

  fun formatLoadedBalesHistory(
    actorName: String,
    blockNumber: String,
    loadedBales: Int,
    remainingBales: Int,
    becameVacant: Boolean,
  ): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    return if (becameVacant) {
      "$cleanActor carregou $loadedBales fardos do bloco $blockNumber (bloco zerado, virou vaga)"
    } else {
      "$cleanActor carregou $loadedBales fardos do bloco $blockNumber (restam $remainingBales)"
    }
  }

  fun formatMergeHistory(
    actorName: String,
    destNumber: String,
    sources: List<BlockLocationEntry>,
    resultingBales: Int,
  ): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    val sourceText =
      sources.joinToString(", ") { entry ->
        val num = entry.slot.number.orEmpty().ifBlank { "sem nº" }
        "$num (${entry.slot.bales})"
      }
    return "$cleanActor juntou $sourceText no $destNumber: agora $resultingBales fardos"
  }

  fun formatBlockToVacantHistory(
    actorName: String,
    blockNumber: String?,
    patioName: String,
    street: String,
    positionNumber: Int,
  ): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    val blockPart = blockNumber?.takeIf { it.isNotBlank() }?.let { "o bloco $it" } ?: "a posição"
    return "$cleanActor excluiu $blockPart no $patioName, rua $street, posição $positionNumber (virou vaga)"
  }

  fun formatVacantCreatedHistory(
    actorName: String,
    patioName: String,
    street: String,
    positionNumber: Int,
  ): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    return "$cleanActor marcou vaga no $patioName, rua $street, posição $positionNumber"
  }

  fun formatMoveHistory(
    actorName: String,
    blockNumber: String?,
    patioName: String,
    street: String,
    fromPosition: Int,
    toPosition: Int,
  ): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    val item = blockNumber?.takeIf { it.isNotBlank() }?.let { "o bloco $it" } ?: "a vaga"
    return "$cleanActor moveu $item no $patioName, rua $street, da posição $fromPosition para $toPosition"
  }

  fun formatPatioCreatedHistory(actorName: String, patioName: String, kind: PatioKind): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    return "$cleanActor criou o ${kind.label.lowercase()} $patioName"
  }

  fun formatPatioUpdatedHistory(actorName: String, patioName: String): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    return "$cleanActor atualizou o local $patioName"
  }

  fun formatPatioDeletedHistory(actorName: String, patioName: String): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    return "$cleanActor excluiu o local $patioName"
  }

  data class SlotConflictInfo(
    val remoteUpdatedBy: String,
    val remoteType: SlotType,
    val remoteNumber: String?,
    val remoteBales: Int,
    val remoteNote: String?,
    val remoteChecked: Boolean,
    val remoteUpdatedAt: Long,
  ) {
    fun summaryText(): String {
      val who = remoteUpdatedBy.ifBlank { "Outro usuário" }
      val whenText = if (remoteUpdatedAt > 0L) " às ${formatHourMinute(remoteUpdatedAt)}" else ""
      return if (remoteType == SlotType.VAGA) {
        "$who alterou esta posição para VAGA$whenText."
      } else {
        "$who alterou para Bloco ${remoteNumber.orEmpty()} com $remoteBales fardos$whenText."
      }
    }
  }

  /**
   * Detecta se a posição aberta para edição foi alterada remotamente por outro usuário/celular.
   */
  fun detectRemoteSlotConflict(
    openedSnapshot: Slot?,
    latestSlot: Slot?,
    currentUserId: String,
  ): SlotConflictInfo? {
    if (latestSlot == null) return null
    val remoteUid = latestSlot.updatedByUid ?: return null
    if (remoteUid == currentUserId) return null

    val sameTimestamp = openedSnapshot != null && latestSlot.updatedAt == openedSnapshot.updatedAt
    val sameContent =
      openedSnapshot != null &&
        openedSnapshot.type == latestSlot.type &&
        openedSnapshot.number == latestSlot.number &&
        openedSnapshot.bales == latestSlot.bales &&
        openedSnapshot.note == latestSlot.note &&
        openedSnapshot.checked == latestSlot.checked

    if (sameTimestamp && sameContent) return null
    if (openedSnapshot == null && latestSlot.updatedAt == 0L) return null

    return SlotConflictInfo(
      remoteUpdatedBy = latestSlot.updatedByName?.takeIf { it.isNotBlank() } ?: "Outro celular",
      remoteType = latestSlot.type,
      remoteNumber = latestSlot.number,
      remoteBales = latestSlot.bales,
      remoteNote = latestSlot.note,
      remoteChecked = latestSlot.checked,
      remoteUpdatedAt = latestSlot.updatedAt,
    )
  }

  data class HistoryDayGroup(
    val dayKey: String,
    val dayLabel: String,
    val entries: List<HistoryEntry>,
  )

  fun groupHistoryByDay(
    entries: List<HistoryEntry>,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): List<HistoryDayGroup> {
    if (entries.isEmpty()) return emptyList()
    val locale = Locale("pt", "BR")
    val keyFormatter = SimpleDateFormat("yyyy-MM-dd", locale).apply { this.timeZone = timeZone }
    val labelFormatter = SimpleDateFormat("dd/MM/yyyy", locale).apply { this.timeZone = timeZone }

    val todayCal = Calendar.getInstance(timeZone, locale).apply { timeInMillis = nowMillis }
    val todayKey = keyFormatter.format(todayCal.time)
    val yesterdayCal =
      (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val yesterdayKey = keyFormatter.format(yesterdayCal.time)

    val limitedSorted =
      entries
        .sortedByDescending { it.ts }
        .take(MAX_HISTORY_ENTRIES)

    return limitedSorted
      .groupBy { entry ->
        if (entry.ts > 0L) keyFormatter.format(Date(entry.ts)) else todayKey
      }
      .map { (dayKey, groupEntries) ->
        val label =
          when (dayKey) {
            todayKey -> "Hoje"
            yesterdayKey -> "Ontem"
            else -> {
              val ts = groupEntries.firstOrNull()?.ts ?: nowMillis
              labelFormatter.format(Date(ts))
            }
          }
        HistoryDayGroup(dayKey = dayKey, dayLabel = label, entries = groupEntries)
      }
  }

  fun formatHourMinute(tsMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    if (tsMillis <= 0L) return ""
    val fmt = SimpleDateFormat("HH:mm", Locale("pt", "BR")).apply { this.timeZone = timeZone }
    return fmt.format(Date(tsMillis))
  }

  fun resolveAuthorName(
    userId: String,
    usersById: Map<String, UserProfile>,
    fallbackName: String? = null,
  ): String {
    return usersById[userId]?.name?.takeIf { it.isNotBlank() }
      ?: fallbackName?.takeIf { it.isNotBlank() }
      ?: "Operador"
  }

  fun computeSyncStatus(
    isOnline: Boolean,
    pendingWritesCount: Int,
    isSyncingFromServer: Boolean = false,
  ): SyncStatus {
    return SyncStatus(
      isOnline = isOnline,
      pendingWritesCount = pendingWritesCount.coerceAtLeast(0),
      isSyncing = isSyncingFromServer,
    )
  }

  // --- Conferência de Blocos ---

  enum class ConferenceFilter(val label: String) {
    TODOS("Todos"),
    FALTAM("Faltam"),
    CONFERIDOS("Conferidos"),
  }

  data class ConferenceBlockItem(
    val blockKey: String,
    val displayNumber: String,
    val totalBales: Int,
    val locations: List<BlockLocationEntry>,
    val isChecked: Boolean,
    val checkedLocationsCount: Int,
  ) {
    val locationCount: Int
      get() = locations.size

    val isOverLimit: Boolean
      get() = excessBales(totalBales) > 0
  }

  data class ConferenceSummary(
    val allItems: List<ConferenceBlockItem>,
    val totalBlocks: Int,
    val checkedBlocks: Int,
    val percent: Int,
  )

  fun buildConferenceSummary(
    patios: List<Patio>,
    slots: List<Slot>,
  ): ConferenceSummary {
    val index = buildBlockOccurrencesIndex(patios, slots)
    val items =
      index.values
        .map { summary ->
          val checkedLocations = summary.locations.count { it.slot.checked }
          val allLocationsChecked =
            summary.locations.isNotEmpty() && checkedLocations == summary.locations.size
          ConferenceBlockItem(
            blockKey = summary.blockKey,
            displayNumber = summary.displayNumber,
            totalBales = summary.totalBales,
            locations = summary.locations,
            isChecked = allLocationsChecked,
            checkedLocationsCount = checkedLocations,
          )
        }
        .sortedWith(
          compareBy<ConferenceBlockItem> { it.displayNumber.toLongOrNull() == null }
            .thenBy { it.displayNumber.toLongOrNull() ?: Long.MAX_VALUE }
            .thenBy { it.displayNumber }
        )

    val total = items.size
    val checked = items.count { it.isChecked }
    val percent = if (total == 0) 0 else ((checked * 100.0) / total).toInt().coerceIn(0, 100)
    return ConferenceSummary(
      allItems = items,
      totalBlocks = total,
      checkedBlocks = checked,
      percent = percent,
    )
  }

  fun filterConferenceItems(
    items: List<ConferenceBlockItem>,
    filter: ConferenceFilter,
    numberPrefixQuery: String,
  ): List<ConferenceBlockItem> {
    val cleanDigits = numberPrefixQuery.filter { it.isDigit() }
    return items.filter { item ->
      val matchesTab =
        when (filter) {
          ConferenceFilter.TODOS -> true
          ConferenceFilter.FALTAM -> !item.isChecked
          ConferenceFilter.CONFERIDOS -> item.isChecked
        }
      val matchesPrefix =
        if (cleanDigits.isEmpty()) true
        else item.blockKey.startsWith(cleanDigits, ignoreCase = true)
      matchesTab && matchesPrefix
    }
  }

  /**
   * Se o bloco for alterado (número, fardos, carregado ou juntado), o visto sai.
   * Mudar apenas a observação mantém o visto.
   */
  fun shouldInvalidateCheckedOnEdit(
    previousSlot: Slot?,
    newType: SlotType,
    newNumber: String?,
    newBales: Int,
  ): Boolean {
    if (previousSlot == null || previousSlot.type != SlotType.BLOCO) return false
    if (newType != SlotType.BLOCO) return true
    return normalizedBlockKey(previousSlot.number) != normalizedBlockKey(newNumber) ||
      previousSlot.bales != newBales
  }

  fun formatConferenceCheckedHistory(
    actorName: String,
    blockNumber: String,
    checked: Boolean,
  ): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    return if (checked) {
      "$cleanActor conferiu o bloco $blockNumber"
    } else {
      "$cleanActor desmarcou a conferência do bloco $blockNumber"
    }
  }

  fun formatClearConferenceHistory(actorName: String, clearedCount: Int): String {
    val cleanActor = actorName.ifBlank { "Operador" }
    return "$cleanActor limpou a conferência ($clearedCount posições)"
  }
}
