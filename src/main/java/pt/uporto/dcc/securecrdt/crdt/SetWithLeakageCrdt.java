package pt.uporto.dcc.securecrdt.crdt;

import lombok.Getter;
import lombok.Setter;
import pt.uminho.haslab.smpc.sharemindImp.Integer.IntSharemindDealer;
import pt.uminho.haslab.smpc.sharemindImp.Integer.IntSharemindSecretFunctions;
import pt.uporto.dcc.securecrdt.communication.ClientMessageHandler;
import pt.uporto.dcc.securecrdt.messages.IntProtocolMessage;
import pt.uporto.dcc.securecrdt.messages.MessageData;
import pt.uporto.dcc.securecrdt.messages.states.SetPlayerState;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static pt.uporto.dcc.securecrdt.util.Standards.*;

@Getter
@Setter
public class SetWithLeakageCrdt {

    private final CrdtPlayer player;

    private ArrayList<Integer> list;

    public SetWithLeakageCrdt(CrdtPlayer player){
        this.player = player;

        this.list = new ArrayList<>();
    }

    public void handleMessage(int type, IntProtocolMessage req) throws IOException {
        ClientMessageHandler clientMessageHandler = player.getIoManager().getServer().getController();

        switch (type) {
            // Update message
            case 1: {
                MessageData data = MessageData.deserialize(req.getData());

                IntSharemindSecretFunctions issf = new IntSharemindSecretFunctions();
                int v = data.getShare();
                int queriedValueExists = 0;
                for(int share : this.list) {
                    int toAdd = issf.shareConv(issf.equal(new int[]{share}, new int[]{v}, this.player.getSmpcPlayer()), this.player.getSmpcPlayer())[0];
                    queriedValueExists = IntSharemindDealer.mod(queriedValueExists + toAdd);
                }

                int res = issf.declassify(new int[]{queriedValueExists}, this.player.getSmpcPlayer())[0];

                if (res == 0) {
                    this.list.add(v);
                }

                if (req.getProtocol().equals(SETWITHLEAKAGE_PROTOCOL_NAME + ".merge")) break;

                DataOutputStream out = clientMessageHandler.getOutStream();
                SetPlayerState state = new SetPlayerState(list, -1);
                IntProtocolMessage response = new IntProtocolMessage(SETWITHLEAKAGE_PROTOCOL_NAME + ".update", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                break;
            }
            // Query message
            case 2: {
                MessageData data = MessageData.deserialize(req.getData());
                DataOutputStream out = clientMessageHandler.getOutStream();

                IntSharemindSecretFunctions issf = new IntSharemindSecretFunctions();
                int v = data.getShare();
                int queriedValueExists = 0;
                for(int share : this.list) {
                    int toAdd = issf.shareConv(issf.equal(new int[]{share}, new int[]{v}, this.player.getSmpcPlayer()), this.player.getSmpcPlayer())[0];
                    queriedValueExists = IntSharemindDealer.mod(queriedValueExists + toAdd);
                }

                SetPlayerState state = new SetPlayerState(new ArrayList<>(), queriedValueExists);
                IntProtocolMessage response = new IntProtocolMessage(SETWITHLEAKAGE_PROTOCOL_NAME + ".query", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                //System.out.println("Returned current state to the controller");
                break;
            }
            // Propagate message
            case 3: {
                DataOutputStream out = clientMessageHandler.getOutStream();

                // Refresh shares
                IntSharemindSecretFunctions issf = new IntSharemindSecretFunctions();
                ArrayList<Integer> newList = new ArrayList<>();
                for(int share : list){
                    share = issf.reshare(new int[]{share}, this.player.getSmpcPlayer())[0];
                    newList.add(share);
                }
                list = newList;

                SetPlayerState state = new SetPlayerState(list, -1);
                IntProtocolMessage response = new IntProtocolMessage(SETWITHLEAKAGE_PROTOCOL_NAME + ".propagate", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                //System.out.println("Returned current state to the controller");
                break;
            }
            // Merge message
            case 4: {
                SetPlayerState incomingState = SetPlayerState.deserialize(req.getData());
                ArrayList<Integer> incomingList = incomingState.getList();

                for(int v : incomingList) {
                    MessageData messageData = new MessageData(null, v, -1, -1);
                    IntProtocolMessage message = new IntProtocolMessage(SETWITHLEAKAGE_PROTOCOL_NAME + ".merge", messageData.serialize());
                    this.handleMessage(1, message);
                }

                DataOutputStream out = clientMessageHandler.getOutStream();
                SetPlayerState state = new SetPlayerState(list, -1);
                IntProtocolMessage response = new IntProtocolMessage(SETWITHLEAKAGE_PROTOCOL_NAME + ".merge", state.serialize());
                out.writeInt(response.serialize().length);
                out.write(response.serialize());
                out.flush();
                break;
            }
        }
    }
}