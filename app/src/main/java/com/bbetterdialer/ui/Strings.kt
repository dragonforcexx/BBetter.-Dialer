package com.bbetterdialer.ui

import com.bbetterdialer.ui.T

import com.bbetterdialer.data.AppLanguage
import com.bbetterdialer.data.AppPreferences

/**
 * ===========================================================================
 *  FELÜLET SZÖVEGEK – magyar / angol
 * ===========================================================================
 *  Minden felirat getterként él, és az [AppPreferences.language] Compose-state-et
 *  olvassa. Így a nyelvváltás **azonnal**, az Activity újratöltése nélkül
 *  végigmegy a teljes felületen, és a beállítás újraindítás után is megmarad.
 *
 *  Használat: `Text(T.settings)`.
 * ===========================================================================
 */
object T {

    private val en: Boolean get() = AppPreferences.language == AppLanguage.EN

    private fun hu(text: String, english: String) = if (en) english else text

    // ---------------------------------------------------------------- általános

    val menu = hu("Menü", "Menu")
    val back = hu("Vissza", "Back")
    val close = hu("Bezárás", "Close")
    val cancel = hu("Mégsem", "Cancel")
    val save = hu("Mentés", "Save")
    val create = hu("Létrehozás", "Create")
    val done = hu("Kész", "Done")
    val call = hu("Hívás", "Call")
    val delete = hu("Törlés", "Delete")
    val reset = hu("Visszaállítás", "Reset")
    val apply = hu("Alkalmaz", "Apply")
    val working = hu("Feldolgozás…", "Working…")
    val activeCallBanner = hu("Vissza a híváshoz", "Back to call")
    val search = hu("Keresés", "Search")

    // ---------------------------------------------------------------- fülek

    val tabContacts = hu("Névjegyek", "Contacts")
    val tabKeypad = hu("Tárcsázó", "Keypad")
    val tabHistory = hu("Előzmények", "History")

    // ---------------------------------------------------------------- tárcsázó

    val dial = hu("Tárcsázz", "Dial")
    val deleteLastDigit = hu("Utolsó számjegy törlése", "Delete last digit")
    val startCall = hu("Hívás indítása", "Start call")

    // ---------------------------------------------------------------- névjegyek

    val searchNameOrNumber = hu("Név vagy szám keresése", "Search name or number")
    val searchContacts = hu("Névjegy keresése", "Search contacts")
    val all = hu("Mind", "All")
    val newFolder = hu("＋ Mappa", "+ Folder")
    val folders = hu("Mappák", "Folders")
    val folder = hu("Mappa", "Folder")
    val none = hu("Nincs", "None")
    val noResults = hu("Nincs találat", "No results")
    val emptyFolder = hu("Ez a mappa üres", "This folder is empty")
    val noContactsDevice = hu("A készüléken nincs olvasható névjegy.", "No readable contacts on this device.")
    val tryOtherName = hu("Próbálj más nevet vagy számot.", "Try another name or number.")
    val emptyFolderBody = hu(
        "Ebben a mappában még nincs névjegy. Válaszd a többi fület, és rendeld mappába.",
        "No contacts in this folder yet. Pick another tab and assign one."
    )
    val noPermissionContacts = hu("Nincs hozzáférés a névjegyekhez", "No access to contacts")
    val noPermissionContactsBody = hu(
        "Engedélyezd a Névjegyek olvasását, hogy név és profilkép alapján tárcsázhass.",
        "Allow reading contacts to dial by name and profile photo."
    )
    val toKeypad = hu("Tárcsázóhoz", "To keypad")
    val newFolderName = hu("Új mappa neve", "New folder name")
    val foldersHint = hu(
        "A mappák az appban tárolt helyi csoportok. A hívásképernyő a hívó fél mappáját címkeként mutatja.",
        "Folders are local groups stored in the app. The call screen shows the caller's folder as a badge."
    )
    val noFoldersYet = hu("Még nincs mappád.", "You have no folders yet.")
    val contactWord = hu("névjegy", "contacts")

    fun contactCount(count: Int) = if (en) "$count contacts" else "$count névjegy"

    // ---------------------------------------------------------------- előzmények

    val today = hu("Mai nap", "Today")
    val yesterday = hu("Tegnap", "Yesterday")
    val thisWeek = hu("A héten", "This week")
    val older = hu("Régebbi", "Older")

    fun callsCount(count: Int) = if (en) "$count calls" else "$count hívás"

    val noPermissionCallLog = hu("Nincs hozzáférés a hívásnaplóhoz", "No access to the call log")
    val noPermissionCallLogBody = hu(
        "Engedélyezd a Hívásnapló olvasását a beállításokban, hogy lásd a legutóbbi hívásokat.",
        "Allow reading the call log in settings to see recent calls."
    )
    val noCallsYet = hu("Még nincs hívásod", "No calls yet")
    val noCallsYetBody = hu(
        "Az itt megjelenő bejegyzéseket a rendszer hívásnaplójából olvassuk, napokra bontva.",
        "Entries come from the system call log, grouped by day."
    )

