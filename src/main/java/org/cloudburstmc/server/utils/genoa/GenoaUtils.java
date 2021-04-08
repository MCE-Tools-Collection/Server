package org.cloudburstmc.server.utils.genoa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.genoa.packet.GenoaInventoryDataPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;

@Log4j2
public class GenoaUtils {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Setter
    private static String serverApiKey;

    public static String SendApiCommand(GenoaServerCommand commandType, Player player, String extraData) {
        try {
            final URL url = new URL("http://" + CloudServer.getInstance().getConfig().getSettings().getEarthApi() + "/v1.1/private/server/command");
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Cloudburst");
            connection.setDoOutput(true);

            ServerCommandRequest command = new ServerCommandRequest();
            command.setCommand(commandType);
            command.setServerId(CloudServer.getInstance().getServerUniqueId());

            if (player != null) {
                final String playerId = player.getSkin().getSkinId().split("-")[5].toUpperCase(); // Since playfab id isnt set normally, we can use this
                command.setPlayerId(playerId);
            }

            command.setApiKey(serverApiKey);

            if (!extraData.equals("none")) command.setRequestData(extraData);

            String request = OBJECT_MAPPER.writeValueAsString(command);

            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.write(request.getBytes());
                outputStream.flush();
            }

            StringBuilder response;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                response = new StringBuilder();

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            return response.toString();

        } catch (Exception e) {
            log.debug("Something went wrong while trying to reach the main api.");
            e.printStackTrace();
            return null;
        }
    }

    public static String SendApiCommand(GenoaServerCommand commandType, Player player) {
        return SendApiCommand(commandType, player, "none");
    }

    public static void NotifyInventoryUpdate(ItemStack newItem, int slotIndex, Player player, boolean removalRequest) {
        try {

        EditInventoryRequest request = new EditInventoryRequest();
        request.setCount(newItem.getAmount());
        request.setIdentifier(newItem.getType().getId().getName());
        request.setMeta(((CloudItemStack) newItem).getNbt().getShort("Damage"));
        request.setRemoveItem(removalRequest);
        request.setSlotIndex(slotIndex);
        request.setHealth(0.0f);

        String jsonData = OBJECT_MAPPER.writeValueAsString(request);
        SendApiCommand(GenoaServerCommand.EditInventory, player, jsonData);

        log.debug("Notified api of inventory update! slotIndex: " + slotIndex);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ApplyHotbarUpdates(Player player, String hotbarTranslations) {

        try {
            HotbarTranslation[] translations = OBJECT_MAPPER.readValue(hotbarTranslations, HotbarTranslation[].class);
            for (HotbarTranslation translation : translations) {

                if (translation.getIdentifier().equals("air"))
                    player.getInventory().clear(translation.getSlotId(), false);
                else {
                    NbtMap nbt = NbtMap.builder()
                            .putString("Name", Identifier.from("minecraft", translation.getIdentifier()).toString())
                            .putByte("Count", (byte) translation.getCount())
                            .putByte("Damage", (byte) translation.getMeta())
                            .build();

                    CloudItemStack itemStack = (CloudItemStack) ItemUtils.deserializeItem(nbt);
                    player.getInventory().setItem(translation.getSlotId(), itemStack, false);
                }
            }
        } catch (Exception e) {
            log.debug("Something went wrong while applying the hotbar updates.");
            e.printStackTrace();
        }
    }

    public static void GetHotbarOnJoin(Player player) {
        String response = SendApiCommand(GenoaServerCommand.GetInventory, player);
        ApplyHotbarUpdates(player, response);
    }
}
