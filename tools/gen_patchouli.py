"""Podręcznik Patchouli dla RedCircuit (EN + PL).

Wywoływane z gen_assets.py (po gen_jei). Generuje:
  data/redcircuit/patchouli_books/guide/book.json
  assets/redcircuit/patchouli_books/guide/<lang>/categories/*.json
  assets/redcircuit/patchouli_books/guide/<lang>/entries/<kategoria>/*.json
oraz recepturę na książkę (tylko gdy Patchouli jest zainstalowane).

Teksty: "||" = nowa strona, "\\n\\n" = $(br2). Zbyt długie strony są dzielone automatycznie na granicy zdań.
"""
import json
import os
import re

M = "redcircuit:"
PAGE_LIMIT = 320          # znaków na stronę tekstu (bez kodów formatowania)

CATEGORIES = [  # id, ikona, EN, PL, opis EN, opis PL
    ("basics", "redcircuit:signal_alloy_ingot", "Getting Started", "Pierwsze kroki",
     "Ores, alloys, crafting tools and components.", "Rudy, stopy, narzędzia do wytwarzania i komponenty."),
    ("wiring", "redcircuit:signal_alloy_wire", "Wiring", "Okablowanie",
     "Wires that carry redstone without loss, insulated wires and bundled cables.",
     "Przewody przenoszące redstone bez strat, przewody izolowane i kable wiązkowe."),
    ("logic", "redcircuit:timer_gate", "Logic", "Logika",
     "Logic gates, timers, counters and lamps.", "Bramki logiczne, zegary, liczniki i lampy."),
    ("logistics", "redcircuit:tube", "Logistics", "Logistyka",
     "Pneumatic tubes, sorting and item handling machines.", "Rury pneumatyczne, sortowanie i maszyny do przedmiotów."),
    ("machines", "redcircuit:deployer", "World Machines", "Maszyny",
     "Machines that place, break and use blocks.", "Maszyny, które stawiają, niszczą i używają bloków."),
    ("energy", "redcircuit:battery_box", "Azurity", "Azurity",
     "Azurity is Forge Energy (FE): generators, storage and powered machines.",
     "Azurity to Forge Energy (FE): generatory, magazyny i maszyny na prąd."),
    ("fluids", "redcircuit:fluid_tank", "Fluids", "Płyny",
     "Pumps, pipes and tanks.", "Pompy, rury i zbiorniki."),
    ("wireless", "redcircuit:wireless_transmitter", "Wireless Redstone", "Redstone bezprzewodowy",
     "Transmitters, receivers and remotes on 5000 frequencies.", "Nadajniki, odbiorniki i piloty na 5000 częstotliwościach."),
    ("tools", "redcircuit:electric_drill", "Tools", "Narzędzia",
     "Electric tools and the multimeter.", "Narzędzia elektryczne i miernik."),
    ("compat", "minecraft:comparator", "Other Mods", "Inne mody",
     "Optional integrations and modpack support.", "Opcjonalne integracje i wsparcie dla modpacków."),
]

DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]


def E(cat, eid, icon, en_name, pl_name, en, pl, recipes=(), spot=(), extra=None):
    return {"cat": cat, "id": eid, "icon": icon, "name": (en_name, pl_name), "text": (en, pl),
            "recipes": list(recipes), "spot": list(spot), "extra": extra or []}


