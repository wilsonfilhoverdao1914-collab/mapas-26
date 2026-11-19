package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.PreferencesRepository
import com.example.data.remote.NetworkMonitor
import com.example.data.repository.LocalPreviewMapaDataSource
import com.example.data.repository.MapaRepository
import com.example.domain.model.Patio
import com.example.domain.model.PatioKind
import com.example.domain.model.Slot
import com.example.domain.model.SlotType
import com.example.domain.model.SyncStateKind
import com.example.domain.model.SyncStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.Workspace
import com.example.domain.rules.MapaRules
import com.example.ui.components.CalculatorSheet
import com.example.ui.components.CorrectBlockAlertDialog
import com.example.ui.components.DeletePatioConfirmDialog
import com.example.ui.components.LoadBalesDialog
import com.example.ui.components.MergeBlocksDialog
import com.example.ui.components.PatioFormDialog
import com.example.ui.components.PatioSelectorSheet
import com.example.ui.components.SlotEditorSheet
import com.example.ui.components.SlotEditorTarget
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BuscarScreen
import com.example.ui.screens.ConferenciaScreen
import com.example.ui.screens.HistoricoScreen
import com.example.ui.screens.MapaScreen
import com.example.ui.screens.PatiosScreen
import com.example.ui.screens.TotaisScreen
import com.example.ui.screens.authStateFlow
import com.example.ui.screens.signOutUser
import com.example.ui.theme.BarracaoAccent
import com.example.ui.theme.BarracaoAccentContainer
import com.example.ui.theme.BarracaoOnAccentContainer
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestAmberContainer
import com.example.ui.theme.OnHarvestAmberContainer
import com.example.ui.theme.SuccessContainerLight
import com.example.ui.theme.SuccessGreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

const val ROUTE_MAPA = "mapa"
const val ROUTE_CONFERENCIA = "conferencia"
const val ROUTE_PATIOS = "patios"
const val ROUTE_BUSCAR = "buscar"
const val ROUTE_TOTAIS = "totais"
const val ROUTE_HISTORICO = "historico"

private data class NavDestinationItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private sealed interface PatioFormState {
  data object Create : PatioFormState
  data class Edit(val patio: Patio) : PatioFormState
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val context = LocalContext.current
  val networkMonitor = remember(context) { NetworkMonitor(context.applicationContext) }
  DisposableEffect(networkMonitor) {
    networkMonitor.start()
    onDispose { networkMonitor.stop() }
  }

  val currentUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser
  var isLocalPreview by rememberSaveable { mutableStateOf(false) }

  if (user != null) {
    isLocalPreview = false
    key(user.uid) {
      val viewModel: MapaViewModel =
        viewModel(
          key = user.uid,
          factory =
            viewModelFactory {
              initializer {
                val app =
                  checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                  }
                MapaViewModel(
                  repository =
                    MapaRepository(
                      context = app.applicationContext,
                      isOnlineProvider = { networkMonitor.isOnline.value },
                    ),
                  prefs = PreferencesRepository(app.applicationContext),
                  currentUserId = user.uid,
                  initialDisplayName = user.displayName,
                  isOnlineFlow = networkMonitor.isOnline,
                )
              }
            },
        )
      MapaApp(
        currentUser = user,
        isLocalPreview = false,
        onRequestGoogleSignIn = {},
        viewModel = viewModel,
      )
    }
  } else if (isLocalPreview) {
    key("local_preview") {
      val viewModel: MapaViewModel =
        viewModel(
          key = "local_preview",
          factory =
            viewModelFactory {
              initializer {
                val app =
                  checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                  }
                MapaViewModel(
                  repository = LocalPreviewMapaDataSource(),
                  prefs = PreferencesRepository(app.applicationContext),
                  currentUserId = "local_preview",
                  initialDisplayName = "Operador Local",
                  isOnlineFlow = networkMonitor.isOnline,
                )
              }
            },
        )
      MapaApp(
        currentUser = null,
        isLocalPreview = true,
        onRequestGoogleSignIn = { isLocalPreview = false },
        viewModel = viewModel,
      )
    }
  } else {
    AuthScreen(
      onAuthSuccess = { isLocalPreview = false },
      onContinueLocalPreview = { isLocalPreview = true },
    )
  }
}

