package com.machiav3lli.fdroid.data.entity

import android.content.Context
import android.content.pm.PermissionGroupInfo
import android.content.pm.PermissionInfo
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import com.machiav3lli.fdroid.FILTER_CATEGORY_ALL
import com.machiav3lli.fdroid.R
import com.machiav3lli.fdroid.data.content.Preferences
import com.machiav3lli.fdroid.data.database.entity.Release
import com.machiav3lli.fdroid.ui.compose.icons.Icon
import com.machiav3lli.fdroid.ui.compose.icons.Phosphor
import com.machiav3lli.fdroid.ui.compose.icons.icon.IcDonateLiberapay
import com.machiav3lli.fdroid.ui.compose.icons.icon.IcDonateLitecoin
import com.machiav3lli.fdroid.ui.compose.icons.icon.IcDonateOpencollective
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ApplePodcastsLogo
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ArrowSquareOut
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Asterisk
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Barbell
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.BookBookmark
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Books
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Brain
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Browser
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Calendar
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Cardholder
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Chat
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CheckSquare
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ChefHat
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CircleWavyWarning
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CirclesFour
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CirclesThreePlus
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Clock
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CloudArrowDown
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CloudSun
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Code
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Command
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Compass
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CurrencyBTC
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CurrencyDollarSimple
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Download
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Envelope
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.GameController
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Ghost
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Globe
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.GlobeSimple
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Graph
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.HeartStraight
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.HeartStraightFill
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.House
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Image
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Images
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Key
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Keyboard
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Leaf
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.MathOperations
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Microphone
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Newspaper
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.NotePencil
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Nut
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.PaintBrush
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Password
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.PenNib
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Phone
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Pizza
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.PlayCircle
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Robot
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.RssSimple
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Scales
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ScribbleLoop
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ShareNetwork
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ShieldCheck
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ShieldStar
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ShoppingCart
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.SlidersHorizontal
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Storefront
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Swatches
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.TrainSimple
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Translate
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.TrashSimple
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.VideoConference
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Wallet
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.WifiHigh
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Wrench
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.X
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Author(val name: String = "", val email: String = "", val web: String = "") {
    fun toJSON() = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String) = Json.decodeFromString<Author>(json)
    }
}

@Serializable
sealed class Donate {
    @Serializable
    data class Regular(val url: String) : Donate()

    @Serializable
    data class Bitcoin(val address: String) : Donate()

    @Serializable
    data class Litecoin(val address: String) : Donate()

    @Serializable
    data class Liberapay(val id: String) : Donate()

    @Serializable
    data class OpenCollective(val id: String) : Donate()

    fun toJSON() = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String) = Json.decodeFromString<Donate>(json)
    }
}

enum class AntiFeature(val key: String, @StringRes val titleResId: Int) {
    ADS("Ads", R.string.has_advertising),
    DEBUGGABLE("ApplicationDebuggable", R.string.compiled_for_debugging),
    DISABLED_ALGORITHM("DisabledAlgorithm", R.string.signed_using_unsafe_algorithm),
    KNOWN_VULN("KnownVuln", R.string.has_security_vulnerabilities),
    NO_SOURCE_SINCE("NoSourceSince", R.string.source_code_no_longer_available),
    NON_FREE_ADD("NonFreeAdd", R.string.promotes_non_free_software),
    NON_FREE_ASSETS("NonFreeAssets", R.string.contains_non_free_media),
    NON_FREE_DEP("NonFreeDep", R.string.has_non_free_dependencies),
    NON_FREE_NET("NonFreeNet", R.string.promotes_non_free_network_services),
    TRACKING("Tracking", R.string.tracks_or_reports_your_activity),
    NON_FREE_UPSTREAM("UpstreamNonFree", R.string.upstream_source_code_is_not_free),
    NSFW("NSFW", R.string.not_safe_for_work)
}

fun String.toAntiFeature(): AntiFeature? = AntiFeature.entries.find { it.key == this }

sealed interface ComponentState {
    val icon: ImageVector
    val textId: Int
}

