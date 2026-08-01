package com.yelle233.liubai.visibility;

public enum EffectiveVisibilityBackend {
    BUILTIN("liubai.hud.backend.builtin"),
    ENTITY_CULLING("liubai.hud.backend.entityCulling"),
    EXTERNAL("liubai.hud.backend.external"),
    DISABLED("liubai.hud.backend.disabled");

    private final String translationKey;

    EffectiveVisibilityBackend(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
