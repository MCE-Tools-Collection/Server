package org.cloudburstmc.server.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.daporkchop.lib.random.impl.FastPRandom.mix32;

@Log4j2
public class BlockPalette {

    public static final BlockPalette INSTANCE = new BlockPalette();

    private final Reference2IntMap<BlockState> stateRuntimeMap = new Reference2IntOpenHashMap<>();
    private final Int2ReferenceMap<BlockState> runtimeStateMap = new Int2ReferenceOpenHashMap<>();
    private final Object2ReferenceMap<NbtMap, BlockState> serializedStateMap = new Object2ReferenceLinkedOpenCustomHashMap<>(new Hash.Strategy<NbtMap>() {
        @Override
        public int hashCode(NbtMap o) {
            return mix32(o.hashCode());
        }

        @Override
        public boolean equals(NbtMap a, NbtMap b) {
            return Objects.equals(a, b);
        }
    });
    private final Reference2ObjectMap<BlockState, NbtMap> stateSerializedMap = new Reference2ObjectLinkedOpenHashMap<>();
    private final AtomicInteger runtimeIdAllocator = new AtomicInteger();
    private final Reference2ReferenceMap<BlockType, BlockState> defaultStateMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Identifier, BlockState> stateMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Identifier, Object2ReferenceMap<NbtMap, BlockState>> stateTraitMap = new Reference2ReferenceOpenHashMap<>();
    private final Map<String, Set<Object>> vanillaTraitMap = new HashMap<>();
    private final SortedMap<String, List<CloudBlockState>> sortedPalette = new Object2ReferenceRBTreeMap<>();

    public void addBlock(BlockType type, BlockSerializer serializer, BlockTrait<?>[] traits) {
        if (this.defaultStateMap.containsKey(type)) {
            log.warn("Duplicate block type: {}", type);
        }

        Map<NbtMap, CloudBlockState> states = getBlockPermutations(type, serializer, traits);

        Map<Map<BlockTrait<?>, Comparable<?>>, CloudBlockState> map = new HashMap<>();
        for (CloudBlockState state : states.values()) {
            map.put(state.getTraits(), state);
        }

        BlockState defaultState = map.get(Arrays.stream(traits).filter(t -> !t.isOnlySerialize()).collect(Collectors.toMap(t -> t, BlockTrait::getDefaultValue)));
        this.defaultStateMap.put(type, defaultState);
        states.forEach((nbt, state) -> {

            String id = nbt.getString("name");
            if (!sortedPalette.containsKey(id)) {
                sortedPalette.put(id, new ArrayList<>());
            }
            sortedPalette.get(id).add(state);

            if (!state.isInitialized()) {
                state.initialize(defaultState, map);

                this.stateMap.putIfAbsent(state.getId(), state.defaultState());
            }

            val stateMap = nbt.getCompound("states");

            val traitMap = stateTraitMap.computeIfAbsent(state.getId(), (v) -> new Object2ReferenceOpenHashMap<>());
            traitMap.put(stateMap, state);

            stateMap.forEach((traitName, traitValue) -> {
                val traitValues = vanillaTraitMap.computeIfAbsent(traitName, (k) -> new LinkedHashSet<>());
                traitValues.add(traitValue);
            });

            this.stateSerializedMap.put(state, nbt);
            this.serializedStateMap.put(nbt, state);

        });
    }

