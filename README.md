Genetics and Pests
=======

A Minecraft mod for NeoForge 1.21.11, by [fauregalliard](https://github.com/FaureGalliard).

| | |
|---|---|
| Mod ID | `geneticsandpests` |
| Minecraft | 1.21.11 |
| NeoForge | 21.11.45 |
| Package | `com.fauregalliard.geneticsandpests` |

Concept: A redesign of Minecraft's farming system that introduces seed genetics with
attributes (NBT data), hostile ecosystems driven by artificial intelligence (winged pests),
and a disease propagation system based on cellular automata.

Core Mechanics:

Seed Genetics (Data Management): Every seed carries unique attributes (Growth, Yield,
Resistance) stored as NBT tags. Crops are crossbred to inherit and mutate these stats.

Airborne Pests (State Machine AI): A new entity (Crow) derived from the vanilla model but
with a rewritten AI (Custom Goals). It scans its surroundings using bounding boxes (AABB)
to locate and destroy vulnerable crops.

Plant Diseases (Cellular Automata): Plants can contract diseases (which modify their
BlockState) that spread to adjacent blocks by rolling probabilities on each server tick,
countered by the plant's Resistance attribute.

Development
=======

Clone the repository and open it in the IDE of your choice (IntelliJ IDEA or Eclipse are the
usual recommendations). A JDK 21 installation is required.

Useful Gradle tasks:

| Task | What it does |
|---|---|
| `gradlew runClient` | Launches a development Minecraft client with the mod loaded |
| `gradlew runServer` | Launches a development dedicated server |
| `gradlew runData` | Runs data generation into `src/generated/resources/` |
| `gradlew build` | Builds the mod JAR into `build/libs/` |

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
