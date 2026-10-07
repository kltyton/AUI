package io.github.kltyton.kltytonui.network.packet;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.network.api.INetworkContext;
import io.github.kltyton.kltytonui.network.api.INetworkPacket;
import io.github.kltyton.kltytonui.network.api.NetworkPacket;
import io.github.kltyton.kltytonui.network.api.Side;
import io.github.kltyton.kltytonui.network.handler.KltytonScreenNetworkHandler;

import java.util.List;

/** 客户端回传服务端已声明 selector 在当前菜单 DOM 中解析出的本地槽位索引。 */
@NetworkPacket(modId = KltytonUI.MODID, id = "selector_filter_indices", side = Side.SERVER)
public record SelectorFilterIndicesPacket(int menuId, String containerId, String selector, List<Integer> localIndices)
        implements INetworkPacket<SelectorFilterIndicesPacket> {
    public SelectorFilterIndicesPacket {
        containerId = containerId == null ? "" : containerId;
        selector = selector == null ? "" : selector;
        localIndices = localIndices == null ? List.of() : List.copyOf(localIndices);
    }

    @Override
    public void handle(INetworkContext context) {
        KltytonScreenNetworkHandler.handleSelectorFilterIndices(this, context);
    }
}
