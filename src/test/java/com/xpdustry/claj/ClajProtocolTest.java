package com.xpdustry.claj;

import java.nio.ByteBuffer;

import arc.util.io.ByteBufferInput;
import arc.util.io.ByteBufferOutput;
import org.junit.Test;

import com.xpdustry.claj.api.ClajClientSerializer;
import com.xpdustry.claj.api.ClajLink;
import com.xpdustry.claj.api.ClajProxy;
import com.xpdustry.claj.common.ClajNet;
import com.xpdustry.claj.common.ClajPackets;
import com.xpdustry.claj.common.packets.ConnectionPayloadPacket;
import com.xpdustry.claj.common.packets.RoomJoinPacket;
import com.xpdustry.claj.common.packets.RoomLinkPacket;
import com.xpdustry.claj.common.status.ClajType;
import com.xpdustry.claj.common.status.ClajVersion;

import static org.junit.Assert.*;

public class ClajProtocolTest {

    @Test
    public void versionParsesAndComparesCorrectly() {
        ClajVersion v245 = ClajVersion.of("2.4.5");
        assertEquals(2, v245.protocolVersion);
        assertEquals(4, v245.majorVersion);
        assertEquals(5, v245.minorVersion);
        assertEquals("2.4.5", v245.toString());

        assertTrue(v245.isAtLeast("2.4.0"));
        assertTrue(v245.isAtLeast("2.4.5"));
        assertFalse(v245.isAtLeast("2.4.6"));
        assertFalse(v245.isAtLeast("3.0.0"));
    }

    @Test
    public void linkRoundTripsWithAndWithoutScheme() {
        long roomId = 0x123456789ABCDEF0L;
        ClajLink original = new ClajLink("n4.xpdustry.com", 50000, roomId);
        String formatted = original.toString();

        assertTrue(formatted.startsWith("claj://n4.xpdustry.com:50000/"));
        assertEquals(original, ClajLink.fromString(formatted));
        assertEquals(original, ClajLink.fromString(formatted.substring(ClajLink.SCHEME.length())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void linkRejectsUncreatedRoomId() {
        new ClajLink("localhost", 7000, ClajProxy.UNCREATED_ROOM);
    }

    @Test
    public void clajPacketsRegisterAndRoundTripPayloadAndRoomJoin() {
        ClajNet.clear();
        ClajPackets.init();

        ConnectionPayloadPacket.serializer = new ConnectionPayloadPacket.Serializer() {
            @Override
            public void read(ConnectionPayloadPacket packet, ByteBufferInput read) {
                int len = read.readInt();
                byte[] bytes = new byte[len];
                read.readFully(bytes);
                packet.object = bytes;
            }

            @Override
            public void write(ConnectionPayloadPacket packet, ByteBufferOutput write) {
                byte[] bytes = (byte[]) packet.object;
                write.writeInt(bytes.length);
                write.write(bytes);
            }
        };

        ClajClientSerializer serializer = new ClajClientSerializer();

        RoomLinkPacket link = new RoomLinkPacket();
        link.roomId = 987654321L;

        ByteBuffer linkBuf = ByteBuffer.allocate(256);
        serializer.write(linkBuf, link);
        linkBuf.flip();

        Object decodedLink = serializer.read(linkBuf);
        assertTrue(decodedLink instanceof RoomLinkPacket);
        assertEquals(987654321L, ((RoomLinkPacket) decodedLink).roomId);

        RoomJoinPacket join = new RoomJoinPacket();
        join.type = ClajType.of("Mindustry");
        join.roomId = 987654321L;
        join.withPassword = true;
        join.password = 1234;

        ByteBuffer joinBuf = ByteBuffer.allocate(256);
        serializer.write(joinBuf, join);
        joinBuf.flip();
        assertTrue(joinBuf.remaining() > 2);

        ConnectionPayloadPacket payload = new ConnectionPayloadPacket();
        payload.conID = 42;
        payload.isTCP = false;
        payload.object = new byte[]{1, 2, 3, 4, 5};

        ByteBuffer payBuf = ByteBuffer.allocate(256);
        serializer.write(payBuf, payload);
        payBuf.flip();

        Object decodedPayload = serializer.read(payBuf);
        assertTrue(decodedPayload instanceof ConnectionPayloadPacket);
        ConnectionPayloadPacket cpp = (ConnectionPayloadPacket) decodedPayload;
        assertEquals(42, cpp.conID);
        assertFalse(cpp.isTCP);
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, (byte[]) cpp.object);
    }
}
