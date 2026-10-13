# BBetter Dialer — v5

Ultra-minimalista Android tárcsázó **Kotlinban, Jetpack Compose-szal**.
Sötét (AMOLED) és világos téma, magyar/angol felület, nulla blur és nulla kék
szín, névjegy-átnevezés, profilkép-kijelölés, profilszínek, kontaktmappák,
oldalsó fiók, működő csengőhang, napi bontású hívásnapló – és új névjegy
létrehozása egy ismeretlen számból.

| Build | Méret |
|---|---|
| `BBetter_Dialer5.0.apk` (R8 + erőforrás-zsugorítás, aláírt) | **1,04 MB** |
| `BBetter_Dialer5.0-debug.apk` | 8,38 MB |

---

## 0. Újdonságok a v5-ben

### 0.1 Javított profilszín – akkor is látszik, ha van fotó

**A hiba:** a kör háttere csak fotó nélkül kapta a választott színt
(`.background(if (image == null) accent else Surface3)`), ezért ha a
névjegynek már volt profilképe, a színválasztásnak nem volt látható hatása.

**A javítás:** fotó esetén a választott szín **gyűrűként** jelenik meg a kör
peremén:

```kotlin
.background(if (image == null) (colorOverride ?: accent) else Surface3)
.then(
    if (image != null && colorOverride != null) Modifier.border(3.dp, colorOverride, CircleShape)
    else Modifier
)
```

### 0.2 Nyelvválasztó (Magyar / English)

Beállítások → **Nyelv**. A felület azonnal átvált (nincs Activity-újratöltés),
és a beállítás újraindítás után is megmarad.

A szövegek egy `T` objektumban élnek getterekként, amelyek az
`AppPreferences.language` Compose-state-et olvassák – így a váltás
automatikusan végigmegy a teljes felületen:

```kotlin
object T {
    private val en: Boolean get() = AppPreferences.language == AppLanguage.EN
    val settings = hu("Beállítások", "Settings")
    ...
}
```

### 0.3 "Névjegy hozzáadása" – animált pirula

Ha a tárcsázóba írt szám (3+ jegy) **nincs a névjegyek között**, a fejléc jobb
szélén:

1. egy **világos szürke kör** jelenik meg – fokozatos halványulással
   (`animateFloatAsState`), nem becsúszva és nem beugorva,
2. **520 ms** után a kör rugós animációval **kapszulává szélesedik**
   (38 dp → 196 dp, `spring`), és megjelenik benne a felirat,
3. koppintásra lebegő ablak nyílik (mint a mappáknál): **név** és **profilszín**,
   majd a névjegy létrejön a rendszer névjegyadatbázisában
   (`RawContacts` + `StructuredName` + `Phone`) – `WRITE_CONTACTS` joggal.

**Hol jelenik meg?** A pirula helye a középső felirathoz igazodik:

* ha **van** középső felirat (App név), a pirula a tárcsázó **országsorában**, a
  kiírt ország nevével egy magasságban jelenik meg;
* ha a középső felirat **üres** („Semmi”), a pirula a fejlécben, a **hamburger
  ikonnal egy magasságban** jelenik meg.

### 0.4 "BBetter." felirat

A fejléc **közepén**, halvány világos szürkével (`TextTertiary`), 15 sp – nem
dominálja a felületet. A szöveg a Beállítások → App testreszabás alatt
átírható.

### 0.5 Világos téma + Téma opció

Beállítások → **Téma**: *Rendszer követése / Sötét / Világos*.

A színpaletta getterekké alakult (`val TextPrimary: Color get() = if (dark) …`),
ezért a témaváltás **minden hívási hely módosítása nélkül** végigmegy az egész
appon – a getterek a `ThemeState.dark` Compose-state-et olvassák.

### 0.6 App testreszabás (név + ikon)

Beállítások → **App testreszabás**:

