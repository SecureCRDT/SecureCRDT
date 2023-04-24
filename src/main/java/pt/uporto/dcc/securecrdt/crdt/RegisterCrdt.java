package pt.uporto.dcc.securecrdt.crdt;


import lombok.Getter;
import pt.uminho.haslab.smpc.sharemindImp.Integer.IntSharemindSecretFunctions;
import pt.uporto.dcc.securecrdt.messages.IntProtocolMessage;
import pt.uporto.dcc.securecrdt.communication.ClientMessageHandler;
import pt.uporto.dcc.securecrdt.messages.states.RegisterPlayerState;
import pt.uporto.dcc.securecrdt.messages.MessageData;

import java.io.DataOutputStream;
import java.io.IOException;

import static pt.uporto.dcc.securecrdt.util.Standards.REGISTER_PROTOCOL_NAME;
import static pt.uporto.dcc.securecrdt.util.Standards.RESHARING_BEFORE_PROPAGATE;

@Getter
public class RegisterCrdt {

    private final CrdtPlayer player;

    private int share;
    private int timestamp;

    public RegisterCrdt(CrdtPlayer player) {
        this.player = player;

        this.share = 0;
        this.timestamp = 0;
    }

    public void handleMessage(int type, IntProtocolMessage req) throws IOException {
        ClientMessageHandler clientMessageHandler = player.getIoManager().getServer().getController();

        switch (type) {
            // Update message
            case 1: {
                MessageData data = MessageData.deserialize(req.getData());
                share = data.getShare();
                timestamp = data.getTimestamp();
                //System.out.println("Share updated with value " + share);

                DataOutputStream out = clientMessageHandler.getOutStream();
                RegisterPlayerState state = new RegisterPlayerState(share, timestamp);
                IntProtocolMessage response = new IntProtocolMessage(REGISTER_PROTOCOL_NAME + ".update", state.serialize());
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
                IntProtocolMessage response = new IntProtocolMessage(REGISTER_PROTOCOL_NAME + ".query", state.serialize());
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
                IntProtocolMessage response = new IntProtocolMessage(REGISTER_PROTOCOL_NAME + ".propagate", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                //System.out.println("Returned to controller the value " + share);
                break;
            }
            // Merge message
            case 4: {
                RegisterPlayerState incomingState = RegisterPlayerState.deserialize(req.getData());
                int newShare = incomingState.getShare();
                int newTimestamp = incomingState.getTimestamp();

                if (newTimestamp > timestamp) {
                    share = newShare;
                    timestamp = newTimestamp;
                    //System.out.println("Share updated with value " + share);
                }

                DataOutputStream out = clientMessageHandler.getOutStream();
                RegisterPlayerState state = new RegisterPlayerState(share, timestamp);
                IntProtocolMessage response = new IntProtocolMessage(REGISTER_PROTOCOL_NAME + ".merge", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                break;
            }
        }
    }
}
