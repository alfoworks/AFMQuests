package ru.alfomine.afmquests.quests.parsers;

import com.google.gson.*;
import ru.alfomine.afmquests.quests.PlayerContribution;
import ru.alfomine.afmquests.quests.Quest;

import java.lang.reflect.Type;

public class PlayerContribtuionSerializer implements JsonSerializer<PlayerContribution> {
    @Override
    public JsonElement serialize(PlayerContribution src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();

        JsonArray completed = new JsonArray();
        JsonArray active = new JsonArray();

        if (src.getCompletedQuests() != null) {
            for (Quest quest: src.getCompletedQuests())
                completed.add(context.serialize(quest));
        }

        for (Quest quest: src.getActiveQuests())
            if (quest != null)
                active.add(context.serialize(quest));

        result.addProperty("player", src.getPlayer().toString());
        result.addProperty("levelId", src.getLevelId());
        result.add("completedQuests", completed);
        result.add("activeQuests", active);

        return result;
    }
}
