package com.example.data.repository

import com.example.domain.model.HistoryEntry
import com.example.domain.model.Patio
import com.example.domain.model.PatioKind
import com.example.domain.model.Slot
import com.example.domain.model.SlotType
import com.example.domain.model.UserProfile
import com.example.domain.model.Workspace
import com.example.domain.rules.MapaRules
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SyncedSnapshot<T>(
  val data: T,
  val pendingWritesCount: Int = 0,
  val isFromCache: Boolean = false,
)

interface MapaDataSource {
  fun observePatios(userId: String): Flow<SyncedSnapshot<List<Patio>>>

  fun observeSlots(userId: String): Flow<SyncedSnapshot<List<Slot>>>

  fun observeHistory(userId: String): Flow<SyncedSnapshot<List<HistoryEntry>>>

  fun observeUserProfile(userId: String): Flow<UserProfile?>

  fun observeWorkspace(workspaceId: String): Flow<Workspace?>

  fun observeWorkspaceUsers(memberUids: List<String>): Flow<Map<String, UserProfile>>

  suspend fun ensureUserAndWorkspace(
    userId: String,
    preferredName: String?,
    fallbackWorkspaceId: String? = null,
  ): Result<UserProfile>

  suspend fun updateUserName(userId: String, newName: String): Result<Unit>

  suspend fun joinWorkspaceByCode(
    userId: String,
    workspaceCode: String,
    userName: String,
  ): Result<Workspace>

  suspend fun propagateWorkspaceMembers(
    userId: String,
    workspace: Workspace,
    patios: List<Patio>,
    slots: List<Slot>,
  ): Result<Unit>

  suspend fun createPatio(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    name: String,
    kind: PatioKind,
    lastLetter: String,
  ): Result<Patio>

  suspend fun updatePatio(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    patioId: String,
    name: String,
    kind: PatioKind,
    lastLetter: String,
  ): Result<Unit>

  suspend fun deletePatio(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    patioId: String,
    patioName: String,
  ): Result<Unit>

  suspend fun upsertSlot(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    patioId: String,
    patioName: String,
    street: String,
    idx: Int,
    type: SlotType,
    number: String?,
    bales: Int,
    note: String?,
    checked: Boolean,
    historyText: String,
  ): Result<Slot>

  suspend fun deleteSlot(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    patioId: String,
    patioName: String,
    street: String,
    idx: Int,
  ): Result<Unit>

  suspend fun moveSlot(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    patioId: String,
    patioName: String,
    street: String,
    fromIdx: Int,
    toIdx: Int,
    maxPositions: Int,
  ): Result<Unit>

  suspend fun mergeBlocks(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    destination: MapaRules.BlockLocationEntry,
    sources: List<MapaRules.BlockLocationEntry>,
  ): Result<Int>

  suspend fun setBlockConferenceChecked(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    blockNumber: String,
    locations: List<MapaRules.BlockLocationEntry>,
    checked: Boolean,
  ): Result<Unit>

  suspend fun clearAllConferenceChecks(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    checkedSlots: List<Slot>,
  ): Result<Int>
}

class LocalPreviewMapaDataSource : MapaDataSource {
  private val localWsId = "LOCAL_EQUIPE"
  private val patiosFlow = MutableStateFlow(SyncedSnapshot(emptyList<Patio>()))
  private val slotsFlow = MutableStateFlow(SyncedSnapshot(emptyList<Slot>()))
  private val historyFlow = MutableStateFlow(SyncedSnapshot(emptyList<HistoryEntry>()))
  private val userProfileFlow =
    MutableStateFlow<UserProfile?>(
      UserProfile(uid = "local_preview", name = "Operador Local", workspaceId = localWsId)
    )
  private val workspaceFlow =
    MutableStateFlow<Workspace?>(
      Workspace(
        id = localWsId,
        ownerId = "local_preview",
        name = "Modo Pré-visualização",
        memberUids = listOf("local_preview"),
      )
    )
  private val usersMapFlow =
    MutableStateFlow(
      mapOf(
        "local_preview" to
          UserProfile(uid = "local_preview", name = "Operador Local", workspaceId = localWsId)
      )
    )

  override fun observePatios(userId: String): Flow<SyncedSnapshot<List<Patio>>> =
    patiosFlow.asStateFlow()

  override fun observeSlots(userId: String): Flow<SyncedSnapshot<List<Slot>>> =
    slotsFlow.asStateFlow()

