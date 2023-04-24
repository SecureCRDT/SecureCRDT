package pt.uporto.dcc.securecrdt.messages.states;

import lombok.Getter;
import pt.uminho.haslab.smpc.sharemindImp.Integer.IntSharemindDealer;
import pt.uporto.dcc.securecrdt.util.ShareTimestampPair;
import pt.uporto.dcc.securecrdt.util.Standards;

import java.nio.ByteBuffer;
import java.util.Arrays;

@Getter
public class MinBoundedCounterPlayerState extends PlayerState {

    private final int limitShare;
    private final ShareTimestampPair[][] rightsMatrix;
    private final ShareTimestampPair[] consumedRights;

    public MinBoundedCounterPlayerState(int limitShare, ShareTimestampPair[][] rightsMatrix, ShareTimestampPair[] consumedRights) {
        this.limitShare = limitShare;
        this.rightsMatrix = rightsMatrix;
        this.consumedRights = consumedRights;
    }

    public int getValue() {
        int totalIncrements = 0;
        int totalDecrements = 0;
        for (int i = 0; i < Standards.NUMBER_OF_REPLICAS; i++) {
            totalIncrements += rightsMatrix[i][i].getShare();
            totalDecrements += consumedRights[i].getShare();
        }
        return IntSharemindDealer.mod(limitShare + totalIncrements - totalDecrements);
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(8 + 24 * rightsMatrix.length);
        buffer.putInt(limitShare);
        buffer.putInt(rightsMatrix.length);
        for (int i = 0; i < rightsMatrix.length; i++) {
            for (int j = 0; j < rightsMatrix.length; j++) {
                buffer.putInt(rightsMatrix[i][j].getShare());
                buffer.putInt(rightsMatrix[i][j].getTimestamp());
            }
        }
        for (int i = 0; i < consumedRights.length; i++) {
            buffer.putInt(consumedRights[i].getShare());
            buffer.putInt(consumedRights[i].getTimestamp());
        }
        buffer.flip();
        byte[] res = buffer.array();
        buffer.clear();
        return res;
    }


    public static MinBoundedCounterPlayerState deserialize(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        int limit = buffer.getInt();
        int arraySize = buffer.getInt();
        ShareTimestampPair[][] rights = new ShareTimestampPair[arraySize][arraySize];
        ShareTimestampPair[] consumed = new ShareTimestampPair[arraySize];
        for (int i = 0; i < arraySize; i++) {
            for (int j = 0; j < arraySize; j++) {
                rights[i][j] = new ShareTimestampPair(buffer.getInt(), buffer.getInt());
            }
        }
        for (int i = 0; i < arraySize; i++) {
                consumed[i] = new ShareTimestampPair(buffer.getInt(), buffer.getInt());
        }
        return new MinBoundedCounterPlayerState(limit, rights, consumed);
    }

    @Override
    public String toString() {
        return "State:{Limit=" + limitShare + ", Rights=" + Arrays.deepToString(rightsMatrix) + ", Consumed=" + Arrays.toString(consumedRights) + "}";
    }
}
