package com.example.data.repository

import android.content.Context
import com.example.data.remote.FirestoreProvider
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.domain.model.HistoryEntry
import com.example.domain.model.Patio
import com.example.domain.model.PatioKind
import com.example.domain.model.Slot
import com.example.domain.model.SlotType
import com.example.domain.model.UserProfile
import com.example.domain.model.Workspace
import com.example.domain.rules.MapaRules
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class MapaRepository(
  private val db: FirebaseFirestore,
  private val isOnlineProvider: () -> Boolean = { true },
  private val auth: FirebaseAuth = Firebase.auth,
) : MapaDataSource {
  constructor(
    context: Context,
    isOnlineProvider: () -> Boolean = { true },
  ) : this(
    db = FirestoreProvider.getDatabase(context),
    isOnlineProvider = isOnlineProvider,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("É necessário entrar com o Google para acessar o Firestore.")
  }

  private fun normalizedMembers(uid: String, memberUids: List<String>): List<String> {
    return (memberUids + uid)
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .distinct()
      .take(10)
  }

  private fun addHistoryToBatch(
    batch: WriteBatch,
    uid: String,
    workspaceId: String,
    members: List<String>,
    text: String,
    patioName: String,
  ) {
    val historyId = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
    val historyRef = db.collection("history").document(historyId)
    val payload =
      mapOf(
        "ts" to FieldValue.serverTimestamp(),
        "u" to uid,
        "t" to text.trim().ifEmpty { "Alteração no mapa" }.take(500),
        "p" to patioName.trim().ifEmpty { "Pátio" }.take(80),
        "workspaceId" to workspaceId,
        "memberUids" to members,
      )
    batch.set(historyRef, payload)
  }

  private suspend fun Task<Void>.awaitCommitOfflineSafe() {
    if (!isOnlineProvider()) {
      // Escrita gravada no cache local persistente; o Firestore envia ao reconectar.
      return
    }
    withTimeoutOrNull(1_500L) {
      await()
    }
  }

  private suspend fun DocumentReference.getCacheThenServer(): DocumentSnapshot? {
    val cached =
      try {
        get(Source.CACHE).await()
      } catch (_: Exception) {
        null
      }
    if (cached != null && cached.exists()) return cached
    if (!isOnlineProvider()) return cached
    return try {
      withTimeoutOrNull(1_200L) { get(Source.SERVER).await() } ?: cached
    } catch (_: Exception) {
      cached
    }
  }

  override fun observePatios(userId: String): Flow<SyncedSnapshot<List<Patio>>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Sessão alterada; acesso negado." }
    val path = "patios"
    emitAll(
      db.collection(path)
        .whereArrayContains("memberUids", uid)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snapshot ->
          val pendingDocs = snapshot.documents.count { it.metadata.hasPendingWrites() }
          val pending =
            if (pendingDocs > 0) pendingDocs
            else if (snapshot.metadata.hasPendingWrites()) 1
            else 0
          SyncedSnapshot(
            data = snapshot.documents.map { it.toPatio() }.sortedBy { it.ts },
            pendingWritesCount = pending,
            isFromCache = snapshot.metadata.isFromCache,
          )
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  override fun observeSlots(userId: String): Flow<SyncedSnapshot<List<Slot>>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Sessão alterada; acesso negado." }
    val path = "slots"
    emitAll(
      db.collection(path)
        .whereArrayContains("memberUids", uid)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snapshot ->
          val pendingDocs = snapshot.documents.count { it.metadata.hasPendingWrites() }
          val pending =
            if (pendingDocs > 0) pendingDocs
            else if (snapshot.metadata.hasPendingWrites()) 1
            else 0
          SyncedSnapshot(
            data =
              snapshot.documents
                .map { it.toSlot() }
                .sortedWith(compareBy<Slot> { it.street }.thenBy { it.idx }),
            pendingWritesCount = pending,
            isFromCache = snapshot.metadata.isFromCache,
          )
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  override fun observeHistory(userId: String): Flow<SyncedSnapshot<List<HistoryEntry>>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Sessão alterada; acesso negado." }
    val path = "history"
    emitAll(
      db.collection(path)
        .whereArrayContains("memberUids", uid)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snapshot ->
          val pendingDocs = snapshot.documents.count { it.metadata.hasPendingWrites() }
          val pending =
            if (pendingDocs > 0) pendingDocs
            else if (snapshot.metadata.hasPendingWrites()) 1
            else 0
          val entries =
            snapshot.documents
              .map { it.toHistoryEntry() }
              .sortedByDescending { it.ts }
              .take(MapaRules.MAX_HISTORY_ENTRIES)
          SyncedSnapshot(
            data = entries,
            pendingWritesCount = pending,
            isFromCache = snapshot.metadata.isFromCache,
          )
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  override fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Sessão alterada; acesso negado." }
    val docPath = "users/$uid"
    emitAll(
      db.collection("users")
        .document(uid)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap -> if (snap.exists()) snap.toUserProfile() else null }
        .catch { error ->
          if (error is CancellationException) throw error
          handleFirestoreError(error, OperationType.GET, docPath)
          emit(null)
        }
    )
  }

  override fun observeWorkspace(workspaceId: String): Flow<Workspace?> = flow {
    if (workspaceId.isBlank()) {
      emit(null)
      return@flow
    }
    requireUserId()
    val docPath = "workspaces/$workspaceId"
    emitAll(
      db.collection("workspaces")
        .document(workspaceId)
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap -> if (snap.exists()) snap.toWorkspace() else null }
        .catch { error ->
          if (error is CancellationException) throw error
          handleFirestoreError(error, OperationType.GET, docPath)
          emit(null)
        }
    )
  }

  override fun observeWorkspaceUsers(memberUids: List<String>): Flow<Map<String, UserProfile>> {
    val cleanUids = memberUids.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(10)
    if (cleanUids.isEmpty()) return flowOf(emptyMap())
    val flows =
      cleanUids.map { memberUid ->
        db.collection("users")
          .document(memberUid)
          .snapshots(MetadataChanges.INCLUDE)
          .map { snap -> if (snap.exists()) snap.toUserProfile() else null }
          .catch { emit(null) }
      }
    return combine(flows) { profiles ->
      profiles.filterNotNull().associateBy { it.uid }
    }
  }

  override suspend fun ensureUserAndWorkspace(
    userId: String,
    preferredName: String?,
    fallbackWorkspaceId: String?,
  ): Result<UserProfile> {
    val userPath = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }

      val userRef = db.collection("users").document(uid)
      val existingSnap = userRef.getCacheThenServer()
      if (existingSnap != null && existingSnap.exists()) {
        val existingProfile = existingSnap.toUserProfile()
        val wsRef = db.collection("workspaces").document(existingProfile.workspaceId)
        val wsSnap = wsRef.getCacheThenServer()
        if (wsSnap == null || !wsSnap.exists()) {
          val wsPayload =
            mapOf(
              "ownerId" to uid,
              "name" to "Equipe Principal",
              "memberUids" to listOf(uid),
              "createdAt" to FieldValue.serverTimestamp(),
            )
          wsRef.set(wsPayload).awaitCommitOfflineSafe()
        }
        return Result.success(existingProfile)
      }

      val cleanName =
        preferredName?.trim()?.take(60)?.takeIf { it.isNotEmpty() }
          ?: auth.currentUser?.displayName?.trim()?.take(60)?.takeIf { it.isNotEmpty() }
          ?: "Operador"
      val newWorkspaceId =
        fallbackWorkspaceId?.takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]{3,64}$")) }
          ?: "EQ_${UUID.randomUUID().toString().replace("-", "").take(6).uppercase()}"

      val wsRef = db.collection("workspaces").document(newWorkspaceId)
      val batch = db.batch()
      batch.set(
        wsRef,
        mapOf(
          "ownerId" to uid,
          "name" to "Equipe Principal",
          "memberUids" to listOf(uid),
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        userRef,
        mapOf(
          "uid" to uid,
          "name" to cleanName,
          "workspaceId" to newWorkspaceId,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().awaitCommitOfflineSafe()

      Result.success(
        UserProfile(
          uid = uid,
          name = cleanName,
          workspaceId = newWorkspaceId,
          updatedAt = System.currentTimeMillis(),
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userPath)
      Result.failure(e)
    }
  }

  override suspend fun updateUserName(userId: String, newName: String): Result<Unit> {
    val userPath = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }
      val cleanName = newName.trim().take(60)
      require(cleanName.isNotEmpty()) { "Informe seu nome." }

      val userRef = db.collection("users").document(uid)
      val existing = userRef.getCacheThenServer()
      if (existing != null && existing.exists()) {
        userRef
          .update(
            mapOf(
              "name" to cleanName,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .awaitCommitOfflineSafe()
      } else {
        ensureUserAndWorkspace(uid, preferredName = cleanName)
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userPath)
      Result.failure(e)
    }
  }

  override suspend fun joinWorkspaceByCode(
    userId: String,
    workspaceCode: String,
    userName: String,
  ): Result<Workspace> {
    val cleanCode = workspaceCode.trim().uppercase()
    val wsPath = "workspaces/$cleanCode"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }
      require(cleanCode.matches(Regex("^[a-zA-Z0-9_\\-]{3,64}$"))) {
        "Código de equipe inválido."
      }
      if (!isOnlineProvider()) {
        return Result.failure(
          IllegalStateException("É preciso estar online para entrar em uma equipe pelo código.")
        )
      }

      val wsRef = db.collection("workspaces").document(cleanCode)
      val wsSnap = wsRef.get(Source.SERVER).await()
      if (!wsSnap.exists()) {
        return Result.failure(
          IllegalArgumentException("Equipe \"$cleanCode\" não encontrada.")
        )
      }

      val workspace = wsSnap.toWorkspace()
      if (uid !in workspace.memberUids && workspace.memberUids.size >= 10) {
        return Result.failure(
          IllegalStateException("Esta equipe já atingiu o limite máximo de 10 aparelhos.")
        )
      }

      val cleanName = userName.trim().take(60).ifEmpty { "Operador" }
      val userRef = db.collection("users").document(uid)
      val existingUserSnap = userRef.getCacheThenServer()

      val batch = db.batch()
      if (uid !in workspace.memberUids) {
        batch.update(wsRef, "memberUids", FieldValue.arrayUnion(uid))
      }
      if (existingUserSnap != null && existingUserSnap.exists()) {
        batch.update(
          userRef,
          mapOf(
            "name" to cleanName,
            "workspaceId" to cleanCode,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        batch.set(
          userRef,
          mapOf(
            "uid" to uid,
            "name" to cleanName,
            "workspaceId" to cleanCode,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()

      val updatedMembers = (workspace.memberUids + uid).distinct()
      Result.success(workspace.copy(memberUids = updatedMembers))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, wsPath)
      Result.failure(e)
    }
  }

  override suspend fun propagateWorkspaceMembers(
    userId: String,
    workspace: Workspace,
    patios: List<Patio>,
    slots: List<Slot>,
  ): Result<Unit> {
    return try {
      val uid = requireUserId()
      if (uid != userId || uid !in workspace.memberUids) return Result.success(Unit)
      val targetMembers = workspace.memberUids.distinct().take(10)
      val targetSet = targetMembers.toSet()

      val outdatedPatios =
        patios.filter {
          it.workspaceId == workspace.id &&
            uid in it.memberUids &&
            it.memberUids.toSet() != targetSet
        }
      val outdatedSlots =
        slots.filter {
          it.workspaceId == workspace.id &&
            uid in it.memberUids &&
            it.memberUids.toSet() != targetSet
        }
      if (outdatedPatios.isEmpty() && outdatedSlots.isEmpty()) {
        return Result.success(Unit)
      }

      val refsToUpdate =
        outdatedPatios.map { db.collection("patios").document(it.id) } +
          outdatedSlots.map { db.collection("slots").document(it.docId) }

      refsToUpdate.chunked(350).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { ref -> batch.update(ref, "memberUids", targetMembers) }
        batch.commit().awaitCommitOfflineSafe()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "workspaces/${workspace.id}")
      Result.failure(e)
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
    var docPath = "patios"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }
      val cleanName = name.trim().take(80)
      require(cleanName.isNotEmpty()) { "Informe o nome do local." }
      val normalizedLast =
        if (kind == PatioKind.BARRACAO) "B"
        else lastLetter.uppercase().firstOrNull()?.coerceIn('A', 'Z')?.toString() ?: "A"
      val members = normalizedMembers(uid, memberUids)

      val docRef = db.collection("patios").document()
      docPath = docRef.path
      val batch = db.batch()
      batch.set(
        docRef,
        mapOf(
          "userId" to uid,
          "workspaceId" to workspaceId,
          "memberUids" to members,
          "name" to cleanName,
          "kind" to kind.dbValue,
          "last" to normalizedLast,
          "ts" to FieldValue.serverTimestamp(),
        ),
      )
      addHistoryToBatch(
        batch = batch,
        uid = uid,
        workspaceId = workspaceId,
        members = members,
        text = MapaRules.formatPatioCreatedHistory(actorName, cleanName, kind),
        patioName = cleanName,
      )
      batch.commit().awaitCommitOfflineSafe()

      Result.success(
        Patio(
          id = docRef.id,
          name = cleanName,
          kind = kind,
          last = normalizedLast,
          ts = System.currentTimeMillis(),
          workspaceId = workspaceId,
          memberUids = members,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
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
    val docPath = "patios/$patioId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }
      require(patioId.isNotBlank()) { "ID do pátio inválido." }
      val cleanName = name.trim().take(80)
      require(cleanName.isNotEmpty()) { "Informe o nome do local." }
      val normalizedLast =
        if (kind == PatioKind.BARRACAO) "B"
        else lastLetter.uppercase().firstOrNull()?.coerceIn('A', 'Z')?.toString() ?: "A"
      val members = normalizedMembers(uid, memberUids)

      val docRef = db.collection("patios").document(patioId)
      val batch = db.batch()
      batch.update(
        docRef,
        mapOf(
          "name" to cleanName,
          "last" to normalizedLast,
          "memberUids" to members,
        ),
      )
      addHistoryToBatch(
        batch = batch,
        uid = uid,
        workspaceId = workspaceId,
        members = members,
        text = MapaRules.formatPatioUpdatedHistory(actorName, cleanName),
        patioName = cleanName,
      )
      batch.commit().awaitCommitOfflineSafe()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  override suspend fun deletePatio(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    patioId: String,
    patioName: String,
  ): Result<Unit> {
    val patioPath = "patios/$patioId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }
      require(patioId.isNotBlank()) { "ID do pátio inválido." }
      val members = normalizedMembers(uid, memberUids)

      val query =
        db.collection("slots")
          .whereArrayContains("memberUids", uid)
          .whereEqualTo("pid", patioId)
      val slotsSnapshot =
        try {
          query.get(Source.CACHE).await()
        } catch (_: Exception) {
          if (isOnlineProvider()) query.get().await() else null
        }

      val slotRefs = slotsSnapshot?.documents?.map { it.reference }.orEmpty()
      val chunks = slotRefs.chunked(350)
      if (chunks.isEmpty()) {
        val batch = db.batch()
        batch.delete(db.collection("patios").document(patioId))
        addHistoryToBatch(
          batch = batch,
          uid = uid,
          workspaceId = workspaceId,
          members = members,
          text = MapaRules.formatPatioDeletedHistory(actorName, patioName),
          patioName = patioName,
        )
        batch.commit().awaitCommitOfflineSafe()
      } else {
        chunks.forEachIndexed { index, chunk ->
          val batch = db.batch()
          chunk.forEach { ref -> batch.delete(ref) }
          if (index == chunks.lastIndex) {
            batch.delete(db.collection("patios").document(patioId))
            addHistoryToBatch(
              batch = batch,
              uid = uid,
              workspaceId = workspaceId,
              members = members,
              text = MapaRules.formatPatioDeletedHistory(actorName, patioName),
              patioName = patioName,
            )
          }
          batch.commit().awaitCommitOfflineSafe()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, patioPath)
      Result.failure(e)
    }
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
    val normalizedStreet = street.uppercase().take(1)
    val docId = "${patioId}_${normalizedStreet}_${idx.toString().padStart(3, '0')}"
    val docPath = "slots/$docId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }
      require(patioId.isNotBlank()) { "ID do pátio inválido." }
      require(idx in 0 until 100) { "Posição fora do limite." }

      val cleanActor = actorName.trim().take(60).ifEmpty { "Operador" }
      val members = normalizedMembers(uid, memberUids)
      val cleanNumber =
        if (type == SlotType.BLOCO) number?.trim()?.take(32)?.takeIf { it.isNotEmpty() } else null
      val cleanBales = if (type == SlotType.BLOCO) bales.coerceIn(0, 1_000_000) else 0
      val cleanNote = note?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
      val cleanChecked = if (type == SlotType.BLOCO && checked) 1 else 0

      val docRef = db.collection("slots").document(docId)
      val existingSnap = docRef.getCacheThenServer()
      val exists = existingSnap != null && existingSnap.exists()

      val batch = db.batch()
      if (!exists) {
        val createPayload =
          mapOf(
            "userId" to uid,
            "workspaceId" to workspaceId,
            "memberUids" to members,
            "pid" to patioId,
            "s" to normalizedStreet,
            "i" to idx,
            "t" to type.dbValue,
            "n" to cleanNumber,
            "f" to cleanBales,
            "o" to cleanNote,
            "c" to cleanChecked,
            "updatedByUid" to uid,
            "updatedByName" to cleanActor,
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        batch.set(docRef, createPayload)
      } else {
        val updatePayload =
          mapOf<String, Any>(
            "t" to type.dbValue,
            "f" to cleanBales,
            "c" to cleanChecked,
            "n" to (cleanNumber ?: FieldValue.delete()),
            "o" to (cleanNote ?: FieldValue.delete()),
            "memberUids" to members,
            "updatedByUid" to uid,
            "updatedByName" to cleanActor,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        batch.update(docRef, updatePayload)
      }

      addHistoryToBatch(
        batch = batch,
        uid = uid,
        workspaceId = workspaceId,
        members = members,
        text = historyText,
        patioName = patioName,
      )
      batch.commit().awaitCommitOfflineSafe()

      Result.success(
        Slot(
          patioId = patioId,
          street = normalizedStreet,
          idx = idx,
          type = type,
          number = cleanNumber,
          bales = cleanBales,
          note = cleanNote,
          checked = cleanChecked == 1,
          updatedAt = System.currentTimeMillis(),
          updatedByUid = uid,
          updatedByName = cleanActor,
          workspaceId = workspaceId,
          memberUids = members,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
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
    val docId = "${patioId}_${normalizedStreet}_${idx.toString().padStart(3, '0')}"
    val docPath = "slots/$docId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }
      val cleanActor = actorName.trim().take(60).ifEmpty { "Operador" }
      val members = normalizedMembers(uid, memberUids)

      val batch = db.batch()
      batch.delete(db.collection("slots").document(docId))
      addHistoryToBatch(
        batch = batch,
        uid = uid,
        workspaceId = workspaceId,
        members = members,
        text = "$cleanActor removeu a posição vazia da Rua $normalizedStreet (${idx + 1}) no $patioName",
        patioName = patioName,
      )
      batch.commit().awaitCommitOfflineSafe()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
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
    val normalizedStreet = street.uppercase().take(1)
    val fromDocId = "${patioId}_${normalizedStreet}_${fromIdx.toString().padStart(3, '0')}"
    val toDocId = "${patioId}_${normalizedStreet}_${toIdx.toString().padStart(3, '0')}"
    val opPath = "slots/$fromDocId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }
      require(patioId.isNotBlank()) { "ID do pátio inválido." }
      require(fromIdx in 0 until maxPositions && toIdx in 0 until maxPositions && fromIdx != toIdx) {
        "Posição de destino fora do limite da rua."
      }
      val cleanActor = actorName.trim().take(60).ifEmpty { "Operador" }
      val members = normalizedMembers(uid, memberUids)

      val fromRef = db.collection("slots").document(fromDocId)
      val toRef = db.collection("slots").document(toDocId)
      val fromSnap = fromRef.getCacheThenServer()
      val toSnap = toRef.getCacheThenServer()

      val fromSlot =
        if (fromSnap != null && fromSnap.exists()) fromSnap.toSlot()
        else Slot(patioId = patioId, street = normalizedStreet, idx = fromIdx, type = SlotType.VAGA)
      val toSlot =
        if (toSnap != null && toSnap.exists()) toSnap.toSlot()
        else Slot(patioId = patioId, street = normalizedStreet, idx = toIdx, type = SlotType.VAGA)

      val batch = db.batch()
      if (toSnap != null && toSnap.exists()) {
        batch.update(toRef, buildSlotUpdatePayload(uid, cleanActor, members, fromSlot))
      } else {
        batch.set(
          toRef,
          buildSlotCreatePayload(uid, cleanActor, workspaceId, members, patioId, normalizedStreet, toIdx, fromSlot),
        )
      }

      if (fromSnap != null && fromSnap.exists()) {
        batch.update(fromRef, buildSlotUpdatePayload(uid, cleanActor, members, toSlot))
      } else {
        batch.set(
          fromRef,
          buildSlotCreatePayload(uid, cleanActor, workspaceId, members, patioId, normalizedStreet, fromIdx, toSlot),
        )
      }

      addHistoryToBatch(
        batch = batch,
        uid = uid,
        workspaceId = workspaceId,
        members = members,
        text =
          MapaRules.formatMoveHistory(
            actorName = cleanActor,
            blockNumber = if (fromSlot.isBlock) fromSlot.number else null,
            patioName = patioName,
            street = normalizedStreet,
            fromPosition = fromIdx + 1,
            toPosition = toIdx + 1,
          ),
        patioName = patioName,
      )
      batch.commit().awaitCommitOfflineSafe()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, opPath)
      Result.failure(e)
    }
  }

  override suspend fun mergeBlocks(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    destination: MapaRules.BlockLocationEntry,
    sources: List<MapaRules.BlockLocationEntry>,
  ): Result<Int> {
    val destDocPath = "slots/${destination.slot.docId}"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }
      val preview = MapaRules.previewMerge(destination, sources)
      require(preview.sources.isNotEmpty()) { "Selecione ao menos um bloco de origem." }
      require(preview.resultingBales in 0..1_000_000) { "Total de fardos fora do limite." }
      val cleanActor = actorName.trim().take(60).ifEmpty { "Operador" }
      val members = normalizedMembers(uid, memberUids)

      val destRef = db.collection("slots").document(destination.slot.docId)
      val sourceRefs = preview.sources.map { db.collection("slots").document(it.slot.docId) }

      val chunks = sourceRefs.chunked(350)
      chunks.forEachIndexed { index, chunk ->
        val batch = db.batch()
        if (index == 0) {
          batch.update(
            destRef,
            mapOf(
              "f" to preview.resultingBales,
              "c" to 0,
              "memberUids" to members,
              "updatedByUid" to uid,
              "updatedByName" to cleanActor,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        val vacantUpdate =
          mapOf<String, Any>(
            "t" to SlotType.VAGA.dbValue,
            "f" to 0,
            "c" to 0,
            "n" to FieldValue.delete(),
            "o" to FieldValue.delete(),
            "memberUids" to members,
            "updatedByUid" to uid,
            "updatedByName" to cleanActor,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        chunk.forEach { srcRef -> batch.update(srcRef, vacantUpdate) }
        if (index == chunks.lastIndex) {
          addHistoryToBatch(
            batch = batch,
            uid = uid,
            workspaceId = workspaceId,
            members = members,
            text =
              MapaRules.formatMergeHistory(
                actorName = cleanActor,
                destNumber = destination.slot.number.orEmpty(),
                sources = preview.sources,
                resultingBales = preview.resultingBales,
              ),
            patioName = destination.patio.name,
          )
        }
        batch.commit().awaitCommitOfflineSafe()
      }

      Result.success(preview.resultingBales)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, destDocPath)
      Result.failure(e)
    }
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
    val firstPath = "slots/${locations.first().slot.docId}"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }
      val cleanActor = actorName.trim().take(60).ifEmpty { "Operador" }
      val members = normalizedMembers(uid, memberUids)
      val cVal = if (checked) 1 else 0

      val chunks = locations.chunked(350)
      chunks.forEachIndexed { index, chunk ->
        val batch = db.batch()
        val payload =
          mapOf<String, Any>(
            "c" to cVal,
            "memberUids" to members,
            "updatedByUid" to uid,
            "updatedByName" to cleanActor,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        chunk.forEach { loc ->
          batch.update(db.collection("slots").document(loc.slot.docId), payload)
        }
        if (index == chunks.lastIndex) {
          addHistoryToBatch(
            batch = batch,
            uid = uid,
            workspaceId = workspaceId,
            members = members,
            text = MapaRules.formatConferenceCheckedHistory(cleanActor, blockNumber, checked),
            patioName = locations.firstOrNull()?.patio?.name ?: "Conferência",
          )
        }
        batch.commit().awaitCommitOfflineSafe()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, firstPath)
      Result.failure(e)
    }
  }

  override suspend fun clearAllConferenceChecks(
    userId: String,
    actorName: String,
    workspaceId: String,
    memberUids: List<String>,
    checkedSlots: List<Slot>,
  ): Result<Int> {
    if (checkedSlots.isEmpty()) return Result.success(0)
    val firstPath = "slots/${checkedSlots.first().docId}"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Usuário alterado; operação recusada." }
      val cleanActor = actorName.trim().take(60).ifEmpty { "Operador" }
      val members = normalizedMembers(uid, memberUids)

      val chunks = checkedSlots.chunked(350)
      chunks.forEachIndexed { index, chunk ->
        val batch = db.batch()
        val payload =
          mapOf<String, Any>(
            "c" to 0,
            "memberUids" to members,
            "updatedByUid" to uid,
            "updatedByName" to cleanActor,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        chunk.forEach { slot ->
          batch.update(db.collection("slots").document(slot.docId), payload)
        }
        if (index == chunks.lastIndex) {
          addHistoryToBatch(
            batch = batch,
            uid = uid,
            workspaceId = workspaceId,
            members = members,
            text = MapaRules.formatClearConferenceHistory(cleanActor, checkedSlots.size),
            patioName = "Conferência",
          )
        }
        batch.commit().awaitCommitOfflineSafe()
      }
      Result.success(checkedSlots.size)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, firstPath)
      Result.failure(e)
    }
  }

  private fun buildSlotCreatePayload(
    uid: String,
    actorName: String,
    workspaceId: String,
    members: List<String>,
    patioId: String,
    street: String,
    targetIdx: Int,
    sourceSlot: Slot,
  ): Map<String, Any> {
    val cleanNumber =
      if (sourceSlot.type == SlotType.BLOCO) sourceSlot.number?.trim()?.take(32)?.takeIf { it.isNotEmpty() }
      else null
    val cleanBales =
      if (sourceSlot.type == SlotType.BLOCO) sourceSlot.bales.coerceIn(0, 1_000_000) else 0
    val cleanNote = sourceSlot.note?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
    val cleanChecked = if (sourceSlot.type == SlotType.BLOCO && sourceSlot.checked) 1 else 0

    return mapOf(
      "userId" to uid,
      "workspaceId" to workspaceId,
      "memberUids" to members,
      "pid" to patioId,
      "s" to street,
      "i" to targetIdx,
      "t" to sourceSlot.type.dbValue,
      "n" to cleanNumber,
      "f" to cleanBales,
      "o" to cleanNote,
      "c" to cleanChecked,
      "updatedByUid" to uid,
      "updatedByName" to actorName,
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
  }

  private fun buildSlotUpdatePayload(
    uid: String,
    actorName: String,
    members: List<String>,
    sourceSlot: Slot,
  ): Map<String, Any> {
    val cleanNumber =
      if (sourceSlot.type == SlotType.BLOCO) sourceSlot.number?.trim()?.take(32)?.takeIf { it.isNotEmpty() }
      else null
    val cleanBales =
      if (sourceSlot.type == SlotType.BLOCO) sourceSlot.bales.coerceIn(0, 1_000_000) else 0
    val cleanNote = sourceSlot.note?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
    val cleanChecked = if (sourceSlot.type == SlotType.BLOCO && sourceSlot.checked) 1 else 0

    return mapOf(
      "t" to sourceSlot.type.dbValue,
      "f" to cleanBales,
      "c" to cleanChecked,
      "n" to (cleanNumber ?: FieldValue.delete()),
      "o" to (cleanNote ?: FieldValue.delete()),
      "memberUids" to members,
      "updatedByUid" to uid,
      "updatedByName" to actorName,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
  }

  private fun DocumentSnapshot.toPatio(): Patio {
    val tsVal =
      getTimestamp("ts", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
    val members =
      (get("memberUids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    return Patio(
      id = id,
      name = getString("name").orEmpty(),
      kind = PatioKind.fromDb(getString("kind")),
      last = getString("last") ?: "A",
      ts = tsVal,
      workspaceId = getString("workspaceId").orEmpty(),
      memberUids = members,
    )
  }

  private fun DocumentSnapshot.toSlot(): Slot {
    val updatedVal =
      getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
        ?: 0L
    val rawData = data
    val idxVal = (rawData?.get("i") as? Number)?.toInt() ?: 0
    val balesVal = (rawData?.get("f") as? Number)?.toInt() ?: 0
    val checkedVal = (rawData?.get("c") as? Number)?.toInt() ?: 0
    val members =
      (rawData?.get("memberUids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    return Slot(
      patioId = getString("pid").orEmpty(),
      street = (getString("s") ?: "A").uppercase(),
      idx = idxVal,
      type = SlotType.fromDb(getString("t")),
      number = getString("n"),
      bales = balesVal,
      note = getString("o"),
      checked = checkedVal == 1,
      updatedAt = updatedVal,
      updatedByUid = getString("updatedByUid"),
      updatedByName = getString("updatedByName"),
      hasPendingWrites = metadata.hasPendingWrites(),
      workspaceId = getString("workspaceId").orEmpty(),
      memberUids = members,
    )
  }

  private fun DocumentSnapshot.toHistoryEntry(): HistoryEntry {
    val tsVal =
      getTimestamp("ts", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
    return HistoryEntry(
      id = id,
      ts = tsVal,
      userId = getString("u").orEmpty(),
      text = getString("t").orEmpty(),
      patioName = getString("p").orEmpty(),
      workspaceId = getString("workspaceId").orEmpty(),
      isPending = metadata.hasPendingWrites(),
    )
  }

  private fun DocumentSnapshot.toUserProfile(): UserProfile {
    val updatedVal =
      getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
        ?: 0L
    return UserProfile(
      uid = getString("uid") ?: id,
      name = getString("name").orEmpty(),
      workspaceId = getString("workspaceId").orEmpty(),
      updatedAt = updatedVal,
    )
  }

  private fun DocumentSnapshot.toWorkspace(): Workspace {
    val createdVal =
      getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
        ?: 0L
    val members =
      (get("memberUids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    return Workspace(
      id = id,
      ownerId = getString("ownerId").orEmpty(),
      name = getString("name").orEmpty(),
      memberUids = members,
      createdAt = createdVal,
    )
  }
}
