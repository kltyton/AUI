package io.github.kltyton.kltytonui.network.packet;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.network.api.INetworkContext;
import io.github.kltyton.kltytonui.network.api.INetworkPacket;
import io.github.kltyton.kltytonui.network.api.NetworkPacket;
import io.github.kltyton.kltytonui.network.api.Side;
import io.github.kltyton.kltytonui.network.handler.KltytonScreenNetworkHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端依据已展开 DOM 解析出的选择器槽位索引；过滤规则始终保留在服务端。
 */
@NetworkPacket(modId = KltytonUI.MODID, id = "resolve_slot_filters", side = Side.SERVER)
public record ResolveSlotFiltersPacket(int menuId,
                                       Map<String, Map<String, List<Integer>>> localIndicesBySelector)
        implements INetworkPacket<ResolveSlotFiltersPacket> {
    public ResolveSlotFiltersPacket {
        menuId = Math.max(0, menuId);
        localIndicesBySelector = copy(localIndicesBySelector);
    }

    private static Map<String, Map<String, List<Integer>>> copy(
            Map<String, Map<String, List<Integer>>> raw) {
        if (raw == null || raw.isEmpty()) return Map.of();

        LinkedHashMap<String, Map<String, List<Integer>>> result = new LinkedHashMap<>();
        raw.forEach((containerId, selectors) -> {
            if (containerId == null || containerId.isBlank() || selectors == null || selectors.isEmpty()) return;
            LinkedHashMap<String, List<Integer>> copiedSelectors = new LinkedHashMap<>();
            selectors.forEach((selector, indices) -> {
                if (selector == null || selector.isBlank() || indices == null || indices.isEmpty()) return;
                ArrayList<Integer> copiedIndices = new ArrayList<>(indices.size());
                for (Integer index : indices) {
                    if (index != null && index >= 0) copiedIndices.add(index);
                }
                if (!copiedIndices.isEmpty()) copiedSelectors.put(selector, List.copyOf(copiedIndices));
            });
            if (!copiedSelectors.isEmpty()) result.put(containerId, Map.copyOf(copiedSelectors));
        });
        return Map.copyOf(result);
    }

    @Override
    public void handle(INetworkContext context) {
        KltytonScreenNetworkHandler.handleResolveSlotFilters(this, context);
    }
}
