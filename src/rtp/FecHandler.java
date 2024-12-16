package rtp;

import java.util.HashMap;
import java.util.List;

public class FecHandler extends FecHandlerDemo {
    public FecHandler(int size) {
        super(size);
    }

    public FecHandler(boolean useFec) {
        super(useFec);
    }

    @Override
    boolean checkCorrection(int nr, HashMap<Integer, RtpPacket> mediaPackets) {
        Integer fecPacketNr = fecNr.get(nr);
        if (fecStack.get(fecPacketNr) == null) {
            return false;
        }
        List<Integer> fecPacketNumbers = fecList.get(nr);
        for (Integer fecNumber : fecPacketNumbers) {
            if (fecNumber != nr && mediaPackets.get(fecNumber) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    RtpPacket correctRtp(int nr, HashMap<Integer, RtpPacket> mediaPackets) {
        Integer fecPacketNr = fecNr.get(nr);
        FecPacket correctedFecPacket = fecStack.get(fecPacketNr);
        List<Integer> fecPacketNumbers = fecList.get(nr);
        for (Integer packetNumber : fecPacketNumbers) {
            if (packetNumber != nr) {
                correctedFecPacket.addRtp(mediaPackets.get(packetNumber));
            }
        }
        return correctedFecPacket.getLostRtp(nr);
    }
}