    public void generateRuntimeIds() {
        if (!this.runtimeStateMap.isEmpty() || !this.stateRuntimeMap.isEmpty()) {
            log.warn("Palette runtime IDs have already been generated!");
            return;
        }

        sortedPalette.forEach((id, states) -> {
            for (BlockState state : states) {
                int runtimeId = runtimeIdAllocator.getAndIncrement();

                /*
                MC:Earths' switch-case of shame
                needed to successfully initialize runtime id palette since they leave some gaps
                 */
                switch (runtimeId) {

                    // case 1987: // Magenta Terracotta
                    //case 2101: // Pink Terracotta
                    //case 2108: // The list lists packed_ice, but we already have it at 2100

                    case 1368: // Fence Gate/Fire, Direction
                    case 1927: // Lit Pumpkin, Direction???
                    case 1932: // Redstone Lamp, Direction
                    case 2169: // portal axis
                    case 2229: // Prismarine Stairs
                    case 2456: // Repeating Command Block/Sand
                    case 2501: // Sea Lantern
                    case 2818: // Stone/Stone Stairs
                    case 2988: // Sweet Berry Bush/Tallgrass
                    case 2995: // Torch

                    case 554: // Pumpkin/Cauldron, Direction NSEW
                    case 525:
                    case 1608: // Invisible Constraint/IronBars
                        runtimeId = runtimeIdAllocator.getAndIncrement();
                        break;

                    //case 405: // Birch Wall Sign/Black Terracotta, FACING_DIRECTION
                    //case 466: // Stairs -> Terracotta, FACING_DIRECTION
                    //case 610: // Chest, FACING_DIRECTION
                    //case 1034: // Diamond Block/Detector Rail
                    //case 1454: // Furnace, FACING_DIRECTION
                    //case 1489: // Gray Terracotta, FACING_DIRECTION
                    //case 1497: // Green Terracotta, FACING_DIRECTION
                    //case 1881: // Lever, Lever_Direction?
                    //case 1889: // White Terracotta, FACING_DIRECTION
                    //case 1913: // Lime Terracotta, FACING_DIRECTION
                    //case 1940: // Smoker, FACING_DIRECTION
                    //case 2115: // Piston, FACING_DIRECTION
                    //case 2123: // Piston Arm Collision, FACING_DIRECTION
                    //case 2130: // Piston Arm Collision Sticky, FACING_DIRECTION
                    //case 2244: // Purple Terracotta, FACING_DIRECTION
                    //case 2809: // Sticky Piston, FACING_DIRECTION
                    //case 3024: // Trapped Chest, FACING_DIRECTION
                    //case 3078: // Underwater Torch, TORCH_DIRECTION
                    //case 3147: // Wall Banner
                    //case 3183: // White Terracotta, FACING_DIRECTION

                    case 417: // Blast Furnace
                    case 269: // Beetroot/Bedrock
                    case 1350: // Fence (Gate), Tree_Species

                    case 2004: // Monster Egg/Mossy Cobble
                    case 2139: // Podzol/Planks, Tree_Species
                    case 2333: // Rainbow Wool/Red Flower
                    case 2406: // Redstone Torch/Wire
                    case 2483: // Sapling, Tree_Species
                    case 2530: // Shulker Box/ White Terracotta

                    case 3018: // Trapdoor, DIRECTION
                    case 3091: // Comparator
                        runtimeIdAllocator.addAndGet(1);
                        runtimeId = runtimeIdAllocator.getAndIncrement();
                        break;

                    //case 3082: // Redstone Torch, TORCH_DIRECTION

                    case 2288: // Quartz Stairs
                    case 2943: // Stonecutter/Stripped Logs, FACING_DIRECTION but still +3 remaining
                        runtimeIdAllocator.addAndGet(2);
                        runtimeId = runtimeIdAllocator.getAndIncrement();
                        break;


                    //case 245: // Barrel
                    // case 583:
                    //case 1060: // Dispenser, FACING_DIRECTION
                    //case 2453: // RepeatingCommandBlock, FACING_DIRECTION + Remove Reserved6
                    //case 2839: // Button/Pressure Plate, FACING_DIRECTION

                    case 213: // Bamboo
                    case 229: // Bamboo Sapling
                    case 443: // Bone Block
                    case 1076: // Double Plant/Double Stone Slabs
                    case 1156: // Wooden Slab/Dragon Egg, Tree_Species

                    case 2282: // Quartz Blocks, Axis

                    case 3267: // Wooden Slab/Wool, Tree_Species?
                        runtimeIdAllocator.addAndGet(3);
                        runtimeId = runtimeIdAllocator.getAndIncrement();
                        break;

                    case 2346: // Red Flower/Red Terracotta
                        runtimeIdAllocator.addAndGet(4);
                        runtimeId = runtimeIdAllocator.getAndIncrement();
                        break;

                    case 1138:// Stone Slabs

                    //case 2087: // Observer/Obsidian, can possibly be reduced by FACING_DIRECTION
                    //case 2560: // Skull+Smoker FACING_DIRECTION

                    case 2309: // Rail/Rainbow Bed
                    case 2917: // Stone Slabs/Stairs, FACING_DIRECTION
                    case 2966: // Stripped Logs, AXIS changes (include x,y,z,w?)
                    case 3079: // Redstone Torch
                        runtimeIdAllocator.addAndGet(5);
                        runtimeId = runtimeIdAllocator.getAndIncrement();
                        break;

                    //case 1328: // Ender Chest/Farmland, might be fixed with FACING_DIRECTION though

                    case 128: // Activator Rail/Adventure Chest
                    case 1513: // Grindstone
                    case 1596: // Ice
                    case 1644: // Iron Trapdoor/Jungle Button
                        runtimeIdAllocator.addAndGet(7);
                        runtimeId = runtimeIdAllocator.getAndIncrement();
                        break;

                    case 3197: // Wood/Wooden Button
                        runtimeIdAllocator.addAndGet(9);
                        runtimeId = runtimeIdAllocator.getAndIncrement();
                        break;

                    case 1849: // Lever/Leaves, Tree_Species
                        runtimeIdAllocator.addAndGet(15);
                        runtimeId = runtimeIdAllocator.getAndIncrement();
                        break;

                    case 697: // Command Block/Composter
                    case 842: // Coral Fan Hang, Direction
                    case 1965: // log/loom
                        runtimeIdAllocator.addAndGet(14);
                        runtimeId = runtimeIdAllocator.getAndIncrement();
                        break;

                    default:
                        break;
                }

                log.debug("RuntimeID: " + runtimeId + " BlockState: " + state);

                this.runtimeStateMap.put(runtimeId, state);
                this.stateRuntimeMap.put(state, runtimeId);
            }
        });

    }

