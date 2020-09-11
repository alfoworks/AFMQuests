package ru.alfomine.afmquests.quests.parsers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import ru.alfomine.afmquests.quests.QuestLevel;
import ru.alfomine.afmquests.quests.QuestLevelContainer;

import java.lang.reflect.Type;

public class QuestLevelContainerSerializer implements JsonSerializer<QuestLevelContainer> {
    @Override
    public JsonElement serialize(QuestLevelContainer src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();

        for (QuestLevel l : src.getQuestLevels()) {
            array.add(context.serialize(l));
        }

        return array;
    }
}
