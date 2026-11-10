# Modifications to GT: Not Leisure (Science Not Leisure)

This repository is an **unofficial modified fork** of GT: Not Leisure. It is not
affiliated with, endorsed by, or supported by the upstream authors.

- Upstream project: <https://github.com/ABKQPO/GT-Not-Leisure>
- Upstream base revision: `082483e468942eb1949975f08bc900510c91d930`
- Upstream version string: `0.2.7-pre3-dev-290.37+082483e468`
- Target modpack: GT: New Horizons `2.9.0-beta-3` (Minecraft 1.7.10, Forge 10.13.4.1614)
- Modification date: **2026-10-18** (glass replacement); **2026-10-20**
  (client crash fixes); **2026-10-24** (casing names and petri dish non-consumption);
  **2026-10-30** (Super Space Elevator glass fix); **2026-11-06**
  (wireless 32k fix, assembly-line sub-panel fix, HighwayToHell rotor fix,
  coil tooltip translation override)
- Modified by: `KurumiMyWaifu` (`https://github.com/KurumiMyWaifu`)
- License of the modifications: **GNU Lesser General Public License v3.0
  (LGPL-3.0)**, the same license as the upstream project. See `LICENSE.txt`.

The upstream project is licensed under **LGPL-3.0**, not GPL-3.0. All original
copyright and license notices are retained. Third-party code and assets keep
their own licenses; see the "Credits" table in `README.md`.

## Purpose of the modifications

The mod used the deprecated IndustrialCraft 2 block `IC2:blockAlloyGlass`
(block ID `183:0`) as a structural block, a recipe input, and a quest material.
This fork replaces every such use with GregTech's `gregtech:gt.blockglass1`
with metadata `10` (block ID `3028:10`).

In GT5-Unofficial, `gt.blockglass1` meta `10` is `ItemList.ReinforcedGlass`
(ore dictionary entry `glassReinforced`), which is the intended replacement for
IC2 alloy glass.

## Detailed changes

All changes are in `src/`. No other functional code was modified.

### 1. Multiblock structure elements (5 files)

Before:

```java
StructureUtility.ofBlockAnyMeta(GameRegistry.findBlock(Mods.IndustrialCraft2.ID, "blockAlloyGlass"))
```

After:

```java
StructureUtility.ofBlock(GregTechAPI.sBlockGlass1, 10)
```

Files:

- `src/main/java/com/science/gtnl/common/machine/multiblock/FuelRefiningComplex.java`
- `src/main/java/com/science/gtnl/common/machine/multiblock/structuralReconstructionPlan/LargeForming.java`
- `src/main/java/com/science/gtnl/common/machine/multiblock/structuralReconstructionPlan/MolecularTransformer.java`
- `src/main/java/com/science/gtnl/common/machine/multiblock/wireless/MegaCanner.java`
- `src/main/java/com/science/gtnl/common/machine/multiblock/wireless/NanoAssemblerMarkL.java`

### 2. Recipe inputs (7 references)

Before:

```java
GTModHandler.getModItem(Mods.IndustrialCraft2.ID, "blockAlloyGlass", <count>, 0)
```

After:

```java
ItemList.ReinforcedGlass.get(<count>)
```

Files:

- `src/main/java/com/science/gtnl/common/recipe/gregtech/AssemblerRecipes.java` (6 references)
- `src/main/java/com/science/gtnl/common/recipe/gtnl/CraftingTableRecipes.java` (1 reference)

### 3. Quest material requirements (7 JSON files)

Each quest item requirement that asked for the deprecated IC2 block now asks
for the GregTech block at metadata 10:

```diff
-          "Damage:2": 0,
+          "Damage:2": 10,
           "OreDict:8": "",
-          "id:8": "IC2:blockAlloyGlass"
+          "id:8": "gregtech:gt.blockglass1"
```

Files:

