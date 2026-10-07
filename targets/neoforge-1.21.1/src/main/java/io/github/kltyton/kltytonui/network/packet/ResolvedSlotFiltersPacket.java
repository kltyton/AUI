package io.github.kltyton.kltytonui.network.packet;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.network.api.INetworkContext;
import io.github.kltyton.kltytonui.network.api.INetworkPacket;
import io.github.kltyton.kltytonui.network.api.NetworkPacket;
import io.github.kltyton.kltytonui.network.api.Side;
import io.github.kltyton.kltytonui.network.handler.KltytonScreenNetworkHandler;

import java.util.List;

/** 客户端完成 DOM selector 解析后回传的已验证本地槽位索引。 */
@NetworkPacket(modId = KltytonUI.MODID, id = "resolved_slot_filters", side = Side.SERVER)
public record ResolvedSlotFiltersPacket(int menuId, String containerId, String selector, List<Integer> localIndices)
        implements INetworkPacket<ResolvedSlotFiltersPacket> {
    public ResolvedSlotFiltersPacket {
        containerId = containerId == null ? "" : containerId.trim();
        selector = selector == null ? "" : selector.trim();
        localIndices = localIndices == null ? List.of() : List.copyOf(localIndices);
    }

    @Override
    public void handle(INetworkContext context) {
        KltytonScreenNetworkHandler.handleResolvedSlotFilters(this, context);
    }
}