    public Map<String, Set<Object>> getVanillaTraitMap() {
        return vanillaTraitMap;
    }

    public BlockState getState(Identifier id) {
        return this.stateMap.get(id);
    }

    public BlockState getState(Identifier id, Map<String, Object> traits) {
        return Optional.ofNullable(this.stateTraitMap.get(id)).map(s -> s.get(traits)).orElse(null);
    }

    public Set<String> getTraits(Identifier blockId) {
        return Optional.ofNullable(this.stateTraitMap.get(blockId)).map(m -> Iterables.getLast(m.keySet()).keySet()).orElse(null);
    }

    public BlockState getDefaultState(BlockType blockType) {
        return this.defaultStateMap.get(blockType);
    }

    public BlockState getBlockState(int runtimeId) {
        BlockState blockState = this.runtimeStateMap.get(runtimeId);
        if (blockState == null) {
            throw new IllegalArgumentException("Invalid runtime ID: " + runtimeId);
        }
        return blockState;
    }

    @Nullable
    public BlockState getBlockState(NbtMap tag) {
        return this.serializedStateMap.get(tag);
    }

    public int getRuntimeId(BlockState blockState) {
        int runtimeId = this.stateRuntimeMap.getInt(blockState);
        if (runtimeId == -1) {
            throw new IllegalArgumentException("Invalid BlockState: " + blockState);
        }
        return runtimeId;
    }

    public NbtMap getSerialized(BlockState state) {
        NbtMap serializedTag = this.stateSerializedMap.get(state);
        if (serializedTag == null) {
            throw new IllegalArgumentException("Invalid BlockState: " + state);
        }
        return serializedTag;
    }

    public Map<NbtMap, BlockState> getSerializedPalette() {
        return this.serializedStateMap;
    }

    public Map<BlockState, NbtMap> getStateMap() {
        return this.stateSerializedMap;
    }

