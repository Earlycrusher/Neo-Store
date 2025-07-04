package com.machiav3lli.fdroid.manager.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.machiav3lli.fdroid.ARG_RESULT_CODE
import com.machiav3lli.fdroid.ARG_VALIDATION_ERROR
import com.machiav3lli.fdroid.ContextWrapperX
import com.machiav3lli.fdroid.NOTIFICATION_CHANNEL_DOWNLOADING
import com.machiav3lli.fdroid.NOTIFICATION_CHANNEL_SYNCING
import com.machiav3lli.fdroid.NOTIFICATION_CHANNEL_UPDATES
import com.machiav3lli.fdroid.NOTIFICATION_CHANNEL_VULNS
import com.machiav3lli.fdroid.NeoApp
import com.machiav3lli.fdroid.R
import com.machiav3lli.fdroid.TAG_BATCH_SYNC_PERIODIC
import com.machiav3lli.fdroid.TAG_SYNC_ONETIME
import com.machiav3lli.fdroid.data.content.Preferences
import com.machiav3lli.fdroid.data.entity.DownloadState
import com.machiav3lli.fdroid.data.entity.ProductItem
import com.machiav3lli.fdroid.data.entity.SyncState
import com.machiav3lli.fdroid.data.entity.ValidationError
import com.machiav3lli.fdroid.data.repository.DownloadedRepository
import com.machiav3lli.fdroid.data.repository.InstalledRepository
import com.machiav3lli.fdroid.data.repository.InstallsRepository
import com.machiav3lli.fdroid.data.repository.ProductsRepository
import com.machiav3lli.fdroid.data.repository.RepositoriesRepository
import com.machiav3lli.fdroid.manager.service.ActionReceiver
import com.machiav3lli.fdroid.utils.Utils
import com.machiav3lli.fdroid.utils.extension.android.Android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class WorkerManager(appContext: Context) : KoinComponent {

    private val workManager: WorkManager by inject()
    private val actionReceiver: ActionReceiver by inject()
    private var appContext: Context = appContext
    private var langContext: Context = ContextWrapperX.wrap(appContext)
    private val notificationManager: NotificationManagerCompat by inject()
    private val downloadedRepo: DownloadedRepository by inject()
    private val productRepo: ProductsRepository by inject()
    private val reposRepo: RepositoriesRepository by inject()
    private val installedRepo: InstalledRepository by inject()
    private val installsRepo: InstallsRepository by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val syncsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val downloadsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadTracker = DownloadsTracker()
    private val syncTracker = SyncsTracker()

    private val syncStateHandler by lazy {
        SyncStateHandler(
            context = langContext,
            scope = syncsScope,
            syncStates = WorkStateHolder(),
            notificationManager = notificationManager
        )
    }
    private val downloadStateHandler by lazy {
        DownloadStateHandler(
            context = langContext,
            scope = downloadsScope,
            downloadStates = WorkStateHolder(),
            notificationManager = notificationManager,
            downloadedRepo = downloadedRepo,
            installsRepo = installsRepo,
        )
    }

    init {
        appContext.registerReceiver(actionReceiver, IntentFilter())
        if (Android.sdk(Build.VERSION_CODES.O)) createNotificationChannels()

        workManager.pruneWork()
        setupWorkInfoCollection()
        monitorWorkProgress()
    }

    fun release(): WorkerManager? {
        appContext.unregisterReceiver(actionReceiver)
        return null
    }

    fun prune() {
        workManager.pruneWork()
    }

    private fun setupWorkInfoCollection() {
        scope.launch {
            workManager.getWorkInfosByTagFlow(SyncWorker::class.qualifiedName!!)
                .retryWhen { cause, attempt ->
                    delay(attempt * 1_000L)
                    cause !is CancellationException
                }
                .collect { workInfos ->
                    try {
                        syncsScope.onSyncProgress(this@WorkerManager, workInfos)
                    } catch (e: Exception) {
                        Log.e("WorkerManager", "Error processing sync updates", e)
                    }
                }
        }

        scope.launch {
            workManager.getWorkInfosByTagFlow(DownloadWorker::class.qualifiedName!!)
                .retryWhen { cause, attempt ->
                    delay(attempt * 1_000L)
                    cause !is CancellationException
                }
                .collect { workInfos ->
                    try {
                        downloadsScope.onDownloadProgress(this@WorkerManager, workInfos)
                    } catch (e: Exception) {
                        Log.e("WorkerManager", "Error processing download updates", e)
                    }
                }
        }
    }

    private fun monitorWorkProgress() {
        scope.launch {
            while (isActive) {
                try {
                    workManager.getWorkInfos(
                        WorkQuery.Builder
                            .fromStates(listOf(WorkInfo.State.RUNNING))
                            .build()
                    ).get().filter { it.runAttemptCount > 5 }.forEach { wi ->
                        workManager.cancelWorkById(wi.id)
                    }
                } catch (e: Exception) {
                    Log.e("WorkerManager", "Error in work monitoring", e)
                }
                delay(TimeUnit.MINUTES.toMillis(5))
            }
        }
    }

    fun enqueueUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy,
        request: OneTimeWorkRequest
    ) = workManager.enqueueUniqueWork(uniqueWorkName, existingWorkPolicy, request)

    internal suspend fun updatePeriodicSyncJob(force: Boolean) = withContext(Dispatchers.IO) {
        val reschedule =
            force || workManager.getWorkInfosForUniqueWork(TAG_BATCH_SYNC_PERIODIC).get().isEmpty()
        if (reschedule) {
            when (val autoSync = Preferences[Preferences.Key.AutoSync]) {
                is Preferences.AutoSync.Never,
                    -> {
                    workManager.cancelUniqueWork(TAG_BATCH_SYNC_PERIODIC)
                    Log.i(this::javaClass.name, "Canceled next auto-sync run.")
                }

                is Preferences.AutoSync.Wifi,
                is Preferences.AutoSync.WifiBattery,
                    -> {
                    autoSync(
                        connectionType = NetworkType.UNMETERED,
                        chargingBattery = autoSync is Preferences.AutoSync.WifiBattery,
                    )
                }

                is Preferences.AutoSync.Battery,
                    -> autoSync(
                    connectionType = NetworkType.CONNECTED,
                    chargingBattery = true,
                )

                is Preferences.AutoSync.Always
                    -> autoSync(
                    connectionType = NetworkType.CONNECTED
                )
            }
        }
    }

    private fun autoSync(
        connectionType: NetworkType,
        chargingBattery: Boolean = false,
    ) {
        BatchSyncWorker.enqueuePeriodic(
            connectionType = connectionType,
            chargingBattery = chargingBattery,
        )
    }

    fun cancelSyncAll() {
        SyncWorker::class.qualifiedName?.let {
            workManager.cancelAllWorkByTag(it)
            prune()
        }
        NeoApp.setProgress() // TODO re-consider
    }

    fun cancelSync(repoId: Long = -1) {
        SyncWorker::class.qualifiedName?.let {
            workManager.cancelAllWorkByTag(
                if (repoId != -1L) "sync_$repoId"
                else TAG_SYNC_ONETIME
            )
        }
    }

    fun cancelDownloadAll() {
        DownloadWorker::class.qualifiedName?.let {
            workManager.cancelAllWorkByTag(it)
            prune()
        }
        NeoApp.setProgress() // TODO re-consider
    }

    fun cancelDownload(packageName: String?) {
        DownloadWorker::class.qualifiedName?.let {
            workManager.cancelAllWorkByTag(
                if (packageName != null) "download_$packageName"
                else it
            )
            // TODO upsert Error to DB.Downloaded
        }
    }

    fun cancelInstall(packageName: String) {
        DownloadWorker::class.qualifiedName?.let {
            workManager.cancelUniqueWork("Installer_$packageName")
            scope.launch {
                installsRepo.delete(packageName)
            }
            //prune()
        }
    }

    fun install(vararg product: ProductItem) = batchUpdate(product.toList(), true)

    fun update(vararg product: ProductItem) = batchUpdate(product.toList(), false)

    private fun batchUpdate(productItems: List<ProductItem>, enforce: Boolean = false) {
        scope.launch {
            productItems.map { productItem ->
                Triple(
                    productItem.packageName,
                    installedRepo.load(productItem.packageName),
                    reposRepo.load(productItem.repositoryId)
                )
            }
                .filter { (_, installed, repo) -> (enforce || installed != null) && repo != null }
                .forEach { (packageName, installed, repo) ->
                    val productRepository = productRepo.loadProduct(packageName)
                        .filter { eProduct -> eProduct.product.repositoryId == repo!!.id }
                        .map { eProduct -> Pair(eProduct, repo!!) }
                    Utils.startUpdate(
                        packageName,
                        installed,
                        productRepository
                    )
                }

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() {
        NotificationChannel(
            NOTIFICATION_CHANNEL_DOWNLOADING,
            langContext.getString(R.string.downloading),
            NotificationManager.IMPORTANCE_LOW
        )
            .apply { setShowBadge(false) }
            .let(notificationManager::createNotificationChannel)
        NotificationChannel(
            NOTIFICATION_CHANNEL_SYNCING,
            langContext.getString(R.string.syncing),
            NotificationManager.IMPORTANCE_LOW
        )
            .apply { setShowBadge(false) }
            .let(notificationManager::createNotificationChannel)
        NotificationChannel(
            NOTIFICATION_CHANNEL_UPDATES,
            langContext.getString(R.string.updates), NotificationManager.IMPORTANCE_LOW
        )
            .let(notificationManager::createNotificationChannel)
        NotificationChannel(
            NOTIFICATION_CHANNEL_VULNS,
            langContext.getString(R.string.vulnerabilities), NotificationManager.IMPORTANCE_HIGH
        ).let(notificationManager::createNotificationChannel)
    }

    companion object {
        const val TAG = "WorkerManager"

        private fun CoroutineScope.onSyncProgress(
            manager: WorkerManager,
            workInfos: List<WorkInfo>? = null,
        ) = launch {
            runCatching {
                val syncs = workInfos
                    ?: manager.workManager
                        .getWorkInfosByTag(SyncWorker::class.qualifiedName!!)
                        .get()
                    ?: return@launch

                manager.updateSyncsRunning(syncs)
            }.onFailure { e ->
                Log.e("WorkerManager", "Error in onDownloadProgress", e)
            }
        }

        private fun WorkerManager.updateSyncsRunning(workers: List<WorkInfo>) {
            workers.forEach { workInfo ->
                val data = workInfo.outputData.takeIf { it != Data.EMPTY }
                    ?: workInfo.progress.takeIf { it != Data.EMPTY }
                    ?: return@forEach

                if (!syncTracker.trackWork(workInfo, data)) {
                    return@forEach // Skip if we've already processed this state
                }

                runCatching {
                    val task = SyncWorker.Companion.getTask(data)
                    val dataState = SyncWorker.Companion.getState(data)

                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.BLOCKED,
                        WorkInfo.State.SUCCEEDED,
                        WorkInfo.State.CANCELLED,
                            -> null

                        WorkInfo.State.RUNNING,
                            -> dataState

                        WorkInfo.State.FAILED,
                            -> SyncState.Failed(task.repoId, task.request, task.repoName)
                    }.let { newState ->
                        syncStateHandler.updateState(task.repoId.toString(), newState)
                    }
                }.onFailure { e ->
                    Log.e("WorkerManager", "Error updating download state", e)
                }
            }
        }

        private fun CoroutineScope.onDownloadProgress(
            manager: WorkerManager,
            workInfos: List<WorkInfo>? = null,
        ) = launch {
            runCatching {
                val downloads = workInfos
                    ?: manager.workManager
                        .getWorkInfosByTag(DownloadWorker::class.qualifiedName!!)
                        .get()
                    ?: return@launch

                manager.updateDownloadsRunning(downloads)

                manager.prune()
            }.onFailure { e ->
                Log.e("WorkerManager", "Error in onDownloadProgress", e)
            }
        }

        private fun WorkerManager.updateDownloadsRunning(workInfos: List<WorkInfo>) {
            workInfos.forEach { workInfo ->
                val data = workInfo.outputData.takeIf { it != Data.EMPTY }
                    ?: workInfo.progress.takeIf { it != Data.EMPTY }
                    ?: return@forEach

                if (downloadTracker.trackWork(workInfo, data)) runCatching {
                    val task = DownloadWorker.Companion.getTask(data)
                    val resultCode = data.getInt(ARG_RESULT_CODE, 0)
                    val validationError = ValidationError.entries[
                        data.getInt(ARG_VALIDATION_ERROR, 0)
                    ]

                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.BLOCKED   -> DownloadState.Pending(
                            packageName = task.packageName,
                            name = task.name,
                            version = task.release.version,
                            cacheFileName = task.release.cacheFileName,
                            repoId = task.repoId,
                            blocked = workInfo.state == WorkInfo.State.BLOCKED
                        )

                        WorkInfo.State.RUNNING   -> {
                            val progress = DownloadWorker.Companion.getProgress(data)
                            DownloadState.Downloading(
                                packageName = task.packageName,
                                name = task.name,
                                version = task.release.version,
                                cacheFileName = task.release.cacheFileName,
                                repoId = task.repoId,
                                read = progress.read,
                                total = progress.total,
                            )
                        }

                        WorkInfo.State.CANCELLED -> DownloadState.Cancel(
                            packageName = task.packageName,
                            name = task.name,
                            version = task.release.version,
                            cacheFileName = task.release.cacheFileName,
                            repoId = task.repoId,
                        )

                        WorkInfo.State.SUCCEEDED -> DownloadState.Success(
                            packageName = task.packageName,
                            name = task.name,
                            version = task.release.version,
                            cacheFileName = task.release.cacheFileName,
                            repoId = task.repoId,
                            release = task.release,
                        )

                        WorkInfo.State.FAILED    -> DownloadState.Error(
                            packageName = task.packageName,
                            name = task.name,
                            version = task.release.version,
                            cacheFileName = task.release.cacheFileName,
                            repoId = task.repoId,
                            resultCode = resultCode,
                            validationError = validationError,
                            stopReason = workInfo.stopReason
                        )
                    }.let { newState ->
                        downloadStateHandler.updateState(task.key, newState)
                    }
                }.onFailure { e ->
                    Log.e("WorkerManager", "Error updating download state", e)
                }
            }
        }
    }
}

val workmanagerModule = module {
    single { WorkerManager(get()) }
    single { WorkManager.getInstance(get()) }
    single { ActionReceiver() }
    single { NotificationManagerCompat.from(get()) }
}
