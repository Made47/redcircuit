# RedCircuit (NeoForge 1.21.1)

*[English version: README.md](README.md)*

> **Stworzone z pomocą AI.** Mod powstał przy pomocy sztucznej inteligencji (Claude od Anthropic): kod źródłowy,
> skrypty generujące tekstury, tłumaczenia i dokumentacja zostały w dużej mierze napisane przez AI pod kierunkiem autora,
> który zaprojektował, sprawdzał i testował mod w grze.


Przewody redstone bez strat sygnału, bramki logiczne, rury pneumatyczne z sortowaniem i energia Azurity. Cały kod i grafiki są autorskie.
Zawiera kable redstone, bramki logiczne, rury z sortowaniem i Azurity.

## Budowanie `.jar`

### Opcja A – na swoim komputerze (Windows)
1. Zainstaluj **JDK 21**, np. Temurin: https://adoptium.net (zaznacz „Set JAVA_HOME”).
2. Rozpakuj projekt, otwórz w tym folderze terminal (PowerShell) i uruchom:
   ```
   .\gradlew.bat build
   ```
   Pierwsze budowanie trwa kilka minut, bo pobiera Minecrafta i NeoForge.
3. Gotowy plik: `build\libs\redcircuit-0.1.0.jar` → wrzuć do `mods/` (NeoForge 21.1.x dla MC 1.21.1).

Do testów w grze bez instalowania: `.\gradlew.bat runClient`.

### Opcja B – GitHub Actions (bez instalowania niczego)
Wrzuć projekt do nowego repozytorium na GitHubie. Workflow `.github/workflows/build.yml`
zbuduje mod przy każdym pushu, a `.jar` pojawi się w zakładce **Actions → (ostatni run) → Artifacts**.

## Zawartość

### Rudy
Każda ruda ma też wersję głębinową (deepslate) i generuje się w Overworldzie. Działa szczęście i jedwabny dotyk.

| Ruda | Wysokość Y | Kilof | Daje |
|---|---|---|---|
| Cyna | 0 – 80 | kamienny | surowa cyna → sztabka cyny |
| Srebro | -48 – 32 | żelazny | surowe srebro → sztabka srebra |
| Azuryt | -64 – 16 | żelazny | 4–5 azurytu (jak redstone) |

Surowce i rudy przetapia się w piecu lub piecu hutniczym. **Stop energetyczny** robi się tak: sztabka srebra + 4 azuryt, wyłącznie w piecu do stopów.
Przedmioty mają tagi `c:` (np. `c:ingots/tin`), więc działają w recepturach innych modów.

### Stół kreślarski i plany
- **Stół kreślarski** – stół rzemieślniczy, który pamięta siatkę 3×3 (przedmioty zostają po zamknięciu) i ma 18 slotów magazynu. Przy wytwarzaniu składniki są brane **najpierw z magazynu**, więc siatka działa jak wzór i można craftować wielokrotnie. Shift+klik na wynik wytwarza do 64 razy.
- **Pusty plan** (papier + niebieski barwnik) – włóż go do slotu po lewej, ułóż recepturę i kliknij przycisk ✎, żeby zapisać **plan**. Plan pokazuje w opisie wynik i składniki.
- Zapisany plan w slocie + przycisk ✎ = wczytanie planu: puste pola siatki uzupełniają się przedmiotami z magazynu.

### Piece do stopów
- **Piec do stopów** (8 bloków cegieł w pierścieniu) – 9 slotów na składniki, slot paliwa (jak zwykły piec) i wynik. Składniki mogą leżeć w dowolnych slotach.
- **Elektryczny piec do stopów** – to samo, ale na energię (25 FE/t) i 2× szybciej. Receptura: 5 sztabek stopu energetycznego, piec do stopów, 2 sztabki żelaza i przewód ze stopu energetycznego.
- Stopy: **miedź + 4 redstone → stop sygnałowy**, **srebro + 4 azuryt → stop energetyczny**, **3 miedzi + 1 cyna → 4 sztabki mosiądzu**, **żelazo + 2 węgle (lub węgle drzewne) → stal** (2× dłużej).
- Automatyzacja: góra – składniki, boki – paliwo (w wersji elektrycznej też składniki), dół – odbiór wyniku.
- Własne receptury dodasz w datapacku, typ `redcircuit:alloying` (przykład w `data/redcircuit/recipe/alloying_signal_alloy_ingot.json`).

### Piły ręczne
- Kamienna, miedziana, złota, żelazna, srebrna i diamentowa: **2 sztuki materiału + patyk w rzędzie**. Netherytowa: ulepszenie diamentowej na stole kowalskim.
- Piła zostaje w siatce po wytworzeniu i traci 1 punkt wytrzymałości (naprawisz ją w kowadle materiałem, z którego jest).
- Piły są składnikiem **tartaku**.

### Zębatki
- **Kamienna:** 4 bruk/czarnokamień w kształcie plusa (pusty środek).
- **Miedziana, złota, żelazna, mosiężna, srebrna, diamentowa:** 4 sztuki materiału wokół kamiennej zębatki.
- **Netherytowa:** ulepszenie diamentowej na stole kowalskim.
- Tagi `c:gears` / `c:gears/<materiał>` – działają w recepturach innych modów.

