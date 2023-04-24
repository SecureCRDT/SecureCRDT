package pt.uporto.dcc.securecrdt.messages;

import lombok.Getter;

import java.nio.ByteBuffer;

@Getter
public class IntProtocolMessage {

    private final String protocol;
    private final byte[] data;

    public IntProtocolMessage(String protocol, byte[] data) {
        this.protocol = protocol;
        this.data = data;
    }

    public byte[] serialize() {
        byte[] protocolAsBytes = this.protocol.getBytes();
        int msgLength = 4 + protocolAsBytes.length + 4 + data.length;
        ByteBuffer buffer = ByteBuffer.allocate(msgLength);
        buffer.putInt(protocolAsBytes.length);
        buffer.put(protocolAsBytes);
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.flip();
        byte[] res = buffer.array();
        buffer.clear();
        return res;
    }

    public static IntProtocolMessage deserialize(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        int protocolSize = buffer.getInt();
        byte[] protocolAsBytes = new byte[protocolSize];
        for (int i = 0; i < protocolSize; i++) {
            protocolAsBytes[i] = buffer.get();
        }
        String protocol = new String(protocolAsBytes);
        int messageSize = buffer.getInt();
        byte[] messageAsBytes = new byte[messageSize];
        for (int i = 0; i < messageSize; i++) {
            messageAsBytes[i] = buffer.get();
        }
        return new IntProtocolMessage(protocol, messageAsBytes);
    }


}
