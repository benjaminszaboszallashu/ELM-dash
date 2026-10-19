# Helyfüggő Útinform Android Autóban – megvalósíthatósági kutatás

Dátum: 2026-10-19. Ez kutatási javaslat, nem implementált funkció. Az alkalmazás viselkedése nem változott. Cél: telefonról vetített Android Auto, Samsung telefon, Carpuride W113; nem Android Automotive OS.

## Összefoglaló

Megvalósítható a hivatalos NAP Útinform-adatfolyam és a telefon helyadata alapján közeli, releváns közúti események kiválasztása. A jelenlegi ELM Dash esetében a legkisebb fejlesztés a médiafelületen megjelenő rövid közlemény. Ez továbbra is kísérleti dashboard-integráció; nem szabad garantálni a Play-elfogadást, a kártya láthatóságát vagy az 1/3-os elrendezést. Saját navigáció nélkül nem ismerjük automatikusan a Waze/Google Maps teljes aktív útvonalát.

## Ellenőrzött adatforrás

A NAP éles, nyilvános katalógusában az „Útinform datex” adatcsomag profilazonosítója 26. Szolgáltató: Magyar Közút Nonprofit Zrt.; több közútkezelő adatai szerepelnek benne. A profil országos lefedettséget, DATEX II 2.3 / XML / HTTP formátumot és **5 perces frissítést** jelöl. Kategóriái között útépítések, váratlan események, ideiglenes forgalmi rend és közlekedésbiztonsági információk vannak. Külön Pest vármegyei csomag is található (97). A kategória/lefedettség nem garantál minden utcára teljes, azonnali információt.

