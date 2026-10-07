package io.github.kltyton.kltytonui.container.datasource;

import io.github.kltyton.kltytonui.container.bind.ContainerBindType;
import io.github.kltyton.kltytonui.container.filter.FilterUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;

/**
 * 服务端容器数据源统一抽象。
 */
public interface ContainerDataSource {
    ContainerBindType bindType();

    int capacity();

    default Slot createSlot(int slotIndex, int x, int y) {
        return createSlot(slotIndex, x, y, null);
    }

    default Slot createSlot(int slotIndex, int x, int y, FilterUtil filter) {
        return createSlot(slotIndex, x, y);
    }

    default boolean stillValid(ServerPlayer player) {
        return true;
    }

    default void onClose(ServerPlayer player) {
    }

    default boolean supportsResize() {
        return false;
    }

    /**
     * 调整容量，直接截断。返回调整后的实际容量。
     */
    default int resize(int newCapacity) {
        return capacity();
    }
}