  override fun observeHistory(userId: String): Flow<SyncedSnapshot<List<HistoryEntry>>> =
    historyFlow.asStateFlow()

  override fun observeUserProfile(userId: String): Flow<UserProfile?> =
    userProfileFlow.asStateFlow()

  override fun observeWorkspace(workspaceId: String): Flow<Workspace?> =
    workspaceFlow.asStateFlow()

  override fun observeWorkspaceUsers(memberUids: List<String>): Flow<Map<String, UserProfile>> =
    usersMapFlow.asStateFlow()

  override suspend fun ensureUserAndWorkspace(
    userId: String,
    preferredName: String?,
    fallbackWorkspaceId: String?,
  ): Result<UserProfile> {
    val cleanName = preferredName?.trim()?.take(60)?.takeIf { it.isNotEmpty() } ?: "Operador Local"
    val profile = UserProfile(uid = userId, name = cleanName, workspaceId = localWsId)
    userProfileFlow.value = profile
    usersMapFlow.value = mapOf(userId to profile)
    return Result.success(profile)
  }

  override suspend fun updateUserName(userId: String, newName: String): Result<Unit> {
    val clean = newName.trim().take(60)
    if (clean.isEmpty()) {
      return Result.failure(IllegalArgumentException("Informe seu nome."))
    }
    val updated = UserProfile(uid = userId, name = clean, workspaceId = localWsId)
    userProfileFlow.value = updated
    usersMapFlow.value = mapOf(userId to updated)
    return Result.success(Unit)
  }

  override suspend fun joinWorkspaceByCode(
    userId: String,
    workspaceCode: String,
    userName: String,
  ): Result<Workspace> {
    return Result.failure(
      IllegalStateException("Entre com o Google para sincronizar equipes entre celulares.")
    )
  }

  override suspend fun propagateWorkspaceMembers(
    userId: String,
    workspace: Workspace,
    patios: List<Patio>,
    slots: List<Slot>,
  ): Result<Unit> = Result.success(Unit)

  private fun appendHistory(userId: String, text: String, patioName: String) {
    val entry =
      HistoryEntry(
        id = "lh_${UUID.randomUUID().toString().take(8)}",
        ts = System.currentTimeMillis(),
        userId = userId,
        text = text,
        patioName = patioName,
        workspaceId = localWsId,
      )
    historyFlow.update { current ->
      val updated = (listOf(entry) + current.data).take(MapaRules.MAX_HISTORY_ENTRIES)
      SyncedSnapshot(updated)
    }
  }

  override suspend fun createPatio(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    name: String,
    kind: PatioKind,
    lastLetter: String,
  ): Result<Patio> {
    val cleanName = name.trim().take(80)
    if (cleanName.isEmpty()) {
      return Result.failure(IllegalArgumentException("Informe o nome do local."))
    }
    val normalizedLast =
      if (kind == PatioKind.BARRACAO) "B"
      else lastLetter.uppercase().firstOrNull()?.coerceIn('A', 'Z')?.toString() ?: "A"
    val created =
      Patio(
        id = "local_${UUID.randomUUID().toString().take(8)}",
        name = cleanName,
        kind = kind,
        last = normalizedLast,
        ts = System.currentTimeMillis(),
        workspaceId = localWsId,
        memberUids = listOf(userId),
      )
    patiosFlow.update { current ->
      SyncedSnapshot((current.data + created).sortedBy { it.ts })
    }
    appendHistory(
      userId = userId,
      text = MapaRules.formatPatioCreatedHistory(actorName, cleanName, kind),
      patioName = cleanName,
    )
    return Result.success(created)
  }

  override suspend fun updatePatio(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    patioId: String,
    name: String,
    kind: PatioKind,
    lastLetter: String,
  ): Result<Unit> {
    val cleanName = name.trim().take(80)
    if (cleanName.isEmpty()) {
      return Result.failure(IllegalArgumentException("Informe o nome do local."))
    }
    val normalizedLast =
      if (kind == PatioKind.BARRACAO) "B"
      else lastLetter.uppercase().firstOrNull()?.coerceIn('A', 'Z')?.toString() ?: "A"
    patiosFlow.update { current ->
      SyncedSnapshot(
        current.data.map { patio ->
          if (patio.id == patioId) patio.copy(name = cleanName, last = normalizedLast) else patio
        }
      )
    }
    appendHistory(
      userId = userId,
      text = MapaRules.formatPatioUpdatedHistory(actorName, cleanName),
      patioName = cleanName,
    )
    return Result.success(Unit)
  }

