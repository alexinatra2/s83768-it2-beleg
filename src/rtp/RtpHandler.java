package rtp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for RTP packets.
 * Processes all RTP packets and provides JPEG images for displaying
 *
 * @author Emanuel Günther
 */
public class RtpHandler {



    public void setJitterBufferStartSize(int jitterBufferStartSize) {
        this.jitterBufferStartSize = jitterBufferStartSize;
    }

    public enum EncryptionMode {
        NONE,
        SRTP,
        JPEG,
        JPEG_ATTACK
    }

    public static final int RTP_PAYLOAD_FEC = 127; // assumed as in RFC 5109, 10.1
    public static final int RTP_PAYLOAD_JPEG = 26;
    private static byte[] defaultKey = new byte[]{
        (byte)0xE1, (byte)0xF9, (byte)0x7A, (byte)0x0D, (byte)0x3E, (byte)0x01, (byte)0x8B, (byte)0xE0,
        (byte)0xD6, (byte)0x4F, (byte)0xA3, (byte)0x2C, (byte)0x06, (byte)0xDE, (byte)0x41, (byte)0x39};
    private static byte[] defaultSalt = new byte[]{
        (byte)0x0E, (byte)0xC6, (byte)0x75, (byte)0xAD, (byte)0x49, (byte)0x8A, (byte)0xFE,
        (byte)0xEB, (byte)0xB6, (byte)0x96, (byte)0x0B, (byte)0x3A, (byte)0xAB, (byte)0xE6};

    private EncryptionMode encryptionMode = EncryptionMode.NONE;
    private FecHandler fecHandler = null;
    private JpegEncryptionHandler jpegEncryptionHandler = null;
    private SrtpHandler srtpHandler = null;

    // server side
    private int currentSeqNb = 0; // sequence number of current frame
    private int currentTS = 0;    // timestamp of a set of frames
    private boolean fecEncodingEnabled = false; // server side
    Random random = new Random(123456); // Channel loss - fixed seed for debugging
    int dropCounter = 0;
    DatagramSocket RTPsocket; // socket to be used to send and receive UDP packets

    // client side
    private boolean fecDecodingEnabled = false; // client side
    private HashMap<Integer, RtpPacket> mediaPackets = null;
    private int playbackIndex = -1;  // iteration index fo rtps for playback
    private int startIndex;         // first RTP of a stream
    private int fecIndex;            // iteration index for fec correction
    private int tsReceive;           // Timestamp of last received media packet
    private int tsIndex;            // Timestamp index for rtps for playback
    private int tsStart;            // Timestamp start for rtps for playback
    private int tsAdd = 0;          // Timestamp add for rtps for playback
    private int jitterBufferStartSize = 25;      // size of the input buffer => start delay
    //private int jitterBufferSize;
    private List<Integer> lostJpegSlices = null;
    private HashMap<Integer, List<Integer>> sameTimestamps = null;
    private HashMap<Integer, Integer> firstRtp= null; // first RTP of a jpeg-frame
    private HashMap<Integer, Integer> lastRtp= null; // last RTP of a jpeg-frame
    private ReceptionStatistic statistics = null;
    private Receiver receiver = null;           // RTP receiver thread

    private Boolean isServer;
    static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * Create a new rtp.RtpHandler as server.
     *
     * @param fecGroupSize Group size for FEC packets. If the value is 0, FEC will be disabled.
     */
    public RtpHandler(int fecGroupSize) {
        isServer = true;
        if (fecGroupSize > 0) {
            fecEncodingEnabled = true;
            fecHandler = new FecHandler(fecGroupSize);
        }
        try {
            RTPsocket = new DatagramSocket();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception caught: " + e);
        }

    }

    /**
     * Create a new rtp.RtpHandler as client.
     *
     * @param useFec Use FEC correction or not
     */
    public RtpHandler(boolean useFec) {
        isServer = false;
        fecDecodingEnabled = useFec;
        fecHandler = new FecHandler(useFec);
        mediaPackets = new HashMap<>();
        sameTimestamps = new HashMap<>();
        firstRtp = new HashMap<>();
        lastRtp = new HashMap<>();
        statistics = new ReceptionStatistic();
    }

    public void reset() {
        currentSeqNb = 0;
        //fecHandler.reset();
        playbackIndex = -1;  // Client
        dropCounter = 0;

        if (!isServer) {
            //mediaPackets.clear();
            //sameTimestamps.clear();
            statistics = new ReceptionStatistic();
        }
    }

    // ********************************** Server side ***************************************


