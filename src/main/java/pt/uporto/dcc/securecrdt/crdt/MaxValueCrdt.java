package pt.uporto.dcc.securecrdt.crdt;


import lombok.Getter;
import pt.uminho.haslab.smpc.exceptions.InvalidSecretValue;
import pt.uminho.haslab.smpc.sharemindImp.Integer.IntSharemindDealer;
import pt.uminho.haslab.smpc.sharemindImp.Integer.IntSharemindSecretFunctions;
import pt.uporto.dcc.securecrdt.communication.ClientMessageHandler;
import pt.uporto.dcc.securecrdt.messages.IntProtocolMessage;
import pt.uporto.dcc.securecrdt.messages.states.RegisterPlayerState;
import pt.uporto.dcc.securecrdt.messages.MessageData;

import java.io.DataOutputStream;
import java.io.IOException;

import static pt.uporto.dcc.securecrdt.util.Standards.MAXVALUE_PROTOCOL_NAME;
import static pt.uporto.dcc.securecrdt.util.Standards.RESHARING_BEFORE_PROPAGATE;

@Getter
public class MaxValueCrdt {

    private final CrdtPlayer player;

    private int share;
    private int timestamp;

    public MaxValueCrdt(CrdtPlayer player) {
        this.player = player;

        this.share = 0;
        this.timestamp = 0;
    }

    public void handleMessage(int type, IntProtocolMessage req) throws IOException, InvalidSecretValue {
        ClientMessageHandler clientMessageHandler = player.getIoManager().getServer().getController();

        switch (type) {
            // Update message
            case 1: {
                MessageData data = MessageData.deserialize(req.getData());
                int u = data.getShare();
                int v = share;

                IntSharemindSecretFunctions issf = new IntSharemindSecretFunctions();

                /*
                   new value = v * (v >= u) + u * (u >= v) - v * (u == v)
                   will be        0 if true      0 if true      1 if true
                   meaning the gte comparisons actually represent a lt comparison
                   hence the new formula being
                   new value = v * (u < v) + u * (v < u) + v * (u == v)
                                    comp1          comp2          comp3
                 */
                int comp1 = issf.greaterOrEqualThan(new int[]{u}, new int[]{v}, this.player.getSmpcPlayer())[0];
                int comp2 = issf.greaterOrEqualThan(new int[]{v}, new int[]{u}, this.player.getSmpcPlayer())[0];
                // equal protocol outputs bitwise share, which must be converted to integer share
                int comp3 = issf.shareConv(issf.equal(new int[]{u}, new int[]{v}, this.player.getSmpcPlayer()), this.player.getSmpcPlayer())[0];

                int mult1 = issf.mult(new int[]{v}, new int[]{comp1}, this.player.getSmpcPlayer())[0];
                int mult2 = issf.mult(new int[]{u}, new int[]{comp2}, this.player.getSmpcPlayer())[0];
                int mult3 = issf.mult(new int[]{v}, new int[]{comp3}, this.player.getSmpcPlayer())[0];

                share = IntSharemindDealer.mod(mult1 + mult2 + mult3);

                timestamp++;
                //System.out.println("Share updated with value " + share);

                DataOutputStream out = clientMessageHandler.getOutStream();
                RegisterPlayerState state = new RegisterPlayerState(share, timestamp);
                IntProtocolMessage response = new IntProtocolMessage(MAXVALUE_PROTOCOL_NAME + ".update", state.serialize());
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
                share = issf.reshare(new int[]{share}, this.player.getSmpcPlayer())[0];

                RegisterPlayerState state = new RegisterPlayerState(share, timestamp);
                IntProtocolMessage response = new IntProtocolMessage(MAXVALUE_PROTOCOL_NAME + ".query", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                //System.out.println("Returned to controller the value " + share);
                break;
            }
            // Propagate message
            case 3: {
                DataOutputStream out = clientMessageHandler.getOutStream();

                if (RESHARING_BEFORE_PROPAGATE) {
                    // Refresh shares
                    IntSharemindSecretFunctions issf = new IntSharemindSecretFunctions();
                    share = issf.reshare(new int[]{share}, this.player.getSmpcPlayer())[0];
                }

                RegisterPlayerState state = new RegisterPlayerState(share, timestamp);
                IntProtocolMessage response = new IntProtocolMessage(MAXVALUE_PROTOCOL_NAME + ".propagate", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                //System.out.println("Returned to controller the value " + share);
                break;
            }
            // Merge message
            case 4: {
                RegisterPlayerState incomingState = RegisterPlayerState.deserialize(req.getData());
                MessageData newData = new MessageData(null, incomingState.getShare(), incomingState.getTimestamp(), -1);
                IntProtocolMessage message = new IntProtocolMessage(MAXVALUE_PROTOCOL_NAME, newData.serialize());

                this.handleMessage(1, message);
                break;
            }
        }
    }
}