### Obwody i obudowy maszyn energetycznej
- **Obwód energetyczny** (dawniej silnik): **żelazo w narożnikach, stop energetyczny u góry, 2 cewki miedziane po bokach, żelazna zębatka w środku, przewód ze stopu energetycznego na dole**. Składnik narzędzi elektrycznych (wiertarka, piła łańcuchowa) i obudowy maszyny energetycznej.
- **Obudowa maszyny energetycznej** (blok): żelazo w narożnikach, 4 sztabki mosiądzu, obwód energetyczny w środku. Jest korpusem maszyn na energię – zastępuje obwód w recepturach: pieca elektrycznego, elektrycznego pieca do stopów, generatora (wirnikowego), pompy, Requestera, akceleratora, maceratora i tartaka (w tym samym miejscu siatki, w którym był silnik).
- **Obwód sygnałowy**: żelazo w narożnikach, stop sygnałowy u góry, 2 pochodnie redstone i płytka obwodu w środkowym rzędzie, przewód ze stopu sygnałowego na dole.
- **Obudowa maszyny sygnałowej** (blok): żelazo w narożnikach, 4 sztabki cyny, obwód sygnałowy w środku. Korpus maszyn sterowanych redstone – zastępuje przewód sygnałowy w recepturach: ekstraktora (a więc i filtra oraz maszyny sortującej), regulatora, relay, wyrzutnika, detektora przedmiotów, zapalarki, aplikatora i niszczarki bloków, a w asemblerze płytkę obwodu.

### Bateria
- Przenośny magazyn Azurity: **50 000 FE**, pasek ładunku pod ikoną i stan w opisie.
- **PPM na akumulatorze** ładuje baterię z akumulatora, **Shift + PPM** przelewa jej energię do akumulatora. W ten sam sposób akumulator ładuje przedmioty FE z innych modów.
- **Slot baterii w urządzeniach**: piec elektryczny (w miejscu slotu paliwa) i elektryczny piec do stopów (pod paskiem energii). Bateria zasila urządzenie, gdy brakuje mu energii z sieci (do 400/500 FE/t).
- **Okno akumulatora** (PPM): górny slot ładuje baterię z akumulatora, dolny rozładowuje ją do akumulatora (1000 FE/t), pasek i licznik energii. Automatyzacja: od góry wkładanie do ładowania, z boków do rozładowania, od dołu wyjmowanie naładowanych/rozładowanych baterii.
- Bateria ma zdolność FE dla przedmiotów, więc działa też z ładowarkami innych modów (do 1000 FE/t).
- W zakładce kreatywnej jest wersja pusta i naładowana. Receptura: miedź u góry, 4 sztabki cyny po bokach, 2 azuryty w środkowej kolumnie.

### Ładowarka, narzędzia elektryczne i miernik
- **Ładowarka** – 4 sloty, ładuje baterie, narzędzia elektryczne i przedmioty FE innych modów (1000 FE/t na slot, bufor 50 000 FE). Okno z paskiem energii; lejki wkładają z góry/boków, od dołu wyjmują naładowane. Świeci podczas ładowania.
- **Wiertarka elektryczna** – kilof + łopata (poziom diamentu, prędkość 10), **50 FE za blok** zamiast wytrzymałości (100 000 FE). Bez energii kopie bardzo wolno. Można ją zaklinać (wydajność, szczęście, jedwabny dotyk). **Tryby 1x1 / 3x3** – Shift + PPM przełącza; w trybie 3x3 rozbija 3×3 bloki w płaszczyźnie ściany, w którą celujesz (wolniej, 50 FE za każdy blok, pomija bloki dużo twardsze od środkowego); trzymanie Shift podczas kopania rozbija tylko jeden blok.
- **Piła łańcuchowa** – siekiera tnąca też liście, wełnę i pajęczyny, mocny atak; 50 FE za blok.
- **Miernik** – PPM na bloku wypisuje w czacie: sygnał (wejście/wyjście albo poziom przewodu), kanały kabla wiązkowego, energię (FE) i ciecze.
- Receptury: ładowarka = żelazo, stop energetyczny, bateria, przewód energetyczny; wiertarka = 3 diamentowe kilofy u góry, żelazo, obwód energetyczny, bateria; piła łańcuchowa = 3 diamentowe siekiery u góry, żelazo, obwód energetyczny, bateria; miernik = komparator + oba przewody + szyba + żelazo.