    /**
     * Retrieve the current FEC packet, if it is available.
     *
     * @return FEC packet as byte array, null if no such packet available
     */
    public byte[] createFecPacket() {
        if (!isFecPacketAvailable()) {
            return null;
        }

        byte[] fecPacket = fecHandler.getPacket();
        byte[] encryptedPacket;

        switch (encryptionMode) {
        case SRTP:
            encryptedPacket = srtpHandler.transformToSrtp(new RtpPacket(fecPacket, fecPacket.length));
            if (encryptedPacket != null) {
                fecPacket = encryptedPacket;
            }
            break;
        case JPEG:
        case JPEG_ATTACK:
        default:
            break;
        }

        return fecPacket;
    }


    /**
     * Check for the availability of an FEC packet.
     *
     * @return boolean value if FEC packet available
     */
    public boolean isFecPacketAvailable() {
        if (fecEncodingEnabled) {
            return fecHandler.isReady();
        } else {
            return false;
        }
    }

    /**
     * Transform a JPEG image to an RTP packet.
     * Takes care of all steps between.
     *
     * @param jpegImage JPEG image as byte array
     * @return RTP packet as byte array
     */
    public List<RtpPacket> jpegToRtpPackets(final byte[] jpegImage, int framerate) {
        byte[] image = switch (encryptionMode) {
          case JPEG -> jpegEncryptionHandler.encrypt(jpegImage);
          default -> jpegImage;
        };

      JpegFrame frame = JpegFrame.getFromJpegBytes(image); // convert JPEG to RTP payload

        List<RtpPacket> rtpPackets = new ArrayList<>();
        currentTS += (90000 / framerate); // TS is the same for all fragments
        int Mark = 0; // Marker bit is 0 for all fragments except the last one

        // iterieren über die JPEG-Fragmente und RTPs bauen
        ListIterator<byte[]> iter = frame.getRtpPayload().listIterator();
        while (iter.hasNext()) {
            byte[] frag = iter.next();
            if (!iter.hasNext()) {
                Mark = 1; // last segment -> set Marker bit
            }
            currentSeqNb++;

            // Build an RTPpacket object containing the image
            // time has to be in scale with 90000 Hz (RFC 2435, 3.)
            RtpPacket packet = new RtpPacket(
                RTP_PAYLOAD_JPEG, currentSeqNb, currentTS, Mark, frag, frag.length);

            rtpPackets.add(packet);
        }

        return rtpPackets;
    }

    public void sendJpeg(final byte[] jpegImage, int framerate, InetAddress clientIp, int clientPort, double lossRate) {
        List<RtpPacket> rtpPackets;    // fragmented RTP packets
        DatagramPacket sendDp;      // UDP packet containing the video frames

        rtpPackets = jpegToRtpPackets(jpegImage, framerate); // gets the fragmented RTP packets

        for (RtpPacket rtpPacket : rtpPackets) {    // Liste der RTP-Pakete

            byte[] packetData;
            if (encryptionMode == EncryptionMode.SRTP) {
                packetData = srtpHandler.transformToSrtp(rtpPacket);
            } else {
                packetData = rtpPacket.getpacket();
            }

            sendDp = new DatagramPacket(packetData, packetData.length, clientIp, clientPort);
            sendPacketWithError(sendDp,lossRate, false); // Send with packet loss

            if (fecEncodingEnabled) fecHandler.setRtp(rtpPacket);
            if (isFecPacketAvailable()) {
                logger.log(Level.FINE, "FEC-Encoder ready...");
                byte[] fecPacket = createFecPacket();
                // send to the FEC dest_port
                sendDp = new DatagramPacket(fecPacket, fecPacket.length, clientIp, clientPort);
                sendPacketWithError(sendDp, lossRate, true);
            }
        }
    }


