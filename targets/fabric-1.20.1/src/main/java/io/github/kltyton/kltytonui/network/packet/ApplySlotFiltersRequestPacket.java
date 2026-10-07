package io.github.kltyton.kltytonui.network.packet;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.network.api.INetworkContext;
import io.github.kltyton.kltytonui.network.api.INetworkPacket;
import io.github.kltyton.kltytonui.network.api.NetworkPacket;
import io.github.kltyton.kltytonui.network.api.Side;
import io.github.kltyton.kltytonui.network.handler.KltytonScreenNetworkHandler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** 客户端确认的 selector 命中槽位索引；过滤声明始终由服务端持有。 */
@NetworkPacket(modId = KltytonUI.MODID, id = "apply_slot_filters", side = Side.SERVER)
public record ApplySlotFiltersRequestPacket(int menuId, List<SelectorSlotMapping> mappings)
        implements INetworkPacket<ApplySlotFiltersRequestPacket> {
    public ApplySlotFiltersRequestPacket {
        ArrayList<SelectorSlotMapping> normalized = new ArrayList<>();
        if (mappings != null) {
            for (SelectorSlotMapping mapping : mappings) {
                if (mapping != null && mapping.isValid()) normalized.add(mapping);
            }
        }
        mappings = List.copyOf(normalized);
    }

    @Override
    public void handle(INetworkContext context) {
        KltytonScreenNetworkHandler.handleApplySlotFiltersRequest(this, context);
    }

    public record SelectorSlotMapping(String containerId, String selector, List<Integer> localIndices) {
        public SelectorSlotMapping {
            containerId = containerId == null ? "" : containerId;
            selector = selector == null ? "" : selector;
            LinkedHashSet<Integer> normalized = new LinkedHashSet<>();
            if (localIndices != null) {
                for (Integer index : localIndices) {
                    if (index != null && index >= 0) normalized.add(index);
                }
            }
            localIndices = List.copyOf(normalized);
        }

        private boolean isValid() {
            return !containerId.isBlank() && !selector.isBlank() && !localIndices.isEmpty();
        }
    }
}
