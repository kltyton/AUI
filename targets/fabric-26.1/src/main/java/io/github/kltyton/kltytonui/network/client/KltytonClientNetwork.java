package io.github.kltyton.kltytonui.network.client;

import io.github.kltyton.kltytonui.network.KltytonNetwork;
import io.github.kltyton.kltytonui.network.packet.CloseContainerRequestPacket;
import io.github.kltyton.kltytonui.network.packet.OpenScreenRequestPacket;
import io.github.kltyton.kltytonui.network.packet.ResolveSlotFiltersPacket;
import net.minecraft.client.Minecraft;

/** Client-only network requests. */
public final class KltytonClientNetwork {
    private KltytonClientNetwork() {
    }

    public static void requestOpenScreen(String path) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        KltytonNetwork.sendToServer(new OpenScreenRequestPacket(path));
    }

    public static void resolveSlotFilters(ResolveSlotFiltersPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || packet == null) return;
        KltytonNetwork.sendToServer(packet);
    }

    public static void requestCloseScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        KltytonNetwork.sendToServer(new CloseContainerRequestPacket());
    }
}
