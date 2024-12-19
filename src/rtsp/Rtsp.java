package rtsp;

import video.VideoMetadata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
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

    @Override
    String getDescribe(VideoMetadata meta, int RTP_dest_port) {
        StringWriter sdpBody = new StringWriter();
        StringWriter response = new StringWriter();

        // Build SDP body for MJPEG
        sdpBody.write("v=0" + CRLF); // Protocol version
        sdpBody.write("o=- 123456 123456 IN IP4 127.0.0.1" + CRLF); // Origin placeholder
        sdpBody.write("s=MJPEG Video Stream" + CRLF); // Session name
        sdpBody.write("i=Motion JPEG Stream Description" + CRLF); // Session information
        sdpBody.write("t=0 0" + CRLF); // Time (start and end times)
        sdpBody.write("m=video " + RTP_dest_port + " RTP/AVP " + MJPEG_TYPE + CRLF); // Media description for MJPEG
        sdpBody.write("a=framerate:" + meta.getFramerate() + CRLF); // Frame rate attribute (custom)

        // Additional placeholder for duration if needed
        if (meta.getDuration() > 0) {
            sdpBody.write("a=duration:" + meta.getDuration() + CRLF); // Custom attribute for duration
        }

        // Build headers
        response.write("Content-Type: application/sdp" + CRLF);
        response.write("Content-Length: " + sdpBody.toString().length() + CRLF);
        response.write(CRLF);
        response.write(sdpBody.toString());

        // Log the response to the console
        logger.log(Level.INFO, "DESCRIBE response:\n" + response);

        return response.toString();
    }
}