* **App név** – szabad szöveg, a fejlécben jelenik meg. A **„Semmi”**
  opcióval teljesen eltüntethető (ekkor a fejléc közepén nem lesz semmi, és a
  „Névjegy hozzáadása” pirula a hamburger mellé kerül).
* **Launcher név** – `Dialer` vagy `Telefon`.
* **App ikon** – *zöld telefon fekete alapon* (ez volt eddig) vagy
  *fehér telefon zöld alapon*.

Az Android a launcherben megjelenő nevet futásidőben nem engedi átírni
(`android:label` statikus erőforrás), ezért a név és az ikon **négy
`activity-alias`** kombinációjából választható, és futásidőben pontosan egy van
engedélyezve (`PackageManager.setComponentEnabledSetting`):

| Alias | Név | Ikon |
|---|---|---|
| `Launcher_dialer_black` | Dialer | zöld telefon / fekete |
| `Launcher_dialer_green` | Dialer | fehér telefon / zöld |
| `Launcher_telefon_black` | Telefon | zöld telefon / fekete |
| `Launcher_telefon_green` | Telefon | fehér telefon / zöld |

A `MainActivity` szándékosan nem launcher belépési pont – a `tel:` és `DIAL`
szűrők viszont rajta maradtak.


Ultra-minimalista Android tárcsázó **Kotlinban, Jetpack Compose-szal**.
Tiszta AMOLED fekete, teljesen lapos felületek, **nulla blur és nulla kék szín**,
névjegy-átnevezés, profilkép-kijelölés, profilszínek, kontaktmappák, oldalsó
fiók, beállítások, működő csengőhang –és napi bontású hívásnapló.

| Build | Méret |
|---|---|
| `BBetter_Dialer5.0.apk` (R8 + erőforrás-zsugorítás, aláírt) | **1,02 MB** |
| `BBetter_Dialer5.0-debug.apk` | 8,34 MB |

---

## 1. Újdonságok a v4-ben

### 1.1 Profilkép: sima kör, forgó barázdált gyűrű hívás közben

A profilkép **többé nem olvad bele a háttérbe** – éles, sima kör
(`ContactAvatar`). Amíg mi kezdeményezzük a hívást és a másik fél nem veszi fel
(`CallStatus.DIALING`), a **kör peremén barázdált gyűrű forog**; a fotó nem
mozog, csak a gyűrű. Amint felveszik, a gyűrű eltűnik, és marad a sima kör.

```kotlin
ContactAvatar(
    image = photo,
    initials = initialsOf(displayName),
    size = 200.dp,
    accent = accent,
    spinning = state.status == CallStatus.DIALING   // felvétel után false
)
```

A barázdák (56 rovat) a kör peremén, a körön **belül** helyezkednek el, így a
váltás nem mozdítja el a körülötte lévő elrendezést. A rotáció végtelen
`animateFloat` 0°→360°, 3,2 s, lineáris.

### 1.2 Profilkép-választó kijelöléssel (zoom + mozgatás)

A galériából kiválasztott kép egy **kör alakú kijelölőben** jelenik meg:
húzással mozgatható, csippentéssel nagyítható (`detectTransformGestures`), és a
kijelölés a kép **bármelyik részére** ráhúzható. A megerősítés után ugyanez a
kivágás kerül a mentett képre.

A matematika normalizált egységnégyzetben fut, ezért felbontásfüggetlen:

```kotlin
val k0 = maxOf(1f / w, 1f / h)          // fedés az egységnégyzetre
val k = k0 * crop.scale
val side = 1f / k                        // kivágandó négyzet oldala (forrás px)
val left = (-imageLeft / k).coerceIn(0f, w - side)
```

### 1.3 Magasabb minőségű profilképek

* A névjegyekhez most a **nagy felbontású** `Phone.PHOTO_URI`-t olvassuk (nem a
  `PHOTO_THUMBNAIL_URI`-t).
