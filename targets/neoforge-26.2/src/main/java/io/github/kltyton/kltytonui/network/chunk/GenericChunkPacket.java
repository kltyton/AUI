package io.github.kltyton.kltytonui.network.chunk;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.network.api.INetworkContext;
import io.github.kltyton.kltytonui.network.api.INetworkPacket;
import io.github.kltyton.kltytonui.network.api.NetworkPacket;
import io.github.kltyton.kltytonui.network.api.Side;
import net.minecraft.resources.Identifier;

import java.util.UUID;

@NetworkPacket(modId = KltytonUI.MODID, id = "generic_chunk", side = Side.BOTH)
public record GenericChunkPacket(UUID sessionId, int totalSize, short chunkIndex, short totalChunks,
                                 Identifier originalTypeId, byte[] chunkData)
        implements INetworkPacket<GenericChunkPacket> {

    @Override
    public void handle(INetworkContext context) {
        NeoForgeChunkAssembler.receiveChunk(sessionId, totalSize, chunkIndex, totalChunks, originalTypeId, chunkData, context);
    }
}
