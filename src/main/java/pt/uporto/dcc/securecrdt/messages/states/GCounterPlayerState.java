package pt.uporto.dcc.securecrdt.messages.states;

import lombok.Getter;
import pt.uporto.dcc.securecrdt.util.ShareTimestampPair;

import java.nio.ByteBuffer;
import java.util.Arrays;

@Getter
public class GCounterPlayerState extends PlayerState {

    private final ShareTimestampPair[] counter;

    public GCounterPlayerState(ShareTimestampPair[] counter) {
        this.counter = counter;

    }

    public int getValue() {
        int totalIncrements = 0;
        for (ShareTimestampPair i : counter) {
            totalIncrements += i.getShare();
        }
        return totalIncrements;
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 8 * counter.length);
        buffer.putInt(counter.length);
        for (ShareTimestampPair i : counter) {
            buffer.putInt(i.getShare());
            buffer.putInt(i.getTimestamp());
        }
        buffer.flip();
        byte[] res = buffer.array();
        buffer.clear();
        return res;
    }


    public static GCounterPlayerState deserialize(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        int arraySize = buffer.getInt();
        ShareTimestampPair[] inc = new ShareTimestampPair[arraySize];
        for (int i = 0; i < arraySize; i++) {
            inc[i] = new ShareTimestampPair(buffer.getInt(), buffer.getInt());
        }
        return new GCounterPlayerState(inc);
    }

    @Override
    public String toString() {
        return "State:{" + Arrays.toString(counter) + "}";
    }
}
