package ru.alfomine.afmquests.quests.parsers;

import com.google.gson.*;
import ru.alfomine.afmquests.quests.QuestFaction;
import ru.alfomine.afmquests.quests.QuestFactionContainer;

import java.lang.reflect.Type;

public class QuestFactionContainerSerializer implements JsonSerializer<QuestFactionContainer> {
    @Override
    public JsonElement serialize(QuestFactionContainer src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();
        for (QuestFaction faction: src.getQuestFactions()) {
            array.add(context.serialize(faction, QuestFaction.class));
        }

        return array;
    }
}
