package pt.uporto.dcc.securecrdt.messages.states;

import lombok.Getter;

import java.nio.ByteBuffer;

@Getter
public class PNCounterPlayerState extends PlayerState {

    private final GCounterPlayerState incrementCounter;
    private final GCounterPlayerState decrementCounter;

    public PNCounterPlayerState(GCounterPlayerState incrementCounter, GCounterPlayerState decrementCounter) {
        this.incrementCounter = incrementCounter;
        this.decrementCounter = decrementCounter;

    }

    public int getValue() {
        int totalIncrements = incrementCounter.getValue();
        int totalDecrements = decrementCounter.getValue();
        return totalIncrements - totalDecrements;
    }

    public byte[] serialize() {
        byte[] incrementCounterAsBytes = incrementCounter.serialize();
        byte[] decrementCounterAsBytes = decrementCounter.serialize();
        ByteBuffer buffer = ByteBuffer.allocate(8 + incrementCounterAsBytes.length + decrementCounterAsBytes.length);
        buffer.putInt(incrementCounterAsBytes.length);
        buffer.putInt(decrementCounterAsBytes.length);
        buffer.put(incrementCounterAsBytes);
        buffer.put(decrementCounterAsBytes);
        buffer.flip();
        byte[] res = buffer.array();
        buffer.clear();
        return res;
    }


    public static PNCounterPlayerState deserialize(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        int incrementCounterSize = buffer.getInt();
        int decrementCounterSize = buffer.getInt();

        byte[] incrementCounterAsBytes = new byte[incrementCounterSize];
        for (int i = 0; i < incrementCounterSize; i++) {
            incrementCounterAsBytes[i] = buffer.get();
        }
        GCounterPlayerState inc = GCounterPlayerState.deserialize(incrementCounterAsBytes);

        byte[] decrementCounterAsBytes = new byte[decrementCounterSize];
        for (int i = 0; i < incrementCounterSize; i++) {
            decrementCounterAsBytes[i] = buffer.get();
        }
        GCounterPlayerState dec = GCounterPlayerState.deserialize(decrementCounterAsBytes);

        return new PNCounterPlayerState(inc, dec);
    }

    @Override
    public String toString() {
        return "State:{" + incrementCounter.toString() + ", " + decrementCounter.toString() + "}";
    }
}