### Generatory Azurity
- **Termostos (Thermopile)** – każda para sąsiadów „gorący” (lawa, ogień, blok magmy, zapalone ognisko) + „zimny” (woda, lód, blok śniegu, sypki śnieg) daje **8 FE/t** (maks. 3 pary = 24 FE/t). Receptura: miedź w narożnikach, cyna na bokach, przewód ze stopu energetycznego w środku.
- **Generator na paliwo stałe** – spala wszystko, co pali się w piecu (węgiel, drewno, wiadro lawy…), i podczas spalania daje **20 FE/t** (węgiel = 32 000 FE, wiadro lawy = 400 000 FE). Bufor 20 000 FE, oddaje energię do sąsiadów (400 FE/t na stronę). Paliwo zużywa tylko, gdy jest miejsce w buforze; sygnał redstone go wstrzymuje. Okno: slot paliwa, płomień z pozostałym czasem, pasek energii i bieżąca produkcja. Lejki: paliwo z góry/boków, puste wiadra od dołu. Komparator czyta naładowanie. Receptura: żelazo (góra i narożniki), 2 cewki miedziane po bokach, piec w środku, przewód ze stopu energetycznego na dole. Moc w konfiguracji: `[generators] solid_fuel_generator`.
- **Generator** – sam nic nie produkuje; z przodu montuje się wirnik (PPM wirnikiem, Shift+PPM pustą ręką zdejmuje):
  - **Wiatrak** (wełna, patyki, żelazna zębatka) – obraca się w polu 5×5 przed generatorem, które musi być puste i pod gołym niebem. **6 FE/t nad poziomem morza, do 32 FE/t wysoko**, ×1,5 w deszczu, ×2 w burzy.
  - **Koło wodne** (deski, patyki, żelazna zębatka) – pole 3×3 przed generatorem: każdy blok **płynącej wody 4 FE/t**, stojącej 1 FE/t (maks. 24 FE/t). Inne bloki w tym polu blokują koło.
- PPM na generatorze pokazuje bieżącą produkcję i bufor.

### Pompa
- Stawia się ją, **klikając na źródło płynu** (zbiornik innego moda albo jezioro/staw): zasysa z tyłu, tłoczy do przodu.
- Z jeziora wypompowuje bloki źródłowe (do 256 bloków połączonego płynu, od góry). Z przodu oddaje płyn do zbiornika/rury innego moda, a gdy jest tam pusto – wylewa blok płynu.
- Zbiornik 8000 mB, **200 FE na wiadro** (1000 mB), wiadro co pół sekundy. Sygnał redstone zatrzymuje pompę. PPM pokazuje zawartość.
- Receptura: żelazo w narożnikach, wiadro na górze, 2 mosiężne zębatki, obudowa maszyny energetycznej w środku, przewód ze stopu energetycznego na dole.

### Rura do płynów
- Mosiężna rura łącząca się z pompą, innymi rurami i każdym zbiornikiem/maszyną obsługującą płyny (także z innych modów).
- Każda rura mieści 1000 mB. Rury wyrównują poziom między sobą, a płyn wypychają do podłączonych zbiorników – z pominięciem strony, z której przyszedł (nie cofa się do źródła). Do 250 mB/t na stronę.
- PPM pokazuje zawartość. Receptura: mosiądz + szkło + mosiądz → 8 rur.

### Śrubokręt
- PPM na bloku **obraca go**: bramki logiczne, maszyny, piece, generatory, pompy, ekstraktory oraz bloki vanilla z kierunkiem (przekaźniki, obserwatory, leje, kłody…). **Shift + PPM** obraca w drugą stronę.
- Działa przed interakcją z blokiem – nie otwiera GUI i nie zmienia czasu timera. Nie obraca łóżek, drzwi, podwójnych skrzyń ani wysuniętych tłoków.
- **Shift + PPM na bramce logicznej** zbiera ją do ekwipunku razem z ustawieniami (czas zegara/sekwencera/komórki stanu, strona wyjścia komórki stanu, maksimum licznika, wyciszenie). Postawiona ponownie ma te same ustawienia. Bramka z domyślnymi ustawieniami stackuje się ze zwykłymi.
- Nie zużywa się (brak wytrzymałości). Receptura: sztabka żelaza nad patykiem (po skosie).

### Nożyce do drutu (Wire Cutters)
- Narzędzie do wytwarzania – jak piła zostają w siatce i tracą 1 punkt wytrzymałości (256, widoczne w opisie). Naprawa sztabką stali.
- Receptura: 3 sztabki stali w kształcie litery X (dwie u góry, jedna w środku) i 2 czerwone barwniki na dole (rączki).
- Sztabka miedzi + nożyce do drutu → **cienki drut miedziany**; sztabka żelaza + nożyce do drutu → **cienki drut żelazny**.
- **Cewka miedziana**: 4 cienkie druty miedziane na krzyż wokół sztabki żelaza. Składnik obwodu energetycznego.

### Rura restrykcyjna i Akcelerator
- **Rura restrykcyjna** – zwykła rura, ale przy wyznaczaniu trasy liczy się jak bardzo długa (koszt 1000 zamiast 1), więc przedmioty jadą nią tylko wtedy, gdy nie ma innej drogi. Trasa jest teraz liczona jako najtańsza (algorytm Dijkstry), nie tylko najkrótsza. Receptura: rura + sztabka żelaza; sama w siatce oddaje rurę. Można ją malować jak zwykłą rurę.
- **Akcelerator** – odcinek rury zasilany energią (przewód energetyczny). Każdy przejeżdżający stos przyspiesza do **4×** (0,5 bloku/tick), a potem zwalnia o 0,02 na każdej kolejnej rurze. Koszt: 20 FE za stos (`accelerator_per_item` w konfiguracji). PPM pokazuje energię, Shift + PPM pustą ręką zmywa farbę. Receptura: żelazo w narożnikach, rury góra/dół, przewody energetyczne po bokach, obwód energetyczny w środku.

