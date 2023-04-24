package pt.uporto.dcc.securecrdt.crdt;

import pt.uminho.haslab.smpc.interfaces.Player;
import pt.uporto.dcc.securecrdt.communication.IntShareMessage;

import java.math.BigInteger;
import java.util.*;

public class SmpcPlayer implements Player {

    private final int playerID;
    private final CrdtPlayer owner;

    private final List<Queue<Integer[]>> intValues;

    public SmpcPlayer(int playerID, CrdtPlayer owner) {
        this.playerID = playerID;
        this.owner = owner;

        intValues = new ArrayList<>();
        intValues.add(new LinkedList<>());
        intValues.add(new LinkedList<>());
        intValues.add(new LinkedList<>());
    }

    /*
        Implementation of the Player interface methods
     */
    public int getPlayerID() {
        return this.playerID;
    }

    public void storeValue(Integer playerDest, Integer playerSource, BigInteger value) {}
    public void storeValues(Integer playerDest, Integer playerSource, List<byte[]> values) {}
    public void storeValues(Integer playerDest, Integer playerSource, int[] values) {
        //System.out.println(this.playerID + " received from " + playerSource + ": " + Arrays.toString(values));

        synchronized (this) {
            Integer[] vals = new Integer[values.length];
            for(int i = 0; i < values.length; i++){
                vals[i] = values[i];
            }

            this.intValues.get(playerSource).add(vals);
            notify();
        }

    }
    public void storeValues(Integer playerDest, Integer playerSource, long[] values) {}

    public synchronized BigInteger getValue(Integer originPlayerId){ return null; }
    public synchronized List<byte[]> getValues(Integer rec) { return null; }
    public synchronized int[] getIntValues(Integer sender) {

        while (this.intValues.get(sender).isEmpty() ) {
            try {
                wait();
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        }

        Integer[] theValues = intValues.get(sender).poll();
        int[] resValues = new int[Objects.requireNonNull(theValues).length];

        for(int i = 0; i < theValues.length; i++){
            resValues[i] = theValues[i];
        }

        return resValues;

    }
    public synchronized long[] getLongValues(Integer rec) { return null; }

    public void sendValueToPlayer(int playerId, BigInteger value) {}
    public void sendValueToPlayer(Integer playerID, List<byte[]> values) {}
    public void sendValueToPlayer(Integer playerID, int[] secrets) {

        IntShareMessage msg = new IntShareMessage(this.playerID, playerID, secrets);

        this.owner.getIoManager().getPlayerClient(playerID).sendSecretsMessage(msg);

        //System.out.println(this.playerID + " sent to " + playerID + ": " + Arrays.toString(secrets));
    }
    public void sendValueToPlayer(Integer playerID, long[] secrets) {}

}
