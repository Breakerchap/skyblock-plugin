package io.github.breakerchap.skyblock.island;

public record IslandDefinition(
    String id,
    String displayName,
    int offsetX,
    int offsetZ,
    int yOffset,
    double discoveryRadius
) {
}
