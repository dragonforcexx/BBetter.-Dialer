package com.bbetterdialer.data

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Országfelismerés a beírt számból.
 *
 * A táblázat szándékosan tömör szövegként tárolódik (kód|ISO|magyar név), és
 * egyszer parse-olódik. A leghosszabb előhívó illeszkedik először, így a
 * `+36` nem "nyeli el" a `+360`-at (nincs ilyen), és a `+1`/`+7` rövidebb
 * kódok csak akkor illeszkednek, ha nincs hosszabb találat.
 *
 * Ismert korlát: a `+1` (NANP) és a `+7` több országot fed, ezért ott az
 * elsődleges országot adja vissza (Amerikai Egyesült Államok, illetve Oroszország).
 */
object CountryCodes {

    data class Country(
        val dialCode: String,
        val iso: String,
        val name: String
    )

    private const val TABLE = """
1|US|Amerikai Egyesült Államok
7|RU|Oroszország
20|EG|Egyiptom
27|ZA|Dél-afrikai Köztársaság
30|GR|Görögország
31|NL|Hollandia
32|BE|Belgium
33|FR|Franciaország
34|ES|Spanyolország
36|HU|Magyarország
39|IT|Olaszország
40|RO|Románia
41|CH|Svájc
43|AT|Ausztria
44|GB|Egyesült Királyság
45|DK|Dánia
46|SE|Svédország
47|NO|Norvégia
48|PL|Lengyelország
49|DE|Németország
51|PE|Peru
52|MX|Mexikó
53|CU|Kuba
54|AR|Argentína
55|BR|Brazília
56|CL|Chile
57|CO|Kolumbia
58|VE|Venezuela
60|MY|Malajzia
61|AU|Ausztrália
62|ID|Indonézia
63|PH|Fülöp-szigetek
64|NZ|Új-Zéland
65|SG|Szingapúr
66|TH|Thaiföld
81|JP|Japán
82|KR|Dél-Korea
84|VN|Vietnám
86|CN|Kína
90|TR|Törökország
91|IN|India
92|PK|Pakisztán
93|AF|Afganisztán
94|LK|Srí Lanka
95|MM|Mianmar
98|IR|Irán
212|MA|Marokkó
213|DZ|Algéria
216|TN|Tunézia
218|LY|Líbia
220|GM|Gambia
221|SN|Szenegál
233|GH|Ghána
234|NG|Nigéria
249|SD|Szudán
251|ET|Etiópia
254|KE|Kenya
255|TZ|Tanzánia
256|UG|Uganda
260|ZM|Zambia
263|ZW|Zimbabwe
264|NA|Namíbia
265|MW|Malawi
267|BW|Botswana
297|AW|Aruba
298|FO|Feröer
299|GL|Grönland
350|GI|Gibraltár
351|PT|Portugália
352|LU|Luxemburg
353|IE|Írország
354|IS|Izland
355|AL|Albánia
356|MT|Málta
357|CY|Ciprus
358|FI|Finnország
359|BG|Bulgária
370|LT|Litvánia
371|LV|Lettország
372|EE|Észtország
373|MD|Moldova
374|AM|Örményország
375|BY|Fehéroroszország
376|AD|Andorra
377|MC|Monaco
378|SM|San Marino
380|UA|Ukrajna
381|RS|Szerbia
382|ME|Montenegró
383|XK|Koszovó
385|HR|Horvátország
386|SI|Szlovénia
387|BA|Bosznia-Hercegovina
389|MK|Észak-Macedónia
420|CZ|Csehország
421|SK|Szlovákia
423|LI|Liechtenstein
501|BZ|Belize
502|GT|Guatemala
503|SV|Salvador
504|HN|Honduras
505|NI|Nicaragua
506|CR|Costa Rica
507|PA|Panama
509|HT|Haiti
591|BO|Bolívia
593|EC|Ecuador
595|PY|Paraguay
598|UY|Uruguay
673|BN|Brunei
675|PG|Pápua Új-Guinea
679|FJ|Fidzsi
850|KP|Észak-Korea
852|HK|Hongkong
853|MO|Makaó
855|KH|Kambodzsa
856|LA|Laosz
880|BD|Banglades
886|TW|Tajvan
960|MV|Maldív-szigetek
961|LB|Libanon
962|JO|Jordánia
963|SY|Szíria
964|IQ|Irak
965|KW|Kuvait
966|SA|Szaúd-Arábia
967|YE|Jemen
968|OM|Omán
971|AE|Egyesült Arab Emírségek
972|IL|Izrael
973|BH|Bahrein
974|QA|Katar
975|BT|Bhután
976|MN|Mongólia
977|NP|Nepál
992|TJ|Tádzsikisztán
993|TM|Türkmenisztán
994|AZ|Azerbajdzsán
995|GE|Grúzia
996|KG|Kirgizisztán
998|UZ|Üzbegisztán
"""

    /** Hossz szerint csökkenő sorrend – így mindig a leghosszabb előhívó illeszkedik. */
    private val table: List<Country> by lazy {
        TABLE.trim().lines()
            .mapNotNull { line ->
                val parts = line.split('|')
                if (parts.size != 3) null
                else Country(parts[0].trim(), parts[1].trim(), parts[2].trim())
            }
            .sortedByDescending { it.dialCode.length }
    }

    /**
     * A készülék "saját" országa – a hálózatról, a SIM-ről, végül a nyelvi
     * beállításból. Erre azért kell, hogy a nemzeti formátumban beírt szám
     * (pl. `06 1 234 5678`) is megkapja az országnevet.
     */
    fun homeCountry(context: Context): Country? {
        val iso = runCatching {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val network = tm?.networkCountryIso?.takeIf { it.isNotBlank() }
            val sim = tm?.simCountryIso?.takeIf { it.isNotBlank() }
            network ?: sim
        }.getOrNull() ?: Locale.getDefault().country

        return byIso(iso) ?: byIso("HU")
    }

    fun byIso(iso: String?): Country? {
        if (iso.isNullOrBlank()) return null
        return table.firstOrNull { it.iso.equals(iso, ignoreCase = true) }
    }

    /**
     * Ország felismerése a beírt számból.
     *
     *  • `+36…` vagy `0036…`  -> nemzetközi előhívó alapján
     *  • bármi más            -> a készülék saját országa
     */
    fun detect(raw: String, home: Country?): Country? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val international = when {
            trimmed.startsWith("+") -> trimmed.removePrefix("+")
            trimmed.startsWith("00") -> trimmed.substring(2)
            else -> null
        }

        if (international != null) {
            val digits = international.takeWhile { it.isDigit() }
            return table.firstOrNull { digits.startsWith(it.dialCode) }
        }

        // Nemzeti formátum: csak akkor van értelme, ha már van legalább egy számjegy.
        return if (trimmed.any { it.isDigit() }) home else null
    }

    /** Az ország zászló-emojija az ISO kódból (két regionális indikátor betű). */
    fun flagOf(iso: String): String {
        if (iso.length != 2) return ""
        val base = 0x1F1E6
        val first = base + (iso[0].uppercaseChar() - 'A')
        val second = base + (iso[1].uppercaseChar() - 'A')
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String(Character.toChars(first)) + String(Character.toChars(second))
        } else ""
    }
}
