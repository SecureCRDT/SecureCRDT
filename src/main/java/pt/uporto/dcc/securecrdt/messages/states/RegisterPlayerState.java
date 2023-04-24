package pt.uporto.dcc.securecrdt.messages.states;

import lombok.Getter;

import java.nio.ByteBuffer;

@Getter
public class RegisterPlayerState extends PlayerState {

    private final int share;
    private final int timestamp;

    public RegisterPlayerState(int share, int timestamp) {
        this.share = share;
        this.timestamp = timestamp;
    }


    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 4);
        buffer.putInt(share);
        buffer.putInt(timestamp);
        buffer.flip();
        byte[] res = buffer.array();
        buffer.clear();
        return res;
    }


    public static RegisterPlayerState deserialize(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        int share = buffer.getInt();
        int timestamp = buffer.getInt();
        return new RegisterPlayerState(share, timestamp);
    }

    @Override
    public String toString() {
        return "(" + share + ", " + timestamp + ")";
    }
}
