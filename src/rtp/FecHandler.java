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
    public boolean checkCorrection(int nr, HashMap<Integer, RtpPacket> mediaPackets) {
        // Check if there is an FEC packet that corresponds to the missing RTP packet
        if (fecNr.containsKey(nr)) {
            // Get list of RTP packets related to this FEC packet
            List<Integer> relatedRtpPackets = fecList.get(nr);

            // Count how many RTP packets are missing
            long missingCount = relatedRtpPackets.stream()
                    .filter(rtpNr -> !mediaPackets.containsKey(rtpNr)) // filter missing packets
                    .count();

            return missingCount == 1;
        }
        return false;
    }

    @Override
    RtpPacket correctRtp(int nr, HashMap<Integer, RtpPacket> mediaPackets) {
        Integer fecPacketNr = fecNr.get(nr);
        FecPacket correctedFecPacket = fecStack.get(fecPacketNr);
        List<Integer> fecPacketNumbers = fecList.get(nr);
        fecPacketNumbers.stream()
                .filter(packetNumber -> packetNumber != nr)
                .map(mediaPackets::get)
                .forEach(correctedFecPacket::addRtp);
        return correctedFecPacket.getLostRtp(nr);
    }
}
