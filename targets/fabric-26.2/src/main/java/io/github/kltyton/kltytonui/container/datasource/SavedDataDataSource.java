package io.github.kltyton.kltytonui.container.datasource;

import io.github.kltyton.kltytonui.config.KltytonSavedData;
import io.github.kltyton.kltytonui.container.bind.ContainerBindType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;

public final class SavedDataDataSource implements ContainerDataSource {
    private final ContainerBindType bindType;
    private final KltytonSavedData savedData;
    private final String inventoryKey;
    private SimpleContainer container;
    public SavedDataDataSource(ContainerBindType bindType, KltytonSavedData savedData, String inventoryKey, SimpleContainer container) { this.bindType = bindType; this.savedData = savedData; this.inventoryKey = inventoryKey; this.container = container; }
    public ContainerBindType bindType() { return bindType; }
    public int capacity() { return container.getContainerSize(); }
    public Slot createSlot(int slotIndex, int x, int y, SlotFilter filter) {
        return new Slot(FilteredContainer.of(container, filter), slotIndex, x, y);
    }
    public boolean supportsResize() { return true; }
    public int resize(int newCapacity) { container = savedData.getOrCreate(inventoryKey, Math.max(1, newCapacity)); return capacity(); }
    public void onClose(ServerPlayer player) { savedData.setDirty(); }
}
