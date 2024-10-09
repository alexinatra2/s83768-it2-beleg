package rtp;

import java.util.HashMap;

public class FECHandler extends FecHandlerDemo {
    public FECHandler(int size) {
        super(size);
    }

    public FECHandler(boolean useFec) {
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
