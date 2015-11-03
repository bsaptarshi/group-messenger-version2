package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by saptarshi on 3/5/15.
 */
public class MessagePacket {
    public String message;
    public String msgId;
    public int sender;
    public int seqNo;
    public int seqNoSuggester;
    public String msgType;
    public String msgStatus;

//    MessagePacket(String message, int sender, int seqNo, int seqNoSuggester, int msgId, String type, String msgStatus) {
//        this.message = message;
//        this.sender = sender;
//        this.seqNo = seqNo;
//        this.msgType = type;
//        this.seqNoSuggester = seqNoSuggester;
//        this.msgId = msgId+"."+sender;
//        this.msgStatus = msgStatus;
//    }

    MessagePacket(String message, int sender, int seqNo, int seqNoSuggester, String msgId, String type, String msgStatus) {
        this.message = message;
        this.sender = sender;
        this.seqNo = seqNo;
        this.msgType = type;
        this.seqNoSuggester = seqNoSuggester;
        this.msgId = msgId;
        this.msgStatus = msgStatus;
    }

    public String toString() {
        return "Message:" + message + "|sender.id:" + new StringBuffer(msgId).reverse().toString() + "|Seq no:" + seqNo + "|Suggester:" + seqNoSuggester +"|Status:" + msgStatus;
    }
}


/////////////msgTypeMessage

// message = message
//sender = sender;
//seqNo = -1;
//msgType = msgTypeMessage;
//seqNoSuggester = -1;
//msgId = msgId+"-"+sender;
//msgStatus = "U";

////////////msgTypeProposeSeqNo

// message = ""
//sender = sender;
//seqNo = seqNo;
//msgType = msgTypeProposeSeqNo;
//seqNoSuggester = sender;
//msgId = msgId;
//msgStatus = "U";

////////////msgTypeAcceptSeqNo

// message = ""
//sender = sender;
//seqNo = seqNo;
//msgType = msgTypeAcceptedSeqNo;
//seqNoSuggester = seqNoSuggester;
//msgId = msgId;
//msgStatus = "U";