### Requester
- Stawia się go, **klikając na rurę**: tył jest podłączony do sieci rur, przód to wyjście (inwentarz, kolejna rura albo ziemia).
- Na **impuls redstone** przeszukuje wszystkie inwentarze podłączone do tej sieci, wyciąga przedmiot pasujący do filtra i wysyła go rurami **do siebie** – przedmiot jedzie tylko do tego Requestera, który go zamówił, a potem wychodzi z przodu.
- Filtr: 9 slotów (PPM), używanych po kolei; liczba sztuk w slocie = ile pobrać. Pusty filtr = cały stos dowolnego przedmiotu.
- Zasilanie: **200 FE za pobranie** (bufor 10 000 FE), Shift + PPM pokazuje energię. Receptura: żelazo w narożnikach, filtr u góry, 2 sztabki złota, obudowa maszyny energetycznej w środku, przewód ze stopu energetycznego na dole.

### Maszyny logistyczne
| Blok | Działanie |
|---|---|
| **Manager** | Stawiany na pilnowanym inwentarzu, przód w sieci rur. Filtr = lista przedmiotów z ilościami: brakujące zamawia z sieci, przedmioty spoza listy odsyła do sieci. 100 FE za zamówienie. |
| **Regulator** | Przenosi z inwentarza z tyłu do inwentarza z przodu, aż ten zawiera ilości z filtra; gdy wszystko jest uzupełnione – sygnał redstone. |
| **Relay** (Przekaźnik przedmiotów) | 9 slotów, przyjmuje z każdej strony poza przodem i stale wypycha przodem. |
| **Item Detector** (Detektor przedmiotów) | W linii rur (tył → przód); każdy przejeżdżający stos daje impuls redstone. |
| **Igniter** (Zapalarka) | Zasilony – zapala i podtrzymuje ogień przed sobą; bez sygnału gasi. |
| **Assembler** (Asembler) | Kabel wiązkowy: kanał N włączony → stawia blok ze slotu N, wyłączony → zbiera go z powrotem. Zwykły impuls: stawia kolejny / zbiera. |
| **Ejector** (Wyrzutnik) | 9 slotów; impuls redstone wypycha przodem jeden stos (sloty po kolei). |
| **Deployer** (Aplikator) | 9 slotów; impuls redstone = PPM przedmiotem na blok przed sobą (stawianie, mączka kostna, wiadra, nożyce…). |
| **Buffer** (Magazyn buforowy) | 18 slotów dostępnych z każdej strony; komparator czyta zapełnienie. |
| **Block Breaker** (Niszczarka bloków) | Impuls redstone rozbija blok przed sobą (jak diamentowy kilof), dropy wychodzą tyłem. |

Maszyny kierunkowe stawia się tak jak ekstraktor – klikając blok, który ma być „z tyłu”; śrubokręt je obraca.

### Modpacki
**Plik konfiguracji** `config/redcircuit-common.toml` (tworzy się przy pierwszym uruchomieniu):
- `[worldgen]` – `generate_tin`, `generate_silver`, `generate_azurite` – wyłączenie rudy (np. gdy cynę daje już inny mod). Zmiana działa po restarcie świata/serwera.
- `[generators]` – FE/t: generator na paliwo stałe, panel słoneczny, termostos (za parę), wiatrak (minimum/maksimum), koło wodne (płynąca/stojąca woda, maksimum).
- `[machines]` – zużycie: piec elektryczny, elektryczny piec do stopów (FE/t), pompa (FE/wiadro), Requester i Manager (FE za operację).

**Wspólne tagi** – receptury używają tagów `c:` zamiast konkretnych przedmiotów (`#c:ingots/tin`, `#c:ingots/silver`, `#c:ingots/copper`, `#c:dusts/redstone`, `#c:gems/diamond`, `#c:glass_blocks`, `#c:chests/wooden`, `#c:dyes/...` itd.), więc cyna, srebro czy mosiądz z innych modów (Mekanism, Thermal, Create…) działają w recepturach i w piecach do stopów. Mod udostępnia też własne tagi: `c:ingots/signal_alloy`, `c:ingots/power_alloy`, `c:dusts/azurite`, `c:gears/*`, `c:wires/copper|iron`, a także rudy, surowce, bryłki i bloki.

**KubeJS / CraftTweaker** – receptury pieca do stopów to zwykły typ `redcircuit:alloying` (JSON w datapacku), więc można je dodawać i usuwać skryptami. Gotowe przykłady są w katalogu `examples/`:
- `examples/kubejs/server_scripts/redcircuit_alloying.js` – `event.custom({type: 'redcircuit:alloying', ...})`, `event.remove(...)`,
- `examples/crafttweaker/scripts/redcircuit_alloying.zs` – `<recipetype:redcircuit:alloying>.addJsonRecipe(...)`, `removeByName(...)`.