sealed class ActionState(
    @StringRes override val textId: Int,
    override val icon: ImageVector = Phosphor.Download,
) : ComponentState {

    data object Install : ActionState(R.string.install, Phosphor.Download)
    data object Update : ActionState(R.string.update, Phosphor.Download)
    data object Uninstall : ActionState(R.string.uninstall, Phosphor.TrashSimple)
    data object Launch : ActionState(R.string.launch, Phosphor.ArrowSquareOut)
    data object Details : ActionState(R.string.details, Phosphor.SlidersHorizontal)
    data object Share : ActionState(R.string.share, Phosphor.ShareNetwork)
    data object CancelPending : ActionState(R.string.pending, Phosphor.X)
    data object CancelConnecting : ActionState(R.string.connecting, Phosphor.X)
    data object CancelDownloading : ActionState(R.string.downloading, Phosphor.X)
    data object NoAction : ActionState(R.string.no_action_possible, Phosphor.X)
    data object Bookmark : ActionState(R.string.favorite_add, Phosphor.HeartStraight)
    data object Bookmarked : ActionState(R.string.favorite_remove, Phosphor.HeartStraightFill)
}

open class LinkType(
    val icon: ImageVector,
    val title: String,
    val link: Uri? = null,
)

class DonateType(donate: Donate, context: Context) : LinkType(
    icon = when (donate) {
        is Donate.Regular        -> Phosphor.CurrencyDollarSimple
        is Donate.Bitcoin        -> Phosphor.CurrencyBTC
        is Donate.Litecoin       -> Icon.IcDonateLitecoin
        is Donate.Liberapay      -> Icon.IcDonateLiberapay
        is Donate.OpenCollective -> Icon.IcDonateOpencollective
    },
    title = when (donate) {
        is Donate.Regular        -> context.getString(R.string.website)
        is Donate.Bitcoin        -> "Bitcoin"
        is Donate.Litecoin       -> "Litecoin"
        is Donate.Liberapay      -> "Liberapay"
        is Donate.OpenCollective -> "Open Collective"
    },
    link = when (donate) {
        is Donate.Regular        -> donate.url.toUri()
        is Donate.Bitcoin        -> "bitcoin:${donate.address}".toUri()
        is Donate.Litecoin       -> "litecoin:${donate.address}".toUri()
        is Donate.Liberapay      -> "https://liberapay.com/${donate.id}".toUri()
        is Donate.OpenCollective -> "https://opencollective.com/${donate.id}".toUri()
    }
)

