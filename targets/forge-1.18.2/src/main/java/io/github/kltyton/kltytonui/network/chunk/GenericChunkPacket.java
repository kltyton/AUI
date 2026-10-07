package io.github.kltyton.kltytonui.network.chunk;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.network.api.INetworkContext;
import io.github.kltyton.kltytonui.network.api.INetworkPacket;
import io.github.kltyton.kltytonui.network.api.NetworkPacket;
import io.github.kltyton.kltytonui.network.api.Side;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

@NetworkPacket(modId = KltytonUI.MODID, id = "generic_chunk", side = Side.BOTH)
public record GenericChunkPacket(UUID sessionId, int totalSize, short chunkIndex, short totalChunks,
                                 ResourceLocation originalTypeId, byte[] chunkData)
        implements INetworkPacket<GenericChunkPacket> {

    @Override
    public void handle(INetworkContext context) {
        GenericChunkAssembler.receiveChunk(sessionId, totalSize, chunkIndex, totalChunks, originalTypeId, chunkData, context);
    }
}