ENTRIES = [
    # ------------------------------------------------------------------ Pierwsze kroki
    E("basics", "introduction", "minecraft:book", "Introduction", "Wstęp",
      "$(l)RedCircuit$() adds lossless redstone wiring, logic gates, pneumatic tubes and $(item)Azurity$() power.\n\n"
      "Start by finding $(item)tin$(), $(item)silver$() and $(item)azurite$() ore, then build an $(l:basics/alloy_furnace)Alloy Furnace$(/l) "
      "to make $(item)Signal Alloy$() and $(item)Power Alloy$().||"
      "Most directional blocks are placed by clicking the block that should be at their back (for example a chest). "
      "A $(l:basics/crafting_tools)Screwdriver$(/l) rotates them later.\n\n"
      "With $(item)JEI$() installed you can look up every recipe; with $(item)Jade$() you see signal levels, energy and timer settings.",
      "$(l)RedCircuit$() dodaje przewody redstone bez strat, bramki logiczne, rury pneumatyczne i energię $(item)Azurity$().\n\n"
      "Zacznij od znalezienia rud $(item)cyny$(), $(item)srebra$() i $(item)azurytu$(), a potem zbuduj "
      "$(l:basics/alloy_furnace)piec do stopów$(/l), żeby wytopić $(item)stop sygnałowy$() i $(item)stop energetyczny$().||"
      "Większość bloków kierunkowych stawia się, klikając blok, który ma być za nimi (np. skrzynię). "
      "Później obróci je $(l:basics/crafting_tools)śrubokręt$(/l).\n\n"
      "Z $(item)JEI$() podejrzysz każdą recepturę, a z $(item)Jade$() poziom sygnału, energię i ustawienia zegarów."),
    E("basics", "world", "redcircuit:azurite_ore", "Ores", "Rudy",
      "All ores also have a deepslate variant; Fortune and Silk Touch work.\n\n"
      "$(li)$(item)Tin$(): Y 0 to 80, stone pickaxe\n$(li)$(item)Silver$(): Y -48 to 32, iron pickaxe\n"
      "$(li)$(item)Azurite$(): Y -64 to 16, drops 4-5 like redstone\n\n"
      "Every ore can be disabled in $(item)config/redcircuit-common.toml$().",
      "Każda ruda ma też wersję głębinową; działa szczęście i jedwabny dotyk.\n\n"
      "$(li)$(item)Cyna$(): Y 0-80, kilof kamienny\n$(li)$(item)Srebro$(): Y -48-32, kilof żelazny\n"
      "$(li)$(item)Azuryt$(): Y -64-16, wypada 4-5 jak redstone\n\n"
      "Każdą rudę można wyłączyć w $(item)config/redcircuit-common.toml$().",
      spot=["redcircuit:tin_ingot", "redcircuit:silver_ingot", "redcircuit:azurite"]),
    E("basics", "alloy_furnace", "redcircuit:alloy_furnace", "Alloy Furnace", "Piec do stopów",
      "The $(item)Alloy Furnace$() has 9 ingredient slots, a fuel slot and an output. Ingredients may lie in any slots.\n\n"
      "$(li)Copper + 4 Redstone = $(item)Signal Alloy Ingot$()\n$(li)Silver + 4 Azurite = $(item)Power Alloy Ingot$()\n"
      "$(li)3 Copper + Tin = 4 $(item)Brass$()||"
      "The $(item)Power Alloy Furnace$() does the same on 25 FE/t, twice as fast, and has a battery slot.\n\n"
      "Automation: ingredients from the top, fuel from the sides, results from the bottom. "
      "Custom recipes use the $(item)redcircuit:alloying$() recipe type (datapacks, KubeJS, CraftTweaker).",
      "$(item)Piec do stopów$() ma 9 slotów na składniki, slot paliwa i wynik. Składniki mogą leżeć w dowolnych slotach.\n\n"
      "$(li)Miedź + 4 redstone = $(item)stop sygnałowy$()\n$(li)Srebro + 4 azuryt = $(item)stop energetyczny$()\n"
      "$(li)3 miedzi + cyna = 4 $(item)mosiądzu$()||"
      "$(item)Elektryczny piec do stopów$() robi to samo na 25 FE/t, 2x szybciej, i ma slot baterii.\n\n"
      "Automatyzacja: składniki od góry, paliwo z boków, wynik od dołu. "
      "Własne receptury: typ $(item)redcircuit:alloying$() (datapacki, KubeJS, CraftTweaker).",
      recipes=["alloy_furnace", "power_alloy_furnace"],
      spot=["redcircuit:signal_alloy_ingot", "redcircuit:power_alloy_ingot", "redcircuit:brass_ingot"]),
    E("basics", "blueprint_table", "redcircuit:blueprint_table", "Blueprint Table & Plans", "Stół kreślarski i plany",
      "The $(item)Blueprint Table$() remembers its 3x3 grid and has 18 storage slots. Ingredients are taken from storage first, "
      "so the grid works as a pattern. Shift-click the result to craft up to 64 times.||"
      "Put a $(item)Blank Plan$() in the left slot, lay out a recipe and press the pen button to save a $(item)Plan$(). "
      "A saved plan plus the button loads the recipe: empty grid slots are filled from storage.",
      "$(item)Stół kreślarski$() pamięta siatkę 3x3 i ma 18 slotów magazynu. Składniki są brane najpierw z magazynu, "
      "więc siatka działa jak wzór. Shift + klik na wynik wytwarza do 64 razy.||"
      "Włóż $(item)pusty plan$() do lewego slotu, ułóż recepturę i naciśnij przycisk z piórem, żeby zapisać $(item)plan$(). "
      "Zapisany plan + przycisk wczytuje recepturę: puste pola siatki uzupełniają się z magazynu.",
      recipes=["blueprint_table", "blank_plan"]),
    E("basics", "crafting_tools", "redcircuit:iron_handsaw", "Crafting Tools", "Narzędzia do wytwarzania",
      "$(item)Handsaws$() (stone to netherite) stay in the grid and lose 1 durability. Any saw is needed for the $(item)Sawmill$().||"
      "$(item)Wire Cutters$() (steel) work the same way: ingot + wire cutters = $(item)Fine Copper/Iron Wire$(). Four fine copper wires around an iron ingot make a $(item)Copper Coil$().\n\n"
      "The $(item)Screwdriver$() rotates gates, machines and vanilla blocks; Shift rotates the other way. Shift-right-click a gate to pick it up with its settings. It never wears out.",
      "$(item)Piły ręczne$() (od kamiennej do netherytowej) zostają w siatce i tracą 1 punkt wytrzymałości. Piła jest składnikiem $(item)tartaku$().||"
      "$(item)Nożyce do drutu$() (ze stali) działają tak samo: sztabka + nożyce = $(item)cienki drut$() miedziany lub żelazny. 4 cienkie druty miedziane wokół żelaza to $(item)cewka miedziana$().\n\n"
      "$(item)Śrubokręt$() obraca bramki, maszyny i bloki vanilla; z Shift w drugą stronę. Shift + PPM na bramce zbiera ją razem z ustawieniami. Nie zużywa się.",
      recipes=["iron_handsaw", "wire_cutters", "fine_copper_wire", "copper_coil", "screwdriver"]),
    E("basics", "components", "redcircuit:power_circuit", "Components", "Komponenty",
      "$(item)Circuit Plates$() (smooth stone slab + redstone) are the base of every gate.\n\n"
      "$(item)Gears$(): stone (4 cobblestone in a plus), then copper, gold, iron, brass, silver and diamond around a stone gear; netherite by smithing.\n\n"
      "The $(item)Power Circuit$() goes into electric tools and the $(item)Power Machine Casing$() (iron, brass, circuit), the body of every powered machine.||"
      "The $(item)Signal Circuit$() (signal alloy, redstone torches, circuit plate) makes the $(item)Signal Machine Casing$() (iron, tin, circuit), "
      "the body of redstone-driven machines: extractor, regulator, relay, ejector, detector, igniter, deployer, block breaker and assembler.",
      "$(item)Płytka obwodu$() (płyta z gładkiego kamienia + redstone) to podstawa każdej bramki.\n\n"
      "$(item)Zębatki$(): kamienna (4 bruk w kształcie plusa), potem miedziana, złota, żelazna, mosiężna, srebrna i diamentowa wokół kamiennej; netherytowa na stole kowalskim.\n\n"
      "$(item)Obwód energetyczny$() trafia do narzędzi elektrycznych i do $(item)obudowy maszyny energetycznej$() (żelazo, mosiądz, obwód), korpusu maszyn na energię.||"
      "$(item)Obwód sygnałowy$() (stop sygnałowy, pochodnie redstone, płytka obwodu) daje $(item)obudowę maszyny sygnałowej$() (żelazo, cyna, obwód), "
      "korpus maszyn sterowanych redstone: ekstraktora, regulatora, relay, wyrzutnika, detektora, zapalarki, aplikatora, niszczarki i asemblera.",
      recipes=["circuit_plate", "stone_gear", "iron_gear", "power_circuit", "power_machine_casing", "signal_circuit", "signal_machine_casing"]),

    # ------------------------------------------------------------------ Okablowanie
    E("wiring", "signal_alloy_wire", "redcircuit:signal_alloy_wire", "Signal Alloy Wire", "Przewód ze stopu sygnałowego",
      "Carries redstone $(l)without any loss$() over any distance. Wires stick to surfaces: click a floor, wall or ceiling. "
      "They connect straight, around inner corners (several faces in one block) and around outer corners over a block edge.||"
      "Breaking a block with several wires removes the one you aim at. Only one wire type or colour can share a block space. "
      "Wires strongly power the block they lie on, like a repeater.",
      "Przewodzi redstone $(l)bez spadku mocy$() na dowolną odległość. Przewody przyklejają się do powierzchni: kliknij podłogę, ścianę lub sufit. "
      "Łączą się prosto, w narożnikach wewnętrznych (kilka ścian w jednym bloku) i zewnętrznych, przez krawędź bloku.||"
      "Rozbijając blok z kilkoma przewodami, usuwasz ten, w który celujesz. W jednym bloku może leżeć tylko jeden typ lub kolor przewodu. "
      "Przewody mocno zasilają blok, na którym leżą, jak przekaźnik.",
      recipes=["signal_alloy_wire"]),
    E("wiring", "insulated_wire", "redcircuit:insulated_wire_red", "Insulated Wire", "Przewód izolowany",
      "16 colours. An insulated wire only connects to the same colour, to Signal Alloy Wire and to Bundled Cable, "
      "so different signals can run side by side.",
      "16 kolorów. Przewód izolowany łączy się tylko z tym samym kolorem, z przewodem ze stopu sygnałowego i z kablem wiązkowym, "
      "więc różne sygnały mogą biec obok siebie.",
      recipes=["insulated_wire_red", "insulated_wire_blue"]),
    E("wiring", "bundled_cable", "redcircuit:bundled_cable", "Bundled Cable", "Kabel wiązkowy",
      "16 independent channels in one cable. An insulated wire of colour X attached to the cable sends and reads channel X.\n\n"
      "The $(item)Assembler$(), CC: Tweaked computers and the multimeter read single channels.",
      "16 niezależnych kanałów w jednym kablu. Przewód izolowany koloru X podłączony do kabla nadaje i odbiera kanał X.\n\n"
      "Kanały odczytują $(item)asembler$(), komputery CC: Tweaked i miernik.",
      recipes=["bundled_cable"]),
    E("wiring", "encased_wire", "redcircuit:encased_signal_alloy_wire", "Encased Wires", "Przewody w obudowie",
      "Every wire type has an encased version that runs through the middle of the block like a pipe. "
      "It works like the normal wire and connects to other encased wires and to wires on faces.\n\n"
      "8 wires around a smooth stone slab make 8 encased wires; an encased wire alone gives the plain wire back.",
      "Każdy rodzaj przewodu ma wersję w obudowie, która biegnie środkiem bloku jak rura. "
      "Działa jak zwykły przewód i łączy się z innymi przewodami w obudowie oraz z przewodami na ścianach.\n\n"
      "8 przewodów wokół płyty z gładkiego kamienia daje 8 w obudowie; przewód w obudowie sam w siatce oddaje zwykły.",
      recipes=["encased_signal_alloy_wire", "encased_bundled_cable"]),

    # ------------------------------------------------------------------ Logika
    E("logic", "gates", "redcircuit:and_gate", "Logic Gates", "Bramki logiczne",
      "Flat gates placed on the floor. The output faces the direction you look in when placing. Inputs: $(l)L$() left, $(l)B$() back, $(l)R$() right.\n\n"
      "$(li)NOT: !B to front, left and right (side outputs switchable)\n$(li)Buffer: B to front, left and right (side outputs switchable)\n$(li)AND: L and B and R, B off by default\n$(li)NAND: not (L and B and R), B off by default\n$(li)OR / NOR: L or B or R\n$(li)XOR / XNOR: L xor R\n\n"
      "Right-click any gate: description, live inputs/outputs and options - switch inputs off (grey plug).",
      "Płaskie bramki stawiane na podłodze. Wyjście jest w kierunku, w który patrzysz przy stawianiu. Wejścia: $(l)L$() lewo, $(l)B$() tył, $(l)R$() prawo.\n\n"
      "$(li)NOT: !B na przód, lewo i prawo (boczne wyjścia wyłączane)\n$(li)Bufor: B na przód, lewo i prawo (boczne wyjścia wyłączane)\n$(li)AND: L i B i R, B domyślnie wył.\n$(li)NAND: nie (L i B i R), B domyślnie wył.\n$(li)OR / NOR: L lub B lub R\n$(li)XOR / XNOR: L xor R\n\n"
      "PPM na dowolnej bramce: opis, podgląd wejść/wyjść i opcje - wyłączanie wejść (szara zaślepka).",
      recipes=["not_gate", "buffer_gate", "and_gate", "nand_gate", "or_gate", "nor_gate", "xor_gate", "xnor_gate"]),
    E("logic", "memory", "redcircuit:rs_latch_gate", "Latches & Pulses", "Zatrzaski i impulsy",
      "$(li)$(item)RS Latch$(): L sets, R resets; front+left on when set, back+right on when reset (sides swappable, quiet mode in the GUI)\n$(li)$(item)Toggle$(): a pulse on L or R switches the output front <-> back; right-click toggles by hand\n"
      "$(li)$(item)Pulse Former$(): rising edge on B gives a 2-tick pulse\n$(li)$(item)Randomizer$(): while B is on, front, left and right get random states every 4 ticks\n"
      "$(li)$(item)Synchronizer$(): waits for pulses on both L and R, then pulses; B resets",
      "$(li)$(item)Zatrzask RS$(): L ustawia, R kasuje; przód+lewo świecą po ustawieniu, tył+prawo po skasowaniu (w oknie: zamiana stron, tryb tylko przód/tył)\n$(li)$(item)Przerzutnik T$(): impuls na L albo R przełącza wyjście przód <-> tył; PPM przełącza ręcznie\n"
      "$(li)$(item)Formownik impulsu$(): zbocze na B daje impuls 2 ticki\n$(li)$(item)Generator losowy$(): gdy B włączone, przód, lewo i prawo losowo co 4 ticki\n"
      "$(li)$(item)Synchronizator$(): czeka na impuls z L i z R, potem impuls; B kasuje",
      recipes=["rs_latch_gate", "toggle_gate", "pulse_gate", "randomizer_gate", "synchronizer_gate"]),
    E("logic", "timing", "redcircuit:timer_gate", "Timers & Counters", "Zegary i liczniki",
      "$(li)$(item)Timer$(): ticks while B is off, pulsing front, left and right\n$(li)$(item)Sequencer$(): signal walks front, right, back, left\n"
      "$(li)$(item)State Cell$(): a pulse on B starts a countdown, the right side (or left, set in the window) is on meanwhile, then the front pulses\n"
      "$(li)$(item)Counter$(): L +1, R -1; front on at maximum, back on at zero; comparators read the count||"
      "Right-click these gates to open the $(l)settings window$(): time in seconds (0.2 s to 1 h) or the counter maximum (1-1024). "
      "They make a quiet tick sound, which can be muted per gate in the window.",
      "$(li)$(item)Zegar$(): tyka, gdy B jest wyłączone; impuls z przodu, z lewej i z prawej\n$(li)$(item)Sekwencer$(): sygnał krąży przód, prawo, tył, lewo\n"
      "$(li)$(item)Komórka stanu$(): impuls z B startuje odliczanie, prawa strona (albo lewa, do ustawienia w oknie) świeci w trakcie, potem impuls z przodu\n"
      "$(li)$(item)Licznik$(): L +1, R -1; przód świeci przy maksimum, tył przy zerze; komparator czyta stan||"
      "PPM na tych bramkach otwiera $(l)okno ustawień$(): czas w sekundach (0,2 s - 1 h) albo maksimum licznika (1-1024). "
      "Bramki cicho tykają; dźwięk można wyciszyć w oknie każdej bramki osobno.",
      recipes=["timer_gate", "sequencer_gate", "state_cell_gate", "counter_gate"]),
    E("logic", "lamps", "redcircuit:lamp_red", "Lamps", "Lampy",
      "Lamps in 16 colours light up (level 15) with a redstone signal, also from RedCircuit wires. "
      "$(item)Inverted Lamps$() glow without a signal. A lamp with a redstone torch converts between the two.\n\n"
      "They turn off with a 4-tick delay, so short pulses do not flicker.",
      "Lampy w 16 kolorach świecą (poziom 15) przy sygnale redstone, także z przewodów RedCircuit. "
      "$(item)Lampy odwrócone$() świecą bez sygnału. Lampa + pochodnia redstone zamienia jedną w drugą.\n\n"
      "Gasną z opóźnieniem 4 ticków, więc krótkie impulsy nie migoczą.",
      recipes=["lamp_white", "inverted_lamp_white"]),

    # ------------------------------------------------------------------ Logistyka
    E("logistics", "tubes", "redcircuit:tube", "Pneumatic Tubes", "Rury pneumatyczne",
      "Items in tubes find the cheapest route to an inventory that accepts them. Hoppers and other mods can insert into tubes.\n\n"
      "Right-click a tube with a $(item)dye$() to paint it: it then only carries items of that colour. Shift-right-click with an empty hand removes paint.",
      "Przedmioty w rurach szukają najtańszej trasy do inwentarza, który je przyjmie. Leje i inne mody mogą wkładać przedmioty do rur.\n\n"
      "PPM $(item)barwnikiem$() maluje rurę: przewozi wtedy tylko przedmioty w tym kolorze. Shift + PPM pustą ręką zmywa farbę.",
      recipes=["tube"]),
    E("logistics", "extractor", "redcircuit:extractor", "Extractor & Filter", "Ekstraktor i filtr",
      "Place an $(item)Extractor$() by clicking the chest it should pull from. A redstone pulse pulls a stack and pushes it out the front (tube, inventory or ground).\n\n"
      "The $(item)Filter$() only pulls items matching its 9 pattern slots (right-click). Empty filter = anything.",
      "$(item)Ekstraktor$() stawia się, klikając skrzynię, z której ma pobierać. Impuls redstone wyciąga stos i wypycha go przodem (rura, inwentarz lub ziemia).\n\n"
      "$(item)Filtr$() pobiera tylko przedmioty z 9 slotów wzorców (PPM). Pusty filtr = wszystko.",
      recipes=["extractor", "filter"]),
    E("logistics", "sorting", "redcircuit:sorting_machine", "Sorting Machine", "Maszyna sortująca",
      "9 columns x 3 rows. Row 1 holds a dye (the colour given to items), rows 2-3 hold item patterns. "
      "A column with a dye but no patterns catches everything else.\n\nColoured items only travel through tubes of their colour or unpainted ones.",
      "9 kolumn x 3 rzędy. Rząd 1 to barwnik (kolor nadawany przedmiotom), rzędy 2-3 to wzorce przedmiotów. "
      "Kolumna z barwnikiem bez wzorców łapie wszystko inne.\n\nKolorowe przedmioty jadą tylko rurami w swoim kolorze albo niepomalowanymi.",
      recipes=["sorting_machine"]),
    E("logistics", "requester", "redcircuit:requester", "Requester & Manager", "Requester i Manager",
      "Place the $(item)Requester$() by clicking a tube. On a redstone pulse it searches every inventory on the tube network and fetches an item matching its filter to itself. 200 FE per fetch.||"
      "The $(item)Manager$() sits on an inventory it keeps stocked: missing items from its list are requested from the network, items not on the list are sent away. 100 FE per request.",
      "$(item)Requester$() stawia się, klikając rurę. Na impuls redstone przeszukuje wszystkie inwentarze w sieci rur i ściąga do siebie przedmiot pasujący do filtra. 200 FE za pobranie.||"
      "$(item)Manager$() stoi na pilnowanym inwentarzu: brakujące przedmioty z listy zamawia z sieci, a przedmioty spoza listy odsyła. 100 FE za zamówienie.",
      recipes=["requester", "manager"]),
    E("logistics", "routing", "redcircuit:regulator", "Regulator, Relay & Buffer", "Regulator, Relay i Buffer",
      "$(li)$(item)Regulator$(): moves items from the back inventory to the front until it holds the filter amounts, then emits redstone\n"
      "$(li)$(item)Relay$(): 9 slots, accepts from every side but the front and keeps pushing out the front\n"
      "$(li)$(item)Buffer$(): 18 slots open on all sides; comparators read the fill level\n"
      "$(li)$(item)Item Detector$(): pulses for every stack passing through",
      "$(li)$(item)Regulator$(): przenosi z inwentarza z tyłu do inwentarza z przodu, aż ten ma ilości z filtra; wtedy daje sygnał\n"
      "$(li)$(item)Relay$(): 9 slotów, przyjmuje z każdej strony poza przodem i stale wypycha przodem\n"
      "$(li)$(item)Buffer$(): 18 slotów dostępnych z każdej strony; komparator czyta zapełnienie\n"
      "$(li)$(item)Detektor przedmiotów$(): impuls przy każdym przejeżdżającym stosie",
      recipes=["regulator", "relay", "buffer", "item_detector"]),
    E("logistics", "tube_upgrades", "redcircuit:accelerator", "Restriction Tube & Accelerator", "Rura restrykcyjna i akcelerator",
      "A $(item)Restriction Tube$() counts as a very long route, so items only use it when there is no other way.\n\n"
      "The $(item)Accelerator$() is a powered tube section: every stack passing through is boosted to 4x speed, then slows down along the following tubes. 20 FE per stack.",
      "$(item)Rura restrykcyjna$() liczy się jak bardzo długa trasa, więc przedmioty jadą nią tylko, gdy nie ma innej drogi.\n\n"
      "$(item)Akcelerator$() to zasilany odcinek rury: każdy stos przyspiesza do 4x, a potem zwalnia na kolejnych rurach. 20 FE za stos.",
      recipes=["restriction_tube", "accelerator"]),

    # ------------------------------------------------------------------ Maszyny
    E("machines", "world_machines", "redcircuit:deployer", "Placing & Using", "Stawianie i używanie",
      "$(li)$(item)Deployer$(): on a pulse, right-clicks the block in front with its items (placing, bone meal, buckets, shears...)\n"
      "$(li)$(item)Ejector$(): pushes one stack out of the front per pulse\n"
      "$(li)$(item)Assembler$(): with a bundled cable, channel N on places the block from slot N, off picks it up again",
      "$(li)$(item)Aplikator$(): na impuls klika PPM przedmiotem na blok przed sobą (stawianie, mączka kostna, wiadra, nożyce...)\n"
      "$(li)$(item)Wyrzutnik$(): na impuls wypycha przodem jeden stos\n"
      "$(li)$(item)Asembler$(): z kablem wiązkowym kanał N włączony stawia blok ze slotu N, wyłączony zbiera go z powrotem",
      recipes=["deployer", "ejector", "assembler"]),
    E("machines", "breaking", "redcircuit:block_breaker", "Breaking & Fire", "Niszczenie i ogień",
      "The $(item)Block Breaker$() breaks the block in front on a pulse, like a diamond pickaxe; drops come out of its back.\n\n"
      "The $(item)Igniter$() lights and keeps a fire in front of itself while powered and puts it out without a signal.",
      "$(item)Niszczarka bloków$() na impuls rozbija blok przed sobą jak diamentowy kilof; dropy wychodzą tyłem.\n\n"
      "$(item)Zapalarka$() zasilona zapala i podtrzymuje ogień przed sobą, a bez sygnału go gasi.",
      recipes=["block_breaker", "igniter"]),

    # ------------------------------------------------------------------ Energia
    E("energy", "azurity", "redcircuit:power_alloy_wire", "Azurity & Wires", "Azurity i przewody",
      "Azurity is $(item)Forge Energy$() (FE), so it works with other mods' cables and machines.\n\n"
      "$(item)Power Alloy Wire$() sticks to surfaces like redstone wire and carries 500 FE/t. Right-click shows its state.",
      "Azurity to $(item)Forge Energy$() (FE), więc działa z kablami i maszynami innych modów.\n\n"
      "$(item)Przewód ze stopu energetycznego$() przykleja się do ścian jak przewody redstone i przesyła 500 FE/t. PPM pokazuje stan.",
      recipes=["power_alloy_wire", "encased_power_alloy_wire"]),
    E("energy", "generators", "redcircuit:solar_panel", "Solar Panel & Thermopile", "Panel słoneczny i termostos",
      "The $(item)Solar Panel$() makes 10 FE/t in daylight under open sky (less in rain).\n\n"
      "The $(item)Thermopile$() makes 8 FE/t for each pair of a hot neighbour (lava, fire, magma, lit campfire) and a cold one (water, ice, snow), up to 24 FE/t.",
      "$(item)Panel słoneczny$() daje 10 FE/t w dzień pod gołym niebem (mniej w deszczu).\n\n"
      "$(item)Termostos$() daje 8 FE/t za każdą parę sąsiadów: gorący (lawa, ogień, magma, ognisko) i zimny (woda, lód, śnieg), maks. 24 FE/t.",
      recipes=["solar_panel", "thermopile"]),
    E("energy", "solid_fuel_generator", "redcircuit:solid_fuel_generator", "Solid Fuel Generator", "Generator na paliwo stałe",
      "Burns anything that works in a furnace and makes 20 FE/t while burning: one coal gives 32,000 FE, a lava bucket 400,000 FE. "
      "It pushes energy into neighbouring cables and machines.\n\n"
      "Fuel is only used when there is room in its 20,000 FE buffer, so nothing is wasted. A redstone signal pauses it.",
      "Spala wszystko, co pali się w piecu, i podczas spalania daje 20 FE/t: węgiel to 32 000 FE, wiadro lawy 400 000 FE. "
      "Energię oddaje do sąsiednich przewodów i maszyn.\n\n"
      "Paliwo zużywa tylko wtedy, gdy w buforze 20 000 FE jest miejsce, więc nic się nie marnuje. Sygnał redstone go wstrzymuje.",
      recipes=["solid_fuel_generator"]),
    E("energy", "kinetic", "redcircuit:windmill", "Windmill & Water Wheel", "Wiatrak i koło wodne",
      "The $(item)Generator$() needs a rotor on its front: right-click it with a Windmill or Water Wheel, Shift-right-click with an empty hand to remove it.||"
      "$(item)Windmill$(): needs a free 5x5 area in front, open to the sky. 6 FE/t at sea level, up to 32 FE/t high up; x1.5 in rain, x2 in storms.\n\n"
      "$(item)Water Wheel$(): flowing water in the 3x3 area in front gives 4 FE/t per block, still water 1 FE/t (max 24 FE/t).",
      "$(item)Generator$() potrzebuje wirnika z przodu: PPM wiatrakiem albo kołem wodnym, Shift + PPM pustą ręką go zdejmuje.||"
      "$(item)Wiatrak$(): wolne pole 5x5 przed generatorem, pod gołym niebem. 6 FE/t nad poziomem morza, do 32 FE/t wysoko; x1,5 w deszczu, x2 w burzy.\n\n"
      "$(item)Koło wodne$(): płynąca woda w polu 3x3 daje 4 FE/t za blok, stojąca 1 FE/t (maks. 24 FE/t).",
      recipes=["generator", "windmill", "water_wheel"]),
    E("energy", "storage", "redcircuit:battery_box", "Batteries & Charger", "Baterie i ładowarka",
      "The $(item)Battery Box$() stores 1,000,000 FE. The portable $(item)Battery$() holds 50,000 FE and powers machines from their battery slot when grid power runs out.||"
      "In the battery box window the top slot charges a battery and the bottom slot discharges it. "
      "The $(item)Charger$() has 4 slots and charges batteries, electric tools and FE items from other mods (1000 FE/t per slot).",
      "$(item)Akumulator$() mieści 1 000 000 FE. Przenośna $(item)bateria$() mieści 50 000 FE i zasila maszyny ze slotu baterii, gdy brakuje prądu z sieci.||"
      "W oknie akumulatora górny slot ładuje baterię, a dolny ją rozładowuje. "
      "$(item)Ładowarka$() ma 4 sloty i ładuje baterie, narzędzia elektryczne i przedmioty FE innych modów (1000 FE/t na slot).",
      recipes=["battery_box", "battery", "charger"]),
    E("energy", "electric_furnace", "redcircuit:electric_furnace", "Electric Furnace", "Piec elektryczny",
      "Smelts like a furnace, twice as fast, using 20 FE/t. The fuel slot is replaced by a battery slot.",
      "Przetapia jak zwykły piec, 2x szybciej, zużywając 20 FE/t. Zamiast slotu paliwa ma slot baterii.",
      recipes=["electric_furnace"]),
    E("energy", "macerator", "redcircuit:macerator", "Macerator", "Macerator",
      "Grinds ores and raw ores into $(l)2 dusts$() (10% chance of a third) and ingots into 1 dust. Smelt the dusts into ingots. "
      "It also crushes cobblestone, gravel, bones, blaze rods, wool, glowstone and more.||"
      "Uses 16 FE/t and has a battery slot. Hoppers insert from any side and take the results out. "
      "Datapacks can add recipes of type $(item)redcircuit:macerating$().",
      "Miele rudy i surowe rudy na $(l)2 pyły$() (10% szans na trzeci), a sztabki na 1 pył. Pyły przetapia się na sztabki. "
      "Kruszy też bruk, żwir, kości, pręty płomienia, wełnę, jasnogłaz i inne.||"
      "Zużywa 16 FE/t i ma slot baterii. Lejki wkładają z każdej strony i wyjmują wyniki. "
      "Datapacki mogą dodawać receptury typu $(item)redcircuit:macerating$().",
      recipes=["macerator"], spot=["redcircuit:iron_dust"]),
    E("energy", "sawmill", "redcircuit:sawmill", "Sawmill", "Tartak",
      "Cuts logs and stems into $(l)6 planks$() instead of 4, always with a bit of $(item)sawdust$(), and planks into 3 sticks. "
      "Uses 12 FE/t and has a battery slot.\n\n"
      "Sawdust burns in furnaces, and 3 sawdust in a row make 2 paper. Datapacks can add recipes of type $(item)redcircuit:sawing$().",
      "Tnie kłody i trzony na $(l)6 desek$() zamiast 4, zawsze z odrobiną $(item)trocin$(), a deski na 3 patyki. "
      "Zużywa 12 FE/t i ma slot baterii.\n\n"
      "Trociny palą się w piecu, a 3 trociny w rzędzie dają 2 papiery. Datapacki mogą dodawać receptury typu $(item)redcircuit:sawing$().",
      recipes=["sawmill", "paper_from_sawdust"]),

    # ------------------------------------------------------------------ Płyny
    E("fluids", "pump", "redcircuit:pump", "Pump & Fluid Pipe", "Pompa i rura do płynów",
      "Place the $(item)Pump$() by clicking the fluid source (a tank or a lake): it sucks from the back and pushes to the front. "
      "It drains up to 256 source blocks of a lake. 200 FE per bucket; a redstone signal stops it.||"
      "$(item)Fluid Pipes$() hold 1000 mB each, level out among themselves and push fluid into every connected tank except the one it came from (250 mB/t per side).",
      "$(item)Pompę$() stawia się, klikając źródło płynu (zbiornik albo jezioro): zasysa z tyłu i tłoczy do przodu. "
      "Z jeziora wypompowuje do 256 bloków źródłowych. 200 FE za wiadro; sygnał redstone ją zatrzymuje.||"
      "$(item)Rury do płynów$() mieszczą po 1000 mB, wyrównują poziom między sobą i wypychają płyn do każdego podłączonego zbiornika poza tym, z którego przyszedł (250 mB/t na stronę).",
      recipes=["pump", "fluid_pipe"]),
    E("fluids", "tank", "redcircuit:fluid_tank", "Fluid Tank", "Zbiornik na ciecz",
      "Holds 16,000 mB of one fluid, visible through the glass. Use buckets on it; pipes and other mods connect on every side. "
      "Fluid flows down into a tank placed below. The tank keeps its contents when broken; comparators read the level.",
      "Mieści 16 000 mB jednej cieczy, widocznej przez szkło. Nalewa się i zabiera wiadrem; rury i inne mody łączą się z każdej strony. "
      "Ciecz sama spływa do zbiornika niżej. Rozbity zbiornik zachowuje zawartość; komparator czyta poziom.",
      recipes=["fluid_tank"]),

    # ------------------------------------------------------------------ Bezprzewodowe
    E("wireless", "wireless", "redcircuit:wireless_transmitter", "Transmitters & Receivers", "Nadajniki i odbiorniki",
      "Frequencies 1-5000 are shared by the whole world, across dimensions and unloaded chunks. Right-click to set the frequency.\n\n"
      "A powered $(item)Transmitter$() broadcasts; a $(item)Receiver$() outputs 15 on every side while its frequency is active.||"
      "The $(item)Wireless Remote$() transmits while you hold right-click; Shift-right-click sets its frequency.",
      "Częstotliwości 1-5000 są wspólne dla całego świata, także między wymiarami i w niezaładowanych chunkach. PPM ustawia częstotliwość.\n\n"
      "Zasilony $(item)nadajnik$() nadaje; $(item)odbiornik$() daje sygnał 15 na wszystkie strony, gdy jego częstotliwość jest aktywna.||"
      "$(item)Pilot bezprzewodowy$() nadaje, gdy trzymasz PPM; Shift + PPM ustawia częstotliwość.",
      recipes=["wireless_transmitter", "wireless_receiver", "wireless_remote"]),

    # ------------------------------------------------------------------ Narzędzia
    E("tools", "electric_tools", "redcircuit:electric_drill", "Electric Tools", "Narzędzia elektryczne",
      "The $(item)Electric Drill$() is a pickaxe and shovel in one (diamond level), using 50 FE per block instead of durability. "
      "Shift-right-click switches between 1x1 and 3x3 mining; hold Shift while mining for a single block.||"
      "The $(item)Electric Chainsaw$() cuts wood, leaves, wool and cobwebs. Both hold 100,000 FE; charge them in a Charger.",
      "$(item)Wiertarka elektryczna$() to kilof i łopata w jednym (poziom diamentu), zużywa 50 FE za blok zamiast wytrzymałości. "
      "Shift + PPM przełącza kopanie 1x1 i 3x3; trzymanie Shift podczas kopania rozbija jeden blok.||"
      "$(item)Piła łańcuchowa$() tnie drewno, liście, wełnę i pajęczyny. Obie mieszczą 100 000 FE; ładuj je w ładowarce.",
      recipes=["electric_drill", "electric_chainsaw"]),
    E("tools", "multimeter", "redcircuit:multimeter", "Multimeter", "Miernik",
      "Right-click a block to print its signal level (input, output or wire level), active bundled channels, stored energy and fluids.",
      "PPM na bloku wypisuje poziom sygnału (wejście, wyjście albo poziom przewodu), aktywne kanały kabla wiązkowego, energię i ciecze.",
      recipes=["multimeter"]),

    # ------------------------------------------------------------------ Integracje
    E("compat", "computercraft", "minecraft:command_block", "CC: Tweaked", "CC: Tweaked",
      "With CC: Tweaked installed, bundled cables connect to computers and turtles: $(item)redstone.setBundledOutput$() drives the cable channels and $(item)redstone.getBundledInput$() reads them.||"
      "Peripherals: $(item)redcircuit_gate$() (timer interval, counter, mute) and $(item)redcircuit_wireless$() (frequency, state). "
      "Machines, tanks and batteries appear as generic $(item)inventory$(), $(item)fluid_storage$() and $(item)energy_storage$() peripherals.",
      "Z CC: Tweaked kable wiązkowe łączą się z komputerami i żółwiami: $(item)redstone.setBundledOutput$() steruje kanałami kabla, a $(item)redstone.getBundledInput$() je odczytuje.||"
      "Peryferia: $(item)redcircuit_gate$() (czas zegara, licznik, wyciszenie) i $(item)redcircuit_wireless$() (częstotliwość, stan). "
      "Maszyny, zbiorniki i baterie są widoczne jako ogólne peryferia $(item)inventory$(), $(item)fluid_storage$() i $(item)energy_storage$()."),
    E("compat", "multipart", "minecraft:smooth_stone_slab", "CB Multipart", "CB Multipart",
      "With CB Multipart, face wires become parts of a multipart block: a wire and a microblock cover can share one block. "
      "Parts connect to normal wires, encased wires and each other, in the same signal, channel and energy networks.",
      "Z CB Multipart przewody na ścianach stają się częściami bloku wieloczęściowego: przewód i osłona z mikrobloku dzielą jeden blok. "
      "Części łączą się ze zwykłymi przewodami, przewodami w obudowie i ze sobą, w tych samych sieciach sygnałów, kanałów i energii."),
    E("compat", "modpacks", "minecraft:writable_book", "Modpacks", "Modpacki",
      "$(item)config/redcircuit-common.toml$() can disable each ore and change generator outputs and machine costs.\n\n"
      "Recipes use common $(item)c:$() tags, so tin, silver or brass from other mods work. Alloy recipes ($(item)redcircuit:alloying$()) can be edited with datapacks, KubeJS or CraftTweaker.||"
      "$(item)Jade$() shows signal levels, bundled channels, gate settings and generator output. $(item)JEI$() shows alloy recipes and info pages.",
      "$(item)config/redcircuit-common.toml$() pozwala wyłączyć każdą rudę i zmienić moc generatorów oraz koszty maszyn.\n\n"
      "Receptury używają wspólnych tagów $(item)c:$(), więc cyna, srebro czy mosiądz z innych modów działają. Receptury stopów ($(item)redcircuit:alloying$()) zmienisz datapackiem, KubeJS lub CraftTweakerem.||"
      "$(item)Jade$() pokazuje poziomy sygnału, kanały kabla, ustawienia bramek i moc generatorów. $(item)JEI$() pokazuje receptury stopów i strony z opisami."),
]


