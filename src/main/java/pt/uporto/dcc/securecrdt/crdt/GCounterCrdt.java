package pt.uporto.dcc.securecrdt.crdt;

import lombok.Getter;
import lombok.Setter;
import pt.uminho.haslab.smpc.sharemindImp.Integer.IntSharemindSecretFunctions;
import pt.uporto.dcc.securecrdt.communication.ClientMessageHandler;
import pt.uporto.dcc.securecrdt.messages.*;
import pt.uporto.dcc.securecrdt.messages.states.GCounterPlayerState;
import pt.uporto.dcc.securecrdt.util.ShareTimestampPair;
import pt.uporto.dcc.securecrdt.util.Standards;

import java.io.DataOutputStream;
import java.io.IOException;

import static pt.uporto.dcc.securecrdt.util.Standards.GCOUNTER_PROTOCOL_NAME;
import static pt.uporto.dcc.securecrdt.util.Standards.RESHARING_BEFORE_PROPAGATE;

@Getter
@Setter
public class GCounterCrdt{

    private final CrdtPlayer player;

    private ShareTimestampPair[] counter;

    public GCounterCrdt(CrdtPlayer player){
        this.player = player;

        this.counter = new ShareTimestampPair[Standards.NUMBER_OF_REPLICAS];
        for (int i = 0; i< counter.length; i++) {
            counter[i] = new ShareTimestampPair();
        }
    }

    public void handleMessage(int type, IntProtocolMessage req) throws IOException {
        ClientMessageHandler clientMessageHandler = player.getIoManager().getServer().getController();

        switch (type) {
            // Update message
            case 1: {
                MessageData data = MessageData.deserialize(req.getData());
                String operation = data.getOperation();
                int share = data.getShare();
                int timestamp = data.getTimestamp();

                if (operation.equals("inc") || operation.equals("dec")) {
                    counter[player.replicaId].setShare(counter[player.replicaId].getShare() + share);
                    counter[player.replicaId].setTimestamp(timestamp);
                }

                if (req.getProtocol().equals(Standards.PNCOUNTER_PROTOCOL_NAME)) break;

                DataOutputStream out = clientMessageHandler.getOutStream();
                GCounterPlayerState state = new GCounterPlayerState(counter);
                IntProtocolMessage response = new IntProtocolMessage(GCOUNTER_PROTOCOL_NAME + ".update", state.serialize());
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
                for (ShareTimestampPair pair : counter) {
                    pair.setShare(issf.reshare(new int[]{pair.getShare()}, this.player.getSmpcPlayer())[0]);
                }

                GCounterPlayerState state = new GCounterPlayerState(counter);
                IntProtocolMessage response = new IntProtocolMessage(GCOUNTER_PROTOCOL_NAME + ".query", state.serialize());
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
                    for (ShareTimestampPair pair : counter) {
                        pair.setShare(issf.reshare(new int[]{pair.getShare()}, this.player.getSmpcPlayer())[0]);
                    }
                }

                GCounterPlayerState state = new GCounterPlayerState(counter);
                IntProtocolMessage response = new IntProtocolMessage(GCOUNTER_PROTOCOL_NAME + ".propagate", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                //System.out.println("Returned current state to the controller");
                break;
            }
            // Merge message
            case 4: {
                GCounterPlayerState incomingState = GCounterPlayerState.deserialize(req.getData());

                for(int i = 0; i < counter.length; i++) {
                    ShareTimestampPair incomingPair = incomingState.getCounter()[i];
                    if (counter[i].getTimestamp() < incomingPair.getTimestamp()) {
                        counter[i].setShare(incomingPair.getShare());
                        counter[i].setTimestamp(incomingPair.getTimestamp());
                    }
                }

                DataOutputStream out = clientMessageHandler.getOutStream();
                GCounterPlayerState state = new GCounterPlayerState(counter);
                IntProtocolMessage response = new IntProtocolMessage(GCOUNTER_PROTOCOL_NAME + ".merge", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                break;
            }
        }
    }
}