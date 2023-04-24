package pt.uporto.dcc.securecrdt.crdt;

import lombok.Getter;
import lombok.Setter;
import pt.uminho.haslab.smpc.sharemindImp.Integer.IntSharemindSecretFunctions;
import pt.uporto.dcc.securecrdt.messages.*;
import pt.uporto.dcc.securecrdt.communication.ClientMessageHandler;
import pt.uporto.dcc.securecrdt.messages.states.GCounterPlayerState;
import pt.uporto.dcc.securecrdt.messages.states.PNCounterPlayerState;
import pt.uporto.dcc.securecrdt.util.ShareTimestampPair;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static pt.uporto.dcc.securecrdt.util.Standards.*;

@Getter
@Setter
public class PNCounterCrdt {
    private final CrdtPlayer player;

    private GCounterCrdt incrementCounter;
    private GCounterCrdt decrementCounter;


    public PNCounterCrdt(CrdtPlayer player) {
        this.player = player;

        this.incrementCounter = new GCounterCrdt(player);
        this.decrementCounter = new GCounterCrdt(player);
    }

    public void handleMessage(int type, IntProtocolMessage req) throws IOException {
        ClientMessageHandler clientMessageHandler = player.getIoManager().getServer().getController();

        switch (type) {
            // Update message
            case 1: {

                MessageData data = MessageData.deserialize(req.getData());
                String operation = data.getOperation();

                if (operation.equals("inc")) {
                    incrementCounter.handleMessage(1, req);
                } else if (operation.equals("dec")) {
                    decrementCounter.handleMessage(1, req);
                }

                DataOutputStream out = clientMessageHandler.getOutStream();
                PNCounterPlayerState state = new PNCounterPlayerState(
                        new GCounterPlayerState(incrementCounter.getCounter()),
                        new GCounterPlayerState(decrementCounter.getCounter())
                );
                IntProtocolMessage response = new IntProtocolMessage(PNCOUNTER_PROTOCOL_NAME + ".update", state.serialize());
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
                for (ShareTimestampPair pair : incrementCounter.getCounter()) {
                    pair.setShare(issf.reshare(new int[]{pair.getShare()}, this.player.getSmpcPlayer())[0]);
                }
                for (ShareTimestampPair pair : decrementCounter.getCounter()) {
                    pair.setShare(issf.reshare(new int[]{pair.getShare()}, this.player.getSmpcPlayer())[0]);
                }

                PNCounterPlayerState state = new PNCounterPlayerState(
                        new GCounterPlayerState(incrementCounter.getCounter()),
                        new GCounterPlayerState(decrementCounter.getCounter())
                );
                IntProtocolMessage response = new IntProtocolMessage(PNCOUNTER_PROTOCOL_NAME + ".query", state.serialize());
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
                    for (ShareTimestampPair pair : incrementCounter.getCounter()) {
                        pair.setShare(issf.reshare(new int[]{pair.getShare()}, this.player.getSmpcPlayer())[0]);
                    }
                    for (ShareTimestampPair pair : decrementCounter.getCounter()) {
                        pair.setShare(issf.reshare(new int[]{pair.getShare()}, this.player.getSmpcPlayer())[0]);
                    }
                }

                PNCounterPlayerState state = new PNCounterPlayerState(
                        new GCounterPlayerState(incrementCounter.getCounter()),
                        new GCounterPlayerState(decrementCounter.getCounter())
                );
                IntProtocolMessage response = new IntProtocolMessage(PNCOUNTER_PROTOCOL_NAME + ".propagate", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                //System.out.println("Returned current state to the controller");
                break;
            }
            // Merge message
            case 4: {
                PNCounterPlayerState incomingState = PNCounterPlayerState.deserialize(req.getData());
                IntProtocolMessage reqP = new IntProtocolMessage(GCOUNTER_PROTOCOL_NAME, incomingState.getIncrementCounter().serialize());
                IntProtocolMessage reqN = new IntProtocolMessage(GCOUNTER_PROTOCOL_NAME, incomingState.getDecrementCounter().serialize());

                incrementCounter.handleMessage(4, reqP);
                decrementCounter.handleMessage(4, reqN);

                DataOutputStream out = clientMessageHandler.getOutStream();
                PNCounterPlayerState state = new PNCounterPlayerState(
                        new GCounterPlayerState(incrementCounter.getCounter()),
                        new GCounterPlayerState(decrementCounter.getCounter())
                );
                IntProtocolMessage response = new IntProtocolMessage(PNCOUNTER_PROTOCOL_NAME + ".merge", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                break;
            }
        }
    }
}