package com.machiav3lli.fdroid.manager.installer

import android.util.Log
import com.machiav3lli.fdroid.data.entity.InstallState
import com.machiav3lli.fdroid.data.entity.StateHolderFlow
import com.machiav3lli.fdroid.manager.installer.type.BaseInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the queue of installation tasks to ensure they run sequentially
 */
class InstallQueue : KoinComponent {
    private val mutex = Mutex()
    private val queueScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var processingStartTime: Long = 0
    private val installer: BaseInstaller by inject()
    private val installStateHolder by lazy {
        StateHolderFlow<InstallState>()
    }

    private val queue = Channel<InstallTask>(UNLIMITED)
    private val queuedPackages = ConcurrentHashMap.newKeySet<String>()
    val isProcessing: StateFlow<Boolean>
        private field = MutableStateFlow(false)
    val inUserInteraction: StateFlow<String>
        private field = MutableStateFlow("")

    private var currentTask: InstallTask? = null
    private var processorJob = queueScope.launch { processTasksFromChannel() }

    /**
     * Reactively handles enqueued installs
     */
    private suspend fun processTasksFromChannel() {
        queue.consumeEach { task ->
            mutex.withLock {
                currentTask = task
                isProcessing.update { true }
                processingStartTime = System.currentTimeMillis()
            }

            if (!installStateHolder.isHeld(task.packageName)
                && queuedPackages.contains(task.packageName)
            ) {
                emitProgress(InstallState.Preparing, task.packageName)
                // start notification
                installer.runInstall(task) { e ->
                    Log.e(
                        BaseInstaller.Companion.TAG,
                        "Unexpected error during installation of ${task.packageName}",
                        e
                    )
                    onInstallationComplete(
                        Result.failure(InstallationError.Unknown("Installation failed: ${e.message}"))
                    )
                }
                // remove notification
                queuedPackages.remove(task.packageName)
            }
            queuedPackages.remove(task.packageName)
        }
    }

    /**
     * Enqueues an installation task
     */
    suspend fun enqueue(
        packageName: String,
        packageLabel: String,
        cacheFileName: String,
        callback: (Result<String>) -> Unit
    ) {
        mutex.withLock {
            // Check if already queued or processing
            if (queuedPackages.contains(packageName) || currentTask?.packageName == packageName) {
                Log.d(TAG, "$packageName is already queued or processing")
                callback(Result.failure(InstallationError.Unknown("Package already in installation queue")))
                return
            }
            queuedPackages.add(packageName)
        }

        val task = InstallTask(packageName, packageLabel, cacheFileName, 0, callback)
        queue.send(task)
        Log.d(TAG, "Enqueued installation task for $packageName")
    }

    /**
     * Report progress stat to the states holder.
     */
    fun emitProgress(progress: InstallState, packageName: String? = null) {
        if (packageName != null) {
            installStateHolder.updateState(packageName, progress)
        } else {
            currentTask?.let { task ->
                installStateHolder.updateState(task.packageName, progress)
            }
        }

        Log.d(
            BaseInstaller.TAG,
            "Installation state updated for ${packageName ?: "current task"}: ${progress::class.simpleName}"
        )
    }

    /**
     * Register starting user interaction for a specific package
     */
    suspend fun startUserInteraction(packageName: String) {
        withContext(queueScope.coroutineContext) {
            inUserInteraction.update { packageName }
        }
    }

    /**
     * Checks if a package is currently in the installation queue
     */
    fun isEnqueued(packageName: String): Boolean {
        return (currentTask?.packageName == packageName || queuedPackages.contains(packageName)).apply {
            // TODO remove when more queueing logic is in place
            if (this) Log.d(
                TAG,
                "$packageName is ${if (currentTask?.packageName == packageName) "the current task" else "in the queue"}"
            )
        }
    }

    /**
     * Checks the health of the installation queue and cleans up any stale tasks
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun checkQueueHealth(): Boolean {
        return withContext(queueScope.coroutineContext) {
            mutex.withLock {
                try {
                    var cleanupPerformed = false

                    // If there's a current task but processing is false, we have an inconsistency
                    if (currentTask != null && !isProcessing.value) {
                        Log.w(
                            TAG,
                            "Queue health check: Inconsistent state detected for ${currentTask?.packageName}, cleaning up"
                        )
                        currentTask?.callback?.invoke(Result.failure(InstallationError.Unknown("Queue cleanup: inconsistent state")))
                        currentTask = null
                        inUserInteraction.update { "" }
                        cleanupPerformed = true
                    }

                    // Check for stuck processing based on time
                    if (isProcessing.value && currentTask != null && processingStartTime > 0) {
                        val processingTime = System.currentTimeMillis() - processingStartTime
                        if (processingTime > MAX_PROCESSING_TIME) {
                            Log.w(
                                TAG,
                                "Queue health check: Task ${currentTask?.packageName} has been processing for ${processingTime}ms, forcing cleanup"
                            )
                            currentTask?.callback?.invoke(Result.failure(InstallationError.Unknown("Installation timeout")))
                            currentTask = null
                            isProcessing.update { false }
                            inUserInteraction.update { "" }
                            processingStartTime = 0
                            cleanupPerformed = true
                        }
                    }

                    // Check for empty queue but still processing
                    if (isProcessing.value && currentTask == null && queue.isEmpty) {
                        Log.w(
                            TAG,
                            "Queue health check: Processing flag set but no tasks, resetting"
                        )
                        isProcessing.update { false }
                        inUserInteraction.update { "" }
                        cleanupPerformed = true
                    }

                    Log.d(
                        TAG,
                        "Queue health check completed. Queued packages: ${queuedPackages.size}, Processing: ${isProcessing.value}, Current task: ${currentTask?.packageName}"
                    )
                    cleanupPerformed
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking queue health: ${e.message}")
                    false
                }
            }
        }
    }

    /**
     * Checks if a package is currently in user interaction
     */
    fun isInUserInteraction(packageName: String?): Boolean {
        return inUserInteraction.value == packageName
    }