- `src/main/resources/assets/sciencenotleisure/quest/Quests/Tier075Superheat-GTNotLeisure75SteamAge==/-I97UUD3oRdWn8Wk-hr6nIg==.json`
- `src/main/resources/assets/sciencenotleisure/quest/Quests/Tier075Superheat-GTNotLeisure75SteamAge==/-PAEAFa5xShavUK6dZgq45Q==.json`
- `src/main/resources/assets/sciencenotleisure/quest/Quests/Tier075Superheat-GTNotLeisure75SteamAge==/-Thn62VlIRL2opp_oKxQanw==.json`
- `src/main/resources/assets/sciencenotleisure/quest/Quests/Tier075Superheat-GTNotLeisure75SteamAge==/-Wl2Do2ocSTamZzrRADtxgw==.json`
- `src/main/resources/assets/sciencenotleisure/quest/Quests/Tier075Superheat-GTNotLeisure75SteamAge==/-mC6_hr7yTOKllBrvuYzNpg==.json`
- `src/main/resources/assets/sciencenotleisure/quest/Quests/Tier075Superheat-GTNotLeisure75SteamAge==/-ryXkmJDfRKuEZdjrMQfcfw==.json`
- `src/main/resources/assets/sciencenotleisure/quest/Quests/Tier0999Supercri-GTNotLeisure99SteamAge==/-ftAZh6RtRQ-EQ3depF4aGw==.json`

### 4. Client crash fixes (2026-10-20)

These are separate from the glass replacement. They fix a client crash that
occurred after placing/loading a GTNL Incubator.

#### 4.1 Incubator render-packet color

`Incubator` sent `PacketBioVatRenderer` with `Color.DARK_GRAY.getRGB()`, which
contains an alpha channel (`0xFF444444`). The packet only encodes the RGB bytes,
so the receiver reconstructs `0x00444444`; the packet checksum therefore never
matched. The client logged `BW Packet was corrupted or modified!`, then
`GTNetwork.decode` called `setINetHandler` on the `null` packet and threw a
`NullPointerException`, disconnecting the client. The color is now masked to RGB
with `& 0xFFFFFF` at all four send sites.

File:

- `src/main/java/com/science/gtnl/common/machine/multiblock/structuralReconstructionPlan/Incubator.java`

#### 4.2 Wireless steam hatch client-side NPE

`WirelessSteamEnergyHatch.onFirstTick` and
`WirelessSteamDynamoHatch.onFirstTick` read the owner UUID and called
`TeamNetworkManager.getTeamId` before checking `isServerSide()`. On the client,
`UsernameCache.getLastKnownUsername` threw for UUIDs that were not in the local
cache. The server-side guard now runs first, matching
`SteamMultiMachineBase.onFirstTick`.

Files:

- `src/main/java/com/science/gtnl/common/machine/hatch/WirelessSteamEnergyHatch.java`
- `src/main/java/com/science/gtnl/common/machine/hatch/WirelessSteamDynamoHatch.java`

#### 4.3 Null-safe player name lookup

`TeamNetworkManager.getPlayerName(UUID)` now reads
`UsernameCache.getMap()` directly and falls back to the UUID string when the
name is not cached. This also fixes the server-side Waila `NullPointerException`
in `SteamMultiMachineBase.getWailaNBTData`.

File:

- `src/main/java/com/science/gtnl/utils/world/teams/TeamNetworkManager.java`

### 5. Repository notice

- Added this `MODIFICATIONS.md`.
- Added a short "unofficial modified fork" notice at the top of `README.md`
  pointing to this file.

### 6. Atomic Energy Excitation Plant casing names (2026-10-24)

`AtomicEnergyExcitationPlant` uses BartWorks casings at metadata
`31_766 + 129` (`31895`) for its `H` and `I` structure elements. The matching
`GTNLMaterials.Neutronium` Werkstoff registration was commented out, so
BartWorks fell back to `Materials._NULL`; the blocks displayed as
`null Casing` and `Bolted null Casing`.

The `Neutronium` Werkstoff is now registered again with
`Materials.Neutronium`, `ADD_CASINGS_ONLY`, `Types.ELEMENT`, and ID
`31_766 + 129`. This also fixes `HighwayToHell`, which uses the same casing
metadata. The `Bronze` Werkstoff (`31_766 + 300`) remains intentionally
disabled.

File:

- `src/main/java/com/science/gtnl/common/material/GTNLMaterials.java`

### 7. Petrochemical Plant petri dish is non-consumed (2026-10-24)

In the Xenoxene Xenoxsis recipe of the Petrochemical Plant, the
`petri dish with a culture` input now uses `GTUtility.copyAmount(0, ...)`.
This is the same `stackSize == 0` convention used by programmed circuits in
assembler recipes: the item is still required to match the recipe, but it is
not consumed.

