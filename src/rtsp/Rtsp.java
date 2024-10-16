package rtsp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;

public class Rtsp extends RtspDemo {
    public Rtsp(URI url, int rtpRcvPort) {
        super(url, rtpRcvPort);
    }

    public Rtsp(BufferedReader RTSPBufferedReader, BufferedWriter RTSPBufferedWriter) {
        super(RTSPBufferedReader, RTSPBufferedWriter);
    }

    @Override
    public boolean play() {
        if (state != State.READY) {
            logger.log(Level.WARNING, "RTSP state: " + state);
            return false;
        }
        RTSPSeqNb++;
        send_RTSP_request("PLAY");
        if (parse_server_response() != 200) {
            logger.log(Level.WARNING, "Invalid Server Response");
            return false;
        } else {
            state = State.PLAYING;
            logger.log(Level.INFO, "New RTSP state: PLAYING\n");
            return true;
        }
    }

    @Override
    public boolean pause() {
        if (state != State.PLAYING) {
            logger.log(Level.WARNING, "RTSP state: " + state);
            return false;
        }
        RTSPSeqNb++;
        send_RTSP_request("PAUSE");
        if (parse_server_response() != 200) {
            logger.log(Level.WARNING, "Invalid Server Response");
            return false;
        } else {
            state = State.READY;
            logger.log(Level.INFO, "New RTSP state: READY\n");
            return true;
        }
    }

    @Override
    public boolean teardown() {
        if (state == State.INIT) {
            logger.log(Level.WARNING, "RTSP state: " + state);
            return false;
        }
        RTSPSeqNb++;
        send_RTSP_request("TEARDOWN");
        if (parse_server_response() != 200) {
            logger.log(Level.WARNING, "Invalid Server Response");
            return false;
        } else {
            state = State.INIT;
            logger.log(Level.INFO, "New RTSP state: INIT\n");
            return true;
        }
    }

    @Override
    public void describe() {
        RTSPSeqNb++;  // Increase the RTSP sequence number
        send_RTSP_request("DESCRIBE");

        // Wait for and parse the server's response
        if (parse_server_response() != 200) {
            logger.log(Level.WARNING, "Invalid Server Response for DESCRIBE");
        } else {
            logger.log(Level.INFO, "DESCRIBE request successful");
        }
    }

    @Override
    public void options() {
        RTSPSeqNb++;  // Increase the RTSP sequence number
        send_RTSP_request("OPTIONS");

        // Wait for and parse the server's response
        if (parse_server_response() != 200) {
            logger.log(Level.WARNING, "Invalid Server Response for OPTIONS");
        } else {
            logger.log(Level.INFO, "OPTIONS request successful");
        }
    }

    @Override
    public void send_RTSP_request(String request_type) {
        try {
            // Construct the request line (e.g., PLAY rtsp://example.com/video RTSP/1.0)
            String request = request_type + " " + url.toString() + " RTSP/1.0" + CRLF;

            // Add the CSeq header
            request += "CSeq: " + RTSPSeqNb + CRLF;

            // Additional headers for SETUP
            if (request_type.equals("SETUP")) {
                request += "Transport: RTP/UDP; client_port=" + RTP_RCV_PORT + CRLF;
            }

            // Add session ID if it has been set (not during SETUP)
            if (!request_type.equals("SETUP") && !RTSPid.equals("0")) {
                request += "Session: " + RTSPid + CRLF;
            }

            // End the request
            request += CRLF;

            // Log and send the request
            logger.log(Level.INFO, "RTSP Request: \n" + request);
            RTSPBufferedWriter.write(request);
            RTSPBufferedWriter.flush();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error sending RTSP request: " + e.getMessage());
        }
    }
}
