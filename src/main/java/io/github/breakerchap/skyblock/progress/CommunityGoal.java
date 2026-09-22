package io.github.breakerchap.skyblock.progress;

public enum CommunityGoal {
    COBBLE("cobble", "cobblestone-mined", "Stone by Stone", 5_000L),
    BUILDER("builder", "blocks-placed", "Somewhere to Live", 3_000L),
    HUNTER("hunter", "hostiles-killed", "Night Shift", 250L),
    LIFE("life", "creatures-bred", "It Takes a Village", 50L),
    FOREST("forest", "trees-grown", "Reforestation", 250L),
    FISH("fish", "fish-caught", "Somehow, Fishing", 100L),
    CRAFTING("crafting", "craft-actions", "Factory Floor", 5_000L),
    HARVEST("harvest", "crops-harvested", "Agricultural Society", 2_000L);

    private final String id;
    private final String metric;
    private final String displayName;
    private final long defaultThreshold;

    CommunityGoal(String id, String metric, String displayName, long defaultThreshold) {
        this.id = id;
        this.metric = metric;
        this.displayName = displayName;
        this.defaultThreshold = defaultThreshold;
    }

    public String id() {
        return id;
    }

    public String metric() {
        return metric;
    }

    public String displayName() {
        return displayName;
    }

    public long defaultThreshold() {
        return defaultThreshold;
    }

    public String advancementId() {
        return "community/" + id;
    }
}
