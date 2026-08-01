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

Vanilla entity culling bounds and Forge `IForgeBlockEntity#getRenderBoundingBox()` bounds are used automatically. Adapters may refine them:

```java
LiubaiApi.registerEntityRenderBoundsRule((entity, current) -> current);
LiubaiApi.registerBlockEntityRenderBoundsRule((blockEntity, current) -> current);
```

Return the supplied bounds when the rule does not apply. Returning a non-finite box or throwing keeps the object on the conservative visible path.

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
- Create 6.0.8 / bundled Flywheel 1.0.5: version-locked `BandedPrimeLimiter` update-divisor adjustment.
- Oculus 1.8.0: reflection-isolated use of its bundled Iris API to bypass per-object policies during shadow passes.
- Other Create/Flywheel or addon versions: no compatibility claim; unsupported limiter adapters remain inactive.
- Sable / Create Aeronautics: no Forge 1.20.1 release was found, so the 1.21.1 dynamic-subworld integration is intentionally absent.