- [Országos adatcsomag](https://napportal.kozut.hu/#/configuration/data-provider/profile/26)
- [Pest vármegyei adatcsomag](https://napportal.kozut.hu/#/configuration/data-provider/profile/97)
- [Hivatalos használati útmutató letöltése](https://napportal.kozut.hu/#/download/useful-download)

A 2023-12-14-i útmutató 17–18. oldala szerint előfizetéskor HTTP/SOAP és Pull/Push mód választható. Pull esetén a rendszer hozzáférési linket generál; Push esetén saját fogadó végpont kell. Az útmutató további formátumválasztásokat is felsorol, de a konkrét feed nyilvános profilján a 2.3/XML formátum igazolt. A profil adatfeltöltési „Pull” mezője önmagában a szolgáltatói oldalra vonatkozik, nem bizonyítja a fogyasztói interfészt.

A [felhasználói ÁSZF v4](https://napportal.kozut.hu/napp-portal-proxy/FileStorage/documents/nap_aszf_felhasznalo_2024-hu9effc1db-23d9-4447-b2fb-a5dfa0bb6fa9.pdf) regisztrációt és hivatalos hozzáférést ír elő. A 13. pont a jogszabályban meghatározott adatok ingyenességéről rendelkezik; a 32. pont kifejezetten engedi az adatok kezelését, átalakítását, szoftverbe integrálását és továbbadását, kereskedelmi célra is. Ez nem jelent korlátlan SLA-t: a 23–24. pont nem garantál teljes, hibátlan, mindig friss szolgáltatást. Az egyedi adatcsomag feltételeit az előfizetéskor is ellenőrizni kell.

**Nem ellenőrzött:** tényleges előfizetési URL, hitelesítés, lekérési limit, válaszméret, konkrét eseményminta és koordináta-reprezentáció. Nem történt regisztráció vagy előfizetés. A katalógus metaadatának régi dátuma nem az események frissességét jelenti.

## Android Auto megjelenítés

| Megoldás | Értékelés |
| --- | --- |
| Meglévő MediaSession felirat és borító | Gyors kísérleti út. A host rajzol, méretez és vághat; másik kiválasztott médialejátszó mellett nem garantált az ELM-kártya. |
| Car App Library POI alkalmazás | Térképes megjelenítés lehetséges ebben a kategóriában, de egy tisztán forgalmi alkalmazás kategória- és minőségi megfelelése külön tisztázandó. Nem automatikus jóváhagyás. |
| Valódi navigációs alkalmazás | Navigációs sablonok és az aktív navigációhoz tartozó figyelmeztetések használhatók, de tényleges útvonalvezetést igényel; lényegesen nagyobb projekt. |
| Tetszőleges felugró üzenet Waze fölött | Általános, garantált lehetőségként nem áll rendelkezésre. Egy telefonos értesítés nem válik automatikusan AA-felugróvá. |

Források: [Car App Library](https://developer.android.com/training/cars/apps/library), [autós minőségi követelmények](https://developer.android.com/docs/quality-guidelines/car-app-quality), [navigációs alkalmazások](https://developer.android.com/training/cars/apps/navigation), [CarAppExtender](https://developer.android.com/reference/androidx/car/app/notification/CarAppExtender), [POI alkalmazások](https://developer.android.com/training/cars/apps/poi).

A navigációs értesítési API nem szabad overlay: a kanyarodási értesítések az aktív navigációhoz kötöttek. Nem javasolt üzenetküldőnek vagy navigációnak álcázni a funkciót. A média-metaadat változása AA-értesítést is kiválthat, ezért a frissítés ütemezését a fejegységen ellenőrizni kell: [média figyelemelterelési szabályok](https://developer.android.com/training/cars/media/distraction-safeguards).

Javasolt felület: fogyasztás fő sora marad; az alsó OBD/rádió sor helyére ideiglenesen egyetlen releváns esemény kerül. Kitalált példa: „M3 · baleset kb. 4 km · Útinform”. A borítón csak rövid, nagy felirat legyen; ne hagyatkozzunk az alsó területre, amelyet a host vezérlői elfednek. Javasolt 15–20 s megjelenítés, eseményenként deduplikálás, villogás nélkül. Ezek tesztelendő termékparaméterek.

Hangos összefoglaló külön, felhasználói bekapcsolással vizsgálható; hangfókuszt, navigációt, hívást és a kézi rádiószünetet tiszteletben kell tartania. A hangos funkció önmagában nem teszi hivatalosan támogatottá a dashboardot.

## Helyfüggőség és útvonal

A GPS közelség önmagában kevés: párhuzamos út, felüljáró, ellenkező menetirány és a már elhagyott esemény kiszűrése szükséges. Útszám, haladási irány, pontosság, útszakasz-geometria és az esemény időbeli érvényessége együtt értékelendő. Bizonytalan esetben „a közelben”, ne „előtted” szerepeljen. Útszakasz esetén ne csak egy reprezentatív ponttól mért távolságot használjunk.

A DATEX adat lehet koordináta, vonal, terület vagy például Alert-C/TMC hivatkozás. A TMC kódot a megfelelő helyazonosító-tábla és verzió nélkül nem lehet GPS-koordinátaként kezelni. A konkrét magyar payloadot kell először megvizsgálni: [DATEX helyhivatkozások](https://docs.datex2.eu/v3.1/level2user/LocationReferencing.html), [Alert-C](https://docs.datex2.eu/levels/mastering/location/alertc/).

A jelenlegi RouteGps percenként kér pozíciót, útvonalrögzítéshez és járó motorhoz kötve. Egy perc alatt 90 km/h-val 1,5 km, 130 km/h-val kb. 2,17 km tehető meg. Forgalmi figyeléshez külön, opcionális helyhasználat indokolt, az útvonalmentéstől és OBD-kapcsolattól függetlenül. Javasolt induló ütemezés: mozgás közben 2–5 s helyfrissítés, helyi relevanciaszámítás 5–10 s, hálózati adatfrissítés kb. 5 perc, a szolgáltató korlátai szerint. Ezek nem garantált Android időzítések.

A helyszolgáltatásnak kezelnie kell az Android háttérindítási és foreground-service korlátait; a háttérhely-engedély önmagában nem mindenre felmentés. [Android FGS korlátozások](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start). WorkManager nem való másodperces helyfigyelésre vagy pontos ötperces aktív pollra.

**Waze/Maps:** nincs a jelenlegi integrációban hozzáférés az aktív útvonalhoz. A Google új [Navigation Connect](https://developers.google.com/maps/documentation/navigation/connect/overview) útvonalmegosztást kínál vállalati háttérrendszerrel és felhasználói hozzájárulással, de dokumentációja kifejezetten kizárja az Android Auto / CarPlay támogatást. A [Waze Transport SDK](https://developers.google.com/waze/intro-transport) támogatása 2026-11-01-én megszűnik; új fejlesztést nem célszerű rá alapozni. Saját tervezett útvonal sem garantáltan egyezik a Waze újratervezéseivel.

## Javasolt modulok és adatkezelés

1. `traffic-source`: NAP letöltés, időkorlátok, gyorsítótár, XML méretkorlát és külső entitások tiltása. Hibánál visszalépés, nem folyamatos újrapróbálás.
2. `traffic-model`: eseményazonosító/verzió, típus, útszakasz, irány, érvényesség, forrás és frissítési idő. Csak ténylegesen rendelkezésre álló mezők.
3. `traffic-relevance`: hely, pontosság, haladás, geometriák, prioritás és deduplikálás. Sebességfüggő előretekintési tartomány, álló autónál stabil kijelzés.
4. `traffic-presentation`: telefonos lista/beállítások és a meglévő AA média-metaadat kapcsolódási pontja; fogyasztás és OBD működése maradjon független.

Személyes MVP-nél közvetlen telefonos NAP Pull lehet a legegyszerűbb, ha a válaszméret és a hozzáférési feltételek megfelelőek. Nyilvánosan terjesztett appnál opcionális backend töltheti és alakíthatja kompakt JSON-ra a közös feedet. Előfizetési titkot ne égessünk APK-ba vagy Gitbe. A backend nem igényel nyers OBD-adatot vagy pontos helyet; országos/regionális gyorsítótárból a telefon helyben szűrhet. A forgalmi helyhasználat nem jelent automatikus útvonalnaplózási vagy WebDAV-feltöltési hozzájárulást.

Sikertelen vagy elavult lekéréskor „nincs friss közlekedési adat”, nem „nincs akadály”. Ötperces forrásból nem ígérhető másodperces balesetészlelés. Érvénytelen, törölt, duplikált és időközben feloldott eseményeket kezelni kell; az esemény állapotváltozása fontosabb lehet a távolságnál.

## Alternatíva

A [TomTom Traffic Incident Details](https://docs.tomtom.com/traffic-api/documentation/tomtom-maps/v1/traffic-incidents/incident-details) földrajzi tartomány alapján lekérhető eseményeket ad; a [lefedettségi táblázat](https://docs.tomtom.com/traffic-api/documentation/tomtom-maps/v1/product-information/market-coverage) Magyarországot is tartalmazza. Új integrációhoz a dokumentáció az Orbis API-k felé irányít. Kvóta, ár és licenc külön ellenőrzendő; ez nem azonos a hitelesen Útinformként megjelölt forrással. Az OSM térkép önmagában nem szolgáltat élő forgalmi eseményeket.

## Következő megvalósítási lépések és elfogadási feltételek

1. NAP regisztráció/előfizetés után tényleges minta letöltése: hitelesítés, méret, frissesség, törlések és helyreferenciák ellenőrzése. Ez az első technikai döntési kapu.
2. Anonim, kitalált helyzetekkel parser és relevanciateszt: párhuzamos út, ellenkező irány, pontatlan GPS, elhagyott esemény, lezárás, lejárt adat, hálózatvesztés. Valódi felhasználói út nem kerülhet Gitbe.
3. A meglévő `mediaDebug` változatban egy rövid forgalmi sor prototípusa. DHU + Carpuride ellenőrzés: teljes és 1/3 nézet, rádió szünet/lejátszás, másik médiaapp, Waze aktív, értesítésgyakoriság, levágott feliratok.
4. Külön opt-in helyfigyelés és beállítások; az OBD, rádió, útrögzítés és GPS szolgáltatások életciklusának regressziós ellenőrzése.
5. Valódi útpróba után küszöbök és jelzésgyakoriság finomítása. Hivatalos Play-terjesztéshez külön kategória-/szabálymegfelelési felülvizsgálat.

Kutatáskor nem történt feed-előfizetés, telefonos vagy fejegységes próba, forgalmi funkció implementálása, telepítés vagy push.
