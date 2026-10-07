package io.github.kltyton.kltytonui.network.packet;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.network.api.INetworkContext;
import io.github.kltyton.kltytonui.network.api.INetworkPacket;
import io.github.kltyton.kltytonui.network.api.NetworkPacket;
import io.github.kltyton.kltytonui.network.api.Side;
import net.minecraft.server.level.ServerPlayer;

@NetworkPacket(modId = KltytonUI.MODID, id = "close_container", side = Side.SERVER)
public record CloseContainerRequestPacket() implements INetworkPacket<CloseContainerRequestPacket> {
    @Override
    public void handle(INetworkContext context) {
        ServerPlayer player = context.sender();
        if (player != null) player.closeContainer();
    }
}
