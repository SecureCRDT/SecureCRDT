package pt.uporto.dcc.securecrdt.client;

import pt.uminho.haslab.smpc.exceptions.InvalidSecretValue;
import pt.uporto.dcc.securecrdt.messages.*;
import pt.uporto.dcc.securecrdt.messages.states.RegisterPlayerState;

import java.io.IOException;

import static pt.uporto.dcc.securecrdt.util.Standards.REGISTER_PROTOCOL_NAME;

public class RegisterCrdtClient extends CrdtClient {
    RegisterPlayerState[] responses = new RegisterPlayerState[3];

    public RegisterCrdtClient(Client client) {
        super(client);
    }

    @Override
    public void update(String input) throws IOException, InvalidSecretValue {
        resetReplyData();

        timestamp++;
        String[] splitInput = input.split("\\s+");
        int[] shares = client.getDealer().share(Integer.parseInt(splitInput[1]));

        for (int i = 0;i < 3; i ++){
            byte[] message = new MessageData(null, shares[i], timestamp, -1).serialize();
            IntProtocolMessage protocolRequest = new IntProtocolMessage(REGISTER_PROTOCOL_NAME, message);
            client.getClient(i).sendUpdateRequest(protocolRequest);
            //System.out.println("Sent to player " + i + ": " + shares[i] + " with timestamp " + timestamp);
        }
        client.waitForResponse();
    }

    @Override
    public int query(String input) throws IOException, InterruptedException {
        resetReplyData();

        for (int i = 0;i < 3; i ++){
            IntProtocolMessage protocolRequest = new IntProtocolMessage(REGISTER_PROTOCOL_NAME, new byte[]{});
            client.getClient(i).sendQueryRequest(protocolRequest);
        }

        client.waitForResponse();
        if (responses[0].getTimestamp() > timestamp) timestamp = responses[0].getTimestamp();
        int currentValues = client.unshare(new int[]{responses[0].getShare(), responses[1].getShare(), responses[2].getShare()});
        //System.out.println(currentValues);
        return currentValues;
    }

    @Override
    public RegisterPlayerState[] propagate() throws IOException, InterruptedException {
        resetReplyData();

        for (int i = 0;i < 3; i ++){
            IntProtocolMessage protocolRequest = new IntProtocolMessage(REGISTER_PROTOCOL_NAME, new byte[]{});
            client.getClient(i).sendPropagateRequest(protocolRequest);
        }
        client.waitForResponse();
        if (responses[0].getTimestamp() > timestamp) timestamp = responses[0].getTimestamp();

        return responses;
    }

    @Override
    public void merge() throws IOException, InvalidSecretValue {
        resetReplyData();

        // FOR TESTING ONLY, SIMULATING STATE COMING FROM OTHER REPLICA
        int customValue = 4;
        int[] newStateShares = client.getDealer().share(customValue);
        int newStateTimestamp = 3;
        // FINISHED FOR TESTING ONLY

        for (int i = 0;i < 3; i ++){
            byte[] message = new RegisterPlayerState(newStateShares[i], newStateTimestamp).serialize();
            IntProtocolMessage protocolRequest = new IntProtocolMessage(REGISTER_PROTOCOL_NAME, message);
            client.getClient(i).sendMergeRequest(protocolRequest);
            //System.out.println("Sent to player " + i + ": " + newStateShares[i] + " with timestamp " + newStateTimestamp);
        }
        client.waitForResponse();
    }

    @Override
    public void resetReplyData() {
        responses = new RegisterPlayerState[3];
        super.resetReplyData();
    }
}
