package org.cloudburstmc.server.inventory;

import org.cloudburstmc.server.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * @author CreeperFace
 */
public interface CraftingRecipe extends Recipe {

    String getRecipeId();

    UUID getId();

    void setId(UUID id);

    boolean requiresCraftingTable();

    List<ItemStack> getExtraResults();

    List<ItemStack> getAllResults();

    int getPriority();

    /**
     * Returns whether the specified list of crafting grid inputs and outputs matches this recipe. Outputs DO NOT
     * include the primary result item.
     *
     * @param input  2D array of items taken from the crafting grid
     * @param output 2D array of items put back into the crafting grid (secondary results)
     * @return bool
     */
    boolean matchItems(ItemStack[][] input, ItemStack[][] output);
}
