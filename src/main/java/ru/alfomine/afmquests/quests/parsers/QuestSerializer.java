package ru.alfomine.afmquests.quests.parsers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.spongepowered.api.text.serializer.TextSerializers;
import ru.alfomine.afmquests.AFMQuests;
import ru.alfomine.afmquests.quests.Quest;

import java.lang.reflect.Type;

public class QuestSerializer implements JsonSerializer<Quest> {
    @Override
    public JsonElement serialize(Quest src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();

        if (src.getParent() == null) {
            // Editor Serializer
            result.addProperty("name", TextSerializers.JSON.serialize(src.getName()));
            result.addProperty("type", src.getType());
            result.addProperty("target", src.getTarget());
            result.addProperty("count", src.getCount());
            result.addProperty("startMessage", TextSerializers.JSON.serialize(src.getStartMessage()));
            result.addProperty("finalMessage", TextSerializers.JSON.serialize(src.getFinalMessage()));
            result.addProperty("lore", TextSerializers.JSON.serialize(src.getLore()));

        } else {
            // Faction Serializer
            // It's way more abstract for the sake of economy
            int questId = AFMQuests.questDataManager
                    .getQuestDifficulties()
                    .getLevelById(AFMQuests.questDataManager.getContribution(src.getParent()).getLevelId())
                    .getQuestId(src.getName().toPlain());
            assert questId != -1; // Means that quest doesn't exist
            result.addProperty("questId", questId);
            result.addProperty("parent", src.getParent().toString());
            //// TODO: Save levelId for completed quests for the sake of not messing up with quests
            if (!src.finished()) {
                result.addProperty("progress", src.getProgress());
            }
        }

        return result;
    }
}
