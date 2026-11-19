package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesRepository
import com.example.data.remote.friendlyFirestoreMessage
import com.example.data.repository.MapaDataSource
import com.example.data.repository.SyncedSnapshot
import com.example.domain.model.HistoryEntry
import com.example.domain.model.Patio
import com.example.domain.model.PatioKind
import com.example.domain.model.PatioTotals
import com.example.domain.model.SearchResultGroup
import com.example.domain.model.Slot
import com.example.domain.model.SlotType
import com.example.domain.model.StreetColumnData
import com.example.domain.model.SyncStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.Workspace
import com.example.domain.rules.MapaRules
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HighlightedSlot(
  val patioId: String,
  val street: String,
  val idx: Int,
)

private data class TransientState(
  val isSaving: Boolean = false,
  val errorMessage: String? = null,
  val snackbarMessage: String? = null,
  val searchQuery: String = "",
  val conferenceFilter: MapaRules.ConferenceFilter = MapaRules.ConferenceFilter.TODOS,
  val conferenceQuery: String = "",
  val highlightedSlot: HighlightedSlot? = null,
)

private sealed interface StreamData<out T> {
  data object Loading : StreamData<Nothing>
  data class Success<T>(val snapshot: SyncedSnapshot<T>) : StreamData<T>
  data class Error(val message: String) : StreamData<Nothing>
}

