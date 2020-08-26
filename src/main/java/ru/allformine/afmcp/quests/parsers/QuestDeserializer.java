package ru.allformine.afmcp.quests.parsers;

import com.google.gson.*;
import org.spongepowered.api.text.serializer.TextSerializers;
import ru.allformine.afmcp.AFMCorePlugin;
import ru.allformine.afmcp.quests.Quest;

import java.lang.reflect.Type;
import java.util.UUID;

public class QuestDeserializer implements JsonDeserializer<Quest> {
    @Override
    public Quest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        /*
                 String name,
                 String type,
                 String target,
                 String startMessage,
                 String finalMessage,
                 String lore,
                 Calendar questStart,
                 int timeLimit, // In minutes
                 int count,
                 PlayerContribution parent
         */

        Quest quest;
        JsonElement parent = jsonObject.get("parent");
        UUID realParent;
        if (!parent.isJsonNull()) {
            realParent = UUID.fromString(parent.getAsString());
            String levelId = AFMCorePlugin.questDataManager.getContribution(realParent).getLevelId();
            int questId = jsonObject.get("questId").getAsInt();
            quest = AFMCorePlugin.questDataManager
                    .getQuestById(levelId, questId);

            // Corrupt quest
            if (quest == null) {
                AFMCorePlugin.logger.error("Corrupted quest data was found during initial deserialization");
                AFMCorePlugin.logger.error("Parent: " + realParent + ", LID: " + levelId + ", QID: " + questId);
                return null;
            }

            quest.setParent(realParent);
            if (!jsonObject.get("progress").isJsonNull())
                quest.setProgress(jsonObject.get("progress").getAsInt());
        } else {
            quest = new Quest(
                    TextSerializers.JSON.deserialize(jsonObject.get("name").getAsString()),
                    jsonObject.get("type").getAsString(),
                    jsonObject.get("target").getAsString(),
                    TextSerializers.JSON.deserialize(jsonObject.get("startMessage").getAsString()),
                    TextSerializers.JSON.deserialize(jsonObject.get("finalMessage").getAsString()),
                    TextSerializers.JSON.deserialize(jsonObject.get("lore").getAsString()),
                    Integer.parseInt(jsonObject.get("count").getAsString()),
                    null
            );
        }


        return quest;
    }
}
