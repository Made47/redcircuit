# RedCircuit (Minecraft 1.7.10, Forge 10.13.4.1614)

*[English version: README.md](README.md)*

> **Stworzone z pomocą AI.** Mod powstał przy pomocy sztucznej inteligencji (Claude od Anthropic): kod źródłowy,
> skrypty generujące tekstury, tłumaczenia i dokumentacja zostały w dużej mierze napisane przez AI pod kierunkiem autora,
> który zaprojektował, sprawdzał i testował mod w grze.


Port moda RedCircuit na 1.7.10 - budowany etapami (plan: README sekcja "Etapy").
Szablon budowania: [GTNH ExampleMod1.7.10](https://github.com/GTNewHorizons/ExampleMod1.7.10) (RetroFuturaGradle).

## Budowanie
1. Zainstaluj **JDK 25** (np. Temurin 25: https://adoptium.net) - wymaga tego wtyczka GTNH 2.x. Mod i tak jest kompilowany do Javy 8.
   Jeśli domyślną Javą ma zostać inna wersja, dopisz do `gradle.properties` (ścieżka przykładowa):
   `org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot`
2. Projekt musi być repozytorium git z co najmniej jednym commitem (wersja moda jest brana z git):
   ```
   git init
   git add .
   git commit -m "start"
   ```
3. `gradlew setupDecompWorkspace` (tylko za pierwszym razem), potem `gradlew build` albo `gradlew runClient`.
4. Gotowy jar: `build/libs/`.

## Etap 1-2 (gotowe)
- Rudy: miedź, cyna, srebro, azuryt (azuryt wypada jako pył 4-5 szt., jak redstone). Konfiguracja: `config/redcircuit.cfg`.
- Sztabki: miedź, cyna, srebro, mosiądz, stal, stop sygnałowy, stop energetyczny; bryłki, pyły, zębatki, bloki materiałów.
- OreDictionary (`ingotCopper`, `dustAzurite`, `gearIron`...) - współpraca z innymi modami.
- **Piec do stopów** (8 cegieł): siatka 3x3, dowolne paliwo, lejki (góra: składniki, boki: paliwo, dół: wynik).
  - 3 miedź + 1 cyna → 4 mosiądz; 1 miedź + 4 redstone → stop sygnałowy;
  - 1 srebro + 4 azuryt → stop energetyczny; 1 żelazo + 2 węgle → stal.

## Etap 3 (gotowe)
- **Przewód ze stopu sygnałowego** (3 sztabki stopu → 12): bez strat sygnału - cała sieć ma poziom najsilniejszego wejścia.
- **Przewody izolowane** w 16 kolorach (wełna + 4 przewody → 4) - łączą się tylko z tym samym kolorem i ze stopem sygnałowym.
- **Kabel wiązkowy** (nić + przewody izolowane → 2): 16 kanałów, kolor przewodu = kanał.
- Przewody przyklejają się do każdej ściany; kilka w jednym bloku łączy się w narożnikach, łączą się też przez krawędzie.
  Zniszczenie usuwa tylko przewód, w który celujesz.
- **Wersje w obudowie** (8 przewodów wokół płyty kamiennej → 8): biegną środkiem bloku jak rura, łączą się z przewodami na ścianach.
- Przewody łączą się z blokami redstone (dźwignie, lampy, tłoki, drzwi, dozowniki...) i silnie zasilają blok, na którym leżą.

## Etap 4 (gotowe)
- **Płytka obwodu** (płyta kamienna + redstone → 2) i 17 **bramek** (bezkształtnie, jak w 1.21): NOT, bufor, AND, NAND, OR, NOR,
  XOR, XNOR, zatrzask RS, przerzutnik T, impuls, zegar, generator losowy, licznik, sekwencer, komórka stanu, synchronizator.
- Bramka leży na bloku, wyjście wskazuje kierunek patrzenia przy stawianiu. Przewody łączą się tylko z wyjściami i włączonymi wejściami.
- **PPM - okno bramki**: opis, schemat wejść/wyjść na żywo, czas (zegar, sekwencer, komórka stanu), maksimum licznika,
  wyłączanie wejść (AND/OR/XOR...) lub bocznych wyjść (bufor, NOT), strony zatrzasku RS i komórki stanu, wyciszenie tykania.
  Przerzutnik T: PPM przełącza, Shift + PPM otwiera okno. Licznik działa z komparatorem.
- **Śrubokręt** (żelazo + patyk): PPM obraca bramkę, piec do stopów i bloki vanilla (Shift - w drugą stronę);
  Shift + PPM na bramce zbiera ją razem z ustawieniami.

## Etap 5 (gotowe)
- **Lampy** w 16 kolorach (szyby, jasnopył, barwnik, redstone): świecą przy sygnale, gasną z opóźnieniem 4 ticków.
  **Lampy odwrócone** świecą bez sygnału; lampa + pochodnia redstone zamienia zwykłą na odwróconą i z powrotem.
- **Nadajnik i odbiornik bezprzewodowy** (płytka, przewód, perła Endu, złoto + pochodnia / komparator): PPM ustawia
  częstotliwość 1-5000. Zasilony nadajnik nadaje wszędzie - też w innych wymiarach i niezaładowanych chunkach.
  Odbiornik daje sygnał 15 na wszystkie strony i mocno zasila blok pod sobą.
- **Pilot** (nadajnik + guzik + żelazo): trzymanie PPM nadaje, Shift + PPM - wybór częstotliwości.

## Etap 6 (gotowe)
- **Rury** (miedź + szkło → 8): przedmioty jadą do najbliższego inwentarza, który je przyjmie; otwarty koniec wyrzuca
  przedmiot. PPM barwnikiem maluje rurę (przyjmuje wtedy tylko przedmioty w tym kolorze), Shift + PPM pustą ręką zmywa farbę.
  **Rura restrykcyjna** (rura + żelazo) jest wybierana tylko wtedy, gdy nie ma innej drogi. Lejki mogą wkładać do rur.
- **Ekstraktor** (impuls: stos z inwentarza za nim → przód), **filtr** (9 wzorców), **maszyna sortująca**
  (9 kolumn: barwnik + 2 wzorce - nadaje przedmiotom kolor rury).
- **Obwód sygnałowy** i **obudowa maszyny sygnałowej** - korpus maszyn:
  przekaźnik (sam wypycha co 4 ticki), wyrzutnik, bufor (18 slotów, komparator), rozmieszczacz (używa przedmiotów jak gracz),
  kruszarka bloków (dropy tyłem), zapalarka, detektor przedmiotów (impuls przy każdym stosie), regulator
  (uzupełnia inwentarz do ilości z filtra, potem daje sygnał), montażownica (16 slotów = 16 kanałów kabla wiązkowego).
- Maszyny stawia się klikiem na blok - przód odwrócony od klikniętej ściany; śrubokręt: kliknięta ściana staje się przodem.

## Etap 7 (gotowe)
- Energia **RF** (dołączone publiczne API CoFH `cofh.api.energy` - działa z maszynami i przewodami innych modów, np. Thermal Expansion,
  EnderIO; bez nich mod działa samodzielnie). Liczby jak w 1.21 (FE = RF). Produkcja i zużycie w `config/redcircuit.cfg`.
- **Przewód ze stopu energetycznego** (na ścianach, jak przewody sygnałowe) i wersja **w obudowie**; PPM pustą ręką - stan energii.
- **Akumulator** (1 000 000 RF, sloty ładowania/rozładowania baterii; PPM baterią - ładuj, Shift - rozładuj), **bateria** (50 000 RF),
  **ładowarka** (4 sloty).
- Generatory: **panel słoneczny**, **termostos** (gorący + zimny sąsiad), **generator** z **wiatrakiem** (5x5 wolnego miejsca pod
  niebem) albo **kołem wodnym** (3x3 wody) - wirnik widoczny i obracający się, **generator na paliwo stałe**.
- Maszyny: **piec elektryczny**, **macerator** (rudy x2, pyły...), **tartak** (kłody x6 desek + trociny, także kłody innych modów),
  **elektryczny piec do stopów**, oraz z logistyki: **Requester**, **Manager**, **akcelerator** rur.
- Narzędzia wytwórcze: **piły ręczne** (6 materiałów) i **nożyce do drutu** - zostają w siatce i tracą wytrzymałość;
  cienki drut, cewka miedziana, obwód energetyczny, obudowa maszyny energetycznej.

## Etap 8 (gotowe)
- **Pompa** (stawiana klikiem na źródło): zasysa z tyłu - ze zbiornika (także innych modów) albo ze świata (do 256 bloków
  połączonego jeziora, od góry), tłoczy do przodu - do zbiornika/rury albo wylewa blok płynu. 200 RF za wiadro, redstone ją zatrzymuje.
- **Rura do płynów** (mosiądz + szkło → 8): 1000 mB, wyrównuje poziom z sąsiednimi rurami i wypycha płyn do podłączonych
  zbiorników i maszyn (Forge `IFluidHandler`), nie oddając go do źródła.
- **Zbiornik na ciecz** (16 wiader): wiadra i pojemniki modów, ciecz widoczna w środku, spływa do zbiornika niżej, komparator,
  światło od lawy; po rozbiciu zawartość zostaje w przedmiocie.

## Etap 9 (gotowe)
- **Wiertarka elektryczna** (kilof + łopata, poziom diamentu) i **piła łańcuchowa** (siekiera, liście, wełna, pajęczyny):
  energia zamiast wytrzymałości (50 RF za blok, 100 000 RF), ładowanie w ładowarce lub akumulatorze. Wiertarka:
  Shift + PPM przełącza kopanie 1x1 / 3x3 (Shift podczas kopania = jeden blok).
- **Miernik**: PPM na bloku - sygnał redstone, kanały kabla wiązkowego, energia RF i ciecze (w czacie).
- **Stół kreślarski**: trwała siatka 3x3, 18 slotów magazynu (składniki brane najpierw z niego), shift-klik na wyniku
  wytwarza wielokrotnie. **Pusty plan** (papier + niebieski barwnik) + przycisk "Plan" zapisuje recepturę;
  zapisany plan wczytuje składniki z magazynu do siatki.

## Etap 10 (gotowe)
Każda integracja włącza się tylko, gdy dany mod jest zainstalowany (`dependencies.gradle` - wszystkie w runClient
poza ComputerCraft i OpenComputers, które są tylko do kompilacji).
- **NEI** (`compat/nei`): receptury pieca do stopów, rozdrabniacza i tartaku (z szansą produktu dodatkowego);
  ukryte świecące lampy i plan bez receptury.
- **Waila** (`compat/waila`): sygnał przewodu, kanały kabla, stan bramki (wyjście, czas, licznik, wyłączone strony),
  lampy, nadajniki (częstotliwość), produkcja generatorów, przedmioty w rurze, energia RF, ciecze, postęp maszyn.
- **ForgeMultipart** (`compat/fmp`): przewody (stop sygnałowy, izolowane, wiązkowe, energetyczne) jako części -
  dzielą blok z mikroblokami i innymi przewodami. Przewód stawiany w bloku wieloczęściowym (lub w zwykłym przewodzie
  na jednej ścianie) staje się częścią; zwykły przewód zamienia się sam, gdy dołożysz do niego mikroblok
  (przewód-blok na kilku ścianach jest rozkładany na kilka części). Na jednej ścianie bloku mieści się jeden przewód.
- **ComputerCraft** (`compat/computer`): kabel wiązkowy ↔ `redstone.getBundledInput/setBundledOutput`; peryferia
  `redcircuit_gate` i `redcircuit_wireless`.
- **OpenComputers**: te same funkcje jako komponenty (przez adapter).

Funkcje `redcircuit_gate`: getGateType, getState, getInterval, setInterval, getCount, getMax, setMax, resetCount,
isMuted, setMuted, isMirrored, setMirrored, setInputEnabled(side, bool), setQuiet. `redcircuit_wireless`:
getFrequency, setFrequency, isTransmitter, isPowered, isChannelActive([freq]).

## Etapy
1-2. szkielet i materiały ✔ · 3. przewody ✔ · 4. bramki ✔ · 5. lampy i bezprzewodowe ✔ · 6. rury i logistyka ✔ ·
7. energia RF ✔ · 8. płyny ✔ · 9. narzędzia i stół kreślarski ✔ · 10. NEI, Waila, ComputerCraft, OpenComputers, ForgeMultipart ✔ · 11. osiągnięcia, poradnik.

## Rozwój
`python tools/gen_textures.py` - tekstury, `python tools/gen_lang.py` - tłumaczenia (część z projektu 1.21 w `../redcircuit`).
