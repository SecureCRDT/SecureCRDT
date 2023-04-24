package pt.uporto.dcc.securecrdt.client;

import pt.uminho.haslab.smpc.exceptions.InvalidSecretValue;
import pt.uporto.dcc.securecrdt.messages.*;
import pt.uporto.dcc.securecrdt.messages.states.GCounterPlayerState;
import pt.uporto.dcc.securecrdt.util.ShareTimestampPair;

import java.io.IOException;
import java.util.Arrays;

import static pt.uporto.dcc.securecrdt.util.Standards.GCOUNTER_PROTOCOL_NAME;

public class GCounterCrdtClient extends CrdtClient {
    public GCounterPlayerState[] responses;

    public GCounterCrdtClient(Client client) {
        super(client);
    }

    @Override
    public void update(String input) throws IOException, InvalidSecretValue {
        resetReplyData();

        timestamp++;
        String[] splitInput = input.split("\\s+");
        int[] shares = client.getDealer().share(Integer.parseInt(splitInput[1]));

        for (int i = 0;i < 3; i ++){
            byte[] message = new MessageData("inc", shares[i], timestamp, -1).serialize();
            IntProtocolMessage protocolRequest = new IntProtocolMessage(GCOUNTER_PROTOCOL_NAME, message);
            client.getClient(i).sendUpdateRequest(protocolRequest);
            //System.out.println("Sent to player " + i + ": " + shares[i] + " with timestamp " + timestamp);
        }
        client.waitForResponse();
    }

    @Override
    public int query(String input) throws IOException, InterruptedException {
        resetReplyData();

        for (int i = 0;i < 3; i ++){
            IntProtocolMessage protocolRequest = new IntProtocolMessage(GCOUNTER_PROTOCOL_NAME, new byte[]{});
            client.getClient(i).sendQueryRequest(protocolRequest);
        }

        client.waitForResponse();
        int currentValues = client.unshare(new int[]{responses[0].getValue(), responses[1].getValue(), responses[2].getValue()});
        //System.out.println(currentValues);
        return currentValues;
    }

    @Override
    public GCounterPlayerState[] propagate() throws IOException, InterruptedException {
        resetReplyData();

        for (int i = 0;i < 3; i ++){
            IntProtocolMessage protocolRequest = new IntProtocolMessage(GCOUNTER_PROTOCOL_NAME, new byte[]{});
            client.getClient(i).sendPropagateRequest(protocolRequest);
        }
        client.waitForResponse();
        return responses;
    }

    @Override
    public void merge() throws IOException, InvalidSecretValue {
        resetReplyData();

        // FOR TESTING ONLY, SIMULATING STATE COMING FROM OTHER REPLICA
        int customValue = 3;
        int timestamp = 3;
        int[] shares = client.getDealer().share(customValue);
        GCounterPlayerState[] incomingStates = new GCounterPlayerState[3];
        for (int i = 0; i < 3; i++) {
            ShareTimestampPair[] array =
                    new ShareTimestampPair[]{new ShareTimestampPair(0,0),
                                             new ShareTimestampPair(shares[i], timestamp)};
            incomingStates[i] = new GCounterPlayerState(array);
        }
        // FINISHED FOR TESTING ONLY

        //System.out.println(Arrays.toString(incomingStates));
        for (int i = 0;i < 3; i ++){
            IntProtocolMessage protocolRequest = new IntProtocolMessage(GCOUNTER_PROTOCOL_NAME, incomingStates[i].serialize());
            client.getClient(i).sendMergeRequest(protocolRequest);
            //System.out.println("Sent to player " + i + ": " + shares[i] + " with timestamp " + timestamp);
        }
        client.waitForResponse();
    }
    
    @Override
    public void resetReplyData() {
        responses = new GCounterPlayerState[3];
        super.resetReplyData();
    }

}