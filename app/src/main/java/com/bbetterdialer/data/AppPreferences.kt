package com.bbetterdialer.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Téma: a rendszert követi, vagy fix sötét / világos. */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

/** Elérhető nyelvek. */
enum class AppLanguage(val code: String) {
    HU("hu"),
    EN("en")
}

/** Launcher ikonváltozatok. */
enum class LauncherIcon(val suffix: String) {
    /** Zöld telefon, fekete háttér (ez volt eddig). */
    GREEN_PHONE_ON_BLACK("black"),
    /** Fehér telefon, zöld háttér. */
    WHITE_PHONE_ON_GREEN("green")
}

/**
 * Launcher névváltozatok.
 *
 * Az Android nem engedi futásidőben átírni egy `activity-alias` címkéjét
 * (a `android:label` statikus erőforrás), ezért a név előre definiált
 * álnevek közül választható. Minden kombinációhoz tartozik egy alias a
 * manifestben, és futásidőben pontosan egy van engedélyezve.
 */
enum class LauncherName(val suffix: String, val label: String) {
    DIALER("dialer", "Dialer"),
    TELEFON("telefon", "Telefon")
}

/**
 * ===========================================================================
 *  APP BEÁLLÍTÁSOK – téma, nyelv, testreszabás
 * ===========================================================================
 *  Minden érték Compose-state, ezért a felület azonnal átvált, és
 *  SharedPreferences-ben tárolódik, így újraindítás után is megmarad.
 * ===========================================================================
 */
object AppPreferences {

    private const val PREFS = "bbetter_app_prefs"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_APP_NAME = "app_name"
    private const val KEY_ICON = "launcher_icon"
    private const val KEY_LAUNCHER_NAME = "launcher_name"

    const val DEFAULT_APP_NAME = "BBetter."

    private var appContext: Context? = null
    private var loaded = false

    var themeMode: ThemeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var language: AppLanguage by mutableStateOf(AppLanguage.HU)
        private set

    /** Az appon belüli felirat (a fejléc közepén). */
    var appName: String by mutableStateOf(DEFAULT_APP_NAME)
        private set

    var launcherIcon: LauncherIcon by mutableStateOf(LauncherIcon.GREEN_PHONE_ON_BLACK)
        private set

    var launcherName: LauncherName by mutableStateOf(LauncherName.DIALER)
        private set

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val ctx = context.applicationContext
        appContext = ctx
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)

        language = runCatching {
            val stored = prefs.getString(KEY_LANGUAGE, null)
            if (stored != null) AppLanguage.valueOf(stored)
            else if (java.util.Locale.getDefault().language == "en") AppLanguage.EN else AppLanguage.HU
        }.getOrDefault(AppLanguage.HU)

        appName = if (prefs.contains(KEY_APP_NAME)) {
            prefs.getString(KEY_APP_NAME, "") ?: ""
        } else {
            DEFAULT_APP_NAME
        }

        launcherIcon = runCatching {
            LauncherIcon.valueOf(prefs.getString(KEY_ICON, null) ?: LauncherIcon.GREEN_PHONE_ON_BLACK.name)
        }.getOrDefault(LauncherIcon.GREEN_PHONE_ON_BLACK)

        launcherName = runCatching {
            LauncherName.valueOf(prefs.getString(KEY_LAUNCHER_NAME, null) ?: LauncherName.DIALER.name)
        }.getOrDefault(LauncherName.DIALER)
    }

    // ------------------------------------------------------------------ írás

    fun applyThemeMode(value: ThemeMode) {
        themeMode = value
        persist { it.putString(KEY_THEME, value.name) }
    }

    fun applyLanguage(value: AppLanguage) {
        language = value
        persist { it.putString(KEY_LANGUAGE, value.name) }
    }

    /** Üres érték is megengedett: olyankor a fejléc közepén nem jelenik meg semmi. */
    fun applyAppName(value: String) {
        appName = value.trim().take(24)
        persist { it.putString(KEY_APP_NAME, appName) }
    }

    fun setLauncherIcon(context: Context, value: LauncherIcon) {
        launcherIcon = value
        persist { it.putString(KEY_ICON, value.name) }
        applyLauncherAlias(context)
    }

    fun setLauncherName(context: Context, value: LauncherName) {
        launcherName = value
        persist { it.putString(KEY_LAUNCHER_NAME, value.name) }
        applyLauncherAlias(context)
    }

    // ------------------------------------------------------------------ launcher alias

    /** Az alias osztályneve a manifestben deklarált névvel egyezik. */
    fun aliasClassName(name: LauncherName, icon: LauncherIcon): String =
        "com.bbetterdialer.Launcher_${name.suffix}_${icon.suffix}"

    /** Pontosan egy launcher belépési pontot hagy engedélyezve. */
    fun applyLauncherAlias(context: Context) {
        val pm = context.packageManager
        val wanted = aliasClassName(launcherName, launcherIcon)
        LauncherName.entries.forEach { name ->
            LauncherIcon.entries.forEach { icon ->
                val className = aliasClassName(name, icon)
                runCatching {
                    pm.setComponentEnabledSetting(
                        ComponentName(context.packageName, className),
                        if (className == wanted) {
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        } else {
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        },
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        }
    }

    private fun persist(block: (android.content.SharedPreferences.Editor) -> Unit) {
        val ctx = appContext ?: return
        val editor = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        block(editor)
        editor.apply()
    }
}
