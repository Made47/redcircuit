# RedCircuit (NeoForge 26.3)

*[Polska wersja: README.pl.md](README.pl.md)*

Lossless redstone wiring, logic gates, pneumatic tubes with sorting and Azurity power. All code and art are original.
Includes redstone cables, logic gates, sorting tubes and Azurity energy.

> **Made with AI.** This mod was created with the help of artificial intelligence (Claude by Anthropic).
> The source code, the texture-generating scripts, translations and this documentation were largely written by AI,
> under the direction of the author, who designed, reviewed and play-tested the mod. Expect occasional bugs – reports are welcome.

## Minecraft 26.3 notes
- Requires **Java 25** (bundled with the Minecraft 26.3 launcher).
- Items, energy and fluids use the new NeoForge transfer API (`ResourceHandler`, `EnergyHandler`) – compatible with other 26.x mods; machine inventories and tanks saved by the 1.21.1 version are read correctly.
- **CB Multipart** has no release for Minecraft 26.x, so wires as multipart parts are not available in this version.
- Recipes use the 1.21.2+ JSON format (`"key": {"A": "minecraft:iron_ingot"}`, tags as `"#c:ingots/tin"`; alloy furnace ingredients as `{"ingredient": "...", "count": n}`).
- The config file is now `config/redcircuit-local.toml` (NeoForge 26.3 replaced the "common" config type with "local"); settings from `redcircuit-common.toml` must be copied over by hand.
- Furnace fuel uses the 26.3 `minecraft:cooking_fuel` item component (fuel from datapacks and other mods works automatically).

## Building the `.jar`

### Option A – on your own computer (Windows)
1. Install **JDK 25**, e.g. Temurin: https://adoptium.net (tick “Set JAVA_HOME”).
2. Unpack the project, open a terminal (PowerShell) in its folder and run:
   ```
   .\gradlew.bat build
   ```
   The first build takes a few minutes because it downloads Minecraft and NeoForge.
3. The finished file: `build\libs\redcircuit-0.1.0.jar` → put it into `mods/` (NeoForge 26.3.0.97 or newer for MC 26.3).

To test in game without installing: `.\gradlew.bat runClient`.

### Option B – GitHub Actions (nothing to install)
Push the project to a GitHub repository. The workflow `.github/workflows/build.yml`
builds the mod on every push, and the `.jar` appears under **Actions → (latest run) → Artifacts**.

## Content

### Ores
Every ore also has a deepslate variant and generates in the Overworld. Fortune and Silk Touch work.

| Ore | Y level | Pickaxe | Drops |
|---|---|---|---|
| Tin | 0 – 80 | stone | raw tin → tin ingot |
| Silver | -48 – 32 | iron | raw silver → silver ingot |
| Azurite | -64 – 16 | iron | 4–5 azurite (like redstone) |

Raw materials and ores are smelted in a furnace or blast furnace. **Power alloy** is made from a silver ingot + 4 azurite, only in the alloy furnace.
Items have `c:` tags (e.g. `c:ingots/tin`), so they work in other mods' recipes.

### Blueprint table and plans
- **Blueprint Table** – a crafting table that remembers its 3×3 grid (items stay after closing) and has 18 storage slots. When crafting, ingredients are taken **from the storage first**, so the grid acts as a pattern and you can craft repeatedly. Shift+click on the result crafts up to 64 times.
- **Blank Plan** (paper + blue dye) – put it into the slot on the left, lay out a recipe and click the ✎ button to save a **plan**. The plan shows its result and ingredients in the tooltip.
- A saved plan in the slot + the ✎ button = load the plan: empty grid slots are filled from the storage.

