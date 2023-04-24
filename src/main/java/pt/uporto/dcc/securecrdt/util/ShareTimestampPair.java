package pt.uporto.dcc.securecrdt.util;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShareTimestampPair {

    private int share;
    private int timestamp;

    public ShareTimestampPair() {
        this.share = 0;
        this.timestamp = 0;
    }

    public ShareTimestampPair(int share, int timestamp) {
        this.share = share;
        this.timestamp = timestamp;
    }



    @Override
    public String toString() {
        return "(" + share + ", " + timestamp + ")";
    }
}