    /**
     * @param senddp Datagram to send
     */
    private void sendPacketWithError(DatagramPacket senddp, double lossRate, boolean fec) {
        String label;
        if (fec) label = " rtp ";
        else label = " media ";
        if (random.nextDouble() > lossRate) {
            logger.log(Level.FINE, "Send frame: " + label + " size: " + senddp.getLength());
          try {
            RTPsocket.send(senddp);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        } else {
            if (!fec) dropCounter++;
            logger.log(Level.INFO, "Dropped frame: " + label + "Counter: " + dropCounter);
        }
    }




    // ********************************** Client side ***************************************

    /**
     * Get statistic values of the reception of the packets.
     *
     * @return Object with statistic values
     */
    public ReceptionStatistic getReceptionStatistic() {
        // update values which are used internally and that are not just statistic
        statistics.playbackIndex = playbackIndex;

        return statistics;
    }

    /**
     * Get next image for playback.
     * This method is the main interface for continuously getting images
     * for the purpose of displaying them.
     *
     * @return Image as byte array
     */
    public byte[] nextPlaybackImage() {
        // Check if jitter buffer is filled
        if (tsReceive <= tsStart + jitterBufferStartSize * tsAdd ) {
            logger.log(Level.FINE, "PLAY: jitter buffer not filled: " + tsReceive + " " + tsStart);
            return null;
        }

        statistics.requestedFrames++;
        //playbackIndex++;   // TODO: check if this is correct
        tsIndex += tsAdd;  // set  TS for next image
        statistics.jitterBufferSize = (tsReceive - tsIndex) / tsAdd;


        ArrayList<RtpPacket> packetList = packetsForNextImage();
        if (packetList == null) {
            logger.log(Level.FINE, "PLAY: no RTPs for playback  TS : " + tsIndex + " " + playbackIndex);
            return null;
        } else logger.log(Level.FINE, "PLAY: RTP list size for rtp: " +playbackIndex + " " + packetList.size());

        // TODO sometimes nullpointer exception
        if (packetList.get(0) == null || packetList.get(packetList.size()-1) == null ) {
            logger.log(Level.WARNING, "Nullpointer -> Check!");
            return null;
        }   
        logger.log(Level.FINER, "PLAY: get RTPs from " + packetList.get(0).getsequencenumber()
            + " to " + packetList.get(packetList.size()-1).getsequencenumber());

        // Combine RTP packets to one JPEG image
        //byte[] image = rtp.JpegFrame.combineToOneImage(packetList);
        ArrayList<JpegFrame> jpeg = new ArrayList<>();
        packetList.forEach( rtp -> jpeg.add( JpegFrame.getFromRtpPayload( rtp.getpayload()) ) );
        JpegFrame jpegs = JpegFrame.combineToOneFrame( jpeg );
        byte[] image =  jpegs.getJpeg();


        logger.log(Level.FINER, "Display TS: "
                + (packetList.get(0).gettimestamp() & 0xFFFFFFFFL)
                + " size: " + image.length);

        byte[] decryptedImage;
        switch (encryptionMode) {
        case JPEG:
            decryptedImage = jpegEncryptionHandler.decrypt(image);
            if (decryptedImage != null) {
                image = decryptedImage;
            }
            break;
        case JPEG_ATTACK:
            decryptedImage = jpegEncryptionHandler.replaceAttackDecryption(image);
            if (decryptedImage != null) {
                image = decryptedImage;
            }
            break;
        case SRTP:
        default:
            break;
        }

        return image;
    }

    public List<Integer> getRtpList() {
        return sameTimestamps.get(tsIndex);
    }

    public List<Integer> getLostJpegSlices() {
        return lostJpegSlices;
    }

    /**
     * Construct a list of RTP packets which contain the data of one jpeg image.
     *
     * @return List of RTP packets for one image
     */
    private ArrayList<RtpPacket> packetsForNextImage() {
        ArrayList<RtpPacket> packetList = new ArrayList<>();

        int snFirst;                    // looking for fist RTP of a jpeg-frame
        // TODO error correction of first packet desirable
        if ( firstRtp.get(tsIndex) == null) {   // no RTPs with this TS

            logger.log(Level.FINER, "PLAY: no firstRTP with this TS: " + tsIndex);
            statistics.framesLost++;        // full frame is lost
            return null;
        } else snFirst = firstRtp.get(tsIndex);
        RtpPacket packet = obtainMediaPacket(snFirst);  // get the first RTP of the image
        packetList.add(packet);                         // add in list

        // looking for last RTP of a jpeg-frame
        int snLast = 0;
        List<Integer> tsList = sameTimestamps.get(tsIndex);     // list of RTPs with this TS
        Collections.sort(tsList);                               // sort list
        if (lastRtp.get(tsIndex) != null) {
            snLast = lastRtp.get(tsIndex);                      // last RTP found in list
        } else if (firstRtp.get(tsIndex + tsAdd) != null) {
            snLast = firstRtp.get(tsIndex + tsAdd) - 1;         // last RTP is missing
        } else if (sameTimestamps.get(tsIndex) != null) {
            snLast = tsList.get( tsList.size() - 1);            // use TS-List
        } else {
            return packetList;                                  // only first RTP used
        }

        lostJpegSlices = new ArrayList<>(); // List of missing RTPs (JPEG slices)
        logger.log(Level.FINER, "PLAY: get RTPs from " + snFirst + " to " + snLast);
        boolean lost = false;
        for (int i = snFirst+1; i <= snLast; i++) {
            packet = obtainMediaPacket(i);
            if (packet == null) {
                lost = true;
                lostJpegSlices.add(i-snFirst);
            }
            else   packetList.add(packet);
        }
        if (lost) statistics.framesPartLost++;
        playbackIndex = snLast;
        return packetList;
    }

    private RtpPacket obtainMediaPacket(final int number) {
        int index = number % 0x10000; // account overflow of SNr (16 Bit)
        RtpPacket packet = mediaPackets.get(index);
        logger.log(Level.FINER, "PLAY: try get RTP nr: " + index);
        return packet;
    }

        /**
         * Get the RTP packet with the given sequence number.
         * This is the main method for getting RTP packets. It currently
         * includes error correction via FEC, but can be extended in the future.
         *
         * @param number Sequence number of the RTP packet
         * @return RTP packet, null if not available and not correctable
         */
    private RtpPacket checkMediaPacket(final int number) {
        int index = number % 0x10000; // account overflow of SNr (16 Bit)
        RtpPacket packet = mediaPackets.get(index);
        logger.log(Level.FINER, "RTP: try get RTP nr: " + index);

        if (packet == null) {
            statistics.packetsLost++;
            logger.log(Level.WARNING, "RTP: Media lost: " + index);

            boolean fecCorrectable = fecHandler.checkCorrection(index, mediaPackets);
            if (fecDecodingEnabled && fecCorrectable) {
                packet = fecHandler.correctRtp(index, mediaPackets);
                statistics.correctedPackets++;
                logger.log(Level.INFO, "---> FEC: correctable: " + index);
            } else {
                statistics.notCorrectedPackets++;
                logger.log(Level.INFO, "---> FEC: not correctable: " + index);
                return null;
            }
        }
        return packet;
    }


    // ***************************** RTP-Receiver  ***************************************

    public void startReceiver(int port) {
        receiver = new Receiver(port);
    }

    public void stopReceiver() {
        logger.log(Level.FINE, "RTP-Receiver try stopping...");
        receiver.interrupt();
        receiver.rtpSocket.close();
    }

    private class Receiver extends Thread {
        DatagramSocket rtpSocket;
        byte[] buffer = new byte[65356];
        int lastSeqNr;

        public Receiver(int port) {
            super("RTP-Receiver");
          try {
            rtpSocket = new DatagramSocket( port );
            logger.log(Level.FINE, "Socket receive buffer: " + rtpSocket.getReceiveBufferSize());
          } catch (SocketException e) {
            throw new RuntimeException(e);
          }
          start();
        }

        public void run() {
            DatagramPacket rcvdp = new DatagramPacket(buffer, buffer.length);
            while (!interrupted()) {
                try {
                    rtpSocket.receive(rcvdp);
                    RtpPacket packet = new RtpPacket(rcvdp.getData(), rcvdp.getLength());
                    if (packet.getpayloadtype() == RTP_PAYLOAD_JPEG) {
                        lastSeqNr = packet.getsequencenumber();
                    }
                    logger.log(Level.FINER, "RTP: received: " + lastSeqNr);
                    processRtpPacket(packet, false);

                    // look for missing RTPs
                    for (int i = fecIndex; i < lastSeqNr - fecHandler.getFecGroupSize()-5; i++) {
                        if (mediaPackets.get(i) == null) {
                            //statistics.packetsLost++;
                            //logger.log(Level.FINER, "RTP: Media lost: " + i);
                            RtpPacket rtp = checkMediaPacket(i);
                            if (rtp != null) {
                                //logger.log(Level.FINER, "RTP: Media corrected: " + i);
                                processRtpPacket(rtp, true);
                            }

                        }
                    }
                    // TODO: check if this is correct
                    int newfec = lastSeqNr - fecHandler.getFecGroupSize()-5;
                    if (newfec > fecIndex) fecIndex = newfec;
                } catch (IOException e) {    // SocketException is part of IOException
                    if (interrupted()) {
                        logger.log(Level.FINE, "RTP-Receiver interrupted");
                        break;
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
            logger.log(Level.FINE, "RTP-Receiver stopped");
        }
    }

    /**
     * Process and store a received RTP packet.
     *
     * @param packet the received RTP packet
     */
    public void processRtpPacket(RtpPacket packet, boolean fec) {
        int seqNr = packet.getsequencenumber();
        RtpPacket decryptedPacket;
        switch (encryptionMode) {
            case SRTP:
                decryptedPacket = srtpHandler.retrieveFromSrtp(packet.getpacket());
                if (decryptedPacket != null) {
                    packet = decryptedPacket;
                }
                break;
            case JPEG:
            case JPEG_ATTACK:
            default:
                break;
        }

        // store the first Timestamp
        if (playbackIndex == -1) {
            playbackIndex = seqNr - 1;
            //startIndex = seqNr;
            fecIndex = seqNr;
            tsStart = packet.gettimestamp();
            tsIndex = tsStart;
            tsAdd = 0;
        }
        // evaluate the correct TS-Offset at the beginning
        if (packet.gettimestamp()  > tsStart  && tsAdd == 0) {
            tsAdd  = packet.gettimestamp() - tsStart;
            logger.log(Level.FINER, "RTP: set tsAdd: " + tsAdd);
        }


        logger.log(Level.FINER,
            "---------------- Receiver RTP-Handler --------------------"
                + "\r\n"
                + "Got RTP packet with SeqNum # "
                + packet.getsequencenumber()
                + " TimeStamp: "
                + (0xFFFFFFFFL & packet.gettimestamp()) // cast to long
                + " ms, of type "
                + packet.getpayloadtype()
                + " Size: " + packet.getlength());

        switch (packet.getpayloadtype()) {
            case RTP_PAYLOAD_JPEG:
                if (!fec) {
                    statistics.receivedPackets++;
                    statistics.latestSequenceNumber = seqNr;
                }
                mediaPackets.put(seqNr, packet);
                // set first RTP of a jpeg-frame
                if (packet.getJpegOffset() == 0) {
                    logger.log(Level.FINER, "got first Paket: " + seqNr);
                    firstRtp.put(packet.gettimestamp(), seqNr);
                }
                // set the last RTP of a jpeg-frame
                if (packet.getMarker() == 1) {
                    logger.log(Level.FINER, "got last Paket: " + seqNr);
                    lastRtp.put(packet.gettimestamp(), seqNr);
                }
                logger.log(Level.FINER, "JPEG-Offset + Marker: "
                    + packet.getJpegOffset() + " " + packet.getMarker());

                // set list of same timestamps
                int ts = packet.gettimestamp();
                tsReceive = ts;

                if (!fec && tsAdd != 0) statistics.jitterBufferSize = (tsReceive - tsIndex) / tsAdd;

                List<Integer> tmpTimestamps = sameTimestamps.get(ts);
                if (tmpTimestamps == null) {
                    tmpTimestamps = new ArrayList<>();
                }
                tmpTimestamps.add(seqNr);
                sameTimestamps.put(ts, tmpTimestamps);
                logger.log(Level.FINER, "RTP: set sameTimestamps: " + (0xFFFFFFFFL & ts)
                    + " " + tmpTimestamps);
                break;


            case RTP_PAYLOAD_FEC:
                fecHandler.rcvFecPacket(packet);
                break;

            default:  // ignore unknown packet
        }

        // TASK remove comment for debugging
        packet.printheader(); // print rtp header bitstream for debugging
    }



    /**
     * Set packet encryption.
     *
     * @param mode The encryption mode.
     * @return true if successful, false otherwise
     */
    public boolean setEncryption(EncryptionMode mode) {
        if (currentSeqNb > 0 || (statistics != null && statistics.latestSequenceNumber > 0)) {
            // Do not change encryption when already started.
            return false;
        }

        encryptionMode = mode;
        switch (encryptionMode) {
        case SRTP:
            /* Use pre-shared key and salt to avoid key management and
             * session initialization with a protocol.
             */
            try {
                srtpHandler = new SrtpHandler(
                        SrtpHandler.EncryptionAlgorithm.AES_CTR,
                        SrtpHandler.MacAlgorithm.NONE,
                        defaultKey, defaultSalt, 0);
            } catch (InvalidKeyException | InvalidAlgorithmParameterException ikex) {
                System.out.println(ikex);
            }
          if (srtpHandler == null) {
                return false;
            }
            break;
        case JPEG:
        case JPEG_ATTACK:
            /* Use pre-shared key and salt to avoid key management and
             * session initialization with a protocol.
             */
            jpegEncryptionHandler = new JpegEncryptionHandler(
                    defaultKey, defaultSalt);
            break;
        case NONE:
        default:
            break;
        }

        return true;
    }

    /**
     * Set if FEC error correction should be used.
     *
     * @param enabled Use the FEC error correction or not.
     */
    public void setFecDecryptionEnabled(boolean enabled) {
        fecDecodingEnabled = enabled;
    }

    /**
     * Set a new group size for the FEC error handling.
     *
     * @param newGroupSize new group size
     */
    public void setFecGroupSize(int newGroupSize) {
        fecHandler.setFecGroupSize(newGroupSize);
    }



}

