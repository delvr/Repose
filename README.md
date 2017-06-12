# Repose

This mod introduces walkable slopes and more to Minecraft.
This means you can now walk up gentle slopes of grass, sand etc. without continually jumping.
This works by making granular blocks* act as stairs, whose shape varies according to the elevation of neighboring blocks.
Visually, these blocks are still the same cubes as before.
A config option allows the same behavior for "natural" stone such as that found in caves.

This mod also implements gravity for most granular block so that they fall like sand or gravel, and then spread into piles
or even trigger avalanches. This behavior is also configurable.

Please note that the source code is in [Scala](http://scala-lang.org) (not Java).
Keeping that in mind, if you have any questions about the code please send me (delvr) a message here on GitHub.
For help with the build process please read [Getting started with ForgeGradle](http://www.minecraftforge.net/forum/index.php/topic,14048.0.html) first.

Questions about the mod itself are best posted to the [discussion thread](http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2076319-repose-walkable-soil-slopes-give-your-spacebar-as).

Note: IDE-specific instructions are for IntelliJ IDEA; see the ForgeGradle documentation for Eclipse equivalents.

## Dependencies Setup
Repose compilation requires [Farseek](https://github.com/delvr/Farseek) and [TerraFirmaCraft](https://github.com/Deadrik/TFCraft) (TFC).
Compatible versions are specified using [Maven version range syntax](https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm#MAVEN402)
in the `modDependencies` and `modOptionalDependencies` properties of `gradle.properties`.
The build process of both Farseek and TFC will output `-deobf` and `-src` jars; for each dependency place both jars in Repose's `libs` subdirectory before running `setupDecompWorkspace`.

## IDE Setup
The IDEA `Update Forge` run configuration will run `setupDecompWorkspace` and `genIntellijRuns`.
After running `Update Forge`, synchronize Gradle in IntelliJ IDEA to set up module configs.

If using IntelliJ 2016 or later, make sure the Gradle plugin setting "Create separate module per source set" is NOT checked.

## Testing
Run the generated `Minecraft Client` or `Minecraft Server` configuration.

Note: TFC is required at compile time but optional at runtime. To test without TFC in IDEA,
switch the TFC dependencies from `Compile` to `Provided` in your module properties after synchronizing Gradle.

## Building
Run the `build` configuration. Jars will be generated in `build/libs`.