File:

- `src/main/java/com/science/gtnl/common/recipe/gtnl/PetrochemicalPlantRecipes.java`

### 8. Super Space Elevator glass block (2026-10-30)

The Super Space Elevator structure used:

```java
ofBlock(bw_realglas, 14)
```

BartWorks `bw_realglas` only has metadata `0` through `10`; metadata `14`
is outside the block's texture array and has no localization, which is why it
displayed as `BW_TieredGlass.14.name` / `BW_glasstier.14.name` and could not
be crafted.

It is replaced with `GTStructureUtility.chainAllGlasses()`, the same generic
tiered-glass element used by other GTNL structures. It accepts the registered
BartWorks borosilicate glasses (including the valid `bw_realglas` and
`bw_realglas2` metadata), GT glasses, and the other glasses registered in
`GlassTier`.

The same invalid `bw_realglas, 14` reference in `OreExtractionModule` is fixed
the same way.

Files:

- `src/main/java/com/science/gtnl/common/machine/multiblock/SuperSpaceElevator.java`
- `src/main/java/com/science/gtnl/common/machine/multiblock/module/nanitesIntegratedProcessingCenter/OreExtractionModule.java`

### 9. Wireless output 32k GUI crash (2026-11-06)

The wireless processing loop appended every cycle's output array to the previous
array. Long-running machines therefore accumulated a huge list of duplicate
outputs; opening the GUI synced that list and the client echoed it back through
the GT5U recipe-info `DynamicSyncHandler`, eventually exceeding the 32k custom
payload limit.

The fix merges outputs by type instead of concatenating arrays:

- `Utils.mergeItemStacks()` merges by item + damage + NBT and sums stack sizes;
- `Utils.mergeFluidStacks()` merges by fluid + NBT and sums amounts;
- the wireless `checkProcessing()` methods clear stale outputs at the start;
- all wireless output merge sites now use the type-aware helpers.

Files:

- `src/main/java/com/science/gtnl/utils/Utils.java`
- `src/main/java/com/science/gtnl/common/machine/multiMachineBase/WirelessEnergyMultiMachineBase.java`
- `src/main/java/com/science/gtnl/common/machine/multiblock/wireless/HighwayToHell.java`
- `src/main/java/com/science/gtnl/common/machine/multiblock/wireless/TransliminalOasis.java`
- `src/main/java/com/science/gtnl/common/machine/multiblock/wireless/NineIndustrialMultiMachine.java`
- `src/main/java/com/science/gtnl/common/machine/multiblock/structuralReconstructionPlan/KuangBiaoOneGiantNuclearFusionReactor.java`

### 10. Grand Assembly Line minimum-recipe-time sub-panel crash (2026-11-06)

`GrandAssemblyLineGui` created its sub-panel with the sub-panel's own
`PanelSyncManager`, but the `grandAssemblyLineMinRecipeTime` handler was
registered on the root `PanelSyncManager`. `createMinRecipeTimePanel()` therefore
threw `NoSuchElementException` on the server when the panel was opened.

The sub-panel builder now uses the captured root `syncManager`:

```java
(panelSyncManager, panelHandler) -> createMinRecipeTimePanel(parent, syncManager)
```

File:

- `src/main/java/com/science/gtnl/common/gui/modularui/GrandAssemblyLineGui.java`

### 11. HighwayToHell rotor assemblies (2026-11-06)

`HighwayToHell` required four filled rotor assemblies during the structure
check, but had no path to fill them: the GT++ `MTEHatchTurbine` GUI and
inventory insertion are read-only, and only the XL turbine controllers call
`insertTurbine()`.

The structure now forms with empty rotor assemblies. `checkProcessing()`:

1. pulls valid turbine rotors from the normal input buses into empty rotor
   assemblies, one per hatch;
2. requires exactly four rotor assemblies, each containing a valid turbine;
3. if the rotors are missing/invalid, returns
   `SimpleCheckRecipeResult.ofFailure("invalid_hatch_configuration")` and does
   not run the crusher recipe;
4. skips surplus valid turbines when deduplicating wireless inputs;
5. computes `turbineTier` from the minimum turbine material tool quality
   (initialized to `Integer.MAX_VALUE`), so the +1% output-chance bonus per
   turbine tier actually applies.

