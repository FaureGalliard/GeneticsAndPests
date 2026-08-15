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
kind. Each gene takes the better parent's value most of the time, and nothing in breeding can
push a gene down — a line you spent an evening building will not decay on a bad roll. One seed
per harvest is rolled for an improvement, and the odds of raising a gene fall off geometrically
with its level, so the first levels come quickly and the last are a project. Keeping a beehive
beside the field widens pollination from the four touching plants to everything within two
blocks.

**Plant Diseases.** Four diseases, each with its own way of spreading, its own damage and its
own remedy. Outbreaks are driven by monoculture: a scattered garden is effectively safe, and a
solid field is a matter of time. A thunderstorm carries spores much further than usual.

| Disease | Spreads by | Effect | Cure |
|---|---|---|---|
| Rust | Neighbours | Halves growth and harvest | Ash |
| Ergot | Neighbours, slowly | Harvest looks fine and is poisonous | Lime |
| Smut | **Seed** | No produce, and the seed carries it onward | Brine, or a water cauldron |
| Blight | Neighbours | Stops growth, then kills | None — tear it out |

A plant that survives an infection comes out with Resistance raised and another trait lowered.
It is the only way to breed Resistance deliberately: you have to let the disease through.

**Grafting.** Improvement picks a gene at random out of nine, so pushing one specific trait to
the ceiling by luck alone is impractical. A **grafting table** takes a **scion** — a cutting
carrying one trait at the level it reached — from a seed, destroying it, and grafts that trait
onto another seed of the same crop. It buys direction, never power. The bench needs a blade
(shears, any sword, a modded knife) which wears down with use, and a catalyst that caps how
good a cutting can be: honeycomb to level 7, amethyst to 14, echo shard to 20.

**Wild strains.** Crops in village fields generate with genomes of their own, so a village is
worth walking to and worth robbing. Farmer villagers deal in the remedies, and the experienced
ones will sell seed from their own stock.

**Airborne Pests (State Machine AI).** *Not yet implemented.* A new entity (Crow) derived from
the vanilla model but with a rewritten AI (Custom Goals), scanning its surroundings using
bounding boxes (AABB) to locate and destroy vulnerable crops, countered by the Camouflage gene.

Mod Compatibility
-----------------

Which plants take part is decided entirely by two datapack tags:

- `#geneticsandpests:genetic_crops` — crop blocks that carry a genome while planted
- `#geneticsandpests:genetic_seeds` — items that carry a genome between harvest and planting

Adding a crop from another farming mod needs no code and no add-on: put its block and its
seed in those tags and it inherits the whole system. Genomes live on chunk data rather than
in block entities precisely so that blocks the mod does not own can carry them, and growth is
read from the `age` blockstate property rather than from any particular block class — which is
why cocoa, nether wart and sweet berries take part despite not being crop blocks at all.

Remedies, catalysts and blades are tags too:

| Tag | What it decides |
|---|---|
| `cures/rust`, `cures/ergot`, `cures/smut` | What cures each disease |
| `catalysts/lesser`, `catalysts/greater`, `catalysts/master` | What a grafting bench accepts, and the level each tier caps at |
| `grafting_blades` | What counts as a knife |

Commands
--------

Both require cheats, and both exist because balancing this mod means watching a number play
out over many harvests.

| Command | What it does |
|---|---|
| `/geneticsandpests genes get \| set \| fill \| clear` | Reads and edits the genome of the held seed |
| `/geneticsandpests config list \| set <key> <value>` | Reads and edits every tunable, applied live and saved |
| `/geneticsandpests disease set <disease> [radius]` | Infects the crop you are looking at, and optionally the area |
| `/geneticsandpests disease clear [radius]` | Cures it again |

Development
-----------

After changing anything that is generated, run **both** data generators — client and server
data are separate runs writing to separate folders:

```
gradlew runData
gradlew runServerData
```

Building
=======

Clone the repository and open it in the IDE of your choice (IntelliJ IDEA or Eclipse are the
usual recommendations). A JDK 21 installation is required.

| Task | What it does |
|---|---|
| `gradlew runClient` | Launches a development Minecraft client with the mod loaded |
| `gradlew runServer` | Launches a development dedicated server |
| `gradlew build` | Builds the mod JAR into `build/libs/` |

[Jade](https://modrinth.com/mod/jade) is an optional integration. To build against it, drop its
jar into `libs/` and point `jade_jar` in `gradle.properties` at the file name; the published mod
never depends on it.

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
