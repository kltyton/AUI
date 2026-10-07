package io.github.kltyton.kltytonui.network;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.network.handler.KltytonScreenNetworkHandler;
import io.github.kltyton.kltytonui.network.packet.CloseContainerRequestPacket;
import io.github.kltyton.kltytonui.network.packet.OpenScreenRequestPacket;
import io.github.kltyton.kltytonui.network.packet.ResolveSlotFiltersPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Fabric play-payload registration for the serverbound KUI requests. */
public final class KltytonNetwork {
    private static boolean registered;

    private KltytonNetwork() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        PayloadTypeRegistry.playC2S().register(
                OpenScreenRequestPacket.TYPE,
                OpenScreenRequestPacket.STREAM_CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                CloseContainerRequestPacket.TYPE,
                CloseContainerRequestPacket.STREAM_CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                ResolveSlotFiltersPacket.TYPE,
                ResolveSlotFiltersPacket.STREAM_CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
                OpenScreenRequestPacket.TYPE,
                (packet, context) -> context.server().execute(() ->
                        KltytonScreenNetworkHandler.handleOpenScreenRequest(context.player(), packet))
        );
        ServerPlayNetworking.registerGlobalReceiver(
                CloseContainerRequestPacket.TYPE,
                (packet, context) -> context.server().execute(() ->
                        KltytonScreenNetworkHandler.handleCloseContainerRequest(context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(
                ResolveSlotFiltersPacket.TYPE,
                (packet, context) -> context.server().execute(() ->
                        KltytonScreenNetworkHandler.handleResolveSlotFilters(context.player(), packet))
        );
    }

    public static void sendToServer(CustomPacketPayload message) {
        if (message == null) return;
        try {
            ClientPlayNetworking.send(message);
        } catch (RuntimeException exception) {
            KltytonUI.LOGGER.warn(
                    "[KUI Network] failed to send packet to server (server may not have this mod) message={}",
                    message.getClass().getSimpleName(),
                    exception
            );
        }
    }
}
