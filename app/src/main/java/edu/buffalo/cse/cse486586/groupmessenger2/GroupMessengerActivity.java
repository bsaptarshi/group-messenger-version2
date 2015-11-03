package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentValues;
import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import android.os.AsyncTask;
import android.util.Log;
import android.telephony.TelephonyManager;
import android.content.Context;

import java.net.Socket;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.Math;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    private static final String REMOTE_PORT0 = "11108";
    private static final String REMOTE_PORT1 = "11112";
    private static final String REMOTE_PORT2 = "11116";
    private static final String REMOTE_PORT3 = "11120";
    private static final String REMOTE_PORT4 = "11124";

    private static final String msgTypeMessage = "MSG";
    private static final String msgTypePropSeq = "PSEQ";
    private static final String msgTypeAccptSeq = "ASEQ";

    private static final String remote_ports[] = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
    private static final int SERVER_PORT = 10000;
    private static String ATTR_key;
    private static String ATTR_value;

    static String myPort;
    private int counterTotal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
        Determine the port no on which AVD listens
     */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        ATTR_key = new String("key");
        ATTR_value = new String("value");

        /*
        Server socket to listen to for incoming connections
         */
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (Exception ex) {
            Log.e("Server_Socket", "Cannot create server socket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editText.getText().toString();
                editText.setText(""); // This is one way to reset the input box.
//                Log.i("GMSNGR2", "Inside onCListenner");
                int sender = whoAmI(myPort);
                int msgId = counterTotal;
                counterTotal = counterTotal + 1;
                MessagePacket msgPacket = generateMessagePacket(message, sender, -1, -1, msgTypeMessage, msgId, "U");
                new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msgPacket);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    public int whoAmI(String myPort) {
        int avdId = -1;
        if (myPort.equals(REMOTE_PORT0)) {
            avdId = 0;
        } else if (myPort.equals(REMOTE_PORT1)) {
            avdId = 1;
        } else if (myPort.equals(REMOTE_PORT2)) {
            avdId = 2;
        } else if (myPort.equals(REMOTE_PORT3)) {
            avdId = 3;
        } else if (myPort.equals(REMOTE_PORT4)) {
            avdId = 4;
        }
        return avdId;
    }

    public String getPortNoFromAvdId(int avdId) {
//        Log.i("GMSNGR2", String.valueOf(avdId));
        String portNo = "";
        if (avdId == 0) {
            portNo = REMOTE_PORT0;
        } else if (avdId == 1) {
            portNo = REMOTE_PORT1;
        } else if (avdId == 2) {
            portNo = REMOTE_PORT2;
        } else if (avdId == 3) {
            portNo = REMOTE_PORT3;
        } else if (avdId == 4) {
            portNo = REMOTE_PORT4;
        }
//        Log.i("GMSNGR2", portNo);
        return portNo;
    }

    /*                  SERVER CLASS                */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        public int sTotal = 0;
        private int expectedFIFOSeqNos[] = new int[] {0,0,0,0,0};
        private HashMap<Integer, ArrayList<MessagePacket>> fifoQ = new HashMap<>();
        private ArrayList<MessagePacket> holdBackQ = new ArrayList<>();
        private HashMap<String, ArrayList<MessagePacket>> seqNoList = new HashMap<>();
        private int fileName = 0;
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket sock = null;
            String message;
            MessagePacket mPacket;
            Log.i("GMSNGR2", "Inside server for "+whoAmI(myPort));
            while (true) {
                try {
                    sock = serverSocket.accept();
                    BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));   //read message from input stream.
                    message = br.readLine();
                    Log.i("GMSNGR2", "Message received --> "+message);
                    mPacket = unserializeMessage(message);

                    if (mPacket.msgType.equals(msgTypeMessage)) {
                        Log.i("GMSNGR2", "B-multicast received --> "+message);
                        ensureTotalDelivery1(mPacket);
                    }
                    else if (mPacket.msgType.equals(msgTypePropSeq)) {
                        Log.i("GMSNGR2", "Proposed seqno received --> "+message);
                        ensureTotalDelivery2(mPacket);
                    } else if (mPacket.msgType.equals(msgTypeAccptSeq)) {
                        Log.i("GMSNGR2", "Accepted seqno received --> "+message);
                        ensureTotalDelivery3(mPacket);
                    }
                    sock.close();
                } catch (IOException err) {
                    Log.e("GMSNGR2", "Server failed---"+err.toString());
                }
            }
        }


        public void ensureTotalDelivery1 (MessagePacket mPacket) {
            sTotal = sTotal + 1;
            int suggesterId = whoAmI(myPort);
            MessagePacket msgPacket = generatePropSeqNoMessagePacket(mPacket, sTotal, suggesterId, msgTypePropSeq);
            Log.i("GMSNGR2", "Inside ensureTotalDelivery1 for "+mPacket.toString());
            msgPacket.seqNo = sTotal;
            insertIntoHoldBackQueue(msgPacket);
            sendProposedPacket(msgPacket);
        }

        public void sendProposedPacket(MessagePacket msgPacket) {
            String msgToSendPS = serializeMessagePacket(msgPacket);
            Log.i("GMSNGR2", "Inside propose message for "+whoAmI(myPort) + "with message = "+msgToSendPS);
            String portNo = getPortNoFromAvdId(msgPacket.sender);
            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(portNo));
                BufferedWriter send_msg = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); //write the message received to the output stream
                send_msg.write(msgToSendPS);
                send_msg.flush();
                socket.close(); //close the socket.
                Log.i("GMSNGR2", "(Poposal of Seq-No) "+ msgToSendPS+" from " + whoAmI(myPort)+ " to "+portNo+" -- > AVD"+whoAmI(portNo));
            } catch (UnknownHostException e) {
                Log.e("PropSeqNo", "PropSeqNo UnknownHostException");
            } catch (IOException e) {
                Log.e("PropSeqNo", "PropSeqNo socket IOException");
            }
        }


        public void ensureTotalDelivery2 (MessagePacket mPacket) {
            if (!seqNoList.containsKey(mPacket.msgId)) {
                seqNoList.put(mPacket.msgId, new ArrayList<MessagePacket>());
            }
            seqNoList.get(mPacket.msgId).add(mPacket);//(mPacket.seqNo, mPacket.seqNoSuggester);
            Log.i("GMSNGR2", "Inside receive proposals for "+whoAmI(myPort)+". Proposal received = "+mPacket.toString());
            if (seqNoList.get(mPacket.msgId).size() == 5) {
                int maxSeqNo = -1;
                int maxSeqNoSuggester = -1;
                Log.i("GMSNGR2", "Seq no proposals for "+mPacket.toString()+" are --->"+seqNoList.get(mPacket.msgId).toString());
                for (int i = 0; i < seqNoList.get(mPacket.msgId).size(); i++) {
                    if (seqNoList.get(mPacket.msgId).get(i).seqNo > maxSeqNo) { //choose max seq number
                        maxSeqNo = seqNoList.get(mPacket.msgId).get(i).seqNo;
                        maxSeqNoSuggester = seqNoList.get(mPacket.msgId).get(i).seqNoSuggester;
                    } else if (seqNoList.get(mPacket.msgId).get(i).seqNo == maxSeqNo) { //same seq no, choose lower suggester
                        if (maxSeqNoSuggester > seqNoList.get(mPacket.msgId).get(i).seqNoSuggester) {
                            maxSeqNo = seqNoList.get(mPacket.msgId).get(i).seqNo;
                            maxSeqNoSuggester = seqNoList.get(mPacket.msgId).get(i).seqNoSuggester;
                        }
                    }
                }

                Log.i("GMSNGR2", maxSeqNo + " is the max seq no suggested by " + maxSeqNoSuggester);
                MessagePacket msgPacket = generateAcceptedSeqNoMessagePacket(mPacket, maxSeqNo, maxSeqNoSuggester, msgTypeAccptSeq);
                Log.i("GMSNGR2", "Generated Accpeted packet to send as "+msgPacket.toString());
                sendAcceptedPacket(msgPacket);
            }
        }

        public void sendAcceptedPacket(MessagePacket msgPacket) {
            String msgToSendAS = serializeMessagePacket(msgPacket);
            Log.i("GMSNGR2", "Inside sendAcceptedPacket for "+whoAmI(myPort) + "with message = "+msgToSendAS);
            try {
                for(int i=0;i<remote_ports.length;i++) {    //send message to all the remote ports, including itself
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remote_ports[i]));
                    BufferedWriter send_msg = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); //write the message received to the output stream
                    send_msg.write(msgToSendAS);
                    Log.i("GMSNGR2", "(Send Final Accept message) "+ msgToSendAS+" from " + whoAmI(myPort)+ " to "+remote_ports[i]+" -- > AVD"+whoAmI(remote_ports[i]));
                    send_msg.flush();
                    socket.close(); //close the socket.
                }
            } catch (UnknownHostException e) {
                Log.e("ClientTask", "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e("ClientTask", "ClientTask socket IOException");
            }
        }

        public void ensureTotalDelivery3 (MessagePacket mPacket) {
            sTotal = Math.max(sTotal, mPacket.seqNo);
            MessagePacket temp;
            for (int i=0;i<holdBackQ.size();i++) {
                temp = holdBackQ.get(i);
                if ((temp.msgId.equals(mPacket.msgId)) && (temp.sender == mPacket.sender)) {
                    temp.seqNo = mPacket.seqNo;
                    temp.seqNoSuggester = mPacket.seqNoSuggester;
                    temp.msgStatus = "D";
                    temp.msgType = "FIFO";
                }
                holdBackQ.set(i, temp);
            }
            sortHoldBackQueue();
            int j=0;
            while ((!holdBackQ.isEmpty()) && (holdBackQ.get(0).msgStatus == "D")) {
                MessagePacket m = holdBackQ.get(0);
                Log.i("GMSNGR2122112", "HoldBackQueue Before remove ---> "+holdBackQ.toString());
                holdBackQ.remove(0);
                Log.i("GMSNGR2122112", "HoldBackQueue After remove ---> "+holdBackQ.toString());
                sortHoldBackQueue();
                Log.i("Total", "Message to FIFO--->" + m.toString());
                ensureFIFODelivery(m);
            }
        }


        public void ensureFIFODelivery (MessagePacket mPacket) {
            int sender = mPacket.sender;
            String[] msgIdentity = mPacket.msgId.split("\\.");
            int messageNumber = Integer.parseInt(msgIdentity[0]);
            Log.i("GMSNGR2", "MSG id in FIFO = "+messageNumber);
            if (expectedFIFOSeqNos[sender] == messageNumber) {
                ensureFinalDelivery(mPacket);
                expectedFIFOSeqNos[sender] += 1;
                if (fifoQ.containsKey(sender) && (fifoQ.get(sender).get(0).seqNo == expectedFIFOSeqNos[sender])) {
                    int i=0;
                    while (fifoQ.get(sender).get(i).seqNo == expectedFIFOSeqNos[sender]) {
                        ensureFinalDelivery(fifoQ.get(sender).get(i));
                        i++;
                        expectedFIFOSeqNos[sender] = expectedFIFOSeqNos[sender] + 1;
                        fifoQ.remove(i);
                        Log.i("GMSNGR2", "ExpectedSeqnum ---> "+Arrays.toString(expectedFIFOSeqNos));
                    }
                }
            } else if (expectedFIFOSeqNos[sender] < messageNumber) {
                //REJECT MESSAGE
                Log.i("GMSNGR2", "Rejecting Message");
                return;
            } else if (expectedFIFOSeqNos[sender] > messageNumber) {
                //BUFFER MESSAGE
                Log.i("GMSNGR2", "BUFFER FIFO Message");
                if (!fifoQ.containsKey(sender)) {
                    fifoQ.put(sender, new ArrayList<MessagePacket>());
                }
                fifoQ.get(sender).add(mPacket);
            }
        }

        public void ensureFinalDelivery(MessagePacket mPacket) {
            Log.i("GMSNGR2", "ensureFinalDelivery for "+mPacket.toString());
            ContentValues cv = new ContentValues();
            cv.put(ATTR_key, fileName);
            fileName++;
            cv.put(ATTR_value, mPacket.message);
            getContentResolver().insert(GroupMessengerProvider.URI_obj, cv);
            String Message = mPacket.msgId + "\t|\t" + mPacket.seqNo + "\n\n";
            publishProgress(Message);
            Log.i("GMSNGR2122", "After a delivery HoldBackQ-->"+holdBackQ.toString());
        }

        protected void onProgressUpdate(String...strings) {
             /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            return;
        }

        public void insertIntoHoldBackQueue(MessagePacket mPacket) {
            holdBackQ.add(mPacket);
            sortHoldBackQueue();
        }

        public void sortHoldBackQueue() {
            Log.i("GMSNGR20000", "HoldBackQ Before Sorting ---> "+holdBackQ.toString());
            MessagePacket temp;
            int n = holdBackQ.size();
            int i = 0, j = 0;
            for (i = 0; i < (n-1); i++) {
                for (j = 0; j < (n-1-i); j++) {
                    if (holdBackQ.get(j).seqNo > holdBackQ.get(j + 1).seqNo) {
                        temp = holdBackQ.get(j);
                        holdBackQ.set(j, holdBackQ.get(j + 1));
                        holdBackQ.set(j + 1, temp);
                    } else if ((holdBackQ.get(j).seqNo == holdBackQ.get(j + 1).seqNo) && (holdBackQ.get(j).seqNoSuggester > holdBackQ.get(j + 1).seqNoSuggester)) {
                        temp = holdBackQ.get(j);
                        holdBackQ.set(j, holdBackQ.get(j + 1));
                        holdBackQ.set(j + 1, temp);
                    } else if ((holdBackQ.get(j).seqNo == holdBackQ.get(j + 1).seqNo) && (holdBackQ.get(j).seqNoSuggester > holdBackQ.get(j + 1).seqNoSuggester) && (holdBackQ.get(j).msgStatus.charAt(0) > holdBackQ.get(j + 1).msgStatus.charAt(0))) {
                        temp = holdBackQ.get(j);
                        holdBackQ.set(j, holdBackQ.get(j + 1));
                        holdBackQ.set(j + 1, temp);
                    }
                }
            }
            Log.i("GMSNGR20000", "HoldBackQ After Sorting ---> "+holdBackQ.toString());
        }
    }
    /*                  CLIENT CLASS                */
    private class ClientTask extends AsyncTask<MessagePacket, Void, Void> {
        protected Void doInBackground(MessagePacket... msgs) {
            try {
                int i=0;
                for(i=0;i<remote_ports.length;i++) {    //send message to all the remote ports, including itself
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remote_ports[i]));
                    MessagePacket msgPacket = msgs[0];
                    String msgToSendBM  = serializeMessagePacket(msgPacket);
                    BufferedWriter send_msg = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); //write the message received to the output stream
                    Log.i("GMSNGR2", "(Inital B-Multicast) "+ msgToSendBM+" from " + whoAmI(myPort)+ " to "+remote_ports[i]+" -- > AVD"+whoAmI(remote_ports[i]));

                    send_msg.write(msgToSendBM);
                    send_msg.flush();
                    socket.close(); //close the socket.
                }
            } catch (UnknownHostException e) {
                Log.e("GMSNGR2", "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e("GMSNGR2", "ClientTask socket IOException in send--->"+e.toString());
                e.printStackTrace();
            }
            return null;
        }
    }

    public MessagePacket generateMessagePacket(String msg, int sender, int seqNo, int seqNoSuggester, String type, int msgId, String msgStatus) {
        MessagePacket m = new MessagePacket(msg, sender, seqNo, seqNoSuggester, msgId+"."+sender, type, msgStatus); //(String message, int sender, int seqNo, int seqNoSuggester, int msgId, String type, String msgStatus)
        return m;
    }

    public MessagePacket generatePropSeqNoMessagePacket(MessagePacket msgPacket, int sTotal, int suggesterId, String msgType) { //(String message, int sender, int seqNo, int seqNoSuggester, String msgId, String type, String msgStatus)
        MessagePacket m = new MessagePacket(msgPacket.message, msgPacket.sender, sTotal, suggesterId, msgPacket.msgId, msgType, "U");
        return m;
    }

    public MessagePacket generateAcceptedSeqNoMessagePacket(MessagePacket msgPacket, int sTotal, int suggesterId, String msgType) { //(String message, int sender, int seqNo, int seqNoSuggester, String msgId, String type, String msgStatus)
        MessagePacket m = new MessagePacket(msgPacket.message, msgPacket.sender, sTotal, suggesterId, msgPacket.msgId, msgType, "U");
        return m;
    }

    public String serializeMessagePacket(MessagePacket m) {
        String selMsg = "";
        selMsg = m.message + "|" + m.msgId + "|" + m.sender + "|" + m.seqNo + "|" + m.seqNoSuggester  + "|" + m.msgType + "|" + m.msgStatus;
        return selMsg;
    }

    public MessagePacket unserializeMessage(String selMsg) {
        String[] tokens = selMsg.split("\\|");
        Log.i("Unserialize", "Unserialize msg--->"+Arrays.toString(tokens));
        MessagePacket m;
        m = new MessagePacket(tokens[0].trim(), Integer.parseInt(tokens[2].trim()), Integer.parseInt(tokens[3].trim()), Integer.parseInt(tokens[4].trim()), tokens[1].trim(), tokens[5].trim(), tokens[6].trim());
        return m;
    }

}