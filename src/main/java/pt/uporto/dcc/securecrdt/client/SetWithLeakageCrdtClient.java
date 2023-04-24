package pt.uporto.dcc.securecrdt.client;

import pt.uminho.haslab.smpc.exceptions.InvalidSecretValue;
import pt.uporto.dcc.securecrdt.messages.IntProtocolMessage;
import pt.uporto.dcc.securecrdt.messages.MessageData;
import pt.uporto.dcc.securecrdt.messages.states.SetPlayerState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static pt.uporto.dcc.securecrdt.util.Standards.SETWITHLEAKAGE_PROTOCOL_NAME;

public class SetWithLeakageCrdtClient extends CrdtClient {
    public SetPlayerState[] responses;

    public SetWithLeakageCrdtClient(Client client) {
        super(client);
    }

    @Override
    public void update(String input) throws IOException, InvalidSecretValue {
        resetReplyData();

        String[] splitInput = input.split("\\s+");
        int[] shares = client.getDealer().share(Integer.parseInt(splitInput[1]));

        for (int i = 0;i < 3; i ++){
            byte[] message = new MessageData(null, shares[i], -1, -1).serialize();
            IntProtocolMessage protocolRequest = new IntProtocolMessage(SETWITHLEAKAGE_PROTOCOL_NAME, message);
            client.getClient(i).sendUpdateRequest(protocolRequest);
            //System.out.println("Sent to player " + i + ": " + shares[i]);
        }
        client.waitForResponse();
    }

    @Override
    public int query(String input) throws IOException, InterruptedException, InvalidSecretValue {
        resetReplyData();

        String[] splitInput = input.split("\\s+");
        if (splitInput.length == 2 && splitInput[1].equals("getall")) {
            SetPlayerState[] states = this.propagate();
            Set<Integer> decodedValues = new HashSet<>();
            for (int i = 0; i < states[0].getList().size(); i++) {
                int[] codedValues = new int[]{states[0].getList().get(i), states[1].getList().get(i), states[2].getList().get(i)};
                decodedValues.add(client.unshare(codedValues));
            }
            //System.out.println(decodedValues);
            return decodedValues.size();
        } else if (splitInput.length != 3 || !splitInput[1].equals("exists")) {
            System.out.println("Invalid command.");
            return -1;
        }

        int[] shares = client.getDealer().share(Integer.parseInt(splitInput[2]));

        for (int i = 0;i < 3; i ++){
            byte[] message = new MessageData("exists", shares[i], -1, -1).serialize();
            IntProtocolMessage protocolRequest = new IntProtocolMessage(SETWITHLEAKAGE_PROTOCOL_NAME, message);
            client.getClient(i).sendQueryRequest(protocolRequest);
        }

        client.waitForResponse();
        int currentValues = client.unshare(new int[]{responses[0].getQueriedValueExists(), responses[1].getQueriedValueExists(), responses[2].getQueriedValueExists()});
        //System.out.println(currentValues);
        return currentValues;
    }

    @Override
    public SetPlayerState[] propagate() throws IOException, InterruptedException {
        resetReplyData();

        for (int i = 0;i < 3; i ++){
            IntProtocolMessage protocolRequest = new IntProtocolMessage(SETWITHLEAKAGE_PROTOCOL_NAME, new byte[]{});
            client.getClient(i).sendPropagateRequest(protocolRequest);
        }
        client.waitForResponse();
        return responses;
    }

    @Override
    public void merge() throws IOException, InvalidSecretValue {
        resetReplyData();

        // FOR TESTING ONLY, SIMULATING STATE COMING FROM OTHER REPLICA
        int[] customValues = new int[]{3, 6, 9};
        int[][] customShares = new int[3][3];
        for (int i = 0; i < 3; i++) {
            customShares[i] = client.getDealer().share(customValues[i]);
        }
        SetPlayerState[] incomingStates = new SetPlayerState[3];
        for (int i = 0; i < 3; i++){
            ArrayList<Integer> stateList = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                stateList.add(customShares[j][i]);
            }
            incomingStates[i] = new SetPlayerState(stateList, -1);
        }
        // FINISHED FOR TESTING ONLY

        //System.out.println(Arrays.toString(incomingStates));
        for (int i = 0;i < 3; i ++){
            IntProtocolMessage protocolRequest = new IntProtocolMessage(SETWITHLEAKAGE_PROTOCOL_NAME, incomingStates[i].serialize());
            client.getClient(i).sendMergeRequest(protocolRequest);
            //System.out.println("Sent to player " + i + ": " + incomingStates[i]);
        }
        client.waitForResponse();
    }
    
    @Override
    public void resetReplyData() {
        responses = new SetPlayerState[3];
        super.resetReplyData();
    }

}