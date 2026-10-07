package io.github.kltyton.kltytonui.container.datasource;

import io.github.kltyton.kltytonui.config.KltytonSavedData;
import io.github.kltyton.kltytonui.container.bind.ContainerBindType;
import io.github.kltyton.kltytonui.container.filter.FilterUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * SavedData 物品槽数据源，支持扩缩容（截断策略）。
 */
public final class SavedDataDataSource implements ContainerDataSource {
    private final ContainerBindType bindType;
    private final KltytonSavedData savedData;
    private final String inventoryKey;
    private ItemStackHandler handler;

    public SavedDataDataSource(ContainerBindType bindType,
                               KltytonSavedData savedData,
                               String inventoryKey,
                               ItemStackHandler handler) {
        this.bindType = bindType;
        this.savedData = savedData;
        this.inventoryKey = inventoryKey;
        this.handler = handler;
    }

    @Override
    public ContainerBindType bindType() {
        return bindType;
    }

    @Override
    public int capacity() {
        return handler.getSlots();
    }

    @Override
    public Slot createSlot(int slotIndex, int x, int y, FilterUtil filter) {
        return new FilterableSlotItemHandler(handler, slotIndex, x, y, filter);
    }

    @Override
    public boolean supportsResize() {
        return true;
    }

    @Override
    public int resize(int newCapacity) {
        int normalized = Math.max(1, newCapacity);
        handler = savedData.getOrCreate(inventoryKey, normalized);
        return handler.getSlots();
    }

    @Override
    public void onClose(ServerPlayer player) {
        savedData.setDirty();
    }
}
