package com.yelle233.liubai.client;

public record RenderDecision(boolean skip, Reason reason) {
    public static final RenderDecision FULL = new RenderDecision(false, Reason.VISIBLE);
    public static final RenderDecision SAFE = new RenderDecision(false, Reason.SAFE);
    public static final RenderDecision ALLOWLISTED = new RenderDecision(false, Reason.ALLOWLISTED);
    public static final RenderDecision OCCLUDED = new RenderDecision(true, Reason.OCCLUDED);
    public static final RenderDecision TOO_SMALL = new RenderDecision(true, Reason.TOO_SMALL);
    public static final RenderDecision DENSITY_LIMITED = new RenderDecision(true, Reason.DENSITY_LIMIT);

    public enum Reason {
        VISIBLE,
        SAFE,
        ALLOWLISTED,
        OCCLUDED,
        TOO_SMALL,
        DENSITY_LIMIT
    }
}
