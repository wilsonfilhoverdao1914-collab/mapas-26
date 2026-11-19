package com.example.domain.model

enum class PatioKind(
  val dbValue: String,
  val label: String,
  val maxPositionsPerStreet: Int,
) {
  PATIO(dbValue = "patio", label = "Pátio", maxPositionsPerStreet = 50),
  BARRACAO(dbValue = "barracao", label = "Barracão", maxPositionsPerStreet = 100);

  companion object {
    fun fromDb(value: String?): PatioKind {
      return when (value?.lowercase()?.trim()) {
        "barracao", "barracão" -> BARRACAO
        else -> PATIO
      }
    }
  }
}

enum class SlotType(val dbValue: String) {
  BLOCO("B"),
  VAGA("V");

  companion object {
    fun fromDb(value: String?): SlotType {
      return if (value?.uppercase()?.trim() == "V") VAGA else BLOCO
    }
  }
}

data class UserProfile(
  val uid: String,
  val name: String,
  val workspaceId: String,
  val updatedAt: Long = 0L,
)

data class Workspace(
  val id: String,
  val ownerId: String,
  val name: String,
  val memberUids: List<String>,
  val createdAt: Long = 0L,
)

data class HistoryEntry(
  val id: String,
  val ts: Long,
  val userId: String,
  val text: String,
  val patioName: String,
  val workspaceId: String = "",
  val isPending: Boolean = false,
)

enum class SyncStateKind {
  ONLINE_SYNCED,
  SYNCING,
  OFFLINE,
}

data class SyncStatus(
  val isOnline: Boolean = true,
  val pendingWritesCount: Int = 0,
  val isSyncing: Boolean = false,
) {
  val kind: SyncStateKind
    get() =
      when {
        !isOnline -> SyncStateKind.OFFLINE
        pendingWritesCount > 0 || isSyncing -> SyncStateKind.SYNCING
        else -> SyncStateKind.ONLINE_SYNCED
      }

  val statusLabel: String
    get() =
      when (kind) {
        SyncStateKind.ONLINE_SYNCED -> "Online, sincronizado"
        SyncStateKind.SYNCING -> "Sincronizando..."
        SyncStateKind.OFFLINE ->
          if (pendingWritesCount == 1) {
            "Sem internet · 1 alteração para enviar"
          } else {
            "Sem internet · $pendingWritesCount alterações para enviar"
          }
      }
}

data class Patio(
  val id: String,
  val name: String,
  val kind: PatioKind,
  val last: String,
  val ts: Long = 0L,
  val workspaceId: String = "",
  val memberUids: List<String> = emptyList(),
) {
  val normalizedLast: Char
    get() =
      when (kind) {
        PatioKind.BARRACAO -> 'B'
        PatioKind.PATIO -> last.uppercase().firstOrNull()?.coerceIn('A', 'Z') ?: 'A'
      }

  val streets: List<String>
    get() =
      when (kind) {
        PatioKind.BARRACAO -> listOf("A", "B")
        PatioKind.PATIO -> ('A'..normalizedLast).map { it.toString() }
      }

  val streetRangeLabel: String
    get() =
      when (kind) {
        PatioKind.BARRACAO -> "Ruas A e B"
        PatioKind.PATIO -> if (normalizedLast == 'A') "Rua A" else "Ruas A–$normalizedLast"
      }
}

data class Slot(
  val patioId: String,
  val street: String,
  val idx: Int,
  val type: SlotType,
  val number: String? = null,
  val bales: Int = 0,
  val note: String? = null,
  val checked: Boolean = false,
  val updatedAt: Long = 0L,
  val updatedByUid: String? = null,
  val updatedByName: String? = null,
  val hasPendingWrites: Boolean = false,
  val workspaceId: String = "",
  val memberUids: List<String> = emptyList(),
) {
  val docId: String
    get() = "${patioId}_${street.uppercase()}_${idx.toString().padStart(3, '0')}"

  val positionNumber: Int
    get() = idx + 1

  val isBlock: Boolean
    get() = type == SlotType.BLOCO
}

data class StreetColumnData(
  val street: String,
  val slots: List<Slot>,
  val blockCount: Int,
  val totalBales: Int,
  val maxPositions: Int,
) {
  val vacantIndices: List<Int>
    get() = slots.filter { it.type == SlotType.VAGA }.map { it.idx }.sorted()

  val hasVacancy: Boolean
    get() = vacantIndices.isNotEmpty()

  val canAddMore: Boolean
    get() = hasVacancy || slots.size < maxPositions

  val nextIdx: Int
    get() = vacantIndices.firstOrNull() ?: slots.size
}

data class PatioTotals(
  val patio: Patio,
  val blockCount: Int,
  val totalBales: Int,
  val vacantCount: Int,
)

data class SearchMatch(
  val patio: Patio,
  val slot: Slot,
)

data class SearchResultGroup(
  val query: String,
  val matches: List<SearchMatch>,
  val totalBales: Int,
  val distinctBlockNumbers: List<String>,
)
