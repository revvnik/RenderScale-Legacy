# RenderScale legacy backport
# DISCLAIMER
This backport was made with GPT 5.6 Sol. Though being a programmer, I am not experienced with Minecraft modding. Maybe someday I will learn Java and backport it myself, but this will have to do.
This archive contains two deliberately separate projects.

## `legacy-1.7.10`

This is the editable Forge 1.7.10 backport. Open this directory in IntelliJ
IDEA and import its Gradle project.

Included:

- Complete Java source and Forge/coremod metadata
- `build.gradle`, `settings.gradle`, and `gradle.properties`
- Gradle 9.4 wrapper for IntelliJ and command-line imports
- Bundled ECJ compiler and all compile-time dependency JARs
- `gradlew` and `gradlew.bat`
- Offline `build.sh` and `build.bat`
- Transformer verification source

The Gradle wrapper downloads Gradle itself on first use. The direct shell and
batch scripts build offline with the bundled compiler and dependencies.

## `upstream-original`

This is an unchanged `git archive` snapshot of every tracked file in the
original RenderScale repository at commit:

`1fed7da4f3f01ba9ee2c8e38474115492be9b6dc`

It contains all 92 tracked upstream files, including Stonecutter build logic,
every upstream Gradle build script, the version catalog, Gradle wrapper, CI
workflows, resources, and modern Fabric/Forge/NeoForge sources.

The upstream project targets modern Minecraft and is included for reference.
It does not build the Forge 1.7.10 backport.
