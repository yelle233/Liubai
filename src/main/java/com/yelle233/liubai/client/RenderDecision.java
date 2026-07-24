package com.yelle233.liubai.client;

public record RenderDecision(boolean skip, int lod, Reason reason) {
    public static final RenderDecision FULL = new RenderDecision(false, 0, Reason.VISIBLE);

    public enum Reason {
        VISIBLE,
        SAFE,
        ALLOWLISTED,
        OCCLUDED,
        TOO_SMALL
    }
}