    // ---------------------------------------------------------------- hívásképernyő

    val statusDialing = hu("TÁRCSÁZÁS", "DIALING")
    val statusIncoming = hu("BEJÖVŐ HÍVÁS", "INCOMING CALL")
    val statusActive = hu("HÍVÁSBAN", "IN CALL")
    val statusHolding = hu("TARTÁSBAN", "ON HOLD")
    val statusEnded = hu("BEFEJEZVE", "ENDED")

    val hintConnecting = hu("Kapcsolódás…", "Connecting…")
    val hintRinging = hu("Csörög…", "Ringing…")
    val hintHolding = hu("Tartásban", "On hold")
    val hintEnded = hu("A hívás befejeződött", "Call ended")

    val mute = hu("Némítás", "Mute")
    val speaker = hu("Kihangosítás", "Speaker")
    val decline = hu("Elutasítás", "Decline")
    val answer = hu("Fogadás", "Answer")
    val endCall = hu("Hívás befejezése", "End call")
    val hideCallScreen = hu("Hívásképernyő elrejtése", "Hide call screen")
    val answerCall = hu("Hívás fogadása", "Answer call")
    val unknownCaller = hu("Ismeretlen hívó", "Unknown caller")

    // ---------------------------------------------------------------- beállítások

    val settings = hu("Beállítások", "Settings")
    val sectionContacts = hu("Névjegyek", "Contacts")
    val name = hu("Név", "Name")
    val nameSub = hu("Névjegyek átnevezése", "Rename contacts")
    val profilePhotos = hu("Profilképek", "Profile photos")
    val profilePhotosSub = hu("Fotó cseréje galériából, kijelöléssel", "Change photo from gallery with cropping")
    val profileColors = hu("Profil színek", "Profile colors")
    val profileColorsSub = hu("Szín választása a fotó nélküli névjegyekhez", "Pick a color for contacts without a photo")
    val sectionAppearance = hu("Megjelenés", "Appearance")
    val theme = hu("Téma", "Theme")
    val themeSub = hu("Sötét, világos vagy rendszer követése", "Dark, light or follow system")
    val themeSystem = hu("Rendszer követése", "Follow system")
    val themeDark = hu("Sötét", "Dark")
    val themeLight = hu("Világos", "Light")
    val language = hu("Nyelv", "Language")
    val languageSub = hu("A felület nyelve", "Interface language")
    val sectionSounds = hu("Hangok", "Sounds")
    val ringtones = hu("Csengőhangok", "Ringtones")
    val ringtonesSub = hu("Általános és kontakt-szintű csengőhang, fájlválasztással", "General and per-contact ringtones with file picker")
    val customization = hu("App testreszabás", "App customization")
    val customizationSub = hu("App név, launcher név és ikon", "App name, launcher name and icon")

    // ---------------------------------------------------------------- testreszabás

    val appName = hu("App név", "App name")
    val appNameNone = hu("Semmi", "None")
    val appNameHint = hu(
        "Ez a felirat jelenik meg a fejléc közepén. A Semmi opcióval eltuntethető.",
        "This label appears in the middle of the header. Use None to hide it."
    )
    val launcherName = hu("Launcher név", "Launcher name")
    val launcherNameHint = hu(
        "Az Android a launcherben megjelenő nevet futásidőben nem engedi átírni, ezért előre beállított változatok közül választható.",
        "Android does not allow changing the launcher label at runtime, so you can pick from preset variants."
    )
    val launcherIcon = hu("App ikon", "App icon")
    val iconGreenOnBlack = hu("Zöld telefon, fekete háttér", "Green phone on black")
    val iconWhiteOnGreen = hu("Fehér telefon, zöld háttér", "White phone on green")
    val launcherApplied = hu(
        "A launcher ikon és név néhány másodpercen belül frissül.",
        "The launcher icon and name update within a few seconds."
    )

    // ---------------------------------------------------------------- név szerkesztés

    val renameTitle = hu("Név szerkesztése", "Edit name")
    val newName = hu("Új név", "New name")
    val originalLabel = hu("Eredeti", "Original")
    val renamedBadge = hu("Átnevezve", "Renamed")
    val renameSub = hu(
        "Válassz névjegyet és írd át a nevét. A módosítás az appban azonnal látszik; a rendszer-névjegyhez WRITE_CONTACTS jog kell.",
        "Pick a contact and edit its name. The change is immediate in the app; the system contact needs WRITE_CONTACTS."
    )
    val nameSavedSystem = hu("Név mentve, és a rendszer-névjegy is frissült.", "Name saved and the system contact was updated.")
    val nameSavedLocal = hu("Név mentve az appban (a rendszer-névjegy nem módosítható).", "Name saved in the app (the system contact is not writable).")
    val nameEmpty = hu("A név nem lehet üres", "The name cannot be empty")
    val nameReset = hu("A név visszaállítva.", "Name restored.")

