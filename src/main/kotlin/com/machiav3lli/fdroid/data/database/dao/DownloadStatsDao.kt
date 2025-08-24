package com.machiav3lli.fdroid.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.machiav3lli.fdroid.ROW_ISO_DATE
import com.machiav3lli.fdroid.ROW_PACKAGE_NAME
import com.machiav3lli.fdroid.data.database.entity.ClientPackageSum
import com.machiav3lli.fdroid.data.database.entity.DownloadStats
import com.machiav3lli.fdroid.data.database.entity.MonthlyPackageSum
import com.machiav3lli.fdroid.data.database.entity.PackageSum
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadStatsDao : BaseDao<DownloadStats> {
    @Query("SELECT COUNT(*) == 0 FROM download_stats")
    fun isEmpty(): Boolean

    @Query("SELECT * FROM download_stats WHERE packageName = :packageName")
    fun get(packageName: String): List<DownloadStats>

    @Query("SELECT * FROM download_stats WHERE packageName = :packageName")
    fun getFlow(packageName: String): Flow<List<DownloadStats>>

    @Query("SELECT * FROM download_stats WHERE packageName = :packageName AND $ROW_ISO_DATE >= :since")
    fun getFlowSince(packageName: String, since: Int): Flow<List<DownloadStats>>

    @Query("SELECT * FROM packagesum WHERE $ROW_PACKAGE_NAME = :packageName")
    fun getFlowPackageSum(packageName: String): Flow<PackageSum>

    @Query("SELECT * FROM clientpackagesum WHERE $ROW_PACKAGE_NAME = :packageName ORDER BY totalCount DESC")
    fun getFlowClientSumForPackage(packageName: String): Flow<List<ClientPackageSum>>

    @Query(
        """
        SELECT *
        FROM   monthlypackagesum
        WHERE  $ROW_PACKAGE_NAME = :packageName
        ORDER BY yearMonth DESC
        """
    )
    fun getFlowMonthlySumForPackage(packageName: String): Flow<List<MonthlyPackageSum>>

    @Query(
        """
        SELECT *
        FROM   packagesum
        GROUP BY $ROW_PACKAGE_NAME
        ORDER BY totalCount DESC
        LIMIT :limit
        """
    )
    fun getFlowOverallTopApps(limit: Int): Flow<List<PackageSum>>

    @Query(
        """
        SELECT *
        FROM   clientpackagesum
        WHERE  client = :clientName
        GROUP BY $ROW_PACKAGE_NAME, client
        ORDER BY totalCount DESC
        LIMIT :limit
        """
    )
    fun getFlowClientTopApps(clientName: String, limit: Int): Flow<List<ClientPackageSum>>

    @Query(
        """
        SELECT $ROW_PACKAGE_NAME   AS packageName,
               SUM(count)          AS totalCount
        FROM   download_stats
        WHERE  client = '_total' AND $ROW_ISO_DATE >= :startDateInclusive
        GROUP BY $ROW_PACKAGE_NAME
        ORDER BY totalCount DESC
        LIMIT :limit
        """
    )
    fun getFlowRecentTopApps(startDateInclusive: Int, limit: Int): Flow<List<PackageSum>>
}