### Alloy furnaces
- **Alloy Furnace** (ring of 8 brick blocks) – 9 ingredient slots, a fuel slot (like a normal furnace) and an output. Ingredients may sit in any slots.
- **Power Alloy Furnace** – the same, but runs on power (25 FE/t) and is 2× faster. Recipe: 5 power alloy ingots, an alloy furnace, 2 iron ingots and a power alloy wire.
- Alloys: **copper + 4 redstone → signal alloy**, **silver + 4 azurite → power alloy**, **3 copper + 1 tin → 4 brass ingots**, **iron + 2 coal (or charcoal) → steel** (2× longer).
- Automation: top – ingredients, sides – fuel (ingredients too in the electric version), bottom – output.
- Add your own recipes in a datapack, type `redcircuit:alloying` (example: `data/redcircuit/recipe/alloying_signal_alloy_ingot.json`).

### Handsaws
- Stone, copper, gold, iron, silver and diamond: **2 material + stick in a row**. Netherite: diamond upgrade on the smithing table.
- The saw stays in the grid after crafting and loses 1 durability (repair it on an anvil with its material).
- Saws are an ingredient of the **sawmill**.

### Gears
- **Stone:** 4 cobblestone/blackstone in a plus shape (empty centre).
- **Copper, gold, iron, brass, silver, diamond:** 4 of the material around a stone gear.
- **Netherite:** diamond upgrade on the smithing table.
- Tags `c:gears` / `c:gears/<material>` – work in other mods' recipes.

### Circuits and machine casings
- **Power Circuit** (formerly motor): **iron in the corners, power alloy on top, 2 copper coils on the sides, an iron gear in the middle, a power alloy wire at the bottom**. Ingredient of electric tools (drill, chainsaw) and the power machine casing.
- **Power Machine Casing** (block): iron in the corners, 4 brass ingots, a power circuit in the middle. The body of powered machines – replaces the circuit in the recipes of: electric furnace, power alloy furnace, (rotor) generator, pump, Requester, accelerator, macerator and sawmill (same grid position the motor used to be).
- **Signal Circuit**: iron in the corners, signal alloy on top, 2 redstone torches and a circuit plate in the middle row, a signal alloy wire at the bottom.
- **Signal Machine Casing** (block): iron in the corners, 4 tin ingots, a signal circuit in the middle. The body of redstone-controlled machines – replaces the signal wire in the recipes of: extractor (and therefore filter and sorting machine), regulator, relay, ejector, item detector, igniter, deployer and block breaker, and the circuit plate in the assembler.

### Battery
- Portable Azurity storage: **50 000 FE**, charge bar under the icon and status in the tooltip.
- **Right-click a battery box** to charge the battery from it, **Shift + right-click** to empty its energy into the battery box. The battery box charges FE items from other mods the same way.
- **Battery slot in devices**: electric furnace (in place of the fuel slot) and power alloy furnace (under the energy bar). The battery powers the device when it lacks grid power (up to 400/500 FE/t).
- **Battery box GUI** (right-click): the top slot charges a battery from the box, the bottom one discharges it into the box (1000 FE/t), energy bar and counter. Automation: insert from the top to charge, from the sides to discharge, extract charged/empty batteries from the bottom.
- The battery has the item FE capability, so it also works with other mods' chargers (up to 1000 FE/t).
- The creative tab has an empty and a charged version. Recipe: copper on top, 4 tin ingots on the sides, 2 azurite in the middle column.

### Charger, electric tools and multimeter
- **Charger** – 4 slots, charges batteries, electric tools and other mods' FE items (1000 FE/t per slot, 50 000 FE buffer). GUI with an energy bar; hoppers insert from the top/sides and extract charged items from the bottom. Lights up while charging.
- **Electric Drill** – pickaxe + shovel (diamond tier, speed 10), **50 FE per block** instead of durability (100 000 FE). Mines very slowly without power. Can be enchanted (Efficiency, Fortune, Silk Touch). **1x1 / 3x3 modes** – Shift + right-click toggles; in 3x3 mode it breaks 3×3 blocks in the plane of the face you're looking at (slower, 50 FE per block, skips blocks much harder than the centre one); holding Shift while mining breaks a single block.
- **Chainsaw** – an axe that also cuts leaves, wool and cobwebs, strong attack; 50 FE per block.
- **Multimeter** – right-click a block to print in chat: signal (input/output or wire level), bundled cable channels, energy (FE) and fluids.
- Recipes: charger = iron, power alloy, battery, power wire; drill = 3 diamond pickaxes on top, iron, power circuit, battery; chainsaw = 3 diamond axes on top, iron, power circuit, battery; multimeter = comparator + both wires + glass pane + iron.

