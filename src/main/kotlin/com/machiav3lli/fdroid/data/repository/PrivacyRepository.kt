package com.machiav3lli.fdroid.data.repository

import com.machiav3lli.fdroid.data.database.dao.DownloadStatsDao
import com.machiav3lli.fdroid.data.database.dao.DownloadStatsFileDao
import com.machiav3lli.fdroid.data.database.dao.ExodusInfoDao
import com.machiav3lli.fdroid.data.database.dao.RBLogDao
import com.machiav3lli.fdroid.data.database.dao.TrackerDao
import com.machiav3lli.fdroid.data.database.entity.DownloadStats
import com.machiav3lli.fdroid.data.database.entity.DownloadStatsFileMetadata
import com.machiav3lli.fdroid.data.database.entity.ExodusInfo
import com.machiav3lli.fdroid.data.database.entity.MonthlyPackageSum
import com.machiav3lli.fdroid.data.database.entity.RBLog
import com.machiav3lli.fdroid.data.database.entity.Tracker
import com.machiav3lli.fdroid.manager.network.DownloadStatsAPI
import com.machiav3lli.fdroid.manager.network.RBAPI
import com.machiav3lli.fdroid.manager.network.RExodusAPI
import com.machiav3lli.fdroid.utils.extension.text.getIsoDateOfMonthsAgo
import com.machiav3lli.fdroid.utils.extension.text.isoDateToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyRepository(
    private val trackerDao: TrackerDao,
    private val rbDao: RBLogDao,
    private val exodusDao: ExodusInfoDao,
    private val downloadStatsDao: DownloadStatsDao,
    private val dsFileDao: DownloadStatsFileDao,
) {
    private val cc = Dispatchers.IO
    private val jcc = Dispatchers.IO + SupervisorJob()

    fun getAllTrackers() = trackerDao.getAllFlow()
        .flowOn(cc)

    fun getExodusInfos(packageName: String): Flow<List<ExodusInfo>> = exodusDao.getFlow(packageName)
        .flowOn(cc)

    fun getRBLogs(packageName: String): Flow<List<RBLog>> = rbDao.getFlow(packageName)
        .flowOn(cc)

    fun getDownloadStats(packageName: String): Flow<List<DownloadStats>> =
        downloadStatsDao.getFlow(packageName)
            .flowOn(cc)

    fun getLatestDownloadStats(packageName: String): Flow<List<DownloadStats>> =
        downloadStatsDao.getFlowSince(packageName, getIsoDateOfMonthsAgo(3).isoDateToInt())
            .flowOn(cc)

    fun getMonthlyDownloadStats(packageName: String): Flow<List<MonthlyPackageSum>> =
        downloadStatsDao.getFlowMonthlySumForPackage(packageName)
            .flowOn(cc)

    suspend fun loadDownloadStatsModifiedMap(): Map<String, String> = withContext(jcc) {
        dsFileDao.getLastModifiedDates()
    }

    suspend fun upsertTracker(trackers: Collection<Tracker>) {
        withContext(jcc) {
            trackerDao.multipleUpserts(trackers.toList())
        }
    }

    suspend fun upsertRBLogs(logs: Collection<RBLog>) {
        withContext(jcc) {
            rbDao.multipleUpserts(logs.toList())
        }
    }

    suspend fun upsertExodusInfo(infos: ExodusInfo) {
        withContext(jcc) {
            exodusDao.upsert(infos)
        }
    }

    suspend fun upsertDownloadStats(downloadStats: Collection<DownloadStats>) {
        withContext(jcc) {
            downloadStatsDao.multipleUpserts(downloadStats)
        }
    }

    suspend fun upsertDownloadStatsFileMetadata(metadata: Collection<DownloadStatsFileMetadata>) {
        withContext(jcc) {
            dsFileDao.multipleUpserts(metadata)
        }
    }

    suspend fun upsertDownloadStatsFileMetadata(
        fileName: String,
        lastModified: String,
        fileSize: Long? = null,
        recordsCount: Int? = null
    ) {
        withContext(jcc) {
            val metadata = DownloadStatsFileMetadata(
                fileName = fileName,
                lastModified = lastModified,
                lastFetched = System.currentTimeMillis(),
                fetchSuccess = true,
                fileSize = fileSize,
                recordsCount = recordsCount
            )
            dsFileDao.upsert(metadata)
        }
    }
}

val privacyModule = module {
    singleOf(::RExodusAPI)
    singleOf(::RBAPI)
    singleOf(::DownloadStatsAPI)
}