package org.cloudburstmc.server.utils.genoa;

import lombok.Data;

import java.util.UUID;

@Data
public class ServerCommandRequest {
    private GenoaServerCommand command;
    private String playerId;
    private UUID apiKey;
    private UUID serverId;
    private String requestData;

}