Format receptury:
```json
{
  "type": "redcircuit:alloying",
  "ingredients": [ {"tag": "c:ingots/silver"}, {"tag": "c:dusts/azurite", "count": 4} ],
  "result": {"id": "redcircuit:power_alloy_ingot", "count": 1},
  "cooktime": 200
}
```

### Integracja z Jade (opcjonalna)
Z zainstalowanym **Jade** podpowiedź po najechaniu na blok pokazuje dodatkowo:
- przewody: poziom sygnału; kabel wiązkowy: aktywne kanały (kolorowe kwadraty z poziomem),
- bramki: stan wyjścia, ustawiony czas (zegar, sekwencer, komórka stanu), stan licznika, wyciszenie dźwięku,
- lampy: świeci / zgaszona, odwrócona,
- generatory: bieżąca produkcja FE/t; rury: liczba przedmiotów w środku; piece: postęp i pozostałe paliwo.
Energia (FE), płyny i zawartość inwentarzy są pokazywane przez Jade automatycznie. Każdą informację można wyłączyć w ustawieniach Jade (wtyczka RedCircuit). Do kompilacji Gradle pobiera Jade z repozytorium Modrinth (tylko `compileOnly`).

### Integracja z CB Multipart (opcjonalna)
Gdy zainstalowany jest **CB Multipart** (z CodeChickenLib, i np. CB Microblocks), przewody przyklejane do ścian
(stop sygnałowy, izolowane, kabel wiązkowy, stop energetyczny) mogą być **częściami bloku wieloczęściowego**:
- wstawienie mikrobloku (osłony, panelu) w blok z przewodem zamienia przewody na części – przewód i osłona dzielą jeden blok,
- przewód postawiony w bloku z mikroblokami (np. obok osłony na ścianie) od razu staje się częścią,
- części łączą się z przewodami-blokami, przewodami w obudowie i między sobą (prosto, narożniki wewnętrzne i zewnętrzne), działają w tej samej sieci sygnałów/kanałów/energii.
Bez CB Multipart mod działa dokładnie jak wcześniej. Do kompilacji Gradle pobiera API z `https://maven.covers1624.net/` (tylko `compileOnly`).

### Bramki z pamięcią i czasem
| Bramka | Działanie |
|---|---|
| **Licznik** (CNT) | Lewe wejście (zbocze) +1, prawe −1. Przód świeci przy maksimum, tył przy zerze. PPM otwiera okno: maksimum (1–1024) i zerowanie. Komparator czyta stan licznika. |
| **Sekwencer** (SEQ) | Sygnał krąży po stronach: przód → prawo → tył → lewo, krok co ustawiony czas (okno ustawień, PPM). Sygnał z tyłu wstrzymuje. |
| **Komórka stanu** (SC) | Impuls z tyłu startuje odliczanie; prawa strona świeci w trakcie (w oknie ustawień można przełączyć na lewą), po czasie przód daje impuls. PPM otwiera okno ustawień czasu i strony. |
| **Synchronizator** (SYN) | Czeka na impuls z lewej i z prawej (w dowolnej kolejności), potem impuls z przodu. Tył kasuje. |

**Okno ustawień bramek** (PPM na zegarze, sekwencerze, komórce stanu lub liczniku): czas wpisywany w sekundach (0,20 s – 1 h, z przyciskami ±0,1 s / ±1 s; pokazuje też ticki), a dla licznika maksimum (1–1024, przyciski ±1 / ±10), bieżący stan i przycisk zerowania. Ustawienia są zapisywane w bramce i synchronizowane z serwerem. Zegar, sekwencer, komórka stanu i licznik **tykają** cichym kliknięciem (zdarzenie dźwiękowe `redcircuit:gate_tick`, z napisem dla niesłyszących); przycisk „Dźwięk: wł./wyciszony” w oknie wycisza konkretną bramkę.

### Redstone bezprzewodowy
Częstotliwości **1–5000**, wspólne dla całego świata (wszystkie wymiary, także niezaładowane chunki).
- **Nadajnik bezprzewodowy** (TX) – płytka z anteną; PPM ustawia częstotliwość; gdy dostaje sygnał redstone, nadaje.
- **Odbiornik bezprzewodowy** (RX) – PPM ustawia częstotliwość; daje sygnał 15 na wszystkie strony (i mocny do bloku pod sobą), gdy na jego częstotliwości nadaje nadajnik albo pilot. Końcówka anteny świeci, gdy jest aktywny.
- **Pilot bezprzewodowy** – trzymanie PPM nadaje (jak wciśnięty przycisk), Shift + PPM ustawia częstotliwość; częstotliwość widać w opisie.
- Okno częstotliwości: pole liczbowe i przyciski ±1 / ±10 / ±100. Jade pokazuje częstotliwość i stan.
- Receptury: płytka obwodu + przewód ze stopu sygnałowego + perła Endu + złoto + pochodnia redstone (nadajnik) / komparator (odbiornik); pilot = nadajnik + przycisk + żelazo.

