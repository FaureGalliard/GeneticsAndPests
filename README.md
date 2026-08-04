Genetics and Pests
=======

A Minecraft mod for NeoForge 1.21.11, by [fauregalliard](https://github.com/FaureGalliard).

| | |
|---|---|
| Mod ID | `geneticsandpests` |
| Minecraft | 1.21.11 |
| NeoForge | 21.11.45 |
| Package | `com.fauregalliard.geneticsandpests` |

Concept: A redesign of Minecraft's farming system that introduces seed genetics, hostile
ecosystems driven by artificial intelligence (winged pests), and a disease propagation
system based on cellular automata.

The mod adds **no seeds and no crops of its own**. It attaches genetics to the plants the
game already has, so vanilla wheat is genetic wheat and there is never a second, parallel
farming tree to learn.

Core Mechanics
--------------

**Seed Genetics.** Every seed carries its own genome, stored as a data component on the
item itself and as chunk data once planted. A seed whose genes are all at the baseline
behaves exactly like vanilla, so nothing about ordinary farming changes until you start
breeding.

| Gene | What it does |
|---|---|
| Growth | Advances through growth stages faster |
| Yield | Drops more produce per harvest |
| Resistance | Shrugs off disease |
| Photosensitivity | Grows in light too dim for vanilla crops — cave farming |
| Thirst | Grows on dry farmland without the usual slowdown |
| Regrowth | Replants itself instead of dying when destroyed |
| Trampling | Survives being walked over |
| Camouflage | Pests are less likely to spot it |
| Fertility | Yields extra seeds and crossbreeds more eagerly |

**Crossbreeding.** Harvesting a mature plant crosses it with a mature neighbour of the same
kind: each gene is inherited from one parent at random and may drift a point in either
direction. Every seed from the harvest is rolled independently, so most come back close to
the parent and roughly one in three comes out with a gene improved. Planting your best seeds
next to each other and harvesting the result is the whole loop — a lone plant self-pollinates
rather than stalling.

**Airborne Pests (State Machine AI).** A new entity (Crow) derived from the vanilla model but
with a rewritten AI (Custom Goals). It scans its surroundings using bounding boxes (AABB)
to locate and destroy vulnerable crops, countered by the Camouflage gene.

**Plant Diseases (Cellular Automata).** Plants can contract diseases that spread to adjacent
blocks by rolling probabilities on a server tick, countered by the Resistance gene.

Mod Compatibility
-----------------

Which plants take part is decided entirely by two datapack tags:

- `#geneticsandpests:genetic_crops` — crop blocks that carry a genome while planted
- `#geneticsandpests:genetic_seeds` — items that carry a genome between harvest and planting

Adding a crop from another farming mod needs no code and no add-on: put its block and its
seed in those tags and it inherits the whole system. Genomes live on chunk data rather than
in block entities precisely so that blocks the mod does not own can carry them.

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