* A helyi felülírás **1024 px**, JPEG 94 (a korábbi 640 px helyett).
* A listák 192 px-t, a hívóképernyő **768 px**-t kér – nincs többé pixelation.
* **Cache-kulcs javítás:** a kép-cache kulcsa mostantól a kért méretet is
  tartalmazza. Korábban egy listához betöltött kis kép „megmérgezte” a
  hívásképernyő nagy felbontású kérését – ez volt a pixelesség egyik oka.

### 1.4 Átnevezés (Beállítások → Név)

Új aloldal: névjegy keresése, koppintás, új név beírása, mentés. A módosítás
**az appban azonnal** látszik (helyi felülírás), és `WRITE_CONTACTS` jog
birtokában a **rendszer-névjegy is frissül** (`StructuredName.DISPLAY_NAME`).
A felület visszajelzi, melyik történt. Átnevezett névjegy visszaállítható.

Az átnevezett név **mindenhol** érvényesül: kontaktlista, tárcsázó-találat,
hívásképernyő, előzmények, beállítások – és a keresés is az új névre illeszkedik.

### 1.5 Profilszínek (Beállítások → Profil színek)

Fotó nélküli névjegyekhez választható háttérszín 12 tagú palettáról – a
**kezdőbetűk változatlanok** (fehérek) maradnak, csak a kör színe változik.
A választék szándékosan kék nélküli: piros, narancs, borostyán, citrom, zöld,
menta, teal, lila, málna, rózsaszín, barna, pala.

### 1.6 Kontaktlista: sima zöld telefon

A sorok végén a hívásgomb már **nem kör**: egy sima, kör nélküli zöld telefon
ikon (`PlainCallIcon`), nyomáskor enyhén elhalványulva.

### 1.7 Keresőmező

A keresősáv **jobb szélén** kis kereső ikon; amint van beírt szöveg, az ikon
fehérre vált.

### 1.8 Előzmények: napi bontás, összevonva

A hívásnapló időszakokra bontva, fentről lefelé:

```
Mai nap   · 12 hívás
  Anna Kovács      6×   ma 14:32
  Péter Nagy       1×   ma 09:10
Tegnap    · 4 hívás
A héten   · 9 hívás
Régebbi   · …
```

* Egy időszakon belül minden hívó **egyszer** szerepel, mellette hogy **hányszor**
  beszéltünk vele (`6×`).
* A sorok az utolsó hívás ideje szerint csökkenő sorrendben.
* Ahol a hívás csak nem fogadott volt, a kör piros.
* A `napokra bontás` `java.time`-mal történik (ma / 1 nap / <7 nap / régebbi).

---

## 2. Korábbi funkciók (változatlanul)

* **Oldalsó fiók** (hamburger, bal felül): balról becsúszó panel, a képernyő
  ~62%-a, lekerekített külső élek, rugós animáció, sötétedő háttér.
* **Beállítások** teljes képernyőn: Név · Profilképek · Profil színek ·
  Csengőhangok.
* **Csengőhang**: általános + kontakt-szintű, beépített hangok listájából vagy
  a fájlkezelőből (`ACTION_OPEN_DOCUMENT`), előhallgatással.
* **Bejövő hívás csengőhangja** a `RingtonePlayer`-rel – a manifest
  `IN_CALL_SERVICE_RINGING` metaadata miatt ezt nekünk kell lejátszanunk.
* **Kontaktmappák** és **mappacímke** a hívásképernyőn.
* **Tárcsázó**: hosszú nyomás a `0`-n → `+`, országfelismerés a kijelző felett,
  sötétzöld→élénk zöld animált hívásgomb.
* **Keresés**: valós időben, kis/nagybetű- és ékezetfüggetlenül, részleges
  egyezéssel, számra is.
* Kékmentes AMOLED paletta, nincs blur, nincs árnyék, nincs gradiens.

---

