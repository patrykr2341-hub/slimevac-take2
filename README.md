# SlimeVac — Fabric 1.21.1

A Slime Rancher-inspired mob vacuum mod for Minecraft Java 1.21.1.

## Requirements
- Minecraft Java Edition **1.21.1**
- Java **21**
- Fabric Loader 0.16+
- Fabric API 0.110.0+1.21.1
- Sodium is optional; this mod does not replace Minecraft's renderer or depend on Sodium internals.

## Build
1. Install JDK 21.
2. Open this folder in IntelliJ IDEA (or use a terminal here).
3. Run `gradle build` (or import the Gradle project and run the `build` task).
4. The mod JAR will be in `build/libs/`.

This source package does not include a precompiled JAR. Gradle downloads Minecraft/Fabric dependencies when you build it.

## Progression
1. Craft **Slime-Steel Blend** from iron + copper + slime ball + amethyst shard.
2. Smelt the blend into **Slime-Steel Ingots**.
3. Craft a **Slime Core** from slime and amethyst.
4. Craft the **Vacpack Fabricator**.
5. Use the Fabricator block as an ingredient to craft the **SlimeVac**.

## Controls
- Hold right-click with an empty SlimeVac to start suction. Mobs in front of you are pulled toward you; one mob is stored when it reaches you.
- Right-click a mob directly to capture it immediately.
- Right-click with a stored mob to release it in front of you.
- Sneak + right-click toggles the displayed mode message between Suction and Release.

## Important gameplay note
This first release stores the captured mob's **entity type**, not its full inventory/health/variant data. Releasing a mob therefore creates a fresh instance of that mob type. Players, items, and non-mob entities cannot be captured. One mob can be stored at a time.

## Sodium compatibility
The mod uses ordinary Fabric item/block registration and server-side entity logic. It does not inject into Sodium rendering code. The held Vacpack is a vanilla block-model item with 3D cuboid geometry.

## License
MIT


## Build on GitHub (no local development tools needed)
This project includes a GitHub Actions workflow at `.github/workflows/build.yml`.
After uploading the project to a GitHub repository, open the **Actions** tab and
select **Build SlimeVac**. It runs automatically after a push to `main`, or you
can select **Run workflow**. When the run finishes, open it and download the
`SlimeVac-Fabric-1.21.1` artifact. The artifact is a ZIP containing the built
mod JAR; extract it and place the `.jar` in your Minecraft `mods` folder.
