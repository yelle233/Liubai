# Liubai Fabric 1.20.1 compatibility and opt-in API

Liubai is client-only. Unknown renderers keep their normal render path whenever bounds or adapter behavior cannot be established safely.

## Safety rules

Third-party client code can force selected objects visible:

```java
LiubaiApi.registerForceVisibleEntityRule(entity -> false);
LiubaiApi.registerForceVisibleBlockEntityRule(blockEntity -> false);
```

A rule throwing a runtime exception is treated as `true`, keeping the object visible.

## Render bounds

Vanilla entity culling bounds are used automatically. Fabric does not expose Forge's block-entity render-bounds extension, so ordinary block entities use their block-sized bounds unless an adapter refines them:

```java
LiubaiApi.registerEntityRenderBoundsRule((entity, current) -> current);
LiubaiApi.registerBlockEntityRenderBoundsRule((blockEntity, current) -> current);
```

Returning the supplied bounds means the rule does not apply. Returning an invalid or non-finite box, or throwing an exception, keeps the object on the conservative visible path.

## Temporal updates and quality

These calls only provide recommendations. Integrating renderers own their cached render state and must never use them to skip game logic:

```java
int interval = LiubaiApi.recommendedRenderUpdateInterval(center, worldRadius, important);
if (LiubaiApi.shouldRunRenderUpdate(stableKey, interval)) {
    updateRenderState();
}

RenderQuality quality = LiubaiApi.recommendedRenderQuality(center, worldRadius, important);
```

`stableKey` should remain stable for the lifetime of the visual so work is distributed across frames.

## Verified integrations

- Entity Culling 1.10.5: `AUTO` delegates generic occlusion and disables Liubai's DDA queue.
- Iris 1.7.6 for Minecraft 1.20.1: the public Iris API protects shadow passes.
- Create Fabric 6.0.8.x / Flywheel 1.0.5.x: version-gated `BandedPrimeLimiter#getUpdateDivisor(double)` adjustment.
- Valkyrien Skies 2.4.x: reflection-isolated ship ownership checks; active ships conservatively protect ownerless particle and Flywheel hooks.
- Sable / Create Aeronautics: no Minecraft 1.20.1 Fabric release was found, so no integration is included.
- Unknown versions remain on their original rendering path and do not prevent the client from starting.