## 3. Projektstruktúra

```
BBetterDialer/app/src/main/java/com/bbetterdialer/
├── MainActivity.kt              egy Activity, edge-to-edge, tel: intentek,
│                                helyi tárolók init
├── data/
│   ├── Models.kt                Contact, CallEntry, CallerIdentity, InCallState
│   ├── PhoneRepository.kt       névjegyek (CONTACT_ID, PHOTO_URI), hívásnapló,
│   │                            PhoneLookup, LRU cache (méret-kulccsal), rawContactId
│   ├── ContactFolders.kt        helyi névjegy-mappák
│   ├── ContactOverrides.kt      ★ név- és színfelülírások
│   ├── AvatarOverrides.kt       ★ profilkép csere kijelöléssel (1024 px)
│   ├── CountryCodes.kt          országfelismerés (150+ előhívó, magyar nevek)
│   └── RingtoneSettings.kt      általános + kontakt-szintű csengőhangok
├── telecom/
│   ├── BBetterInCallService.kt    InCallService: állapotok, hívófél, mappa, csengőhang
│   ├── RingtonePlayer.kt        MediaPlayer-alapú csengőhang-lejátszó
│   ├── InCallBridge.kt          Compose state <-> Telecom híd
│   └── CallController.kt        ACTION_CALL / ACTION_DIAL
└── ui/
    ├── DialerApp.kt             fülek, csúszó kapszula, fiók, beállítás overlay
    ├── Common.kt                formázás, kereső-matcher, profilkép betöltő state
    ├── theme/   Palette.kt, Theme.kt
    ├── design/  Surfaces.kt, Buttons.kt, SearchField.kt, Dialog.kt,
    │            FolderBadge.kt, Avatar.kt ★, AvatarCropper.kt ★, Drawer.kt
    └── screens/ ContactsScreen.kt, DialerScreen.kt, HistoryScreen.kt ★,
                 InCallScreen.kt, HintCard.kt, SettingsScreen.kt,
                 NameSettingsScreen.kt ★, PhotoSettingsScreen.kt,
                 ColorSettingsScreen.kt ★, RingtoneSettingsScreen.kt
```

**Csomagnév:** `com.bbetterdialer` · **Projektmappa:** `BBetterDialer` · **APK:** `BBetter_Dialer5.0.apk`

**Jogosultságok:** `CALL_PHONE`, `READ_CONTACTS`, `WRITE_CONTACTS`,
`READ_CALL_LOG`, `READ_PHONE_STATE`, `READ_PHONE_NUMBERS`, `MANAGE_OWN_CALLS`,
`POST_NOTIFICATIONS`.

**Függőségek** – változatlanul minimális: Compose BOM + `ui`, `foundation`,
`material3`, `animation`, `activity-compose`, `core-ktx`, `coroutines`.

---

## 4. Build és telepítés

```bash
./gradlew :app:assembleRelease
adb install -r BBetter_Dialer5.0.apk
adb shell am start -n com.bbetterdialer/.MainActivity
```

A beállítások a bal felső **hamburger** ikonnal érhetők el.

## 5. Ismert korlátok

* A `+1` (NANP) és a `+7` több országot fed – ott az elsődleges ország nevét
  írja ki.
* A mappák, csengőhangok, név- és színfelülírások **app-helyiek**; a név és a
  profilkép írása a rendszer-névjegybe `WRITE_CONTACTS` jogot igényel, és
  csak olvasható (szinkronizált) névjegynél nem sikerül – ilyenkor a helyi
  felülírás akkor is érvényben marad.
* A profilkép kijelölő `detectTransformGestures`-t használ: egérrel/asztali
  előnézetben nem működik, érintőképernyőn igen.
* Teljes értékű tárcsázóhoz még hiányzik a `CallStyle` értesítés, a hívásnapló
  írása (`WRITE_CALL_LOG`) és a többhívásos kezelés.
