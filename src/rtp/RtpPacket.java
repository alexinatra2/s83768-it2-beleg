package rtp;

public class RtpPacket extends RtpPacketDemo {
    public RtpPacket(int PType, int Framenb, int Time, int Mar, byte[] data, int data_length) {
        super(PType, Framenb, Time, Mar, data, data_length);
    }

    public RtpPacket(byte[] packet, int packet_size) {
        super(packet, packet_size);
    }

    @Override
    void setRtpHeader() {
        // Byte 0: Version (2), Padding (0), Extension (0), CC (0)
        // Version = 2, Padding = 0, Extension = 0, CC = 0
        header[0] = (byte) (Version << 6);

        // Byte 1: Marker (1 Bit), Payload Type (7 Bits)
        header[1] = (byte) ((Marker << 7) | (PayloadType & 0x7F)); // Marker und Payload-Type setzen

        // Byte 2-3: Sequence Number (16 Bits)
        header[2] = (byte) (SequenceNumber >> 8); // High Byte
        header[3] = (byte) (SequenceNumber);      // Low Byte

        // Byte 4-7: Timestamp (32 Bits)
        header[4] = (byte) (TimeStamp >> 24);
        header[5] = (byte) (TimeStamp >> 16);
        header[6] = (byte) (TimeStamp >> 8);
        header[7] = (byte) (TimeStamp);

        // Byte 8-11: SSRC (32 Bits)
        header[8] = (byte) (Ssrc >> 24);
        header[9] = (byte) (Ssrc >> 16);
        header[10] = (byte) (Ssrc >> 8);
        header[11] = (byte) (Ssrc);
    }
}