### Lampy
- **Lampy w 16 kolorach** – świecą (poziom 15) przy sygnale redstone, także z przewodów RedCircuit obok; zapalona lampa ma jasny, „świecący” klosz.
- **Lampy odwrócone** – świecą bez sygnału, gasną przy sygnale. Lampa + pochodnia redstone zamienia zwykłą na odwróconą (i z powrotem).
- Gaszenie z opóźnieniem 4 ticków (jak lampa z gry), więc krótkie impulsy nie powodują migotania.
- Receptura: szyby po bokach, jasnopył u góry, barwnik w środku, redstone na dole.

### Zbiornik na ciecz
- Mieści **16 000 mB** (16 wiader) jednej cieczy; ciecz widać przez szklane ściany (poziom zależy od wypełnienia).
- PPM wiadrem (lub innym pojemnikiem na ciecz) nalewa/zabiera, pustą ręką pokazuje zawartość.
- Rury, pompy i maszyny innych modów łączą się z każdej strony. Ciecz sama spływa do zbiornika postawionego niżej (wieże zbiorników).
- Po rozbiciu **zachowuje zawartość** (widoczną w opisie przedmiotu). Komparator odczytuje poziom, lawa w zbiorniku świeci.
- Receptura: żelazo w narożnikach, szkło po bokach, pusty środek.

### Kable
| Blok | Opis |
|---|---|
| Przewód ze stopu sygnałowego | Przewodzi redstone **bez spadku mocy** na dowolną odległość. Łączy się z każdym przewodem izolowanym. |
| Przewód izolowany (16 kolorów) | Łączy się tylko z tym samym kolorem, z Signal Alloy i z kablem wiązkowym. |
| Kabel wiązkowy | 16 osobnych kanałów. Przewód izolowany koloru X podpięty do kabla nadaje/odbiera kanał X. |

Przewody **przyklejają się do powierzchni**: stawiasz je, klikając podłogę, ścianę lub sufit.
Łączą się prosto, w narożnikach wewnętrznych (kilka przewodów tego samego typu w jednym bloku, np. podłoga + ściana)
i zewnętrznych (zawijają się przez krawędź bloku). Niszcząc blok z kilkoma przewodami, usuwasz ten, w który celujesz.
Ograniczenie: w jednej przestrzeni bloku może leżeć tylko jeden typ/kolor przewodu, a przewody w tym samym bloku są zawsze połączone elektrycznie.

**Przewody w obudowie (Encased)** – każdy rodzaj przewodu (stop sygnałowy, 16 izolowanych, kabel wiązkowy, stop energetyczny) ma wersję w twardej obudowie, która biegnie **środkiem bloku jak rura**. Działa tak samo jak zwykły przewód tego rodzaju i łączy się zarówno z innymi przewodami w obudowie, jak i z przewodami przyklejonymi do ścian (po tej samej stronie). Czerwony/izolowany w obudowie łączy się też z urządzeniami redstone, azurytowy – z maszynami na energię. Receptura: 8 przewodów wokół płyty z gładkiego kamienia → 8 w obudowie; przewód w obudowie sam w siatce → zwykły przewód.

### Bramki logiczne
Płaskie bramki stawiane na podłodze. Wyjście = kierunek, w który patrzysz przy stawianiu.
Wejścia: **L** (lewo), **B** (tył), **R** (prawo). Przewody łączą się tylko ze stronami, które są wejściami albo wyjściami bramki.

| Bramka | Logika |
|---|---|
| NOT | !B wychodzi z przodu, z lewej i z prawej; boczne wyjścia można wyłączyć w oknie |
| Bufor | B wychodzi z przodu, z lewej i z prawej; boczne wyjścia można wyłączyć w oknie |
| AND | L i B i R; tylne wejście domyślnie wyłączone |
| NAND | nie (L i B i R); tylne wejście domyślnie wyłączone |
| OR / NOR | L lub B lub R |
| XOR / XNOR | L xor R |
| Zatrzask RS | L ustawia, R kasuje; wyjścia komplementarne: przód+lewo (stan 1) / tył+prawo (stan 0) |
| Przerzutnik T | impuls na L albo R przełącza wyjście przód ↔ tył (zawsze świeci jedno); PPM przełącza ręcznie, Shift+PPM otwiera okno |
| Formownik impulsu | zbocze na B → impuls 2 ticki |
| Zegar | tyka, gdy B jest wyłączone; impuls wychodzi z przodu, z lewej i z prawej; **PPM otwiera okno ustawień** (okres 0,20 s – 1 h) |
| Generator losowy | gdy B włączone, co 4 ticki przód, lewo i prawo dostają niezależnie losowy stan |