### Azurity generators
- **Thermopile** – every pair of “hot” (lava, fire, magma block, lit campfire) + “cold” (water, ice, snow block, snow layer) neighbours gives **8 FE/t** (max. 3 pairs = 24 FE/t). Recipe: copper in the corners, tin on the sides, a power alloy wire in the middle.
- **Solid Fuel Generator** – burns anything a furnace burns (coal, wood, lava bucket…) and produces **20 FE/t** while burning (coal = 32 000 FE, lava bucket = 400 000 FE). 20 000 FE buffer, outputs to neighbours (400 FE/t per side). Only uses fuel when there is room in the buffer; a redstone signal pauses it. GUI: fuel slot, flame with remaining time, energy bar and current output. Hoppers: fuel from the top/sides, empty buckets from the bottom. A comparator reads the charge. Recipe: iron (top and corners), 2 copper coils on the sides, a furnace in the middle, a power alloy wire at the bottom. Output in the config: `[generators] solid_fuel_generator`.
- **Generator** – produces nothing by itself; a rotor is mounted on its front (right-click with a rotor, Shift+right-click with an empty hand removes it):
  - **Windmill** (wool, sticks, iron gear) – turns in a 5×5 area in front of the generator, which must be empty and open to the sky. **6 FE/t at sea level, up to 32 FE/t high up**, ×1.5 in rain, ×2 in thunderstorms.
  - **Water Wheel** (planks, sticks, iron gear) – 3×3 area in front of the generator: each **flowing water block gives 4 FE/t**, still water 1 FE/t (max. 24 FE/t). Other blocks in the area block the wheel.
- Right-clicking the generator shows current output and the buffer.

