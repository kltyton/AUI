package io.github.kltyton.kltytonui.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kltyton.kltytonui.KltytonUI;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用世界级库存 SavedData。
 * 热重载时直接截断：按目标容量物理重建，超出部分丢弃。
 *
 * <p>26.1 使用 codec 驱动的 {@link SavedDataType}，因此序列化改为
 * {@link #CODEC}，数据格式为 {@code Map<String, List<ItemStack>>}（每个库存一个列表）。</p>
 */
public final class KltytonSavedData extends SavedData {
    private static final String INVENTORIES_KEY = "inventories";

    public static final Codec<KltytonSavedData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, ItemStack.OPTIONAL_CODEC.listOf())
                            .fieldOf(INVENTORIES_KEY)
                            .forGetter(KltytonSavedData::exportInventories)
            ).apply(instance, KltytonSavedData::fromInventories));

    private final LinkedHashMap<String, SimpleContainer> inventories = new LinkedHashMap<>();

    public static KltytonSavedData get(MinecraftServer server, String dataName) {
        String name = dataName == null || dataName.isBlank() ? "kltytonui_data" : dataName.trim();
        Identifier id;
        try {
            id = Identifier.fromNamespaceAndPath(KltytonUI.MODID, name);
        } catch (Exception e) {
            id = Identifier.fromNamespaceAndPath(KltytonUI.MODID, "kltytonui_data");
        }
        SavedDataType<KltytonSavedData> type = new SavedDataType<>(
                id, KltytonSavedData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
        return server.overworld().getDataStorage().computeIfAbsent(type);
    }

    private Map<String, List<ItemStack>> exportInventories() {
        LinkedHashMap<String, List<ItemStack>> result = new LinkedHashMap<>();
        for (Map.Entry<String, SimpleContainer> entry : inventories.entrySet()) {
            SimpleContainer container = entry.getValue();
            ArrayList<ItemStack> stacks = new ArrayList<>(container.getContainerSize());
            for (int i = 0; i < container.getContainerSize(); i++) {
                stacks.add(container.getItem(i));
            }
            result.put(entry.getKey(), stacks);
        }
        return result;
    }

    private static KltytonSavedData fromInventories(Map<String, List<ItemStack>> map) {
        KltytonSavedData data = new KltytonSavedData();
        if (map != null) {
            map.forEach((key, stacks) -> {
                if (key == null || stacks == null) return;
                SimpleContainer container = data.createContainer(stacks.size());
                for (int i = 0; i < stacks.size(); i++) {
                    ItemStack stack = stacks.get(i);
                    if (stack != null && !stack.isEmpty()) {
                        container.setItem(i, stack.copy());
                    }
                }
                data.inventories.put(key, container);
            });
        }
        return data;
    }

    /**
     * 获取或创建指定容量的库存。
     * 若已存在且容量不同，按目标容量截断重建。
     */
    public SimpleContainer getOrCreate(String inventoryKey, int slotCount) {
        String key = normalizeInventoryKey(inventoryKey);
        int normalizedSlotCount = Math.max(1, slotCount);

        SimpleContainer existing = inventories.get(key);
        if (existing == null) {
            SimpleContainer created = createContainer(normalizedSlotCount);
            inventories.put(key, created);
            setDirty();
            return created;
        }

        if (existing.getContainerSize() == normalizedSlotCount) {
            return existing;
        }

        // 截断：按目标容量物理重建，保留可容纳的物品，超出部分丢弃。
        SimpleContainer resized = createContainer(normalizedSlotCount);
        int copyCount = Math.min(existing.getContainerSize(), normalizedSlotCount);
        for (int i = 0; i < copyCount; i++) {
            ItemStack stack = existing.getItem(i);
            if (stack.isEmpty()) continue;
            resized.setItem(i, stack.copy());
        }
        inventories.put(key, resized);
        setDirty();
        return resized;
    }

    private String normalizeInventoryKey(String inventoryKey) {
        if (inventoryKey == null || inventoryKey.trim().isEmpty()) {
            return "__default__";
        }
        return inventoryKey.trim();
    }

    private SimpleContainer createContainer(int slotCount) {
        int normalized = Math.max(1, slotCount);
        return new SimpleContainer(normalized) {
            @Override
            public void setChanged() {
                KltytonSavedData.this.setDirty();
            }
        };
    }
}