Files:

- `src/main/java/com/science/gtnl/common/machine/multiblock/wireless/HighwayToHell.java`
- `src/main/resources/assets/sciencenotleisure/lang/en_US.lang`
- `src/main/resources/assets/sciencenotleisure/lang/zh_CN.lang`

### 12. Coil tooltip “Format error” translation override (2026-11-06)

The GT5U `HeatingCoilSpecialValueFormatter` passes one argument to
`GT5U.nei.heat_capacity`; older Chinese translations used two `%s`
placeholders, causing vanilla `StringTranslate` to return
`Format error: 热容：%s K （%s）`.

No GT5U class is patched in this build. Instead, `zh_CN.lang` now overrides the
key with the single-placeholder form:

```text
GT5U.nei.heat_capacity=炉温：%s K
GT5U.nei.heat_capacity.material=线圈：%s
```

The external translation pack should use the same single-placeholder form. The
separate `gt5u-coil-tooltip-fix/` directory remains available for users who
prefer the GT5U class patch instead of a translation override.

## Verification

- `grep -r "blockAlloyGlass" src/` returns no matches.
- The built jar contains no `blockAlloyGlass` references.
- `StructureUtility.ofBlock(GregTechAPI.sBlockGlass1, 10)` and
  `ItemList.ReinforcedGlass.get(...)` are present in the compiled bytecode.
- All modified quest JSON files parse as valid JSON.
- The compiled `Incubator` masks the packet color with `0xFFFFFF` at all four
  send sites.
- Both wireless steam hatch `onFirstTick` implementations check
  `isServerSide()` before any team/username lookup.
- `TeamNetworkManager.getPlayerName` does not throw for uncached UUIDs.
- The compiled `GTNLMaterials.run()` registers a `Werkstoff` for ID `31895`
  with `Materials.Neutronium`.
- The compiled Petrochemical Plant recipe passes `0` to
  `GTUtility.copyAmount(...)` for the Xenoxene Xenoxsis petri dish.
- The compiled Super Space Elevator and Ore Extraction Module use
  `GTStructureUtility.chainAllGlasses()` for their glass elements.
- The compiled wireless output merge sites use `Utils.mergeItemStacks()` /
  `Utils.mergeFluidStacks()` instead of `Utils.mergeArray()`.
- The compiled `GrandAssemblyLineGui` sub-panel lookup uses the root
  `PanelSyncManager`.
- The compiled `HighwayToHell` no longer rejects empty rotor assemblies during
  `checkHatch`, pulls turbines from input buses in `checkProcessing`, returns
  `invalid_hatch_configuration` when fewer than four valid rotors are present,
  and initializes `turbineTier` to `Integer.MAX_VALUE` before taking the
  material-quality minimum.
- `zh_CN.lang` contains the single-placeholder `GT5U.nei.heat_capacity` and
  `GT5U.nei.heat_capacity.material` entries.
- `gradle assemble` completes successfully.

## Building

Requirements:

- Gradle 9.7.0 (the version pinned by `gradle/wrapper/gradle-wrapper.properties`)
- JDK 25 to run Gradle, plus Azul Zulu JDK 8, 17, and 21 toolchains for
  RetroFuturaGradle/JvmDowngrader.

Build the production jar with:

```bash
VERSION=0.2.7-pre3-dev-290.37+082483e468 ./gradlew assemble
```

`VERSION` is needed when the source directory is not a Git repository; otherwise
the jar and `mcmod.info` are stamped `NO-GIT-TAG-SET`.

If Blowdryer cannot reach its GitHub settings source (for example due to a
certificate/network issue), temporarily set:

```properties
gtnh.settings.blowdryerTag = LOCAL
```

The repository already contains `gtnhShared/spotless.gradle`,
`spotless.eclipseformat.xml`, and `spotless.importorder`, so an offline build
still has the code-style files. Restore the upstream value `0.2.2` afterwards.

The output jar is written to `build/libs/`.

## License and warranty

The original work and these modifications are distributed under the
**GNU Lesser General Public License v3.0**. See `LICENSE.txt` for the full
license text. There is **no warranty**, to the extent permitted by law.

If you redistribute a compiled jar, you must also make the corresponding source
code available under LGPL-3.0. Publishing this repository and the source archive
alongside the jar satisfies that requirement.
