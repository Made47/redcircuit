# RedCircuit (Minecraft 1.7.10, Forge 10.13.4.1614)

*[Polska wersja: README.pl.md](README.pl.md)*

A port of the RedCircuit mod to 1.7.10, built in stages (plan: the "Stages" section below).
Build template: [GTNH ExampleMod1.7.10](https://github.com/GTNewHorizons/ExampleMod1.7.10) (RetroFuturaGradle).

> **Made with AI.** This mod was created with the help of artificial intelligence (Claude by Anthropic).
> The source code, the texture-generating scripts, translations and this documentation were largely written by AI,
> under the direction of the author, who designed, reviewed and play-tested the mod. Expect occasional bugs – reports are welcome.

## Building
1. Install **JDK 25** (e.g. Temurin 25: https://adoptium.net) – required by the GTNH 2.x plugin. The mod itself is still compiled for Java 8.
   If another Java version should stay the default, add to `gradle.properties` (example path):
   `org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot`
2. The project must be a git repository with at least one commit (the mod version is taken from git tags, e.g. `0.1.0`).
3. `gradlew setupDecompWorkspace` (first time only), then `gradlew build` or `gradlew runClient`.
4. The finished jar: `build/libs/`.

## Stage 1-2 (done)
- Ores: copper, tin, silver, azurite (azurite drops 4-5 dust, like redstone). Config: `config/redcircuit.cfg`.
- Ingots: copper, tin, silver, brass, steel, signal alloy, power alloy; nuggets, dusts, gears, storage blocks.
- OreDictionary (`ingotCopper`, `dustAzurite`, `gearIron`...) – works with other mods.
- **Alloy Furnace** (8 bricks): 3x3 grid, any fuel, hoppers (top: ingredients, sides: fuel, bottom: output).
  - 3 copper + 1 tin → 4 brass; 1 copper + 4 redstone → signal alloy;
  - 1 silver + 4 azurite → power alloy; 1 iron + 2 coal → steel.

## Stage 3 (done)
- **Signal alloy wire** (3 alloy ingots → 12): no signal loss – the whole network has the level of its strongest input.
- **Insulated wires** in 16 colours (wool + 4 wires → 4) – connect only to the same colour and to signal alloy.
- **Bundled cable** (string + insulated wires → 2): 16 channels, wire colour = channel.
- Wires stick to any face; several in one block connect around inner corners, they also connect around outer edges.
  Breaking removes only the wire you're aiming at.
- **Encased versions** (8 wires around a stone slab → 8): run through the middle of the block like a pipe, connect to face wires.
- Wires connect to redstone blocks (levers, lamps, pistons, doors, dispensers...) and strongly power the block they lie on.

## Stage 4 (done)
- **Circuit plate** (stone slab + redstone → 2) and 17 **gates** (shapeless, like in 1.21): NOT, buffer, AND, NAND, OR, NOR,
  XOR, XNOR, RS latch, T flip-flop, pulse former, timer, randomizer, counter, sequencer, state cell, synchronizer.
- A gate lies on a block, its output points in the direction you face when placing. Wires connect only to outputs and enabled inputs.
- **Right-click – gate window**: description, live input/output diagram, time (timer, sequencer, state cell), counter maximum,
  disabling inputs (AND/OR/XOR...) or side outputs (buffer, NOT), RS latch and state cell sides, tick mute.
  T flip-flop: right-click toggles, Shift + right-click opens the window. The counter works with a comparator.
- **Screwdriver** (iron + stick): right-click rotates gates, the alloy furnace and vanilla blocks (Shift – the other way);
  Shift + right-click on a gate picks it up together with its settings.

## Stage 5 (done)
- **Lamps** in 16 colours (glass panes, glowstone dust, dye, redstone): lit with a signal, turn off after a 4-tick delay.
  **Inverted lamps** are lit without a signal; lamp + redstone torch converts a normal lamp to inverted and back.
- **Wireless transmitter and receiver** (circuit plate, wire, ender pearl, gold + torch / comparator): right-click sets
  the frequency 1-5000. A powered transmitter transmits everywhere – also in other dimensions and unloaded chunks.
  The receiver outputs signal 15 on all sides and strongly powers the block below it.
- **Remote** (transmitter + button + iron): holding right-click transmits, Shift + right-click – frequency selection.

## Stage 6 (done)
- **Tubes** (copper + glass → 8): items travel to the nearest inventory that accepts them; an open end drops
  the item. Right-click with a dye paints the tube (it then accepts only items of that colour), Shift + right-click with an empty hand removes paint.
  The **restriction tube** (tube + iron) is chosen only when there is no other way. Hoppers can insert into tubes.
- **Extractor** (pulse: a stack from the inventory behind → front), **filter** (9 patterns), **sorting machine**
  (9 columns: dye + 2 patterns – gives items the tube colour).
- **Signal circuit** and **signal machine casing** – the body of machines:
  relay (pushes out every 4 ticks by itself), ejector, buffer (18 slots, comparator), deployer (uses items like a player),
  block breaker (drops out of the back), igniter, item detector (pulse for every stack), regulator
  (stocks an inventory up to the filter amounts, then outputs a signal), assembler (16 slots = 16 bundled cable channels).
- Machines are placed by clicking a block – the front faces away from the clicked face; screwdriver: the clicked face becomes the front.

## Stage 7 (done)
- **RF** energy (the public CoFH API `cofh.api.energy` is included – works with other mods' machines and cables, e.g. Thermal Expansion,
  EnderIO; without them the mod works on its own). Numbers as in 1.21 (FE = RF). Production and consumption in `config/redcircuit.cfg`.
- **Power alloy wire** (on faces, like signal wires) and an **encased** version; right-click with an empty hand – energy state.
- **Battery box** (1 000 000 RF, battery charge/discharge slots; right-click with a battery – charge, Shift – discharge), **battery** (50 000 RF),
  **charger** (4 slots).
- Generators: **solar panel**, **thermopile** (hot + cold neighbour), **generator** with a **windmill** (5x5 free space under
  open sky) or a **water wheel** (3x3 water) – the rotor is visible and spins, **solid fuel generator**.
- Machines: **electric furnace**, **macerator** (ores x2, dusts...), **sawmill** (logs → 6 planks + sawdust, other mods' logs too),
  **power alloy furnace**, and for logistics: **Requester**, **Manager**, tube **accelerator**.
- Crafting tools: **handsaws** (6 materials) and **wire cutters** – stay in the grid and lose durability;
  fine wire, copper coil, power circuit, power machine casing.

## Stage 8 (done)
- **Pump** (placed by clicking the source): sucks from the back – from a tank (other mods' too) or from the world (up to 256 blocks
  of a connected lake, from the top), pushes to the front – into a tank/pipe or places a fluid block. 200 RF per bucket, redstone stops it.
- **Fluid pipe** (brass + glass → 8): 1000 mB, levels out with neighbouring pipes and pushes fluid into connected
  tanks and machines (Forge `IFluidHandler`), without giving it back to the source.
- **Fluid tank** (16 buckets): buckets and mod containers, fluid visible inside, flows down into a tank below, comparator,
  light from lava; the contents stay in the item when broken.

## Stage 9 (done)
- **Electric drill** (pickaxe + shovel, diamond tier) and **chainsaw** (axe, leaves, wool, cobwebs):
  energy instead of durability (50 RF per block, 100 000 RF), charged in the charger or battery box. Drill:
  Shift + right-click toggles 1x1 / 3x3 mining (Shift while mining = a single block).
- **Multimeter**: right-click a block – redstone signal, bundled cable channels, RF energy and fluids (in chat).
- **Blueprint table**: persistent 3x3 grid, 18 storage slots (ingredients taken from it first), shift-click on the result
  crafts repeatedly. **Blank plan** (paper + blue dye) + the "Plan" button saves the recipe;
  a saved plan loads the ingredients from the storage into the grid.

## Stage 10 (done)
Each integration is enabled only when the given mod is installed (`dependencies.gradle` – all of them run in runClient
except ComputerCraft and OpenComputers, which are compile-only).
- **NEI** (`compat/nei`): alloy furnace, macerator and sawmill recipes (with the bonus output chance);
  lit lamps and the empty-recipe plan are hidden.
- **Waila** (`compat/waila`): wire signal, cable channels, gate state (output, time, counter, disabled sides),
  lamps, transmitters (frequency), generator output, items in a tube, RF energy, fluids, machine progress.
- **ForgeMultipart** (`compat/fmp`): wires (signal alloy, insulated, bundled, power) as parts –
  they share a block with microblocks and other wires. A wire placed in a multipart block (or in a normal wire block)
  becomes a part; a normal wire converts by itself when you add a microblock to it
  (a block wire on several faces is split into several parts). One face of a block holds one wire.
- **ComputerCraft** (`compat/computer`): bundled cable ↔ `redstone.getBundledInput/setBundledOutput`; peripherals
  `redcircuit_gate` and `redcircuit_wireless`.
- **OpenComputers**: the same functions as components (through the adapter).

`redcircuit_gate` functions: getGateType, getState, getInterval, setInterval, getCount, getMax, setMax, resetCount,
isMuted, setMuted, isMirrored, setMirrored, setInputEnabled(side, bool), setQuiet. `redcircuit_wireless`:
getFrequency, setFrequency, isTransmitter, isPowered, isChannelActive([freq]).

## Stages
1-2. skeleton and materials ✔ · 3. wires ✔ · 4. gates ✔ · 5. lamps and wireless ✔ · 6. tubes and logistics ✔ ·
7. RF energy ✔ · 8. fluids ✔ · 9. tools and blueprint table ✔ · 10. NEI, Waila, ComputerCraft, OpenComputers, ForgeMultipart ✔ · 11. achievements, guide book.

## Development
`python tools/gen_textures.py` – textures, `python tools/gen_lang.py` – translations (partly taken from the 1.21 project in `../redcircuit`,
e.g. the `neoforge-1.21.1` branch checked out with `git worktree add ../redcircuit neoforge-1.21.1`).