**PPM na dowolnej bramce** otwiera okno: opis działania, podgląd wejść i wyjść na żywo oraz opcje:
- wyłączanie pojedynczych wejść (AND, NAND, OR, NOR, XOR, XNOR, przerzutnik T) i bocznych wyjść bufora oraz NOT – tekstura traci wypustkę, przewody się od niego odłączają, a wyłączone wejście nie wpływa na wynik,
- zatrzask RS: zamiana stron (prawe ustawia, lewe kasuje) i tryb „tylko przód/tył” (boki nie świecą; oba wejścia naraz = stan bez zmian),
- komórka stanu: strona wyjścia w trakcie odliczania; czas / maksimum licznika; dźwięk tykania.
Ustawienia zostają w przedmiocie po zebraniu bramki śrubokrętem (Shift + PPM).

### Rury i sortowanie
- **Rura pneumatyczna** – przedmioty same szukają najbliższego inwentarza, który je przyjmie.
  PPM barwnikiem maluje rurę (przyjmuje wtedy tylko przedmioty w tym kolorze), Shift+PPM pustą ręką czyści farbę.
  Lejki i inne mody mogą wkładać przedmioty do rur.
- **Ekstraktor** – postaw go, **klikając na skrzynię**, z której ma pobierać. Impuls redstone → wyciąga stos i wypycha do przodu (rura / inwentarz / na ziemię).
- **Filtr** – jak ekstraktor, ale pobiera tylko przedmioty z 9 slotów wzorców (PPM). Pusty filtr = wszystko.
- **Maszyna sortująca** – PPM otwiera 9 kolumn × 3 rzędy:
  rząd 1 = barwnik (kolor nadawany przedmiotom), rzędy 2–3 = wzorce przedmiotów.
  Kolumna z barwnikiem bez wzorców łapie wszystko inne. Kolorowe przedmioty jadą tylko rurami w swoim kolorze lub niepomalowanymi.

### Azurity
Azurity to **Forge Energy (FE)**, więc działa też z maszynami/kablami innych modów.
- **Przewód ze stopu energetycznego** – przyklejany do ścian jak przewody redstone, przesyła energię (500 FE/t), PPM pokazuje stan.
- **Panel słoneczny** – 10 FE/t w dzień pod gołym niebem (mniej w deszczu).
- **Akumulator** – 1 000 000 FE.
- **Macerator** – miele rudy i surowe rudy na **2 pyły** (10% szans na trzeci), sztabki na 1 pył; pyły (żelazo, złoto, miedź, cyna, srebro) przetapia się na sztabki. Kruszy też bruk→żwir→piasek (+krzemień), kamień, piaskowiec, jasnogłaz, kości, pręty płomienia, wełnę, trzcinę oraz rudy węgla, redstone, lapisu, kwarcu i azurytu. 16 FE/t (`macerator_per_tick`), slot baterii, wyjście główne i uboczne, lejki z każdej strony. Receptura: 3 krzemienie, 2 żelazne zębatki, obudowa maszyny energetycznej, żelazo, przewód ze stopu energetycznego. Własne receptury: typ `redcircuit:macerating` (`ingredient`, `result`, opcjonalnie `bonus` + `bonus_chance`, `time`), widoczne w JEI.
- **Tartak** – tnie kłody i trzony (wszystkie drewna z gry, także bambus) na **6 desek** zamiast 4 + trociny, a deski na 3 patyki (25% szans na trociny). 12 FE/t (`sawmill_per_tick`), slot baterii, lejki z każdej strony. **Trociny** palą się w piecu (5 s) i dają papier (3 w rzędzie → 2). Receptura: deski, piła ręczna, 2 żelazne zębatki, obudowa maszyny energetycznej, żelazo, przewód ze stopu energetycznego. Własne receptury: typ `redcircuit:sawing` (jak w maceratorze), widoczne w JEI.
- **Piec elektryczny** – przetapia jak piec, 2× szybciej, zużywa 20 FE/t.

## Receptury (skrót)
- Sztabki stopu sygnałowego i energetycznego robi się **tylko w piecu do stopów** (zwykłym lub elektrycznym): miedź + 4 redstone, srebro + 4 azuryt.
- Przewód Signal Alloy: 3 sztabki w rzędzie → 12. Przewód Power Alloy: analogicznie.
- Przewód izolowany: wełna + 4 przewody Signal Alloy → 4.
- Kabel wiązkowy: 5 przewodów izolowanych + 4 nici (X) → 2.
- Płytka obwodu: gładka płyta kamienna + redstone → 2. Bramki = płytka + pochodnie / przewody / dźwignia / przekaźnik / zegar / kwarc.
- Rura: miedź, szkło, miedź → 8.

Pełna lista: `src/main/resources/data/redcircuit/recipe/`. Najwygodniej podglądać w grze przez JEI/EMI.

## Integracja z JEI
Jeśli zainstalujesz **JEI (Just Enough Items)** dla NeoForge 1.21.1, zobaczysz:
- osobną kategorię **Piec do stopów** ze wszystkimi recepturami stopów (także z datapacków),
- piece do stopów, piec elektryczny i stół kreślarski jako stacje wytwarzania,
- opisy (zakładka „Informacje”) dla generatorów, wirników, rur, maszyny sortującej, stołu kreślarskiego i pił.

Mod działa też bez JEI. Przy `gradlew runClient` JEI uruchamia się automatycznie.