    // ---------------------------------------------------------------- profilképek

    val profilePhotosHint = hu(
        "Válassz névjegyet, majd egy képet a galériából. A következő lépésben a körben beállíthatod, melyik rész kerüljön a profilba.",
        "Pick a contact, then an image from the gallery. In the next step you can position it inside the circle."
    )
    val cropTitle = hu("Profilkép beállítása", "Set profile photo")
    val cropHint = hu(
        "Húzással mozgathatod, csippentéssel nagyíthatod. A kör mutatja, mi kerül a profilba.",
        "Drag to move, pinch to zoom. The circle shows what goes into the profile."
    )
    val customPhotoBadge = hu("Egyéni kép", "Custom photo")
    val changePhoto = hu("Kép cseréje", "Change photo")
    val resetPhoto = hu("Helyi kép visszaállítása", "Reset local photo")
    val photoSavedSystem = hu("Kép mentve, és a rendszer-névjegy is frissült.", "Photo saved and the system contact was updated.")
    val photoSavedLocal = hu("Kép mentve az appban (a rendszer-névjegy nem módosítható).", "Photo saved in the app (the system contact is not writable).")
    val imageUnreadable = hu("A kiválasztott kép nem olvasható", "The selected image cannot be read")
    val photoReset = hu("A helyi kép visszaállítva.", "Local photo reset.")
    val noContactsShown = hu("Nincs megjeleníthető névjegy.", "No contacts to show.")

    // ---------------------------------------------------------------- profilszínek

    val profileColorsHint = hu(
        "Válassz színt azoknak a névjegyeknek, amelyeknek nincs fotójuk – fotó esetén a szín gyűrűként jelenik meg. A kezdőbetűk változatlanok maradnak.",
        "Pick a color for contacts without a photo – with a photo the color appears as a ring. The initials stay unchanged."
    )
    val coloredBadge = hu("Színezve", "Colored")
    val pickColor = hu("Szín választása", "Pick color")
    val deleteColor = hu("Szín törlése", "Remove color")
    val profileColor = hu("Profilszín", "Profile color")
    val profileColorHint = hu(
        "Ez a szín jelenik meg a névjegy körében, ha nincs profilképe.",
        "This color appears in the contact's circle when there is no profile photo."
    )
    val defaultColor = hu("Alapértelmezett", "Default")

    // ---------------------------------------------------------------- új névjegy

    val addContact = hu("Névjegy hozzáadása", "Add contact")
    val addContactShort = hu("+", "+")
    val newContact = hu("Új névjegy", "New contact")
    val contactNumber = hu("Telefonszám", "Phone number")
    val contactSaved = hu("Névjegy elmentve.", "Contact saved.")
    val contactSaveFailed = hu("Nem sikerült elmenteni a névjegyet.", "Could not save the contact.")
    val contactNoPermission = hu(
        "A névjegy létrehozásához WRITE_CONTACTS jogosultság kell.",
        "Creating a contact requires the WRITE_CONTACTS permission."
    )
    val pickNameAndColor = hu("Név és profilszín", "Name and profile color")

    // ---------------------------------------------------------------- csengőhangok

    val general = hu("Általános – minden hívás", "General – all calls")
    val ringtone = hu("Csengőhang", "Ringtone")
    val contactRingtone = hu("Kontaktokhoz rendelt csengőhang", "Per-contact ringtone")
    val systemDefault = hu("Rendszer alapértelmezett", "System default")
    val pickFile = hu("Fájl kiválasztása a telefonról…", "Pick a file from the phone…")
    val builtInSounds = hu("Beépített hangok", "Built-in sounds")
    val preview = hu("Meghallgatás", "Preview")
    val stopPlayback = hu("Leállítás", "Stop")
    val pickRingtone = hu("Csengőhang választása", "Choose ringtone")
    val removeOwnRingtone = hu("Saját csengőhang törlése", "Remove custom ringtone")
    val ringtonePickerHint = hu(
        "Válassz a beépített hangok közül, vagy nyisd meg a fájlkezelőt.",
        "Pick a built-in sound or open the file picker."
    )

    fun generalFallback(label: String) = if (en) "General: $label" else "Általános: $label"

    // ---------------------------------------------------------------- relatív idő

    val timeNow = hu("most", "just now")
    val timeYesterday = hu("tegnap", "yesterday")

    fun minutesAgo(minutes: Long) = if (en) "${minutes}m ago" else "$minutes perce"
    fun hoursAgo(hours: Long) = if (en) "${hours}h ago" else "$hours órája"
    fun daysAgo(days: Long) = if (en) "${days}d ago" else "$days napja"
}
