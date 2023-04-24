package pt.uporto.dcc.securecrdt.client;

import lombok.Getter;
import pt.uminho.haslab.smpc.exceptions.InvalidSecretValue;
import pt.uporto.dcc.securecrdt.messages.states.PlayerState;

import java.io.IOException;

@Getter
public abstract class CrdtClient {
    public final Client client;

    public int timestamp;

    public boolean[] gotPlayerResults;

    public CrdtClient(Client client) {
        this.client = client;
        this.timestamp = 0;
    }

    public abstract void update(String input) throws IOException, InvalidSecretValue;

    public abstract int query(String input) throws IOException, InterruptedException, InvalidSecretValue;

    public abstract PlayerState[] propagate() throws IOException, InterruptedException;

    public abstract void merge() throws IOException, InvalidSecretValue;

    public void resetReplyData() {
        gotPlayerResults = new boolean[]{false, false, false};
    }

    public int convertUnderflowToNegative(int n) {
        String resultAsBinaryString = Integer.toBinaryString(n);
        if (resultAsBinaryString.length() == 30) {
            resultAsBinaryString = resultAsBinaryString.replaceAll("0", "2");
            resultAsBinaryString = resultAsBinaryString.replaceAll("1", "0");
            resultAsBinaryString = resultAsBinaryString.replaceAll("2", "1");
            n = (Integer.parseInt(resultAsBinaryString, 2) * (-1)) - 1;
        }
        return n;
    }
}
