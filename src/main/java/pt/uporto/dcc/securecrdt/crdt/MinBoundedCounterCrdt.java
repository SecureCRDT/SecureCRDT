package pt.uporto.dcc.securecrdt.crdt;

import lombok.Getter;
import lombok.Setter;
import pt.uminho.haslab.smpc.exceptions.InvalidSecretValue;
import pt.uminho.haslab.smpc.sharemindImp.Integer.IntSharemindDealer;
import pt.uminho.haslab.smpc.sharemindImp.Integer.IntSharemindSecretFunctions;
import pt.uporto.dcc.securecrdt.communication.ClientMessageHandler;
import pt.uporto.dcc.securecrdt.messages.*;
import pt.uporto.dcc.securecrdt.messages.states.MinBoundedCounterPlayerState;
import pt.uporto.dcc.securecrdt.util.ShareTimestampPair;
import pt.uporto.dcc.securecrdt.util.Standards;

import java.io.DataOutputStream;
import java.io.IOException;

import static pt.uporto.dcc.securecrdt.util.Standards.MINBOUNDEDCOUNTER_PROTOCOL_NAME;
import static pt.uporto.dcc.securecrdt.util.Standards.RESHARING_BEFORE_PROPAGATE;

@Getter
@Setter
public class MinBoundedCounterCrdt {

    private final CrdtPlayer player;

    private int limitShare;
    private boolean isLimitShareSet = false;
    private final ShareTimestampPair[][] rightsMatrix;
    private final ShareTimestampPair[] consumedRights;

    public MinBoundedCounterCrdt(CrdtPlayer player){
        this.player = player;

        this.limitShare = 0;
        this.rightsMatrix = new ShareTimestampPair[Standards.NUMBER_OF_REPLICAS][Standards.NUMBER_OF_REPLICAS];
        this.consumedRights = new ShareTimestampPair[Standards.NUMBER_OF_REPLICAS];
        for (int i = 0; i < Standards.NUMBER_OF_REPLICAS; i++) {
            for (int j = 0; j < Standards.NUMBER_OF_REPLICAS; j++) {
                rightsMatrix[i][j] = new ShareTimestampPair();
            }
            consumedRights[i] = new ShareTimestampPair();
        }
    }