  override suspend fun deletePatio(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    patioId: String,
    patioName: String,
  ): Result<Unit> {
    patiosFlow.update { current -> SyncedSnapshot(current.data.filterNot { it.id == patioId }) }
    slotsFlow.update { current -> SyncedSnapshot(current.data.filterNot { it.patioId == patioId }) }
    appendHistory(
      userId = userId,
      text = MapaRules.formatPatioDeletedHistory(actorName, patioName),
      patioName = patioName,
    )
    return Result.success(Unit)
  }

  override suspend fun upsertSlot(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    patioId: String,
    patioName: String,
    street: String,
    idx: Int,
    type: SlotType,
    number: String?,
    bales: Int,
    note: String?,
    checked: Boolean,
    historyText: String,
  ): Result<Slot> {
    if (idx !in 0 until 100) {
      return Result.failure(IllegalArgumentException("Posição fora do limite."))
    }
    val normalizedStreet = street.uppercase().take(1)
    val cleanNumber =
      if (type == SlotType.BLOCO) number?.trim()?.take(32)?.takeIf { it.isNotEmpty() } else null
    val cleanBales = if (type == SlotType.BLOCO) bales.coerceIn(0, 1_000_000) else 0
    val cleanNote = note?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
    val cleanChecked = type == SlotType.BLOCO && checked

    val saved =
      Slot(
        patioId = patioId,
        street = normalizedStreet,
        idx = idx,
        type = type,
        number = cleanNumber,
        bales = cleanBales,
        note = cleanNote,
        checked = cleanChecked,
        updatedAt = System.currentTimeMillis(),
        updatedByUid = userId,
        updatedByName = actorName,
      )

    slotsFlow.update { current ->
      val filtered =
        current.data.filterNot {
          it.patioId == patioId && it.street.equals(normalizedStreet, ignoreCase = true) && it.idx == idx
        }
      SyncedSnapshot((filtered + saved).sortedWith(compareBy<Slot> { it.street }.thenBy { it.idx }))
    }
    appendHistory(userId = userId, text = historyText, patioName = patioName)
    return Result.success(saved)
  }

  override suspend fun deleteSlot(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    patioId: String,
    patioName: String,
    street: String,
    idx: Int,
  ): Result<Unit> {
    val normalizedStreet = street.uppercase().take(1)
    slotsFlow.update { current ->
      SyncedSnapshot(
        current.data.filterNot {
          it.patioId == patioId && it.street.equals(normalizedStreet, ignoreCase = true) && it.idx == idx
        }
      )
    }
    appendHistory(
      userId = userId,
      text = "$actorName removeu a posição vazia da Rua $normalizedStreet (${idx + 1}) no $patioName",
      patioName = patioName,
    )
    return Result.success(Unit)
  }

  override suspend fun moveSlot(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    patioId: String,
    patioName: String,
    street: String,
    fromIdx: Int,
    toIdx: Int,
    maxPositions: Int,
  ): Result<Unit> {
    if (fromIdx !in 0 until maxPositions || toIdx !in 0 until maxPositions || fromIdx == toIdx) {
      return Result.failure(IllegalArgumentException("Posição de destino fora do limite da rua."))
    }
    val normalizedStreet = street.uppercase().take(1)
    val now = System.currentTimeMillis()
    var movedNumber: String? = null
    slotsFlow.update { current ->
      val fromSlot =
        current.data.firstOrNull {
          it.patioId == patioId && it.street.equals(normalizedStreet, ignoreCase = true) && it.idx == fromIdx
        }
          ?: Slot(patioId = patioId, street = normalizedStreet, idx = fromIdx, type = SlotType.VAGA)
      val toSlot =
        current.data.firstOrNull {
          it.patioId == patioId && it.street.equals(normalizedStreet, ignoreCase = true) && it.idx == toIdx
        }
          ?: Slot(patioId = patioId, street = normalizedStreet, idx = toIdx, type = SlotType.VAGA)
      movedNumber = if (fromSlot.isBlock) fromSlot.number else null

      val others =
        current.data.filterNot {
          it.patioId == patioId &&
            it.street.equals(normalizedStreet, ignoreCase = true) &&
            (it.idx == fromIdx || it.idx == toIdx)
        }
      val newTo =
        fromSlot.copy(
          idx = toIdx,
          updatedAt = now,
          updatedByUid = userId,
          updatedByName = actorName,
        )
      val newFrom =
        toSlot.copy(
          idx = fromIdx,
          updatedAt = now,
          updatedByUid = userId,
          updatedByName = actorName,
        )
      SyncedSnapshot(
        (others + newTo + newFrom).sortedWith(compareBy<Slot> { it.street }.thenBy { it.idx })
      )
    }
    appendHistory(
      userId = userId,
      text =
        MapaRules.formatMoveHistory(
          actorName = actorName,
          blockNumber = movedNumber,
          patioName = patioName,
          street = normalizedStreet,
          fromPosition = fromIdx + 1,
          toPosition = toIdx + 1,
        ),
      patioName = patioName,
    )
    return Result.success(Unit)
  }