@Composable
fun MapaApp(
  currentUser: FirebaseUser?,
  isLocalPreview: Boolean,
  onRequestGoogleSignIn: () -> Unit,
  viewModel: MapaViewModel,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: ROUTE_MAPA
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var showSelectorSheet by remember { mutableStateOf(false) }
  var patioFormState by remember { mutableStateOf<PatioFormState?>(null) }
  var deletePatioTarget by remember { mutableStateOf<Patio?>(null) }
  var slotEditorTarget by remember { mutableStateOf<SlotEditorTarget?>(null) }
  var correctBlockTarget by remember { mutableStateOf<Pair<MapaRules.SlotAlertState, Patio>?>(null) }
  var loadBalesTarget by remember { mutableStateOf<Pair<Patio, Slot>?>(null) }
  var mergeDialogInitialDest by remember { mutableStateOf<MapaRules.BlockLocationEntry?>(null) }
  var showMergeDialog by remember { mutableStateOf(false) }
  var showAccountDialog by remember { mutableStateOf(false) }
  var showCalculatorSheet by remember { mutableStateOf(false) }

  LaunchedEffect(uiState.snackbarMessage) {
    val msg = uiState.snackbarMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbar()
    }
  }

  val navItems = remember {
    listOf(
      NavDestinationItem(
        route = ROUTE_MAPA,
        label = "Mapa",
        selectedIcon = Icons.Filled.GridOn,
        unselectedIcon = Icons.Outlined.GridOn,
        testTag = "nav_mapa",
      ),
      NavDestinationItem(
        route = ROUTE_CONFERENCIA,
        label = "Conferir",
        selectedIcon = Icons.Filled.CheckBox,
        unselectedIcon = Icons.Outlined.CheckBox,
        testTag = "nav_conferencia",
      ),
      NavDestinationItem(
        route = ROUTE_PATIOS,
        label = "Pátios",
        selectedIcon = Icons.Filled.ViewList,
        unselectedIcon = Icons.Outlined.ViewList,
        testTag = "nav_patios",
      ),
      NavDestinationItem(
        route = ROUTE_TOTAIS,
        label = "Totais",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment,
        testTag = "nav_totais",
      ),
      NavDestinationItem(
        route = ROUTE_HISTORICO,
        label = "Histórico",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_historico",
      ),
    )
  }

  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        Column(modifier = Modifier.fillMaxWidth()) {
          MapaTopBar(
            selectedPatio = uiState.selectedPatio,
            isSearchActive = currentRoute == ROUTE_BUSCAR,
            isSaving = uiState.isSaving,
            onOpenPatioSelector = { showSelectorSheet = true },
            onOpenSearch = { navigateToTab(ROUTE_BUSCAR) },
            onOpenCalculator = { showCalculatorSheet = true },
            onOpenAccount = { showAccountDialog = true },
          )
          SyncStatusBar(
            syncStatus = uiState.syncStatus,
            workspace = uiState.workspace,
            isLocalPreview = isLocalPreview,
            onOpenTeamOrAccount = { showAccountDialog = true },
          )
        }
      },
      bottomBar = {
        if (!useRail) {
          NavigationBar(
            windowInsets = WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            navItems.forEach { item ->
              val selected = currentRoute == item.route
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = if (item.route == ROUTE_CONFERENCIA) "Conferência" else item.label,
                  )
                },
                label = {
                  Text(
                    text = item.label,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      ) {
        if (useRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            navItems.forEach { item ->
              val selected = currentRoute == item.route
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
          if (isLocalPreview) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Pré-visualização local (sem nuvem)",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.weight(1f),
                )
                TextButton(
                  onClick = onRequestGoogleSignIn,
                  modifier = Modifier.testTag("connect_google_button"),
                ) {
                  Text("Entrar com Google")
                }
              }
            }
          }

          if (uiState.errorMessage != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = uiState.errorMessage.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { viewModel.clearError() }) {
                  Text("Fechar")
                }
              }
            }
          }

          NavHost(
            navController = navController,
            startDestination = ROUTE_MAPA,
            modifier = Modifier.weight(1f).fillMaxWidth(),
          ) {
            composable(ROUTE_MAPA) {
              MapaScreen(
                patio = uiState.selectedPatio,
                patioTotals = uiState.selectedPatioTotals,
                streetColumns = uiState.streetColumns,
                blockOccurrences = uiState.blockOccurrences,
                usersById = uiState.usersById,
                canMergeBlocks = uiState.allBlockLocations.size >= 2,
                highlightedSlot = uiState.highlightedSlot,
                isLoading = uiState.isLoading,
                onOpenSlotEditor = { target -> slotEditorTarget = target },
                onOpenCorrectBlockDialog = { alert, patio ->
                  correctBlockTarget = alert to patio
                },
                onOpenMergeDialog = {
                  mergeDialogInitialDest = null
                  showMergeDialog = true
                },
                onCreateFirstPatio = { patioFormState = PatioFormState.Create },
              )
            }

            composable(ROUTE_CONFERENCIA) {
              ConferenciaScreen(
                summary = uiState.conferenceSummary,
                filteredItems = uiState.filteredConferenceItems,
                activeFilter = uiState.conferenceFilter,
                query = uiState.conferenceQuery,
                onFilterChange = viewModel::updateConferenceFilter,
                onQueryChange = viewModel::updateConferenceQuery,
                onToggleBlock = viewModel::toggleBlockConference,
                onClearConference = viewModel::clearAllConference,
              )
            }

            composable(ROUTE_PATIOS) {
              PatiosScreen(
                patioTotals = uiState.allPatioTotals,
                selectedPatioId = uiState.selectedPatio?.id,
                onCreateClick = { patioFormState = PatioFormState.Create },
                onSelectAndOpenMap = { patio ->
                  viewModel.selectPatio(patio.id)
                  navigateToTab(ROUTE_MAPA)
                },
                onRenameClick = { patio -> patioFormState = PatioFormState.Edit(patio) },
                onDeleteClick = { patio -> deletePatioTarget = patio },
              )
            }

            composable(ROUTE_BUSCAR) {
              BuscarScreen(
                query = uiState.searchQuery,
                results = uiState.searchResults,
                onQueryChange = viewModel::updateSearchQuery,
                onMatchClick = { match ->
                  viewModel.jumpToSlot(
                    patioId = match.patio.id,
                    street = match.slot.street,
                    idx = match.slot.idx,
                  )
                  navigateToTab(ROUTE_MAPA)
                },
              )
            }

            composable(ROUTE_TOTAIS) {
              TotaisScreen(
                patioTotals = uiState.allPatioTotals,
                grandTotalBlocks = uiState.grandTotalBlocks,
                grandTotalBales = uiState.grandTotalBales,
                onOpenPatio = { patio ->
                  viewModel.selectPatio(patio.id)
                  navigateToTab(ROUTE_MAPA)
                },
              )
            }

            composable(ROUTE_HISTORICO) {
              HistoricoScreen(
                historyGroups = uiState.historyGroups,
                usersById = uiState.usersById,
                isLoading = uiState.isLoading,
              )
            }
          }
        }
      }
    }
  }

  if (showSelectorSheet) {
    PatioSelectorSheet(
      patioTotals = uiState.allPatioTotals,
      selectedPatioId = uiState.selectedPatio?.id,
      onSelectPatio = { patio ->
        viewModel.selectPatio(patio.id)
        navigateToTab(ROUTE_MAPA)
      },
      onCreateClick = { patioFormState = PatioFormState.Create },
      onRenameClick = { patio ->
        showSelectorSheet = false
        patioFormState = PatioFormState.Edit(patio)
      },
      onDeleteClick = { patio ->
        showSelectorSheet = false
        deletePatioTarget = patio
      },
      onDismiss = { showSelectorSheet = false },
    )
  }

  patioFormState?.let { form ->
    val editing = (form as? PatioFormState.Edit)?.patio
    PatioFormDialog(
      editingPatio = editing,
      onConfirm = { name, kind, lastLetter ->
        if (editing == null) {
          viewModel.createPatio(name = name, kind = kind, lastLetter = lastLetter)
        } else {
          viewModel.updatePatio(patio = editing, newName = name, newLastLetter = lastLetter)
        }
        patioFormState = null
      },
      onDismiss = { patioFormState = null },
    )
  }

  deletePatioTarget?.let { patio ->
    val blocks = uiState.allPatioTotals.firstOrNull { it.patio.id == patio.id }?.blockCount ?: 0
    DeletePatioConfirmDialog(
      patio = patio,
      blockCount = blocks,
      onConfirm = {
        viewModel.deletePatio(patio)
        deletePatioTarget = null
      },
      onDismiss = { deletePatioTarget = null },
    )
  }

  slotEditorTarget?.let { target ->
    val latestRemoteSlot =
      uiState.allSlots.firstOrNull {
        it.patioId == target.patio.id &&
          it.street.equals(target.street, ignoreCase = true) &&
          it.idx == target.idx
      }
    SlotEditorSheet(
      target = target,
      latestRemoteSlot = latestRemoteSlot,
      currentUserId = currentUser?.uid ?: "local_preview",
      blockOccurrences = uiState.blockOccurrences,
      onSave = { targetIdx, type, number, bales, note, checked ->
        viewModel.upsertSlot(
          patioId = target.patio.id,
          street = target.street,
          idx = targetIdx,
          type = type,
          number = number,
          bales = bales,
          note = note,
          checked = checked,
          isEditingExistingBlock = target.existingSlot?.type == SlotType.BLOCO,
        )
        slotEditorTarget = null
      },
      onConvertBlockToVacant = {
        viewModel.convertBlockToVacant(
          patioId = target.patio.id,
          street = target.street,
          idx = target.idx,
          blockNumber = target.existingSlot?.number,
        )
        slotEditorTarget = null
      },
      onDeletePosition = {
        viewModel.deleteSlot(
          patioId = target.patio.id,
          street = target.street,
          idx = target.idx,
        )
        slotEditorTarget = null
      },
      onMoveUp = {
        viewModel.moveSlot(
          patio = target.patio,
          street = target.street,
          fromIdx = target.idx,
          toIdx = target.idx - 1,
        )
        slotEditorTarget = null
      },
      onMoveDown = {
        viewModel.moveSlot(
          patio = target.patio,
          street = target.street,
          fromIdx = target.idx,
          toIdx = target.idx + 1,
        )
        slotEditorTarget = null
      },
      onOpenLoadBales = { slot ->
        slotEditorTarget = null
        loadBalesTarget = target.patio to slot
      },
      onOpenMerge = { slot ->
        slotEditorTarget = null
        mergeDialogInitialDest = MapaRules.BlockLocationEntry(patio = target.patio, slot = slot)
        showMergeDialog = true
      },
      onDismiss = { slotEditorTarget = null },
    )
  }

  correctBlockTarget?.let { (alert, patio) ->
    CorrectBlockAlertDialog(
      alert = alert,
      patioName = patio.name,
      onEditClick = {
        correctBlockTarget = null
        val streetSlots =
          uiState.streetColumns
            .firstOrNull { it.street.equals(alert.slot.street, ignoreCase = true) }
            ?.slots
            .orEmpty()
        val isLast = streetSlots.lastOrNull()?.idx == alert.slot.idx
        slotEditorTarget =
          SlotEditorTarget(
            patio = patio,
            street = alert.slot.street,
            idx = alert.slot.idx,
            existingSlot = alert.slot,
            canDeletePosition = isLast,
          )
      },
      onDismiss = { correctBlockTarget = null },
    )
  }

  loadBalesTarget?.let { (patio, slot) ->
    LoadBalesDialog(
      blockNumber = slot.number.orEmpty(),
      locationLabel = "${patio.name} · Rua ${slot.street} · Pos. ${slot.positionNumber}",
      currentBales = slot.bales,
      onConfirmLoad = { loadedBales ->
        viewModel.loadBalesFromSlot(
          patioId = patio.id,
          street = slot.street,
          idx = slot.idx,
          currentSlot = slot,
          loadedBales = loadedBales,
        )
        loadBalesTarget = null
      },
      onDismiss = { loadBalesTarget = null },
    )
  }

  if (showMergeDialog) {
    MergeBlocksDialog(
      allBlocks = uiState.allBlockLocations,
      initialDestination = mergeDialogInitialDest,
      onConfirmMerge = { dest, sources ->
        viewModel.mergeBlocks(destination = dest, sources = sources)
        showMergeDialog = false
      },
      onDismiss = { showMergeDialog = false },
    )
  }

  if (showAccountDialog) {
    AccountAndTeamDialog(
      isLocalPreview = isLocalPreview,
      currentUser = currentUser,
      userProfile = uiState.userProfile,
      workspace = uiState.workspace,
      syncStatus = uiState.syncStatus,
      onSaveName = { name -> viewModel.updateUserName(name) },
      onJoinWorkspace = { code -> viewModel.joinWorkspaceByCode(code) },
      onCopyTeamCode = { code ->
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Código da equipe", code))
        scope.launch {
          snackbarHostState.showSnackbar("Código $code copiado.")
        }
      },
      onSwitchToGoogle = {
        showAccountDialog = false
        onRequestGoogleSignIn()
      },
      onSignOut = {
        showAccountDialog = false
        signOutUser(credentialManager = credentialManager, scope = scope)
      },
      onDismiss = { showAccountDialog = false },
    )
  }

  if (showCalculatorSheet) {
    CalculatorSheet(onDismiss = { showCalculatorSheet = false })
  }
}

