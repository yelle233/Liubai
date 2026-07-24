package com.yelle233.liubai.client;

public record RenderDecision(boolean skip, Reason reason) {
    public static final RenderDecision FULL = new RenderDecision(false, Reason.VISIBLE);

    public enum Reason {
        VISIBLE,
        SAFE,
        ALLOWLISTED,
        OCCLUDED,
        TOO_SMALL,
        DENSITY_LIMIT
    }
}