    public void handleMessage(int type, IntProtocolMessage req) throws IOException, InvalidSecretValue {
        ClientMessageHandler clientMessageHandler = player.getIoManager().getServer().getController();

        switch (type) {
            // Update message
            case 1: {
                MessageData data = MessageData.deserialize(req.getData());
                String operation = data.getOperation();

                switch (operation) {
                    case "setup": {
                        if (!isLimitShareSet) {
                            limitShare = data.getShare();
                        }
                        break;
                    }
                    case "inc": {
                        int u = data.getShare();
                        int v = rightsMatrix[player.replicaId][player.replicaId].getShare();
                        int t = rightsMatrix[player.replicaId][player.replicaId].getTimestamp();
                        rightsMatrix[player.replicaId][player.replicaId].setShare(v + u);
                        rightsMatrix[player.replicaId][player.replicaId].setTimestamp(t + 1);
                        break;
                    }
                    case "dec": {
                        // Check if localrights >= share using safe operations
                        int u = data.getShare();
                        int v = consumedRights[player.replicaId].getShare();
                        int t = consumedRights[player.replicaId].getTimestamp();
                        int rights = localRights();

                    /*
                        newV = v * (rights<u) + (v+u) * (u<rights) + (v+u) * (u=rights)
                     */

                        IntSharemindSecretFunctions issf = new IntSharemindSecretFunctions();

                        int comp1 = issf.greaterOrEqualThan(new int[]{rights}, new int[]{u}, this.player.getSmpcPlayer())[0];
                        int comp2 = issf.greaterOrEqualThan(new int[]{u}, new int[]{rights}, this.player.getSmpcPlayer())[0];
                        // equal protocol outputs bitwise share, which must be converted to integer share
                        int comp3 = issf.shareConv(issf.equal(new int[]{u}, new int[]{rights}, this.player.getSmpcPlayer()), this.player.getSmpcPlayer())[0];

                        int mult1 = issf.mult(new int[]{v}, new int[]{comp1}, this.player.getSmpcPlayer())[0];
                        int mult2 = issf.mult(new int[]{v + u}, new int[]{comp2}, this.player.getSmpcPlayer())[0];
                        int mult3 = issf.mult(new int[]{v + u}, new int[]{comp3}, this.player.getSmpcPlayer())[0];

                        int newV = IntSharemindDealer.mod(mult1 + mult2 + mult3);

                        consumedRights[player.replicaId].setShare(newV);
                        consumedRights[player.replicaId].setTimestamp(t + 1);
                        break;
                    }
                    case "transfer": {
                        int toPlayer = data.getToPlayer();
                        int u = data.getShare();
                        int v = rightsMatrix[player.replicaId][toPlayer].getShare();
                        int t = rightsMatrix[player.replicaId][toPlayer].getTimestamp();
                        int rights = localRights();

                    /*
                        newV = v * (rights<u) + (v+u) * (u<rights) + (v+u) * (u=rights)
                     */

                        IntSharemindSecretFunctions issf = new IntSharemindSecretFunctions();

                        int comp1 = issf.greaterOrEqualThan(new int[]{rights}, new int[]{u}, this.player.getSmpcPlayer())[0];
                        int comp2 = issf.greaterOrEqualThan(new int[]{u}, new int[]{rights}, this.player.getSmpcPlayer())[0];
                        // equal protocol outputs bitwise share, which must be converted to integer share
                        int comp3 = issf.shareConv(issf.equal(new int[]{u}, new int[]{rights}, this.player.getSmpcPlayer()), this.player.getSmpcPlayer())[0];

                        int mult1 = issf.mult(new int[]{v}, new int[]{comp1}, this.player.getSmpcPlayer())[0];
                        int mult2 = issf.mult(new int[]{v + u}, new int[]{comp2}, this.player.getSmpcPlayer())[0];
                        int mult3 = issf.mult(new int[]{v + u}, new int[]{comp3}, this.player.getSmpcPlayer())[0];

                        int newV = IntSharemindDealer.mod(mult1 + mult2 + mult3);

                        rightsMatrix[player.replicaId][toPlayer].setShare(newV);
                        rightsMatrix[player.replicaId][toPlayer].setTimestamp(t + 1);
                        break;
                    }
                }

                DataOutputStream out = clientMessageHandler.getOutStream();
                MinBoundedCounterPlayerState state = new MinBoundedCounterPlayerState(limitShare, rightsMatrix, consumedRights);
                IntProtocolMessage response = new IntProtocolMessage(MINBOUNDEDCOUNTER_PROTOCOL_NAME + ".update", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                break;
            }
            // Query message
            case 2: {
                DataOutputStream out = clientMessageHandler.getOutStream();

                // Refresh shares
                IntSharemindSecretFunctions issf = new IntSharemindSecretFunctions();
                limitShare = issf.reshare(new int[]{limitShare}, this.player.getSmpcPlayer())[0];
                for (ShareTimestampPair[] line : rightsMatrix) {
                    for(ShareTimestampPair pair : line) {
                        pair.setShare(issf.reshare(new int[]{pair.getShare()}, this.player.getSmpcPlayer())[0]);
                    }
                }
                for (ShareTimestampPair pair : consumedRights) {
                    pair.setShare(issf.reshare(new int[]{pair.getShare()}, this.player.getSmpcPlayer())[0]);
                }

                MinBoundedCounterPlayerState state = new MinBoundedCounterPlayerState(limitShare, rightsMatrix, consumedRights);
                IntProtocolMessage response = new IntProtocolMessage(MINBOUNDEDCOUNTER_PROTOCOL_NAME + ".query", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                //System.out.println("Returned current state to the controller");
                break;
            }
            // Propagate message
            case 3: {
                DataOutputStream out = clientMessageHandler.getOutStream();

                if (RESHARING_BEFORE_PROPAGATE) {
                    // Refresh shares
                    IntSharemindSecretFunctions issf = new IntSharemindSecretFunctions();
                    limitShare = issf.reshare(new int[]{limitShare}, this.player.getSmpcPlayer())[0];
                    for (ShareTimestampPair[] line : rightsMatrix) {
                        for(ShareTimestampPair pair : line) {
                            pair.setShare(issf.reshare(new int[]{pair.getShare()}, this.player.getSmpcPlayer())[0]);
                        }
                    }
                    for (ShareTimestampPair pair : consumedRights) {
                        pair.setShare(issf.reshare(new int[]{pair.getShare()}, this.player.getSmpcPlayer())[0]);
                    }
                }

                MinBoundedCounterPlayerState state = new MinBoundedCounterPlayerState(limitShare, rightsMatrix, consumedRights);
                IntProtocolMessage response = new IntProtocolMessage(MINBOUNDEDCOUNTER_PROTOCOL_NAME + ".propagate", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                //System.out.println("Returned current state to the controller");
                break;
            }
            // Merge message
            case 4: {
                MinBoundedCounterPlayerState incomingState = MinBoundedCounterPlayerState.deserialize(req.getData());

                for(int i = 0; i < Standards.NUMBER_OF_REPLICAS; i++) {
                    for(int j = 0; j < Standards.NUMBER_OF_REPLICAS; j++) {
                        if (incomingState.getRightsMatrix()[i][j].getTimestamp() > rightsMatrix[i][j].getTimestamp()) {
                            rightsMatrix[i][j].setShare(incomingState.getRightsMatrix()[i][j].getShare());
                            rightsMatrix[i][j].setTimestamp(incomingState.getRightsMatrix()[i][j].getTimestamp());
                        }
                    }
                    if (incomingState.getConsumedRights()[i].getTimestamp() > consumedRights[i].getTimestamp()) {
                        consumedRights[i].setShare(incomingState.getConsumedRights()[i].getShare());
                        consumedRights[i].setTimestamp(incomingState.getConsumedRights()[i].getTimestamp());
                    }
                }

                DataOutputStream out = clientMessageHandler.getOutStream();
                MinBoundedCounterPlayerState state = new MinBoundedCounterPlayerState(limitShare, rightsMatrix, consumedRights);
                IntProtocolMessage response = new IntProtocolMessage(MINBOUNDEDCOUNTER_PROTOCOL_NAME + ".merge", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                break;
            }
        }
        isLimitShareSet = true;
    }

    private int localRights() {
        int rights1 = 0;
        int rights2 = 0;
        for (int i = 0; i < Standards.NUMBER_OF_REPLICAS; i++) {
            if (i != player.getPlayerID()) {
                rights1 = IntSharemindDealer.mod(rightsMatrix[i][player.replicaId].getShare() + rights1);
                rights2 = IntSharemindDealer.mod(rightsMatrix[player.replicaId][i].getShare() + rights2);
            }
        }
        int rights = rightsMatrix[player.replicaId][player.replicaId].getShare();
        int consumed = consumedRights[player.replicaId].getShare();
        return IntSharemindDealer.mod(rights + rights1 - rights2 - consumed);
    }
}