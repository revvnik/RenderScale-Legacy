# RenderScale Legacy

Forge 1.7.10 client-side backport of RenderScale's separate scaled-world
render target architecture.

The 3D world is rendered into a scaled framebuffer. It is then resolved into
Minecraft's original framebuffer before the HUD is drawn. Minecraft retains
full control of the final window presentation, including on macOS.

## Installation

Place `RenderScale-Legacy-1.7.10-1.0.1.jar` in the instance's `mods` folder.
The mod appears as **RenderScale Legacy** in the Forge mod list.

The first launch creates `config/renderscale.cfg`. The default `scaleFactor`
is `2.0`, meaning twice the native width and twice the native height (four
times as many 3D pixels). Close Minecraft before editing the file.

`linearFilter=true` is recommended for supersampling.

This is a client-only mod. Do not install it on a dedicated server.

## Building from source

The development archive includes a complete IntelliJ/Gradle project, the
Gradle wrapper, the compiler, and all compile-time dependency JARs. It does
not require Maven, ForgeGradle, or a Minecraft JAR.

### IntelliJ IDEA

1. Open the `legacy-1.7.10` directory as a project.
2. Import the detected Gradle project.
3. Set the Gradle JVM to Java 17 or newer.
4. Run the Gradle `build` task.

The bundled Eclipse compiler is used for production classes because current
JDK compilers no longer produce Java 7 bytecode.

### Direct scripts

On macOS or Linux, open Terminal in the extracted directory and run:

```sh
chmod +x build.sh
./build.sh
```

On Windows 10 or 11, double-click `build.bat`, or run it from Command Prompt:

```bat
build.bat
```

Java 11 or newer is required on every platform. macOS/Linux also requires the
standard `zip` and `unzip` commands. Windows uses its included PowerShell.
The finished mod is written to:

- Gradle: `build/libs/RenderScale-Legacy-1.7.10-1.0.1.jar`
- Shell/batch scripts: `build/RenderScale-Legacy-1.7.10-1.0.1.jar`
