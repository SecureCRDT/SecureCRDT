package pt.uporto.dcc.securecrdt.client;

import pt.uminho.haslab.smpc.exceptions.InvalidSecretValue;
import pt.uporto.dcc.securecrdt.messages.*;
import pt.uporto.dcc.securecrdt.messages.states.MinBoundedCounterPlayerState;
import pt.uporto.dcc.securecrdt.util.ShareTimestampPair;
import pt.uporto.dcc.securecrdt.util.Standards;

import java.io.IOException;

import static pt.uporto.dcc.securecrdt.util.Standards.*;

public class MinBoundedCounterCrdtClient extends CrdtClient {
    public MinBoundedCounterPlayerState[] responses;

    public MinBoundedCounterCrdtClient(Client client) {
        super(client);
    }

    @Override
    public void update(String input) throws IOException, InvalidSecretValue {
        resetReplyData();

        timestamp++;
        String[] splitInput = input.split("\\s+");
        if (splitInput.length != 3) {
            System.out.println("Invalid command.");
            return;
        }
        int[] shares = client.getDealer().share(Integer.parseInt(splitInput[2]));

        for (int i = 0;i < 3; i ++){
            byte[] message = new MessageData(splitInput[1], shares[i], -1, -1).serialize();
            IntProtocolMessage protocolRequest = new IntProtocolMessage(MINBOUNDEDCOUNTER_PROTOCOL_NAME, message);
            client.getClient(i).sendUpdateRequest(protocolRequest);
            //System.out.println("Sent to player " + i + ": " + shares[i] + " with timestamp " + timestamp);
        }
        client.waitForResponse();
    }

    @Override
    public int query(String input) throws IOException, InterruptedException {
        resetReplyData();

        for (int i = 0;i < 3; i ++){
            IntProtocolMessage protocolRequest = new IntProtocolMessage(MINBOUNDEDCOUNTER_PROTOCOL_NAME, new byte[]{});
            client.getClient(i).sendQueryRequest(protocolRequest);
        }

        client.waitForResponse();
        int currentValues = client.unshare(new int[]{responses[0].getValue(), responses[1].getValue(), responses[2].getValue()});
        //System.out.println(currentValues);
        return currentValues;
    }

    @Override
    public MinBoundedCounterPlayerState[] propagate() throws IOException, InterruptedException {
        resetReplyData();

        for (int i = 0;i < 3; i ++){
            IntProtocolMessage protocolRequest = new IntProtocolMessage(MINBOUNDEDCOUNTER_PROTOCOL_NAME, new byte[]{});
            client.getClient(i).sendPropagateRequest(protocolRequest);
        }
        client.waitForResponse();
        return responses;
    }

    @Override
    public void merge() throws IOException, InvalidSecretValue {
        resetReplyData();

        // FOR TESTING ONLY, SIMULATING STATE COMING FROM OTHER REPLICA
        int simulatingReplica = 1;
        int customIncrements = 6;
        int customDecrements = 3;
        int customTimestamp = 4;
        int[] incrementShares = client.getDealer().share(customIncrements);
        int[] decrementShares = client.getDealer().share(customDecrements);
        MinBoundedCounterPlayerState[] incomingStates = new MinBoundedCounterPlayerState[3];
        for (int i = 0; i < 3; i++) {
            ShareTimestampPair[][] rightsMatrix = new ShareTimestampPair[NUMBER_OF_REPLICAS][NUMBER_OF_REPLICAS];
            ShareTimestampPair[] consumedRights = new ShareTimestampPair[NUMBER_OF_REPLICAS];
            for (int j = 0; j < Standards.NUMBER_OF_REPLICAS; j++) {
                for (int k = 0; k < Standards.NUMBER_OF_REPLICAS; k++) {
                    if (j == simulatingReplica && k == simulatingReplica)
                        rightsMatrix[j][k] = new ShareTimestampPair(incrementShares[i], customTimestamp);
                    else
                        rightsMatrix[j][k] = new ShareTimestampPair();
                }
                if (j == simulatingReplica)
                    consumedRights[j] = new ShareTimestampPair(decrementShares[i], customTimestamp);
                else
                    consumedRights[j] = new ShareTimestampPair();
            }
            incomingStates[i] = new MinBoundedCounterPlayerState(-1, rightsMatrix, consumedRights);
        }
        // FINISHED FOR TESTING ONLY

        //System.out.println(Arrays.toString(incomingStates));
        for (int i = 0;i < 3; i ++){
            IntProtocolMessage protocolRequest = new IntProtocolMessage(MINBOUNDEDCOUNTER_PROTOCOL_NAME, incomingStates[i].serialize());
            client.getClient(i).sendMergeRequest(protocolRequest);
            //System.out.println("Sent to player " + i + ": " + shares[i] + " with timestamp " + timestamp);
        }
        client.waitForResponse();
    }
    
    @Override
    public void resetReplyData() {
        responses = new MinBoundedCounterPlayerState[3];
        super.resetReplyData();
    }

}