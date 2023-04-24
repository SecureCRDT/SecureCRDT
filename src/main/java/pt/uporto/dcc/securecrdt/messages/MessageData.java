package pt.uporto.dcc.securecrdt.messages;

import lombok.Getter;

import java.nio.ByteBuffer;

@Getter
public class MessageData {

    private final String operation;
    private final int share;
    private final int timestamp;
    private final int toPlayer;

    public MessageData(String operation, int share, int timestamp, int toPlayer) {
        this.operation = operation;
        this.share = share;
        this.timestamp = timestamp;
        this.toPlayer = toPlayer;
    }

    public byte[] serialize() {
        byte[] operationAsBytes = operation == null ? new byte[0] :  operation.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(4 + operationAsBytes.length + 4 + 4 + 4);

        buffer.putInt(operationAsBytes.length);
        if (operationAsBytes.length != 0) {
            buffer.put(operationAsBytes);
        }

        buffer.putInt(share);
        buffer.putInt(timestamp);

        buffer.putInt(toPlayer);

        buffer.flip();
        byte[] res = buffer.array();
        buffer.clear();
        return res;
    }


    public static MessageData deserialize(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);

        int operationSize = buffer.getInt();
        byte[] operationAsBytes = new byte[operationSize];
        for (int i = 0; i < operationSize; i++) {
            operationAsBytes[i] = buffer.get();
        }
        String operation = new String(operationAsBytes);

        int share = buffer.getInt();
        int timestamp = buffer.getInt();

        int toPlayer = buffer.getInt();

        return new MessageData(operation, share, timestamp, toPlayer);
    }

    @Override
    public String toString() {
        return "(" + operation + ", " + share + ", " + timestamp + ", " + toPlayer + ")";
    }
}