### Pump
- Placed by **clicking the fluid source** (another mod's tank or a lake/pond): it sucks from the back and pushes to the front.
- From a lake it pumps source blocks (up to 256 blocks of connected fluid, from the top). At the front it gives the fluid to another mod's tank/pipe, and if there's nothing there – places a fluid block.
- 8000 mB tank, **200 FE per bucket** (1000 mB), one bucket every half second. A redstone signal stops the pump. Right-click shows its contents.
- Recipe: iron in the corners, a bucket on top, 2 brass gears, a power machine casing in the middle, a power alloy wire at the bottom.

### Fluid pipe
- A brass pipe that connects to the pump, other pipes and any tank/machine handling fluids (also from other mods).
- Each pipe holds 1000 mB. Pipes level out between each other and push fluid into connected tanks – skipping the side it came from (it doesn't flow back to the source). Up to 250 mB/t per side.
- Right-click shows the contents. Recipe: brass + glass + brass → 8 pipes.

### Screwdriver
- Right-click a block to **rotate it**: logic gates, machines, furnaces, generators, pumps, extractors and directional vanilla blocks (repeaters, observers, hoppers, logs…). **Shift + right-click** rotates the other way.
- Acts before the block's own interaction – it doesn't open GUIs or change the timer. Doesn't rotate beds, doors, double chests or extended pistons.
- **Shift + right-click on a logic gate** picks it up together with its settings (timer/sequencer/state cell interval, state cell output side, counter maximum, mute). Placed again, it keeps the same settings. A gate with default settings stacks with normal ones.
- Doesn't wear out (no durability). Recipe: iron ingot above a stick (diagonally).

### Wire Cutters
- A crafting tool – like saws, it stays in the grid and loses 1 durability (256, shown in the tooltip). Repaired with a steel ingot.
- Recipe: 3 steel ingots in an X shape (two on top, one in the middle) and 2 red dyes at the bottom (handles).
- Copper ingot + wire cutters → **fine copper wire**; iron ingot + wire cutters → **fine iron wire**.
- **Copper Coil**: 4 fine copper wires in a cross around an iron ingot. Ingredient of the power circuit.

### Restriction tube and Accelerator
- **Restriction Tube** – a normal tube, but routing treats it as very long (cost 1000 instead of 1), so items use it only when there is no other way. Routes are now the cheapest (Dijkstra's algorithm), not just the shortest. Recipe: tube + iron ingot; on its own in the grid it gives the tube back. Can be painted like a normal tube.
- **Accelerator** – a powered tube segment (power wire). Every passing stack speeds up to **4×** (0.5 blocks/tick), then slows down by 0.02 on each following tube. Cost: 20 FE per stack (`accelerator_per_item` in the config). Right-click shows energy, Shift + right-click with an empty hand removes paint. Recipe: iron in the corners, tubes top/bottom, power wires on the sides, a power circuit in the middle.

### Requester
- Placed by **clicking a tube**: the back connects to the tube network, the front is the output (inventory, another tube or the ground).
- On a **redstone pulse** it searches all inventories connected to that network, pulls an item matching the filter and sends it through the tubes **to itself** – the item travels only to the Requester that ordered it, then leaves from the front.
- Filter: 9 slots (right-click), used in order; the stack size in a slot = how many to pull. Empty filter = a full stack of any item.
- Power: **200 FE per request** (10 000 FE buffer), Shift + right-click shows energy. Recipe: iron in the corners, a filter on top, 2 gold ingots, a power machine casing in the middle, a power alloy wire at the bottom.

### Logistics machines
| Block | Function |
|---|---|
| **Manager** | Placed on the inventory it manages, front in the tube network. Filter = list of items with amounts: missing ones are ordered from the network, items not on the list are sent back to the network. 100 FE per order. |
| **Regulator** | Moves items from the inventory behind into the inventory in front until it contains the filter amounts; when everything is stocked – redstone signal. |
| **Relay** | 9 slots, accepts from every side except the front and constantly pushes out the front. |
| **Item Detector** | In a tube line (back → front); every passing stack gives a redstone pulse. |
| **Igniter** | Powered – lights and keeps a fire in front of itself; without a signal it extinguishes it. |
| **Assembler** | Bundled cable: channel N on → places the block from slot N, off → picks it back up. Plain pulse: places the next / picks up. |
| **Ejector** | 9 slots; a redstone pulse pushes one stack out of the front (slots in order). |
| **Deployer** | 9 slots; a redstone pulse = right-click with an item on the block in front (placing, bone meal, buckets, shears…). |
| **Buffer** | 18 slots accessible from every side; a comparator reads the fill level. |
| **Block Breaker** | A redstone pulse breaks the block in front (like a diamond pickaxe), drops come out of the back. |

Directional machines are placed like the extractor – by clicking the block that should be “behind” them; the screwdriver rotates them.

### Modpacks
**Config file** `config/redcircuit-common.toml` (created on first launch):
- `[worldgen]` – `generate_tin`, `generate_silver`, `generate_azurite` – disable an ore (e.g. when another mod already provides tin). Takes effect after a world/server restart.
- `[generators]` – FE/t: solid fuel generator, solar panel, thermopile (per pair), windmill (minimum/maximum), water wheel (flowing/still water, maximum).
- `[machines]` – consumption: electric furnace, power alloy furnace (FE/t), pump (FE/bucket), Requester and Manager (FE per operation).

**Common tags** – recipes use `c:` tags instead of specific items (`#c:ingots/tin`, `#c:ingots/silver`, `#c:ingots/copper`, `#c:dusts/redstone`, `#c:gems/diamond`, `#c:glass_blocks`, `#c:chests/wooden`, `#c:dyes/...` etc.), so tin, silver or brass from other mods (Mekanism, Thermal, Create…) work in recipes and alloy furnaces. The mod also provides its own tags: `c:ingots/signal_alloy`, `c:ingots/power_alloy`, `c:dusts/azurite`, `c:gears/*`, `c:wires/copper|iron`, plus ores, raw materials, nuggets and storage blocks.

**KubeJS / CraftTweaker** – alloy furnace recipes are a regular `redcircuit:alloying` type (JSON in a datapack), so they can be added and removed with scripts. Ready examples are in `examples/`:
- `examples/kubejs/server_scripts/redcircuit_alloying.js` – `event.custom({type: 'redcircuit:alloying', ...})`, `event.remove(...)`,
- `examples/crafttweaker/scripts/redcircuit_alloying.zs` – `<recipetype:redcircuit:alloying>.addJsonRecipe(...)`, `removeByName(...)`.

Recipe format:
```json
{
  "type": "redcircuit:alloying",
  "ingredients": [ {"tag": "c:ingots/silver"}, {"tag": "c:dusts/azurite", "count": 4} ],
  "result": {"id": "redcircuit:power_alloy_ingot", "count": 1},
  "cooktime": 200
}
```

### Jade integration (optional)
With **Jade** installed, the block tooltip additionally shows:
- wires: signal level; bundled cable: active channels (coloured squares with levels),
- gates: output state, set interval (timer, sequencer, state cell), counter value, sound mute,
- lamps: lit / off, inverted,
- generators: current FE/t output; tubes: number of items inside; furnaces: progress and remaining fuel.
Energy (FE), fluids and inventory contents are shown by Jade automatically. Each line can be disabled in the Jade settings (RedCircuit plugin). For compilation Gradle downloads Jade from the Modrinth repository (`compileOnly` only).

### CB Multipart integration (optional)
**Not available for Minecraft 26.x** – CB Multipart has no 26.x release. The integration exists in the `neoforge-1.21.1` branch.

### Gates with memory and timing
| Gate | Function |
|---|---|
| **Counter** (CNT) | Left input (edge) +1, right −1. The front lights at the maximum, the back at zero. Right-click opens a window: maximum (1–1024) and reset. A comparator reads the counter value. |
| **Sequencer** (SEQ) | The signal rotates around the sides: front → right → back → left, one step per set interval (settings window, right-click). A signal at the back pauses it. |
| **State Cell** (SC) | A pulse at the back starts a countdown; the right side is lit while counting (can be switched to the left in the settings window), after the time the front gives a pulse. Right-click opens the time and side settings. |
| **Synchronizer** (SYN) | Waits for a pulse from the left and from the right (in any order), then pulses the front. The back resets. |

**Gate settings window** (right-click on a timer, sequencer, state cell or counter): time entered in seconds (0.20 s – 1 h, with ±0.1 s / ±1 s buttons; also shows ticks), and for the counter the maximum (1–1024, ±1 / ±10 buttons), the current value and a reset button. Settings are stored in the gate and synced with the server. Timer, sequencer, state cell and counter **tick** with a quiet click (sound event `redcircuit:gate_tick`, with subtitles); the “Sound: on/muted” button in the window mutes a specific gate.

### Wireless redstone
Frequencies **1–5000**, shared by the whole world (all dimensions, also unloaded chunks).
- **Wireless Transmitter** (TX) – a plate with an antenna; right-click sets the frequency; transmits while it receives a redstone signal.
- **Wireless Receiver** (RX) – right-click sets the frequency; outputs signal 15 on all sides (and strong power into the block below) when a transmitter or remote is transmitting on its frequency. The antenna tip glows while active.
- **Wireless Remote** – holding right-click transmits (like a held button), Shift + right-click sets the frequency; the frequency is shown in the tooltip.
- Frequency window: number field and ±1 / ±10 / ±100 buttons. Jade shows the frequency and state.
- Recipes: circuit plate + signal alloy wire + ender pearl + gold + redstone torch (transmitter) / comparator (receiver); remote = transmitter + button + iron.

### Lamps
- **Lamps in 16 colours** – light up (level 15) with a redstone signal, also from adjacent RedCircuit wires; a lit lamp has a bright, “glowing” shade.
- **Inverted lamps** – lit without a signal, off with a signal. Lamp + redstone torch turns a normal lamp into an inverted one (and back).
- Turning off is delayed by 4 ticks (like the vanilla lamp), so short pulses don't cause flicker.
- Recipe: glass panes on the sides, glowstone dust on top, dye in the middle, redstone at the bottom.

### Fluid tank
- Holds **16 000 mB** (16 buckets) of one fluid; the fluid is visible through the glass walls (level depends on fill).
- Right-click with a bucket (or another fluid container) fills/drains, with an empty hand shows the contents.
- Pipes, pumps and other mods' machines connect from every side. Fluid flows down into a tank placed below (tank towers).
- **Keeps its contents** when broken (shown in the item tooltip). A comparator reads the level, lava in the tank emits light.
- Recipe: iron in the corners, glass on the sides, empty middle.

### Cables
| Block | Description |
|---|---|
| Signal alloy wire | Carries redstone **without signal loss** over any distance. Connects to every insulated wire. |
| Insulated wire (16 colours) | Connects only to the same colour, to signal alloy and to bundled cable. |
| Bundled cable | 16 separate channels. An insulated wire of colour X attached to the cable sends/receives channel X. |

Wires **stick to surfaces**: place them by clicking a floor, wall or ceiling.
They connect straight, around inner corners (several wires of the same type in one block, e.g. floor + wall)
and outer corners (they wrap around a block edge). Breaking a block with several wires removes the one you're aiming at.
Limitation: one block space can hold only one wire type/colour, and wires in the same block are always electrically connected.

**Encased wires** – every wire type (signal alloy, 16 insulated, bundled cable, power alloy) has a hard-cased version that runs **through the middle of the block like a pipe**. It works like the normal wire of that type and connects both to other encased wires and to face-mounted wires (on the same side). Red/insulated encased wires also connect to redstone devices, power ones – to powered machines. Recipe: 8 wires around a smooth stone slab → 8 encased; an encased wire alone in the grid → normal wire.

### Logic gates
Flat gates placed on the floor. Output = the direction you face when placing.
Inputs: **L** (left), **B** (back), **R** (right). Wires connect only to sides that are inputs or outputs of the gate.

| Gate | Logic |
|---|---|
| NOT | !B goes out of the front, left and right; side outputs can be disabled in the window |
| Buffer | B goes out of the front, left and right; side outputs can be disabled in the window |
| AND | L and B and R; back input disabled by default |
| NAND | not (L and B and R); back input disabled by default |
| OR / NOR | L or B or R |
| XOR / XNOR | L xor R |
| RS latch | L sets, R resets; complementary outputs: front+left (state 1) / back+right (state 0) |
| Toggle (T flip-flop) | a pulse on L or R toggles the output front ↔ back (one is always lit); right-click toggles manually, Shift+right-click opens the window |
| Pulse former | edge on B → 2-tick pulse |
| Timer | ticks while B is off; the pulse goes out of the front, left and right; **right-click opens the settings window** (period 0.20 s – 1 h) |
| Randomizer | while B is on, every 4 ticks the front, left and right get independent random states |

**Right-click on any gate** opens a window: description, live preview of inputs and outputs, and options:
- disabling individual inputs (AND, NAND, OR, NOR, XOR, XNOR, T flip-flop) and the side outputs of buffer and NOT – the texture loses the nub, wires disconnect and a disabled input doesn't affect the result,
- RS latch: swapped sides (right sets, left resets) and “front/back only” mode (sides don't light; both inputs at once = no change),
- state cell: output side during the countdown; time / counter maximum; tick sound.
Settings stay in the item after picking the gate up with a screwdriver (Shift + right-click).

### Tubes and sorting
- **Pneumatic Tube** – items find the nearest inventory that accepts them.
  Right-click with a dye paints the tube (it then accepts only items of that colour), Shift+right-click with an empty hand removes paint.
  Hoppers and other mods can insert items into tubes.
- **Extractor** – place it by **clicking the chest** it should pull from. Redstone pulse → pulls a stack and pushes it out of the front (tube / inventory / ground).
- **Filter** – like the extractor, but only pulls items matching its 9 pattern slots (right-click). Empty filter = everything.
- **Sorting Machine** – right-click opens 9 columns × 3 rows:
  row 1 = dye (colour given to items), rows 2–3 = item patterns.
  A column with a dye but no patterns catches everything else. Coloured items travel only through tubes of their colour or unpainted ones.

### Azurity
Azurity is **Forge Energy (FE)**, so it also works with other mods' machines and cables.
- **Power alloy wire** – attached to faces like redstone wires, transfers energy (500 FE/t), right-click shows its state.
- **Solar Panel** – 10 FE/t during the day under open sky (less in rain).
- **Battery Box** – 1 000 000 FE.
- **Macerator** – grinds ores and raw ores into **2 dusts** (10% chance of a third), ingots into 1 dust; dusts (iron, gold, copper, tin, silver) smelt into ingots. Also crushes cobblestone→gravel→sand (+flint), stone, sandstone, glowstone, bones, blaze rods, wool, sugar cane and coal, redstone, lapis, quartz and azurite ores. 16 FE/t (`macerator_per_tick`), battery slot, main and secondary output, hoppers from every side. Recipe: 3 flint, 2 iron gears, power machine casing, iron, power alloy wire. Custom recipes: type `redcircuit:macerating` (`ingredient`, `result`, optional `bonus` + `bonus_chance`, `time`), shown in JEI.
- **Sawmill** – cuts logs and stems (all vanilla woods, bamboo too) into **6 planks** instead of 4 + sawdust, and planks into 3 sticks (25% chance of sawdust). 12 FE/t (`sawmill_per_tick`), battery slot, hoppers from every side. **Sawdust** burns in a furnace (5 s) and makes paper (3 in a row → 2). Recipe: planks, handsaw, 2 iron gears, power machine casing, iron, power alloy wire. Custom recipes: type `redcircuit:sawing` (like the macerator), shown in JEI.
- **Electric Furnace** – smelts like a furnace, 2× faster, uses 20 FE/t.

## Recipes (summary)
- Signal alloy and power alloy ingots are made **only in the alloy furnace** (normal or electric): copper + 4 redstone, silver + 4 azurite.
- Signal alloy wire: 3 ingots in a row → 12. Power alloy wire: the same.
- Insulated wire: wool + 4 signal alloy wires → 4.
- Bundled cable: 5 insulated wires + 4 string (X) → 2.
- Circuit plate: smooth stone slab + redstone → 2. Gates = plate + torches / wires / lever / repeater / clock / quartz.
- Tube: copper, glass, copper → 8.

Full list: `src/main/resources/data/redcircuit/recipe/`. The easiest way to browse them in game is JEI/EMI.

## JEI integration
With **JEI (Just Enough Items)** for NeoForge 26.3 installed you get:
- a separate **Alloy Furnace** category with all alloy recipes (datapack ones too),
- alloy furnaces, the electric furnace and the blueprint table as crafting stations,
- descriptions (“Information” tab) for generators, rotors, tubes, the sorting machine, the blueprint table and saws.

The mod also works without JEI. With `gradlew runClient` JEI starts automatically.

## Development
Textures, models, recipes and translations are generated by `tools/gen_assets.py` (Python 3 + Pillow):
```
python tools/gen_assets.py
```


## CC: Tweaked integration (optional)
Works automatically when CC: Tweaked is installed (tested with CC: Tweaked 1.120.x for 26.3).
- **Bundled cable ↔ computers and turtles**: the cable (normal, encased and as a CB Multipart part) visually connects to a computer. `redstone.setBundledOutput` powers cable channels, and `redstone.getBundledInput` reads the channels active in the network (channel = insulated wire colour, as in `colors`). Also works with the Redstone Relay.
- **`redcircuit_gate` peripheral** (gates with settings): `getGateType()`, `getState()`, `getInterval()/setInterval(ticks)`, `getCount()`, `getMax()/setMax(n)`, `resetCount()`, `isMuted()/setMuted(bool)`, `isMirrored()/setMirrored(bool)` (state cell: “counting” output on the left).
- **`redcircuit_wireless` peripheral** (transmitter/receiver): `getFrequency()/setFrequency(n)`, `isTransmitter()`, `isPowered()`, `isChannelActive([freq])`.
- Machines, batteries, tanks etc. are available as generic CC peripherals (`inventory`, `energy_storage`, `fluid_storage`) – e.g. `peripheral.wrap("left").getEnergy()`.
- Example program: `examples/computercraft/redcircuit_demo.lua`.

## Patchouli guide book (optional)
With **Patchouli** installed there is a **RedCircuit Guide** book in game (EN/PL, depending on the game language):
- recipe: **book + redstone**; it is also in the creative tab “RedCircuit: Tools & Crafting”,
- 10 categories: Getting Started, Wiring, Logic, Logistics, Machines, Azurity, Fluids, Wireless Redstone, Tools, Other Mods,
- recipe pages are taken from the game (datapack changes show up immediately).
The content is generated by `tools/gen_patchouli.py` (called from `gen_assets.py`): files in `data/redcircuit/patchouli_books/guide/` and `assets/redcircuit/patchouli_books/guide/<language>/`. Without Patchouli the mod works normally and the book recipe is not loaded.

## Advancements
A **RedCircuit** advancement tab (30 entries, EN/PL) with a smooth stone background. It starts when you find the first ore from the mod, then branches into:
- **alloys and wiring** – alloy furnace → signal alloy → wire → insulated → bundled cable; challenge “Full Spectrum” (16 wire colours, 50 XP),
- **logic** – first gate, timer, counter, lamp, wireless redstone; challenge “Integrated Circuit” (all 17 gates, 100 XP),
- **Azurity** – power alloy, generator, windmill/water wheel, battery box, “Fully Charged” (50 000 FE battery), power circuit and casing, drill, pump, tank,
- **tubes** – tube, sorting machine, Requester, accelerator,
- **tools** – wire cutters, blueprint table, screwdriver.
Generator: `tools/gen_advancements.py` (also adds the `redcircuit:gates` and `lamps` tags).

## Creative tabs
Items are split into 5 tabs (placed after all Minecraft tabs):
- **RedCircuit: Redstone** – signal, insulated and bundled wires (also encased), gates, lamps, wireless transmitter and receiver,
- **Logistics & Machines** – tubes, accelerator, extractor, filter, sorting, Requester, Manager, Regulator, Relay, Buffer, detector, machines (deployer, ejector, assembler, block breaker, igniter),
- **Energy & Fluids** – power wires, generators (incl. solid fuel), windmill and water wheel, battery box, batteries (empty and charged), charger, electric furnaces, macerator, sawmill, pump, pipes and tank,
- **Tools & Crafting** – screwdriver, wire cutters, saws, multimeter, drill and chainsaw (empty and charged), remote, alloy furnace, blueprint table, plans, Patchouli book,
- **Materials & Blocks** – ores, raw materials, ingots, nuggets, dusts, blocks, gears, wires, coil, circuit plate, circuits and machine casings.
The split is in `ModTabs.categoryOf()` (by item ID); items without a category go to “Materials & Blocks”.