# ---------------------------------------------------------------------------------------------

def _plain_len(s):
    return len(re.sub(r"\$\([^)]*\)", "", s))


def _split(chunk):
    """Dzieli tekst strony na kilka, gdy jest za długi (na granicy akapitów, potem zdań)."""
    if _plain_len(chunk) <= PAGE_LIMIT:
        return [chunk]
    parts = re.split(r"(?<=[.!?])\s+|(?=\n\n)|(?=\$\(li\))", chunk)
    pages, cur = [], ""
    for p in parts:
        if not p:
            continue
        cand = (cur + " " + p).strip() if cur else p.strip()
        if cur and _plain_len(cand) > PAGE_LIMIT:
            pages.append(cur.strip())
            cur = p.strip()
        else:
            cur = cand
    if cur:
        pages.append(cur.strip())
    return pages


def _fmt(text):
    return text.replace("\n\n", "$(br2)").replace("\n", "$(br)")


def gen_patchouli(root, write_json):
    res = os.path.join(root, "src", "main", "resources")
    data = os.path.join(res, "data", "redcircuit")
    assets = os.path.join(res, "assets", "redcircuit")
    recipe_dir = os.path.join(data, "recipe")

    write_json(os.path.join(data, "patchouli_books", "guide", "book.json"), {
        "name": "book.redcircuit.guide.name",
        "landing_text": "book.redcircuit.guide.landing",
        "subtitle": "book.redcircuit.guide.subtitle",
        "version": 1,
        "use_resource_pack": True,
        "creative_tab": "redcircuit:tools",
        "book_texture": "patchouli:textures/gui/book_red.png",
        "model": "patchouli:book_red",
        "show_progress": False,
        "i18n": False,
        "macros": {"$(item)": "$(#b0102a)"},
    })

    # Receptura: książka + redstone (tylko z Patchouli)
    write_json(os.path.join(recipe_dir, "guide_book.json"), {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "patchouli"}],
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [{"item": "minecraft:book"}, {"tag": "c:dusts/redstone"}],
        "result": {"id": "patchouli:guide_book", "count": 1, "components": {"patchouli:book": "redcircuit:guide"}},
    })

    for li, lang in enumerate(("en_us", "pl_pl")):
        base = os.path.join(assets, "patchouli_books", "guide", lang)
        for n, (cid, icon, en, pl, den, dpl) in enumerate(CATEGORIES):
            write_json(os.path.join(base, "categories", f"{cid}.json"), {
                "name": (en, pl)[li], "description": (den, dpl)[li], "icon": icon, "sortnum": n})
        for n, e in enumerate(ENTRIES):
            pages = []
            for chunk in e["text"][li].split("||"):
                for t in _split(chunk):
                    pages.append({"type": "patchouli:text", "text": _fmt(t)})
            rec = [r for r in e["recipes"] if os.path.exists(os.path.join(recipe_dir, f"{r}.json"))]
            missing = [r for r in e["recipes"] if r not in rec]
            if missing:
                print("  [patchouli] brak receptur:", missing)
            for i in range(0, len(rec), 2):
                page = {"type": "patchouli:crafting", "recipe": M + rec[i]}
                if i + 1 < len(rec):
                    page["recipe2"] = M + rec[i + 1]
                pages.append(page)
            for s in e["spot"]:
                pages.append({"type": "patchouli:spotlight", "item": s})
            write_json(os.path.join(base, "entries", e["cat"], f"{e['id']}.json"), {
                "name": e["name"][li], "icon": e["icon"], "category": f"redcircuit:{e['cat']}",
                "sortnum": n, "pages": pages})

    texts = {
        "en_us": {"book.redcircuit.guide.name": "RedCircuit Manual",
                  "book.redcircuit.guide.subtitle": "Wires, gates, tubes & Azurity",
                  "book.redcircuit.guide.landing": "Welcome, engineer! This manual describes every block and machine of $(l)RedCircuit$(). "
                                                   "Pick a category, or start with $(l:basics/introduction)Getting Started$(/l)."},
        "pl_pl": {"book.redcircuit.guide.name": "Podręcznik RedCircuit",
                  "book.redcircuit.guide.subtitle": "Przewody, bramki, rury i Azurity",
                  "book.redcircuit.guide.landing": "Witaj, inżynierze! Ten podręcznik opisuje wszystkie bloki i maszyny $(l)RedCircuit$(). "
                                                   "Wybierz kategorię albo zacznij od $(l:basics/introduction)Pierwszych kroków$(/l)."},
    }
    for lang, kv in texts.items():
        path = os.path.join(assets, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
        d.update(kv)
        write_json(path, d)
