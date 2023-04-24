package pt.uporto.dcc.securecrdt.messages.states;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.util.ArrayList;

@Getter
public class SetPlayerState extends PlayerState {

    private final ArrayList<Integer> list;
    private final int queriedValueExists;

    public SetPlayerState(ArrayList<Integer> list, int queriedValueExists) {
        this.list = list;
        this.queriedValueExists = queriedValueExists;
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + list.size() * 4 + 4);
        buffer.putInt(list.size());
        for (int value : list) {
            buffer.putInt(value);
        }

        buffer.putInt(queriedValueExists);

        buffer.flip();
        byte[] res = buffer.array();
        buffer.clear();
        return res;
    }


    public static SetPlayerState deserialize(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        int arraySize = buffer.getInt();
        ArrayList<Integer> newList = new ArrayList<>();
        for (int i = 0; i < arraySize; i++) {
            newList.add(buffer.getInt());
        }
        int queriedValueExists = buffer.getInt();
        return new SetPlayerState(newList, queriedValueExists);
    }

    @Override
    public String toString() {
        return "State:{" + list.toString() + "}";
    }
}
