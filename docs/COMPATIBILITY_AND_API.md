# Liubai compatibility and opt-in API

Liubai is client-only. Unknown renderers keep their normal render path whenever bounds or adapter behavior cannot be established safely.

## Safety rules

Third-party client code can force selected objects visible:

```java
LiubaiApi.registerForceVisibleEntityRule(entity -> false);
LiubaiApi.registerForceVisibleBlockEntityRule(blockEntity -> false);
```

A rule throwing a runtime exception is treated as `true`, keeping the object visible.

## Render bounds

Vanilla entity culling bounds and NeoForge block-entity renderer bounds are used automatically. Adapters may refine them:

```java
LiubaiApi.registerEntityRenderBoundsRule((entity, current) -> current);
LiubaiApi.registerBlockEntityRenderBoundsRule((blockEntity, current) -> current);
```

Return the supplied bounds when the rule does not apply. Returning `AABB.INFINITE`, a non-finite box, or throwing keeps the object on the conservative visible path.

## Temporal updates and quality

These calls only provide recommendations. The integrating renderer owns its cached render state and must never use them to skip game logic:

```java
int interval = LiubaiApi.recommendedRenderUpdateInterval(center, worldRadius, important);
if (LiubaiApi.shouldRunRenderUpdate(stableKey, interval)) {
    updateRenderState();
}

RenderQuality quality = LiubaiApi.recommendedRenderQuality(center, worldRadius, important);
```

`stableKey` should remain stable for the lifetime of the visual so work is distributed across frames.

## Built-in integrations

- Entity Culling: AUTO delegates generic occlusion and disables Liubai's DDA queue.
- Iris: the public API is queried to conservatively bypass entity, block-entity, and shadow policies during shadow passes. This is not a shader performance optimization.

These integrations remain in the source; compiling does not establish in-game compatibility with every third-party version.

## Version availability (checked 2026-10-23)

This document describes only the `26.1.2neoforge` branch (Minecraft 26.1.2, NeoForge).

| Project | Public release evidence | Decision for this branch |
| --- | --- | --- |
| [Create](https://modrinth.com/mod/create/versions) | Official Modrinth project lists releases through Minecraft 1.21.1, with no 26.1.2 files. | Remove the inherited Create/Flywheel limiter adapter and optional dependency declarations. |
| [Create Aeronautics](https://modrinth.com/mod/create-aeronautics/versions) | Published files target Minecraft 1.21.1, not 26.1.2. | Remove Aeronautics detection and its dedicated safety mode. |
| [Sable](https://modrinth.com/mod/sable/versions) | Published files target Minecraft 1.21.1, not 26.1.2. | Remove the reflection bridge and per-object/sublevel checks. |
| [Create Fly](https://modrinth.com/mod/create-fly/versions) | Version `26.1.2-6.0.9-4` targets Minecraft 26.1.2, but its loader is Fabric, not NeoForge. | Does not justify retaining NeoForge compatibility code. |

Verification used each project's Modrinth `/v2/project/{slug}/version` API, checking the game version and loader of actual files rather than search tags or addon listings. No matching NeoForge port was found in the searched results; this does not rule out private or unpublished builds.

The obsolete Flywheel configuration section, related HUD counters/translations, and default `create`/`flywheel` namespace exclusions were also removed. The general namespace allowlist, force-visible/bounds rules, and temporal-update API remain available. Existing user configuration files are not rewritten by this source change; legacy namespace entries may still be present in an existing profile.

If matching NeoForge releases become available, inspect their actual rendering/sublevel APIs and test them in-game before reintroducing adapters. Do not reuse the Minecraft 1.21.1 version assumptions as a compatibility claim for 26.1.2. Other branches are unaffected.
