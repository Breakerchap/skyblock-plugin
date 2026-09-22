package io.github.breakerchap.skyblock.advancement;

import org.bukkit.Material;

import java.util.Locale;

public record AdvancementDefinition(
    String id,
    String parentId,
    String title,
    String description,
    Material icon,
    String frame,
    boolean hidden,
    int experience
) {
    public String toJson() {
        StringBuilder json = new StringBuilder("{");
        if (parentId != null) {
            json.append("\"parent\":\"skyblock:").append(escape(parentId)).append("\",");
        }

        json.append("\"display\":{")
            .append("\"icon\":{\"id\":\"minecraft:")
            .append(icon.name().toLowerCase(Locale.ROOT))
            .append("\"},")
            .append("\"title\":{\"text\":\"").append(escape(title)).append("\"},")
            .append("\"description\":{\"text\":\"").append(escape(description)).append("\"},")
            .append("\"frame\":\"").append(escape(frame)).append("\",")
            .append("\"show_toast\":").append(parentId != null).append(",")
            .append("\"announce_to_chat\":").append(parentId != null).append(",")
            .append("\"hidden\":").append(hidden);

        if (parentId == null) {
            json.append(",\"background\":\"minecraft:gui/advancements/backgrounds/stone\"");
        }

        json.append("},")
            .append("\"criteria\":{\"plugin\":{\"trigger\":\"minecraft:impossible\"}}");

        if (experience > 0) {
            json.append(",\"rewards\":{\"experience\":").append(experience).append("}");
        }

        return json.append("}").toString();
    }

    static String escape(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}