## Rozwój
Tekstury, modele, receptury i tłumaczenia generuje `tools/gen_assets.py` (Python 3 + Pillow):
```
python tools/gen_assets.py
```


## Integracja z CC: Tweaked (opcjonalna)
Działa automatycznie, gdy zainstalowany jest CC: Tweaked (testowane API 1.120.x dla 1.21.1).
- **Kabel wiązkowy ↔ komputery i żółwie**: kabel (zwykły, w obudowie i jako część CB Multipart) łączy się wizualnie z komputerem. `redstone.setBundledOutput` zasila kanały kabla, a `redstone.getBundledInput` odczytuje kanały aktywne w sieci (kanał = kolor przewodu izolowanego, jak w `colors`). Działa też z przekaźnikiem redstone (Redstone Relay).
- **Peryferium `redcircuit_gate`** (bramki z ustawieniami): `getGateType()`, `getState()`, `getInterval()/setInterval(ticks)`, `getCount()`, `getMax()/setMax(n)`, `resetCount()`, `isMuted()/setMuted(bool)`, `isMirrored()/setMirrored(bool)` (komórka stanu: wyjście „w trakcie” po lewej).
- **Peryferium `redcircuit_wireless`** (nadajnik/odbiornik): `getFrequency()/setFrequency(n)`, `isTransmitter()`, `isPowered()`, `isChannelActive([freq])`.
- Maszyny, baterie, zbiorniki itp. są dostępne jako peryferia ogólne CC (`inventory`, `energy_storage`, `fluid_storage`) – np. `peripheral.wrap("left").getEnergy()`.
- Przykładowy program: `examples/computercraft/redcircuit_demo.lua`.

## Podręcznik Patchouli (opcjonalny)
Z zainstalowanym **Patchouli** w grze jest książka **Podręcznik RedCircuit** (EN/PL, zależnie od języka gry):
- receptura: **książka + redstone**; jest też w zakładce kreatywnej „RedCircuit: Narzędzia i wytwarzanie”,
- 10 kategorii: Pierwsze kroki, Okablowanie, Logika, Logistyka, Maszyny, Azurity, Płyny, Redstone bezprzewodowy, Narzędzia, Inne mody,
- strony z recepturami są pobierane z gry (zmiany w datapackach są widoczne od razu).
Treść generuje `tools/gen_patchouli.py` (wywoływany z `gen_assets.py`): pliki w `data/redcircuit/patchouli_books/guide/` i `assets/redcircuit/patchouli_books/guide/<język>/`. Bez Patchouli mod działa normalnie, a receptura książki się nie ładuje.

## Osiągnięcia
Zakładka **RedCircuit** w osiągnięciach (30 pozycji, EN/PL), tło z gładkiego kamienia. Startuje po znalezieniu pierwszej rudy z moda, dalej gałęzie:
- **stopy i okablowanie** – piec do stopów → stop sygnałowy → przewód → izolowany → kabel wiązkowy; wyzwanie „Pełne spektrum” (16 kolorów przewodów, 50 XP),
- **logika** – pierwsza bramka, zegar, licznik, lampa, redstone bezprzewodowy; wyzwanie „Układ scalony” (wszystkie 17 bramek, 100 XP),
- **Azurity** – stop energetyczny, generator, wiatrak/koło wodne, akumulator, „W pełni naładowana” (bateria 50 000 FE), obwód i obudowa maszyny energetycznej, wiertarka, pompa, zbiornik,
- **rury** – rura, maszyna sortująca, Requester, akcelerator,
- **narzędzia** – nożyce do drutu, stół kreślarski, śrubokręt.
Generator: `tools/gen_advancements.py` (dodaje też tagi `redcircuit:gates` i `lamps`).

## Zakładki trybu kreatywnego
Przedmioty są podzielone na 5 zakładek (ustawionych za wszystkimi zakładkami Minecrafta):
- **RedCircuit: Redstone** – przewody sygnałowe, izolowane, kable wiązkowe (też w obudowie), bramki, lampy, nadajnik i odbiornik bezprzewodowy,
- **Logistyka i maszyny** – rury, akcelerator, ekstraktor, filtr, sortowanie, Requester, Manager, Regulator, Relay, Buffer, detektor, maszyny (aplikator, wyrzutnik, asembler, niszczarka, zapalarka),
- **Energia i płyny** – przewody energetyczne, generatory (w tym na paliwo stałe), wiatrak i koło wodne, akumulator, baterie (pusta i naładowana), ładowarka, piece elektryczne, macerator, tartak, pompa, rury i zbiornik,
- **Narzędzia i wytwarzanie** – śrubokręt, nożyce do drutu, piły, miernik, wiertarka i piła łańcuchowa (puste i naładowane), pilot, piec do stopów, stół kreślarski, plany, książka Patchouli,
- **Materiały i bloki** – rudy, surowce, sztabki, bryłki, pyły, bloki, zębatki, druty, cewka, płytka obwodu, obwody i obudowy maszyn.
Podział jest w `ModTabs.categoryOf()` (po ID przedmiotu); przedmioty bez kategorii trafiają do „Materiały i bloki”.
