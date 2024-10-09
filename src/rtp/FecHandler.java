package rtp;

import java.util.HashMap;

public class FecHandler extends FecHandlerDemo {
    public FecHandler(int size) {
        super(size);
    }

    public FecHandler(boolean useFec) {
        super(useFec);
    }

    @Override
    boolean checkCorrection(int nr, HashMap<Integer, RtpPacket> mediaPackets) {
        return false;
    }

    @Override
    RtpPacket correctRtp(int nr, HashMap<Integer, RtpPacket> mediaPackets) {
        return null;
    }
}