data class Request(
    val id: Int,
    val installed: Boolean,
    val updates: Boolean,
    val updateCategory: UpdateCategory,
    val section: Section,
    val order: Order,
    val ascending: Boolean,
    val category: String,
    val filteredOutRepos: Set<String>,
    val filteredAntiFeatures: Set<String>,
    val filteredLicenses: Set<String>,
    val numberOfItems: Int = 0,
    val minSDK: Int = 0,
    val targetSDK: Int = 0,
) {
    companion object {
        val All: Request
            get() = Request(
                id = Source.AVAILABLE.ordinal,
                installed = false,
                updates = false,
                updateCategory = UpdateCategory.ALL,
                section = Section.All,
                order = Preferences[Preferences.Key.SortOrderExplore].order,
                ascending = Preferences[Preferences.Key.SortOrderAscendingExplore],
                category = Preferences[Preferences.Key.CategoriesFilterExplore],
                filteredOutRepos = Preferences[Preferences.Key.ReposFilterExplore],
                filteredAntiFeatures = Preferences[Preferences.Key.AntifeaturesFilterExplore],
                filteredLicenses = Preferences[Preferences.Key.LicensesFilterExplore],
                minSDK = Preferences[Preferences.Key.MinSDKExplore].ordinal,
                targetSDK = Preferences[Preferences.Key.TargetSDKExplore].ordinal,
            )

        val Favorites: Request
            get() = Request(
                id = Source.FAVORITES.ordinal,
                installed = false,
                updates = false,
                updateCategory = UpdateCategory.ALL,
                section = Section.FAVORITE,
                order = Preferences[Preferences.Key.SortOrderExplore].order,
                ascending = Preferences[Preferences.Key.SortOrderAscendingExplore],
                category = Preferences[Preferences.Key.CategoriesFilterExplore],
                filteredOutRepos = Preferences[Preferences.Key.ReposFilterExplore],
                filteredAntiFeatures = Preferences[Preferences.Key.AntifeaturesFilterExplore],
                filteredLicenses = Preferences[Preferences.Key.LicensesFilterExplore],
            )

        val Search: Request
            get() = Request(
                id = Source.SEARCH.ordinal,
                installed = false,
                updates = false,
                updateCategory = UpdateCategory.ALL,
                section = Section.All,
                order = Preferences[Preferences.Key.SortOrderSearch].order,
                ascending = Preferences[Preferences.Key.SortOrderAscendingSearch],
                category = Preferences[Preferences.Key.CategoriesFilterSearch],
                filteredOutRepos = Preferences[Preferences.Key.ReposFilterSearch],
                filteredAntiFeatures = Preferences[Preferences.Key.AntifeaturesFilterSearch],
                filteredLicenses = Preferences[Preferences.Key.LicensesFilterSearch],
                minSDK = Preferences[Preferences.Key.MinSDKSearch].ordinal,
                targetSDK = Preferences[Preferences.Key.TargetSDKSearch].ordinal,
            )

        val Installed: Request
            get() = Request(
                id = Source.INSTALLED.ordinal,
                installed = true,
                updates = false,
                updateCategory = UpdateCategory.ALL,
                section = Section.All,
                order = Preferences[Preferences.Key.SortOrderInstalled].order,
                ascending = Preferences[Preferences.Key.SortOrderAscendingInstalled],
                category = Preferences[Preferences.Key.CategoriesFilterInstalled],
                filteredOutRepos = Preferences[Preferences.Key.ReposFilterInstalled],
                filteredAntiFeatures = Preferences[Preferences.Key.AntifeaturesFilterInstalled],
                filteredLicenses = Preferences[Preferences.Key.LicensesFilterInstalled],
                minSDK = Preferences[Preferences.Key.MinSDKInstalled].ordinal,
                targetSDK = Preferences[Preferences.Key.TargetSDKInstalled].ordinal,
            )

        val SearchInstalled: Request
            get() = Request(
                id = Source.SEARCH_INSTALLED.ordinal,
                installed = true,
                updates = false,
                updateCategory = UpdateCategory.ALL,
                section = Section.All,
                order = Preferences[Preferences.Key.SortOrderSearch].order,
                ascending = Preferences[Preferences.Key.SortOrderAscendingSearch],
                category = Preferences[Preferences.Key.CategoriesFilterSearch],
                filteredOutRepos = Preferences[Preferences.Key.ReposFilterSearch],
                filteredAntiFeatures = Preferences[Preferences.Key.AntifeaturesFilterSearch],
                filteredLicenses = Preferences[Preferences.Key.LicensesFilterSearch],
                minSDK = Preferences[Preferences.Key.MinSDKSearch].ordinal,
                targetSDK = Preferences[Preferences.Key.TargetSDKSearch].ordinal,
            )

        val Updates: Request
            get() = Request(
                id = Source.UPDATES.ordinal,
                installed = true,
                updates = true,
                updateCategory = UpdateCategory.ALL,
                section = Section.All,
                order = Order.NAME,
                ascending = true,
                category = FILTER_CATEGORY_ALL,
                filteredOutRepos = emptySet(),
                filteredAntiFeatures = emptySet(),
                filteredLicenses = emptySet(),
            )

        val Updated: Request
            get() = Request(
                id = Source.UPDATED.ordinal,
                installed = false,
                updates = false,
                updateCategory = if (Preferences[Preferences.Key.HideNewApps]) UpdateCategory.ALL
                else UpdateCategory.UPDATED,
                section = Section.All,
                order = Preferences[Preferences.Key.SortOrderLatest].order,
                ascending = Preferences[Preferences.Key.SortOrderAscendingLatest],
                category = Preferences[Preferences.Key.CategoriesFilterLatest],
                filteredOutRepos = Preferences[Preferences.Key.ReposFilterLatest],
                filteredAntiFeatures = Preferences[Preferences.Key.AntifeaturesFilterLatest],
                filteredLicenses = Preferences[Preferences.Key.LicensesFilterLatest],
                numberOfItems = Preferences[Preferences.Key.UpdatedApps],
                minSDK = Preferences[Preferences.Key.MinSDKLatest].ordinal,
                targetSDK = Preferences[Preferences.Key.TargetSDKLatest].ordinal,
            )

        val New: Request
            get() = Request(
                id = Source.NEW.ordinal,
                installed = false,
                updates = false,
                updateCategory = UpdateCategory.NEW,
                section = Section.All,
                order = Order.DATE_ADDED,
                ascending = false,
                category = FILTER_CATEGORY_ALL,
                filteredOutRepos = emptySet(),
                filteredAntiFeatures = emptySet(),
                filteredLicenses = emptySet(),
                numberOfItems = Preferences[Preferences.Key.NewApps],
                minSDK = Preferences[Preferences.Key.MinSDKLatest].ordinal,
                targetSDK = Preferences[Preferences.Key.TargetSDKLatest].ordinal,
            )

        val SearchNew: Request
            get() = Request(
                id = Source.SEARCH.ordinal,
                installed = false,
                updates = false,
                updateCategory = UpdateCategory.NEW,
                section = Section.All,
                order = Order.DATE_ADDED,
                ascending = false,
                category = Preferences[Preferences.Key.CategoriesFilterSearch],
                filteredOutRepos = Preferences[Preferences.Key.ReposFilterSearch],
                filteredAntiFeatures = Preferences[Preferences.Key.AntifeaturesFilterSearch],
                filteredLicenses = Preferences[Preferences.Key.LicensesFilterSearch],
                minSDK = Preferences[Preferences.Key.MinSDKSearch].ordinal,
                targetSDK = Preferences[Preferences.Key.TargetSDKSearch].ordinal,
            )

        val None: Request
            get() = Request(
                id = Source.NONE.ordinal,
                installed = false,
                updates = false,
                updateCategory = UpdateCategory.ALL,
                section = Section.NONE,
                order = Order.DATE_ADDED,
                ascending = false,
                category = FILTER_CATEGORY_ALL,
                filteredOutRepos = emptySet(),
                filteredAntiFeatures = emptySet(),
                filteredLicenses = emptySet(),
                numberOfItems = 0,
            )
    }
}