data class MapaUiState(
  val patios: List<Patio> = emptyList(),
  val selectedPatio: Patio? = null,
  val streetColumns: List<StreetColumnData> = emptyList(),
  val selectedPatioTotals: PatioTotals? = null,
  val allPatioTotals: List<PatioTotals> = emptyList(),
  val grandTotalBlocks: Int = 0,
  val grandTotalBales: Int = 0,
  val grandTotalVacant: Int = 0,
  val blockOccurrences: Map<String, MapaRules.BlockOccurrenceSummary> = emptyMap(),
  val allBlockLocations: List<MapaRules.BlockLocationEntry> = emptyList(),
  val allSlots: List<Slot> = emptyList(),
  val searchResults: SearchResultGroup = SearchResultGroup("", emptyList(), 0, emptyList()),
  val searchQuery: String = "",
  val conferenceSummary: MapaRules.ConferenceSummary =
    MapaRules.ConferenceSummary(emptyList(), 0, 0, 0),
  val conferenceFilter: MapaRules.ConferenceFilter = MapaRules.ConferenceFilter.TODOS,
  val conferenceQuery: String = "",
  val filteredConferenceItems: List<MapaRules.ConferenceBlockItem> = emptyList(),
  val historyEntries: List<HistoryEntry> = emptyList(),
  val historyGroups: List<MapaRules.HistoryDayGroup> = emptyList(),
  val userProfile: UserProfile? = null,
  val workspace: Workspace? = null,
  val usersById: Map<String, UserProfile> = emptyMap(),
  val syncStatus: SyncStatus = SyncStatus(),
  val isLoading: Boolean = true,
  val isSaving: Boolean = false,
  val errorMessage: String? = null,
  val snackbarMessage: String? = null,
  val highlightedSlot: HighlightedSlot? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MapaViewModel(
  private val repository: MapaDataSource,
  private val prefs: PreferencesRepository,
  private val currentUserId: String,
  private val initialDisplayName: String? = null,
  private val isOnlineFlow: StateFlow<Boolean> = MutableStateFlow(true),
) : ViewModel() {

  private val transientState = MutableStateFlow(TransientState())

  private val userProfileStream: StateFlow<UserProfile?> =
    repository
      .observeUserProfile(currentUserId)
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  private val workspaceStream: StateFlow<Workspace?> =
    userProfileStream
      .map { it?.workspaceId.orEmpty() }
      .distinctUntilChanged()
      .flatMapLatest { wsId ->
        if (wsId.isBlank()) flowOf(null) else repository.observeWorkspace(wsId)
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  private val workspaceUsersStream: StateFlow<Map<String, UserProfile>> =
    workspaceStream
      .map { it?.memberUids ?: listOf(currentUserId) }
      .distinctUntilChanged()
      .flatMapLatest { members -> repository.observeWorkspaceUsers(members) }
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

  private val patiosStream =
    repository
      .observePatios(currentUserId)
      .map<SyncedSnapshot<List<Patio>>, StreamData<List<Patio>>> { StreamData.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(StreamData.Error(friendlyFirestoreMessage(firestoreError ?: error)))
      }

  private val slotsStream =
    repository
      .observeSlots(currentUserId)
      .map<SyncedSnapshot<List<Slot>>, StreamData<List<Slot>>> { StreamData.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(StreamData.Error(friendlyFirestoreMessage(firestoreError ?: error)))
      }

  private val historyStream =
    repository
      .observeHistory(currentUserId)
      .map<SyncedSnapshot<List<HistoryEntry>>, StreamData<List<HistoryEntry>>> {
        StreamData.Success(it)
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(StreamData.Error(friendlyFirestoreMessage(firestoreError ?: error)))
      }

  init {
    viewModelScope.launch {
      val cachedName = prefs.getCachedDisplayName()
      val cachedWsId = prefs.getCachedWorkspaceId()
      val preferred =
        cachedName?.takeIf { it.isNotBlank() }
          ?: initialDisplayName?.takeIf { it.isNotBlank() }
      repository
        .ensureUserAndWorkspace(
          userId = currentUserId,
          preferredName = preferred,
          fallbackWorkspaceId = cachedWsId,
        )
        .onSuccess { profile ->
          prefs.saveUserSessionCache(profile.name, profile.workspaceId)
        }
    }

    viewModelScope.launch {
      userProfileStream.collect { profile ->
        if (profile != null) {
          prefs.saveUserSessionCache(profile.name, profile.workspaceId)
        }
      }
    }

    viewModelScope.launch {
      combine(workspaceStream, patiosStream, slotsStream) { ws, patiosRes, slotsRes ->
        val patios = (patiosRes as? StreamData.Success)?.snapshot?.data.orEmpty()
        val slots = (slotsRes as? StreamData.Success)?.snapshot?.data.orEmpty()
        Triple(ws, patios, slots)
      }
        .collect { (ws, patios, slots) ->
          if (ws != null && currentUserId in ws.memberUids) {
            repository.propagateWorkspaceMembers(
              userId = currentUserId,
              workspace = ws,
              patios = patios,
              slots = slots,
            )
          }
        }
    }
  }

  private val coreStreamsFlow =
    combine(patiosStream, slotsStream, historyStream, prefs.selectedPatioId) {
      patiosRes,
      slotsRes,
      historyRes,
      savedPatioId ->
      CoreBundle(patiosRes, slotsRes, historyRes, savedPatioId)
    }

  private val userContextFlow =
    combine(userProfileStream, workspaceStream, workspaceUsersStream, isOnlineFlow) {
      profile,
      workspace,
      usersById,
      isOnline ->
      UserContextBundle(profile, workspace, usersById, isOnline)
    }

  val uiState: StateFlow<MapaUiState> =
    combine(coreStreamsFlow, userContextFlow, transientState) { core, userCtx, transient ->
      val isLoading =
        core.patiosRes is StreamData.Loading || core.slotsRes is StreamData.Loading
      val streamError =
        (core.patiosRes as? StreamData.Error)?.message
          ?: (core.slotsRes as? StreamData.Error)?.message
          ?: (core.historyRes as? StreamData.Error)?.message

      val patiosSnap = (core.patiosRes as? StreamData.Success)?.snapshot
      val slotsSnap = (core.slotsRes as? StreamData.Success)?.snapshot
      val historySnap = (core.historyRes as? StreamData.Success)?.snapshot

      val patios = patiosSnap?.data.orEmpty()
      val slots = slotsSnap?.data.orEmpty()
      val history = historySnap?.data.orEmpty()

      val totalPending =
        (patiosSnap?.pendingWritesCount ?: 0) +
          (slotsSnap?.pendingWritesCount ?: 0) +
          (historySnap?.pendingWritesCount ?: 0)
      val syncStatus =
        MapaRules.computeSyncStatus(
          isOnline = userCtx.isOnline,
          pendingWritesCount = totalPending,
          isSyncingFromServer = transient.isSaving && userCtx.isOnline,
        )

      buildUiState(
        patios = patios,
        slots = slots,
        history = history,
        savedPatioId = core.savedPatioId,
        userCtx = userCtx,
        syncStatus = syncStatus,
        isLoading = isLoading,
        streamError = streamError,
        transient = transient,
      )
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapaUiState(),
      )

  private data class CoreBundle(
    val patiosRes: StreamData<List<Patio>>,
    val slotsRes: StreamData<List<Slot>>,
    val historyRes: StreamData<List<HistoryEntry>>,
    val savedPatioId: String?,
  )

  private data class UserContextBundle(
    val profile: UserProfile?,
    val workspace: Workspace?,
    val usersById: Map<String, UserProfile>,
    val isOnline: Boolean,
  )

  private fun currentActorName(): String {
    return uiState.value.userProfile?.name?.takeIf { it.isNotBlank() }
      ?: initialDisplayName?.takeIf { it.isNotBlank() }
      ?: "Operador"
  }

  private fun currentWorkspaceId(): String {
    return uiState.value.userProfile?.workspaceId?.takeIf { it.isNotBlank() }
      ?: uiState.value.workspace?.id?.takeIf { it.isNotBlank() }
      ?: "EQ_${currentUserId.take(8)}"
  }

  private fun currentMemberUids(): List<String> {
    val wsMembers = uiState.value.workspace?.memberUids.orEmpty()
    return (wsMembers + currentUserId).distinct().take(10)
  }

  private fun resolvePatioName(patioId: String): String {
    return uiState.value.patios.firstOrNull { it.id == patioId }?.name ?: "Pátio"
  }

  fun selectPatio(patioId: String) {
    viewModelScope.launch { prefs.saveSelectedPatioId(patioId) }
  }

  fun updateUserName(newName: String) {
    val clean = newName.trim().take(60)
    if (clean.isEmpty()) {
      transientState.update { it.copy(errorMessage = "Informe seu nome para aparecer no histórico.") }
      return
    }
    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      prefs.saveUserSessionCache(clean, null)
      repository
        .updateUserName(userId = currentUserId, newName = clean)
        .onSuccess {
          transientState.update {
            it.copy(isSaving = false, snackbarMessage = "Nome atualizado para \"$clean\".")
          }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun joinWorkspaceByCode(code: String) {
    val cleanCode = code.trim().uppercase()
    if (cleanCode.isEmpty()) {
      transientState.update { it.copy(errorMessage = "Informe o código da equipe.") }
      return
    }
    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      repository
        .joinWorkspaceByCode(
          userId = currentUserId,
          workspaceCode = cleanCode,
          userName = currentActorName(),
        )
        .onSuccess { ws ->
          prefs.saveUserSessionCache(currentActorName(), ws.id)
          transientState.update {
            it.copy(
              isSaving = false,
              snackbarMessage = "Conectado à equipe ${ws.id} (${ws.memberUids.size} aparelhos).",
            )
          }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun createPatio(name: String, kind: PatioKind, lastLetter: String) {
    if (name.isBlank()) return
    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      repository
        .createPatio(
          userId = currentUserId,
          actorName = currentActorName(),
          workspaceId = currentWorkspaceId(),
          memberUids = currentMemberUids(),
          name = name,
          kind = kind,
          lastLetter = lastLetter,
        )
        .onSuccess { created ->
          prefs.saveSelectedPatioId(created.id)
          transientState.update {
            it.copy(
              isSaving = false,
              snackbarMessage = "${kind.label} \"${created.name}\" criado.",
            )
          }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun updatePatio(patio: Patio, newName: String, newLastLetter: String) {
    if (newName.isBlank()) return
    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      repository
        .updatePatio(
          userId = currentUserId,
          actorName = currentActorName(),
          workspaceId = patio.workspaceId.ifBlank { currentWorkspaceId() },
          memberUids = currentMemberUids(),
          patioId = patio.id,
          name = newName,
          kind = patio.kind,
          lastLetter = newLastLetter,
        )
        .onSuccess {
          transientState.update {
            it.copy(isSaving = false, snackbarMessage = "\"${newName.trim()}\" atualizado.")
          }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun deletePatio(patio: Patio) {
    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      repository
        .deletePatio(
          userId = currentUserId,
          actorName = currentActorName(),
          workspaceId = patio.workspaceId.ifBlank { currentWorkspaceId() },
          memberUids = currentMemberUids(),
          patioId = patio.id,
          patioName = patio.name,
        )
        .onSuccess {
          transientState.update {
            it.copy(isSaving = false, snackbarMessage = "\"${patio.name}\" excluído.")
          }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun upsertSlot(
    patioId: String,
    street: String,
    idx: Int,
    type: SlotType,
    number: String?,
    bales: Int,
    note: String?,
    checked: Boolean,
    isEditingExistingBlock: Boolean = false,
  ) {
    val patioName = resolvePatioName(patioId)
    val actorName = currentActorName()
    val historyText =
      if (type == SlotType.BLOCO) {
        if (isEditingExistingBlock) {
          MapaRules.formatEditedBlockHistory(
            actorName = actorName,
            blockNumber = number.orEmpty(),
            bales = bales,
            patioName = patioName,
            street = street.uppercase(),
            positionNumber = idx + 1,
          )
        } else {
          MapaRules.formatAddedBlockHistory(
            actorName = actorName,
            blockNumber = number.orEmpty(),
            bales = bales,
            patioName = patioName,
            street = street.uppercase(),
            positionNumber = idx + 1,
          )
        }
      } else {
        MapaRules.formatVacantCreatedHistory(
          actorName = actorName,
          patioName = patioName,
          street = street.uppercase(),
          positionNumber = idx + 1,
        )
      }

    val existingSlot =
      uiState.value.allSlots.firstOrNull {
        it.patioId == patioId && it.street.equals(street, ignoreCase = true) && it.idx == idx
      }
    val invalidated =
      MapaRules.shouldInvalidateCheckedOnEdit(
        previousSlot = existingSlot,
        newType = type,
        newNumber = number,
        newBales = bales,
      )
    val effectiveChecked = if (type == SlotType.BLOCO && !invalidated) checked else false

    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      repository
        .upsertSlot(
          userId = currentUserId,
          actorName = actorName,
          workspaceId = currentWorkspaceId(),
          memberUids = currentMemberUids(),
          patioId = patioId,
          patioName = patioName,
          street = street,
          idx = idx,
          type = type,
          number = number,
          bales = bales,
          note = note,
          checked = effectiveChecked,
          historyText = historyText,
        )
        .onSuccess { saved ->
          val msg =
            if (saved.type == SlotType.BLOCO) {
              val excess = MapaRules.excessBales(saved.bales)
              if (excess > 0) {
                "Bloco ${saved.number.orEmpty()} salvo na Rua ${saved.street} (passa $excess fardos)."
              } else {
                "Bloco ${saved.number.orEmpty()} salvo na Rua ${saved.street}."
              }
            } else {
              "Posição ${saved.street}${saved.positionNumber} marcada como vaga."
            }
          transientState.update { it.copy(isSaving = false, snackbarMessage = msg) }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun convertBlockToVacant(
    patioId: String,
    street: String,
    idx: Int,
    blockNumber: String?,
  ) {
    val patioName = resolvePatioName(patioId)
    val actorName = currentActorName()
    val historyText =
      MapaRules.formatBlockToVacantHistory(
        actorName = actorName,
        blockNumber = blockNumber,
        patioName = patioName,
        street = street.uppercase(),
        positionNumber = idx + 1,
      )
    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      repository
        .upsertSlot(
          userId = currentUserId,
          actorName = actorName,
          workspaceId = currentWorkspaceId(),
          memberUids = currentMemberUids(),
          patioId = patioId,
          patioName = patioName,
          street = street,
          idx = idx,
          type = SlotType.VAGA,
          number = null,
          bales = 0,
          note = null,
          checked = false,
          historyText = historyText,
        )
        .onSuccess {
          val label = blockNumber?.takeIf { it.isNotBlank() }?.let { "Bloco $it excluído. " } ?: ""
          transientState.update {
            it.copy(
              isSaving = false,
              snackbarMessage = "${label}Posição ${street}${idx + 1} virou VAGA.",
            )
          }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun loadBalesFromSlot(
    patioId: String,
    street: String,
    idx: Int,
    currentSlot: Slot,
    loadedBales: Int,
  ) {
    when (val check = MapaRules.validateLoadedBales(currentSlot.bales, loadedBales)) {
      is MapaRules.LoadResult.Invalid -> {
        transientState.update { it.copy(errorMessage = check.message) }
      }
      is MapaRules.LoadResult.Valid -> {
        val patioName = resolvePatioName(patioId)
        val actorName = currentActorName()
        val historyText =
          MapaRules.formatLoadedBalesHistory(
            actorName = actorName,
            blockNumber = currentSlot.number.orEmpty(),
            loadedBales = loadedBales,
            remainingBales = check.remainingBales,
            becameVacant = check.becomesVacant,
          )
        viewModelScope.launch {
          transientState.update { it.copy(isSaving = true, errorMessage = null) }
          val result =
            if (check.becomesVacant) {
              repository.upsertSlot(
                userId = currentUserId,
                actorName = actorName,
                workspaceId = currentWorkspaceId(),
                memberUids = currentMemberUids(),
                patioId = patioId,
                patioName = patioName,
                street = street,
                idx = idx,
                type = SlotType.VAGA,
                number = null,
                bales = 0,
                note = null,
                checked = false,
                historyText = historyText,
              )
            } else {
              repository.upsertSlot(
                userId = currentUserId,
                actorName = actorName,
                workspaceId = currentWorkspaceId(),
                memberUids = currentMemberUids(),
                patioId = patioId,
                patioName = patioName,
                street = street,
                idx = idx,
                type = SlotType.BLOCO,
                number = currentSlot.number,
                bales = check.remainingBales,
                note = currentSlot.note,
                checked = false,
                historyText = historyText,
              )
            }
          result
            .onSuccess {
              val msg =
                if (check.becomesVacant) {
                  "Carregamento total do bloco ${currentSlot.number.orEmpty()}. Posição virou VAGA."
                } else {
                  "Carregados $loadedBales fardos do bloco ${currentSlot.number.orEmpty()}. Restam ${check.remainingBales}."
                }
              transientState.update { it.copy(isSaving = false, snackbarMessage = msg) }
            }
            .onFailure { err ->
              transientState.update {
                it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
              }
            }
        }
      }
    }
  }

  fun moveSlot(
    patio: Patio,
    street: String,
    fromIdx: Int,
    toIdx: Int,
  ) {
    val maxPositions = MapaRules.maxPositionsFor(patio.kind)
    if (toIdx !in 0 until maxPositions) return
    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      repository
        .moveSlot(
          userId = currentUserId,
          actorName = currentActorName(),
          workspaceId = patio.workspaceId.ifBlank { currentWorkspaceId() },
          memberUids = currentMemberUids(),
          patioId = patio.id,
          patioName = patio.name,
          street = street,
          fromIdx = fromIdx,
          toIdx = toIdx,
          maxPositions = maxPositions,
        )
        .onSuccess {
          val dir = if (toIdx < fromIdx) "cima" else "baixo"
          transientState.update {
            it.copy(
              isSaving = false,
              snackbarMessage = "Posição movida para $dir (${street}${toIdx + 1}).",
            )
          }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun mergeBlocks(
    destination: MapaRules.BlockLocationEntry,
    sources: List<MapaRules.BlockLocationEntry>,
  ) {
    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      repository
        .mergeBlocks(
          userId = currentUserId,
          actorName = currentActorName(),
          workspaceId = destination.patio.workspaceId.ifBlank { currentWorkspaceId() },
          memberUids = currentMemberUids(),
          destination = destination,
          sources = sources,
        )
        .onSuccess { newTotal ->
          val excess = MapaRules.excessBales(newTotal)
          val msg =
            if (excess > 0) {
              "Fardos juntados no bloco ${destination.slot.number.orEmpty()}: $newTotal fardos (passa $excess)."
            } else {
              "Fardos juntados no bloco ${destination.slot.number.orEmpty()}: $newTotal fardos."
            }
          transientState.update { it.copy(isSaving = false, snackbarMessage = msg) }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun deleteSlot(patioId: String, street: String, idx: Int) {
    val patioName = resolvePatioName(patioId)
    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      repository
        .deleteSlot(
          userId = currentUserId,
          actorName = currentActorName(),
          workspaceId = currentWorkspaceId(),
          memberUids = currentMemberUids(),
          patioId = patioId,
          patioName = patioName,
          street = street,
          idx = idx,
        )
        .onSuccess {
          transientState.update {
            it.copy(isSaving = false, snackbarMessage = "Posição removida da Rua $street.")
          }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun updateSearchQuery(query: String) {
    transientState.update { it.copy(searchQuery = query) }
  }

  fun updateConferenceFilter(filter: MapaRules.ConferenceFilter) {
    transientState.update { it.copy(conferenceFilter = filter) }
  }

  fun updateConferenceQuery(query: String) {
    val digitsOnly = query.filter { it.isDigit() }
    transientState.update { it.copy(conferenceQuery = digitsOnly) }
  }

  fun toggleBlockConference(item: MapaRules.ConferenceBlockItem) {
    if (item.locations.isEmpty()) return
    val newChecked = !item.isChecked
    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      repository
        .setBlockConferenceChecked(
          userId = currentUserId,
          actorName = currentActorName(),
          workspaceId = currentWorkspaceId(),
          memberUids = currentMemberUids(),
          blockNumber = item.displayNumber,
          locations = item.locations,
          checked = newChecked,
        )
        .onSuccess {
          val msg =
            if (newChecked) "Bloco ${item.displayNumber} marcado como conferido."
            else "Conferência do bloco ${item.displayNumber} desmarcada."
          transientState.update { it.copy(isSaving = false, snackbarMessage = msg) }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun clearAllConference() {
    val checkedSlots = uiState.value.allSlots.filter { it.isBlock && it.checked }
    if (checkedSlots.isEmpty()) {
      transientState.update {
        it.copy(snackbarMessage = "Nenhum bloco está conferido no momento.")
      }
      return
    }
    viewModelScope.launch {
      transientState.update { it.copy(isSaving = true, errorMessage = null) }
      repository
        .clearAllConferenceChecks(
          userId = currentUserId,
          actorName = currentActorName(),
          workspaceId = currentWorkspaceId(),
          memberUids = currentMemberUids(),
          checkedSlots = checkedSlots,
        )
        .onSuccess { count ->
          transientState.update {
            it.copy(
              isSaving = false,
              snackbarMessage = "Conferência limpa ($count posições desmarcadas).",
            )
          }
        }
        .onFailure { err ->
          transientState.update {
            it.copy(isSaving = false, errorMessage = friendlyFirestoreMessage(err))
          }
        }
    }
  }

  fun jumpToSlot(patioId: String, street: String, idx: Int) {
    selectPatio(patioId)
    transientState.update {
      it.copy(highlightedSlot = HighlightedSlot(patioId = patioId, street = street, idx = idx))
    }
    viewModelScope.launch {
      delay(5_000)
      transientState.update { state ->
        if (
          state.highlightedSlot?.patioId == patioId &&
            state.highlightedSlot.street == street &&
            state.highlightedSlot.idx == idx
        ) {
          state.copy(highlightedSlot = null)
        } else {
          state
        }
      }
    }
  }

  fun clearSnackbar() {
    transientState.update { it.copy(snackbarMessage = null) }
  }

  fun clearError() {
    transientState.update { it.copy(errorMessage = null) }
  }

  private fun buildUiState(
    patios: List<Patio>,
    slots: List<Slot>,
    history: List<HistoryEntry>,
    savedPatioId: String?,
    userCtx: UserContextBundle,
    syncStatus: SyncStatus,
    isLoading: Boolean,
    streamError: String?,
    transient: TransientState,
  ): MapaUiState {
    val slotsByPatio = slots.groupBy { it.patioId }
    val selectedPatio =
      patios.firstOrNull { it.id == savedPatioId } ?: patios.firstOrNull()

    val allPatioTotals = MapaRules.calculatePatioTotals(patios, slots)
    val selectedPatioTotals = allPatioTotals.firstOrNull { it.patio.id == selectedPatio?.id }

    val streetColumns =
      if (selectedPatio == null) {
        emptyList()
      } else {
        MapaRules.buildStreetColumns(selectedPatio, slotsByPatio[selectedPatio.id].orEmpty())
      }

    val blockOccurrences = MapaRules.buildBlockOccurrencesIndex(patios, slots)
    val patioMap = patios.associateBy { it.id }
    val allBlockLocations =
      slots
        .asSequence()
        .filter { it.type == SlotType.BLOCO && !it.number.isNullOrBlank() }
        .mapNotNull { slot ->
          val patio = patioMap[slot.patioId] ?: return@mapNotNull null
          MapaRules.BlockLocationEntry(patio = patio, slot = slot)
        }
        .sortedWith(
          compareBy<MapaRules.BlockLocationEntry> { it.patio.id != selectedPatio?.id }
            .thenBy { it.patio.name }
            .thenBy { it.slot.street }
            .thenBy { it.slot.idx }
        )
        .toList()

    val searchResultGroup = MapaRules.searchBlocks(transient.searchQuery, patios, slots)
    val conferenceSummary = MapaRules.buildConferenceSummary(patios, slots)
    val filteredConferenceItems =
      MapaRules.filterConferenceItems(
        items = conferenceSummary.allItems,
        filter = transient.conferenceFilter,
        numberPrefixQuery = transient.conferenceQuery,
      )
    val historyGroups = MapaRules.groupHistoryByDay(history)

    return MapaUiState(
      patios = patios,
      selectedPatio = selectedPatio,
      streetColumns = streetColumns,
      selectedPatioTotals = selectedPatioTotals,
      allPatioTotals = allPatioTotals,
      grandTotalBlocks = allPatioTotals.sumOf { it.blockCount },
      grandTotalBales = allPatioTotals.sumOf { it.totalBales },
      grandTotalVacant = allPatioTotals.sumOf { it.vacantCount },
      blockOccurrences = blockOccurrences,
      allBlockLocations = allBlockLocations,
      allSlots = slots,
      searchResults = searchResultGroup,
      searchQuery = transient.searchQuery,
      conferenceSummary = conferenceSummary,
      conferenceFilter = transient.conferenceFilter,
      conferenceQuery = transient.conferenceQuery,
      filteredConferenceItems = filteredConferenceItems,
      historyEntries = history,
      historyGroups = historyGroups,
      userProfile = userCtx.profile,
      workspace = userCtx.workspace,
      usersById = userCtx.usersById,
      syncStatus = syncStatus,
      isLoading = isLoading && streamError == null,
      isSaving = transient.isSaving,
      errorMessage = transient.errorMessage ?: streamError,
      snackbarMessage = transient.snackbarMessage,
      highlightedSlot = transient.highlightedSlot,
    )
  }
}