    /**
     * Cancels all pending installation tasks for a specific package
     */
    suspend fun cancel(packageName: String) {
        withContext(queueScope.coroutineContext) {
            mutex.withLock {
                queuedPackages.remove(packageName)

                // If current task is for this package, report cancellation
                if (currentTask?.packageName == packageName) {
                    currentTask?.callback?.invoke(Result.failure(InstallationError.UserCancelled()))
                    currentTask = null
                    isProcessing.update { false }
                    inUserInteraction.update { "" }
                }
            }
        }
    }

    /**
     * Called by installers when an installation completes (success or failure)
     */
    suspend fun onInstallationComplete(result: Result<String>) {
        withContext(queueScope.coroutineContext) {
            mutex.withLock {
                try {
                    val currentPackage = currentTask?.packageName

                    result.fold(
                        onSuccess = {
                            Log.d(
                                TAG,
                                "Installation completed successfully for $currentPackage"
                            )
                        },
                        onFailure = { error ->
                            // Only retry on specific errors
                            val shouldRetry = error !is InstallationError.UserCancelled &&
                                    error !is InstallationError.ConflictingSignature &&
                                    error !is InstallationError.Downgrade &&
                                    error !is InstallationError.Incompatible

                            Log.w(
                                TAG,
                                "Installation failed for $currentPackage: ${error.message}, shouldRetry=$shouldRetry"
                            )

                            if (shouldRetry && currentTask != null) {
                                val retryTask = currentTask
                                if (retryTask != null && retryTask.retryCount < MAX_RETRIES) { // Max 3 retries
                                    Log.d(
                                        TAG,
                                        "Re-enqueueing installation task for $currentPackage"
                                    )
                                    // Re-add to queued packages and send to channel
                                    queuedPackages.add(retryTask.packageName)
                                    queueScope.launch {
                                        queue.send(retryTask.copy(retryCount = retryTask.retryCount + 1))
                                    }
                                } else {
                                    Log.w(
                                        TAG,
                                        "Max retries reached for $currentPackage, not re-enqueueing"
                                    )
                                }
                            }
                        }
                    )

                    currentTask?.callback?.invoke(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing installation completion: ${e.message}")
                } finally {
                    currentTask = null
                    isProcessing.update { false }
                    inUserInteraction.update { "" }
                    processingStartTime = 0

                    if (queue.isEmpty && queuedPackages.isEmpty()) {
                        Log.d(TAG, "No more installation tasks in queue")
                    } else {
                        Log.d(TAG, "Queue has ${queuedPackages.size} tasks waiting")
                    }
                }
            }
        }
    }

    /**
     * Returns the current task being processed, if any
     */
    suspend fun getCurrentTask(): InstallTask? {
        return withContext(queueScope.coroutineContext) {
            mutex.withLock {
                currentTask
            }
        }
    }

    /**
     * Clears all pending tasks
     */
    suspend fun clear() {
        withContext(queueScope.coroutineContext) {
            mutex.withLock {
                queue.cancel()
                queuedPackages.clear()
                if (currentTask != null) {
                    currentTask?.callback?.invoke(Result.failure(InstallationError.UserCancelled()))
                    currentTask = null
                }
                isProcessing.update { false }
                inUserInteraction.update { "" }
                // Restart the processor with a fresh channel
                processorJob.cancel()
                processorJob = queueScope.launch { processTasksFromChannel() }
            }
        }
    }

    companion object {
        private const val TAG = "InstallQueue"
        private const val MAX_RETRIES = 3
        private const val MAX_PROCESSING_TIME = 5 * 60 * 1000L // 5 minutes

        data class InstallTask(
            val packageName: String,
            val packageLabel: String,
            val cacheFileName: String,
            val retryCount: Int = 0,
            val callback: (Result<String>) -> Unit
        )
    }
}