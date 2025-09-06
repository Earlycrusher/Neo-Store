package com.machiav3lli.fdroid.ui.pages

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.machiav3lli.backup.ui.compose.icons.phosphor.Plus
import com.machiav3lli.fdroid.INTENT_ACTION_BINARY_EYE
import com.machiav3lli.fdroid.NeoActivity
import com.machiav3lli.fdroid.R
import com.machiav3lli.fdroid.manager.work.SyncWorker
import com.machiav3lli.fdroid.ui.components.RepositoryItem
import com.machiav3lli.fdroid.ui.components.WideSearchField
import com.machiav3lli.fdroid.ui.components.prefs.PreferenceGroupHeading
import com.machiav3lli.fdroid.ui.compose.icons.Phosphor
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.QrCode
import com.machiav3lli.fdroid.utils.extension.koinNeoViewModel
import com.machiav3lli.fdroid.utils.extension.text.toAddressFingerprint
import com.machiav3lli.fdroid.viewmodels.PrefsVM
import com.machiav3lli.fdroid.viewmodels.SheetNavigationData
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun PrefsReposPage(viewModel: PrefsVM = koinNeoViewModel()) {
    val mActivity = LocalActivity.current as NeoActivity
    val scope = rememberCoroutineScope()
    val paneNavigator = rememberListDetailPaneScaffoldNavigator<Any>()

    val repos by viewModel.filteredRepositories.collectAsState()
    val partedRepos by remember {
        derivedStateOf {
            repos.partition { it.enabled }
        }
    }

    val query by viewModel.reposSearchQuery.collectAsState()
    val sheetData: MutableState<SheetNavigationData?> = remember { mutableStateOf(null) }
    val intentAddressFingerprint by viewModel.addressFingerprint.collectAsState()

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scan = result.data?.getStringExtra("SCAN_RESULT")
            scan?.replaceFirst("fdroidrepo", "http")
            (scan?.toUri() ?: Uri.EMPTY).toAddressFingerprint()
                .takeUnless { it.first.isEmpty() }
                ?.let { (address, fingerprint) ->
                    scope.launch {
                        paneNavigator.navigateTo(
                            ListDetailPaneScaffoldRole.Detail,
                            SheetNavigationData(
                                viewModel.addNewRepository(
                                    address = address,
                                    fingerprint = fingerprint,
                                ),
                                true
                            )
                        )
                    }
                }
        }
    }

    LaunchedEffect(intentAddressFingerprint) {
        if (intentAddressFingerprint.first.isNotEmpty()) {
            scope.launch {
                paneNavigator.navigateTo(
                    ListDetailPaneScaffoldRole.Detail,
                    SheetNavigationData(
                        viewModel.addNewRepository(
                            address = intentAddressFingerprint.first,
                            fingerprint = intentAddressFingerprint.second,
                        ),
                        true
                    )
                )
                viewModel.setIntent("", "")
            }
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = paneNavigator,
        listPane = {
            AnimatedPane {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    floatingActionButtonPosition = FabPosition.Center,
                    floatingActionButton = {
                        if (Intent(INTENT_ACTION_BINARY_EYE).resolveActivity(mActivity.packageManager) != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                ExtendedFloatingActionButton(
                                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    onClick = {
                                        scope.launch {
                                            paneNavigator.navigateTo(
                                                ListDetailPaneScaffoldRole.Detail,
                                                SheetNavigationData(
                                                    viewModel.addNewRepository(),
                                                    true
                                                )
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Phosphor.Plus,
                                        contentDescription = stringResource(id = R.string.add_repository)
                                    )
                                }
                                ExtendedFloatingActionButton(
                                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    onClick = { scanLauncher.launch(Intent(INTENT_ACTION_BINARY_EYE)) }
                                ) {
                                    Icon(
                                        imageVector = Phosphor.QrCode,
                                        contentDescription = stringResource(id = R.string.scan_qr_code)
                                    )
                                }
                            }
                        } else {
                            ExtendedFloatingActionButton(
                                shape = MaterialTheme.shapes.extraLarge,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    scope.launch {
                                        paneNavigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            SheetNavigationData(
                                                viewModel.addNewRepository(),
                                                true
                                            )
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Phosphor.Plus,
                                    contentDescription = stringResource(id = R.string.add_repository)
                                )
                            }
                        }
                    }
                ) { _ ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            WideSearchField(
                                query = query,
                                modifier = Modifier.fillMaxWidth(),
                                label = stringResource(R.string.search_for_repository),
                                focusOnCompose = false,
                                onClose = { viewModel.setSearchQuery("") },
                                onQueryChanged = { newQuery ->
                                    if (newQuery != query) viewModel.setSearchQuery(newQuery)
                                }
                            )
                        }
                        item {
                            PreferenceGroupHeading(heading = stringResource(id = R.string.enabled))
                        }
                        items(items = partedRepos.first, key = { it.id }) {
                            RepositoryItem(
                                modifier = Modifier.animateItem(),
                                repository = it,
                                onSwitch = { repo ->
                                    viewModel.viewModelScope.launch {
                                        SyncWorker.enableRepo(repo, !repo.enabled)
                                    }
                                },
                                onClick = { repo ->
                                    scope.launch {
                                        paneNavigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            SheetNavigationData(repo.id, false)
                                        )
                                    }
                                }
                            )
                        }
                        item {
                            PreferenceGroupHeading(heading = stringResource(id = R.string.disabled))
                        }
                        items(items = partedRepos.second, key = { it.id }) {
                            RepositoryItem(
                                modifier = Modifier.animateItem(),
                                repository = it,
                                onSwitch = { repo ->
                                    viewModel.viewModelScope.launch {
                                        SyncWorker.enableRepo(repo, !repo.enabled)
                                    }
                                },
                                onClick = { repo ->
                                    scope.launch {
                                        paneNavigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            SheetNavigationData(repo.id, false)
                                        )
                                    }
                                }
                            )
                        }
                        item {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                            )
                        }
                    }
                }
            }
        },
        detailPane = {
            sheetData.value = paneNavigator.currentDestination
                ?.takeIf { it.pane == this.paneRole }?.contentKey
                ?.let { it as? SheetNavigationData }

            sheetData.value?.let {
                AnimatedPane {
                    RepoPage(
                        repositoryId = it.repositoryId,
                        initEditMode = it.editMode,
                        onDismiss = {
                            scope.launch {
                                paneNavigator.navigateBack()
                            }
                        }
                    ) { newRepo -> viewModel.updateRepo(newRepo) }
                }
            }
        }
    )
}