sealed class DialogKey {
    data class Link(val uri: Uri) : DialogKey()
    open class Action(
        val label: String,
        val action: () -> Unit
    ) : DialogKey()

    class Download(
        label: String,
        action: () -> Unit
    ) : Action(label, action)

    class Uninstall(
        label: String,
        action: () -> Unit
    ) : Action(label, action)

    class BatchDownload(
        val labels: List<String>,
        val action: () -> Unit
    ) : DialogKey()

    data class ReleaseIncompatible(
        val incompatibilities: List<Release.Incompatibility>,
        val platforms: List<String>,
        val minSdkVersion: Int,
        val maxSdkVersion: Int,
    ) : DialogKey()

    data class ReleaseIssue(val resId: Int) : DialogKey()
    data class Launch(
        val packageName: String,
        val launcherActivities: List<Pair<String, String>>,
    ) : DialogKey()
}

data class Permission(
    val nameId: Int,
    val icon: ImageVector,
    val descriptionId: Int,
    val warningTextId: Int = -1,
    val ignorePref: Preferences.Key<Boolean>? = null,
) {
    companion object {
        val BatteryOptimization = Permission(
            R.string.ignore_battery_optimization_title,
            Phosphor.Leaf,
            R.string.ignore_battery_optimization_message,
            R.string.warning_disable_battery_optimization,
            Preferences.Key.IgnoreDisableBatteryOptimization,
        )
        val PostNotifications = Permission(
            R.string.post_notifications_permission_title,
            Phosphor.CircleWavyWarning,
            R.string.post_notifications_permission_message,
            R.string.warning_show_notification,
            Preferences.Key.IgnoreShowNotifications,
        )
        val InstallPackages = Permission(
            R.string.install_packages_permission_title,
            Phosphor.Download,
            R.string.install_packages_permission_message,
            -1,
        )
    }
}

class PermissionsType(
    val group: PermissionGroupInfo?,
    val permissions: List<PermissionInfo>,
)

