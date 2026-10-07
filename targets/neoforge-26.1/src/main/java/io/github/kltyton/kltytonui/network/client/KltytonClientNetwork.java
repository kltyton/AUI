package io.github.kltyton.kltytonui.network.client;


import io.github.kltyton.kltytonui.network.packet.CloseContainerRequestPacket;
import io.github.kltyton.kltytonui.network.packet.OpenScreenRequestPacket;
import io.github.kltyton.kltytonui.network.packet.SelectorFilterIndicesPacket;

import java.util.List;
import io.github.kltyton.kltytonui.network.api.NetworkManager;
import net.minecraft.client.Minecraft;


/** Client-only network requests. */
public final class KltytonClientNetwork {
    private KltytonClientNetwork() {
    }

    public static void requestOpenScreen(String path) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        NetworkManager.sendToServer(new OpenScreenRequestPacket(path));
    }

    public static void sendSelectorFilterIndices(int menuId, String containerId, String selector, List<Integer> localIndices) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || containerId == null || containerId.isBlank()
                || selector == null || selector.isBlank() || localIndices == null || localIndices.isEmpty()) {
            return;
        }
        NetworkManager.sendToServer(new SelectorFilterIndicesPacket(menuId, containerId, selector, localIndices));
    }

    public static void requestCloseScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        NetworkManager.sendToServer(new CloseContainerRequestPacket());
    }
}
