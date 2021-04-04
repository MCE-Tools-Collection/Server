package org.cloudburstmc.server.utils.genoa;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditInventoryRequest {
    private String identifier;
    private int meta;
    private int count;
    private int slotIndex;
    private float health;
    private boolean removeItem;

}
