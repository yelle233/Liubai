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
- Create 6.0.10 / Flywheel 1.0.6: version-locked `BandedPrimeLimiter` update-divisor adjustment.
- Sable 2.0.3 / Create Aeronautics 1.3.0: reflection-isolated safe mode for dynamic sublevels. Sublevel entities, block entities, and Plot-local particles bypass incompatible per-object policies; Liubai's extra Flywheel multiplier is suspended while active Aeronautics sublevels exist.
- Other Create/Flywheel or addon versions: no compatibility claim; unsupported limiter adapters remain inactive.

The Sable/Aeronautics integration is a conservative compatibility fallback, not structure-level LOD, proxy rendering, or a claim of complete large-contraption performance coverage.