val String.appCategoryIcon: ImageVector
    get() = when (this.lowercase()) {
        FILTER_CATEGORY_ALL.lowercase()    -> Phosphor.CirclesFour
        "app store & updater"              -> Phosphor.Storefront
        "audio"                            -> Phosphor.Microphone
        "audiovideo"                       -> Phosphor.PlayCircle
        "automation"                       -> Phosphor.Robot
        "bookmark"                         -> Phosphor.BookBookmark
        "browser"                          -> Phosphor.Browser
        "connectivity"                     -> Phosphor.WifiHigh
        "communication"                    -> Phosphor.Chat
        "calculator"                       -> Phosphor.MathOperations
        "calendar"                         -> Phosphor.Calendar
        "calendar & agenda"                -> Phosphor.Calendar
        "cloudstorage & file sync"         -> Phosphor.CloudArrowDown
        "development"                      -> Phosphor.Code
        "dns & hosts"                      -> Phosphor.ShieldCheck
        "draw"                             -> Phosphor.ScribbleLoop
        "education"                        -> Phosphor.Brain
        "e-book reader"                    -> Phosphor.BookBookmark
        "e-mail"                           -> Phosphor.Envelope
        "fdroid"                           -> Phosphor.Asterisk
        "fedilab"                          -> Phosphor.Graph
        "feed"                             -> Phosphor.RssSimple
        "file encryption & vault"          -> Phosphor.Key
        "file transfer"                    -> Phosphor.ShareNetwork
        "finance manager"                  -> Phosphor.CurrencyDollarSimple
        "food"                             -> Phosphor.Pizza
        "game"                             -> Phosphor.GameController
        "gallery"                          -> Phosphor.Images
        "games"                            -> Phosphor.GameController
        "graphics"                         -> Phosphor.PaintBrush
        "guardian project"                 -> Phosphor.ShieldStar
        "icon pack"                        -> Phosphor.CirclesThreePlus
        "internet"                         -> Phosphor.Globe
        "kde"                              -> Phosphor.Code
        "keyboard & ime"                   -> Phosphor.Keyboard
        "kidsgame"                         -> Phosphor.GameController
        "launcher"                         -> Phosphor.House
        "local media player"               -> Phosphor.PlayCircle
        "math"                             -> Phosphor.MathOperations
        "messaging"                        -> Phosphor.Chat
        "money"                            -> Phosphor.CurrencyDollarSimple
        "multimedia"                       -> Phosphor.PlayCircle
        "navigation"                       -> Phosphor.Compass
        "network"                          -> Phosphor.GlobeSimple
        "news"                             -> Phosphor.Newspaper
        "note"                             -> Phosphor.NotePencil
        "office"                           -> Phosphor.Books
        "offline"                          -> Phosphor.Leaf
        "online media player"              -> Phosphor.PlayCircle
        "osmand"                           -> Phosphor.Compass
        "pass wallet"                      -> Phosphor.Cardholder
        "password & 2fa"                   -> Phosphor.Password
        "phone & sms"                      -> Phosphor.Phone
        "podcast"                          -> Phosphor.ApplePodcastsLogo
        "productivity"                     -> Phosphor.Compass
        "public transport map & timetable" -> Phosphor.TrainSimple
        "qt"                               -> Phosphor.Books
        "reading"                          -> Phosphor.BookBookmark
        "recipe manager"                   -> Phosphor.ChefHat
        "recorder"                         -> Phosphor.Microphone
        "religion"                         -> Phosphor.Command
        "science"                          -> Phosphor.Brain
        "science & education"              -> Phosphor.Brain
        "security"                         -> Phosphor.ShieldStar
        "shopping list"                    -> Phosphor.ShoppingCart
        "social network"                   -> Phosphor.Graph
        "sports & health"                  -> Phosphor.Barbell
        "system"                           -> Phosphor.Nut
        "task"                             -> Phosphor.CheckSquare
        "text editor"                      -> Phosphor.NotePencil
        "theming"                          -> Phosphor.Swatches
        "time"                             -> Phosphor.Clock
        "translation & dictionary"         -> Phosphor.Translate
        "unit convertor"                   -> Phosphor.Scales
        "utility"                          -> Phosphor.Wrench
        "video"                            -> Phosphor.PlayCircle
        "voice & video chat"               -> Phosphor.VideoConference
        "vpn & proxy"                      -> Phosphor.Ghost
        "wallet"                           -> Phosphor.Wallet
        "wallpaper"                        -> Phosphor.Image
        "weather"                          -> Phosphor.CloudSun
        "workout"                          -> Phosphor.Barbell
        "writing"                          -> Phosphor.PenNib
        "xposed"                           -> Phosphor.ShieldStar
        else                               -> Phosphor.Asterisk
    }
/*
- Default (default apps from ROMs)
 */