package org.cloudburstmc.server.utils.genoa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nukkitx.protocol.genoa.packet.GenoaInventoryDataPacket;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

@Log4j2
public class GenoaUtils {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String SendApiCommand(GenoaServerCommand commandType, UUID playerId, String extraData) {
        try {
            final URL url = new URL(CloudServer.getInstance().getConfig().getSettings().getEarthApi() + "/v1.1/private/server/command");
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Cloudburst");
            connection.setDoOutput(true);

            ServerCommandRequest command = new ServerCommandRequest();
            command.setApiKey(UUID.randomUUID());// TODO: Need to get this from the allocator
            command.setCommand(commandType);
            command.setPlayerId("InputOwnUserId"); // TODO: Need to get this from the allocator (Edit your own for testing)
            command.setServerId(UUID.randomUUID());// TODO: Need to get this from the allocator

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

    public static String SendApiCommand(GenoaServerCommand commandType, UUID playerId) {
        return SendApiCommand(commandType, playerId, "none");
    }

    public static void NotifyInventoryUpdate(ItemStack newItem,int slotIndex, UUID playerId, boolean removalRequest) {
        try {

        EditInventoryRequest request = new EditInventoryRequest();
        request.setCount(newItem.getAmount());
        request.setIdentifier(newItem.getType().getId().getName());
        request.setMeta(((CloudItemStack) newItem).getNbt().getShort("Damage"));
        request.setRemoveItem(removalRequest);
        request.setSlotIndex(slotIndex);
        request.setHealth(0.0f);

        String jsonData = OBJECT_MAPPER.writeValueAsString(request);
        SendApiCommand(GenoaServerCommand.EditInventory, playerId, jsonData);

        log.debug("Notified api of inventory update! slotIndex: " + slotIndex);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