    private static Collection<NbtMap> serialize(BlockType type, BlockSerializer serializer, Map<BlockTrait<?>, Comparable<?>> traits) {
        List<NbtMapBuilder> tags = new LinkedList<>();
        serializer.serialize(tags, type, traits);

        for (NbtMapBuilder tagBuilder : tags) {
            if (tagBuilder.containsKey("name")) {
                BlockStateUpdaters.serializeCommon(tagBuilder, (String) tagBuilder.get("name"));
            } else {
                Preconditions.checkState(type.getId() != null, "BlockType has not an identifier assigned");
                BlockStateUpdaters.serializeCommon(tagBuilder, type.getId().toString());
            }
        }

        return tags.stream().map(NbtMapBuilder::build).collect(Collectors.toList());
    }

    private static Map<NbtMap, CloudBlockState> getBlockPermutations(BlockType type, BlockSerializer serializer, BlockTrait<?>[] traits) {
        if (traits == null || traits.length == 0) {
            Preconditions.checkNotNull(type.getId(), "", type);
            val tags = serialize(type, serializer, ImmutableMap.of());
            val state = new CloudBlockState(type.getId(), type, ImmutableMap.of(),
                    Reference2IntMaps.emptyMap()/*, ImmutableList.copyOf(tags)*/);
            // No traits so 1 permutation.
            return tags.stream().collect(Collectors.toMap(nbt -> nbt, (s) -> state));
        }

        Reference2IntMap<BlockTrait<?>> traitPalette = new Reference2IntOpenHashMap<>();
        int id = 0;
        for (BlockTrait<?> trait : traits) {
            traitPalette.put(trait, id++);
        }

        Map<Map<BlockTrait<?>, Comparable<?>>, CloudBlockState> duplicated = new Object2ReferenceOpenHashMap<>();
        Map<NbtMap, CloudBlockState> permutations = new Object2ReferenceLinkedOpenHashMap<>();
        int n = traits.length;

        // To keep track of next element in each of the n arrays
        int[] indices = new int[n];

        // Initialize with first element's index
        for (int i = 0; i < n; i++) {
            indices[i] = 0;
        }

        while (true) {
            // Add current combination
            ImmutableMap.Builder<BlockTrait<?>, Comparable<?>> builder = ImmutableMap.builder();
            ImmutableMap.Builder<BlockTrait<?>, Comparable<?>> serializeBuilder = ImmutableMap.builder();
            for (int i = 0; i < n; i++) {
                BlockTrait<?> trait = traits[i];
                Comparable<?> val = trait.getPossibleValues().get(indices[i]);

                if (!trait.isOnlySerialize()) {
                    builder.put(trait, val);
                }
                serializeBuilder.put(trait, val);
            }

            val traitMap = builder.build();
            Collection<NbtMap> tags = serialize(type, serializer, serializeBuilder.build());
            Preconditions.checkArgument(!tags.isEmpty(), "Block state must have at least one nbt tag");
            Preconditions.checkArgument(
                    tags.stream().map(m -> m.getString("name")).collect(Collectors.toSet()).size() == 1,
                    "Block state cannot represent multiple block ids"
            );

            CloudBlockState state = duplicated.get(traitMap);

            if (state == null) {
                state = new CloudBlockState(
                        Identifier.fromString(Iterables.getLast(tags).getString("name")),
                        type,
                        traitMap,
                        traitPalette
//                        ImmutableList.copyOf(tags)
                );
                duplicated.put(traitMap, state);
            }

//            if (state.getId() == BlockIds.HAY_BLOCK) {
//                for (NbtMap tag : tags) {
//                    log.info("palette: " + tag);
//                }
//            }

            for (NbtMap tag : tags) {
                permutations.put(tag, state);
            }

            // Find the rightmost array that has more elements left after the current element in that array
            int next = n - 1;
            while (next >= 0 && (indices[next] + 1 >= traits[next].getPossibleValues().size())) {
                next--;
            }

            // No such array is found so no more combinations left
            if (next < 0) break;

            // If found move to next element in that array
            indices[next]++;

            // For all arrays to the right of this array current index again points to first element
            for (int i = next + 1; i < n; i++) {
                indices[i] = 0;
            }
        }

        return permutations;
    }
}
