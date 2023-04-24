package pt.uporto.dcc.securecrdt.communication;

import java.nio.ByteBuffer;

public class IntShareMessage {

    private final int sourcePlayer;
    private final int destPlayer;
    private final int[] values;

    public IntShareMessage(int sourcePlayer, int destPlayer, int[] values) {
        this.sourcePlayer = sourcePlayer;
        this.destPlayer = destPlayer;
        this.values = values;
    }

    public byte[] serialize() {
        int msgLength = 3 * 4 + values.length * 4;
        ByteBuffer buffer = ByteBuffer.allocate(msgLength);
        buffer.putInt(this.sourcePlayer);
        buffer.putInt(this.destPlayer);
        buffer.putInt(this.values.length);
        for (int val : this.values) {
            buffer.putInt(val);
        }
        buffer.flip();
        byte[] res = buffer.array();
        buffer.clear();
        return res;
    }

    public static IntShareMessage deserialize(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        int sourcePlayer = buffer.getInt();
        int destPlayer = buffer.getInt();
        int valuesLength = buffer.getInt();
        int[] values = new int[valuesLength];
        for (int i = 0; i < valuesLength; i++) {
            values[i] = buffer.getInt();
        }
        return new IntShareMessage(sourcePlayer, destPlayer, values);
    }

    public int getSourcePlayer() {
        return sourcePlayer;
    }

    public int getDestPlayer() {
        return destPlayer;
    }

    public int[] getValues() {
        return values;
    }
}