@Composable
private fun SyncStatusBar(
  syncStatus: SyncStatus,
  workspace: Workspace?,
  isLocalPreview: Boolean,
  onOpenTeamOrAccount: () -> Unit,
) {
  val bgColor =
    when {
      isLocalPreview -> MaterialTheme.colorScheme.surfaceVariant
      syncStatus.kind == SyncStateKind.OFFLINE -> ErrorContainerLight
      syncStatus.kind == SyncStateKind.SYNCING -> HarvestAmberContainer
      else -> SuccessContainerLight
    }
  val contentColor =
    when {
      isLocalPreview -> MaterialTheme.colorScheme.onSurfaceVariant
      syncStatus.kind == SyncStateKind.OFFLINE -> ErrorRed
      syncStatus.kind == SyncStateKind.SYNCING -> OnHarvestAmberContainer
      else -> SuccessGreen
    }
  val icon =
    when {
      isLocalPreview -> Icons.Default.CloudOff
      syncStatus.kind == SyncStateKind.OFFLINE -> Icons.Default.CloudOff
      syncStatus.kind == SyncStateKind.SYNCING -> Icons.Default.Sync
      else -> Icons.Default.CloudDone
    }

  Surface(
    color = bgColor,
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onOpenTeamOrAccount)
        .testTag("sync_status_bar"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isLocalPreview) "Modo local offline" else syncStatus.statusLabel,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = contentColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (!isLocalPreview && workspace != null) {
        val count = workspace.memberUids.size
        Text(
          text =
            if (count > 1) "$count cel. · ${workspace.id}"
            else "Eq. ${workspace.id}",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = contentColor.copy(alpha = 0.85f),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountAndTeamDialog(
  isLocalPreview: Boolean,
  currentUser: FirebaseUser?,
  userProfile: UserProfile?,
  workspace: Workspace?,
  syncStatus: SyncStatus,
  onSaveName: (String) -> Unit,
  onJoinWorkspace: (String) -> Unit,
  onCopyTeamCode: (String) -> Unit,
  onSwitchToGoogle: () -> Unit,
  onSignOut: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var nameDraft by remember(userProfile?.name, currentUser?.displayName) {
    mutableStateOf(
      userProfile?.name?.takeIf { it.isNotBlank() }
        ?: currentUser?.displayName.orEmpty()
    )
  }
  var joinCodeDraft by remember { mutableStateOf("") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AccountCircle, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            if (isLocalPreview) "Modo de pré-visualização local"
            else userProfile?.name?.takeIf { it.isNotBlank() } ?: currentUser?.displayName ?: "Minha Conta",
          style = MaterialTheme.typography.titleLarge,
        )
      }

      if (isLocalPreview) {
        Text(
          text =
            "Os dados criados agora ficam apenas nesta sessão. Entre com o Google para sincronizar entre 2 a 5 celulares e salvar online/offline.",
          style = MaterialTheme.typography.bodySmall,
        )
      } else {
        Text(
          text = "Status: ${syncStatus.statusLabel}",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )

        OutlinedTextField(
          value = nameDraft,
          onValueChange = { nameDraft = it.take(60) },
          label = { Text("Seu nome no mapa e histórico") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("account_name_input"),
        )

        Button(
          onClick = { onSaveName(nameDraft) },
          enabled = nameDraft.trim().isNotEmpty(),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save_name_button"),
        ) {
          Text("Salvar nome")
        }

        HorizontalDivider()

        if (workspace != null) {
          Text(
            text = "Sincronizar celulares (2 a 5 aparelhos)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Código desta equipe: ${workspace.id}",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${workspace.memberUids.size} aparelho(s) conectado(s)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(
                onClick = { onCopyTeamCode(workspace.id) },
                modifier = Modifier.size(44.dp),
              ) {
                Icon(
                  Icons.Default.ContentCopy,
                  contentDescription = "Copiar código da equipe",
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = joinCodeDraft,
          onValueChange = { joinCodeDraft = it.uppercase().take(32) },
          label = { Text("Entrar em outra equipe (código)") },
          placeholder = { Text("Ex.: EQ_1A2B3C") },
          singleLine = true,
          keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(
              capitalization = KeyboardCapitalization.Characters,
            ),
          modifier = Modifier.fillMaxWidth().testTag("join_workspace_input"),
        )

        OutlinedButton(
          onClick = {
            onJoinWorkspace(joinCodeDraft)
            joinCodeDraft = ""
          },
          enabled = joinCodeDraft.trim().length >= 3,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("join_workspace_button"),
        ) {
          Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Conectar à equipe")
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (isLocalPreview) {
          Button(
            onClick = onSwitchToGoogle,
            modifier = Modifier.heightIn(min = 48.dp).testTag("switch_to_google_signin_button"),
          ) {
            Text("Entrar com Google")
          }
        } else {
          TextButton(
            onClick = onSignOut,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.heightIn(min = 48.dp).testTag("sign_out_button"),
          ) {
            Icon(
              Icons.AutoMirrored.Filled.Logout,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sair da conta")
          }
        }

        TextButton(
          onClick = onDismiss,
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text("Fechar")
        }
      }
    }
  }
}

@Composable
private fun MapaTopBar(
  selectedPatio: Patio?,
  isSearchActive: Boolean,
  isSaving: Boolean,
  onOpenPatioSelector: () -> Unit,
  onOpenSearch: () -> Unit,
  onOpenCalculator: () -> Unit,
  onOpenAccount: () -> Unit,
) {
  val isBarracao = selectedPatio?.kind == PatioKind.BARRACAO

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        modifier =
          Modifier
            .weight(1f)
            .heightIn(min = 46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenPatioSelector)
            .testTag("top_patio_selector"),
        shape = RoundedCornerShape(12.dp),
        color =
          when {
            selectedPatio == null -> MaterialTheme.colorScheme.surfaceVariant
            isBarracao -> BarracaoAccentContainer
            else -> MaterialTheme.colorScheme.primaryContainer
          },
        border =
          BorderStroke(
            1.dp,
            when {
              selectedPatio == null -> MaterialTheme.colorScheme.outline
              isBarracao -> BarracaoAccent.copy(alpha = 0.5f)
              else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            },
          ),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector =
              if (isBarracao) Icons.Default.Warehouse else Icons.Default.WbSunny,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint =
              when {
                selectedPatio == null -> MaterialTheme.colorScheme.onSurfaceVariant
                isBarracao -> BarracaoOnAccentContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
              },
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = selectedPatio?.name ?: "Selecionar Pátio",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            color =
              when {
                selectedPatio == null -> MaterialTheme.colorScheme.onSurface
                isBarracao -> BarracaoOnAccentContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
              },
          )
          Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Abrir lista de pátios e barracões",
            tint =
              when {
                selectedPatio == null -> MaterialTheme.colorScheme.onSurfaceVariant
                isBarracao -> BarracaoOnAccentContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
              },
          )
        }
      }

      Spacer(modifier = Modifier.width(2.dp))

      IconButton(
        onClick = onOpenSearch,
        modifier = Modifier.size(44.dp).testTag("top_search_button"),
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Buscar bloco",
          tint =
            if (isSearchActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
      }

      IconButton(
        onClick = onOpenCalculator,
        modifier = Modifier.size(44.dp).testTag("top_calculator_button"),
      ) {
        Icon(
          imageVector = Icons.Default.Calculate,
          contentDescription = "Calculadora",
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }

      if (isSaving) {
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
        }
      } else {
        IconButton(
          onClick = onOpenAccount,
          modifier = Modifier.size(44.dp).testTag("top_account_button"),
        ) {
          Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Conta e equipe",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}