  override suspend fun mergeBlocks(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    destination: MapaRules.BlockLocationEntry,
    sources: List<MapaRules.BlockLocationEntry>,
  ): Result<Int> {
    val preview = MapaRules.previewMerge(destination, sources)
    if (preview.sources.isEmpty()) {
      return Result.failure(IllegalArgumentException("Selecione ao menos um bloco de origem."))
    }
    val now = System.currentTimeMillis()
    val destSlot = destination.slot
    val sourceKeys =
      preview.sources.map { Triple(it.slot.patioId, it.slot.street.uppercase(), it.slot.idx) }.toSet()

    slotsFlow.update { current ->
      SyncedSnapshot(
        current.data.map { slot ->
          val key = Triple(slot.patioId, slot.street.uppercase(), slot.idx)
          when {
            slot.patioId == destSlot.patioId &&
              slot.street.equals(destSlot.street, ignoreCase = true) &&
              slot.idx == destSlot.idx -> {
              slot.copy(
                bales = preview.resultingBales,
                checked = false,
                updatedAt = now,
                updatedByUid = userId,
                updatedByName = actorName,
              )
            }
            key in sourceKeys -> {
              slot.copy(
                type = SlotType.VAGA,
                number = null,
                bales = 0,
                note = null,
                checked = false,
                updatedAt = now,
                updatedByUid = userId,
                updatedByName = actorName,
              )
            }
            else -> slot
          }
        }
      )
    }
    appendHistory(
      userId = userId,
      text =
        MapaRules.formatMergeHistory(
          actorName = actorName,
          destNumber = destSlot.number.orEmpty(),
          sources = preview.sources,
          resultingBales = preview.resultingBales,
        ),
      patioName = destination.patio.name,
    )
    return Result.success(preview.resultingBales)
  }

  override suspend fun setBlockConferenceChecked(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    blockNumber: String,
    locations: List<MapaRules.BlockLocationEntry>,
    checked: Boolean,
  ): Result<Unit> {
    if (locations.isEmpty()) return Result.success(Unit)
    val now = System.currentTimeMillis()
    val targetKeys =
      locations.map { Triple(it.slot.patioId, it.slot.street.uppercase(), it.slot.idx) }.toSet()
    slotsFlow.update { current ->
      SyncedSnapshot(
        current.data.map { slot ->
          val key = Triple(slot.patioId, slot.street.uppercase(), slot.idx)
          if (key in targetKeys) {
            slot.copy(
              checked = checked,
              updatedAt = now,
              updatedByUid = userId,
              updatedByName = actorName,
            )
          } else {
            slot
          }
        }
      )
    }
    appendHistory(
      userId = userId,
      text = MapaRules.formatConferenceCheckedHistory(actorName, blockNumber, checked),
      patioName = locations.firstOrNull()?.patio?.name ?: "Conferência",
    )
    return Result.success(Unit)
  }

  override suspend fun clearAllConferenceChecks(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    checkedSlots: List<Slot>,
  ): Result<Int> {
    if (checkedSlots.isEmpty()) return Result.success(0)
    val now = System.currentTimeMillis()
    val targetKeys =
      checkedSlots.map { Triple(it.patioId, it.street.uppercase(), it.idx) }.toSet()
    slotsFlow.update { current ->
      SyncedSnapshot(
        current.data.map { slot ->
          val key = Triple(slot.patioId, slot.street.uppercase(), slot.idx)
          if (key in targetKeys) {
            slot.copy(
              checked = false,
              updatedAt = now,
              updatedByUid = userId,
              updatedByName = actorName,
            )
          } else {
            slot
          }
        }
      )
    }
    appendHistory(
      userId = userId,
      text = MapaRules.formatClearConferenceHistory(actorName, checkedSlots.size),
      patioName = "Conferência",
    )
    return Result.success(checkedSlots.size)
  }
}
