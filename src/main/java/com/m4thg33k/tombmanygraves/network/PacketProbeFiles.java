package com.m4thg33k.tombmanygraves.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetHandlerPlayServer;

// Unused, only registered to keep the discriminators of the other packets stable
public class PacketProbeFiles extends BaseThreadsafePacket {

    @Override
    public void handleClientSafe(NetHandlerPlayClient netHandler) {
    }

    @Override
    public void handleServerSafe(NetHandlerPlayServer netHandler) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        buf.skipBytes(buf.readableBytes());
    }
}
