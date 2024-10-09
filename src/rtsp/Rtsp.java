package rtsp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.URI;

public class Rtsp extends RtspDemo {
    public Rtsp(URI url, int rtpRcvPort) {
        super(url, rtpRcvPort);
    }

    public Rtsp(BufferedReader RTSPBufferedReader, BufferedWriter RTSPBufferedWriter) {
        super(RTSPBufferedReader, RTSPBufferedWriter);
    }

    @Override
    public boolean play() {
        return false;
    }

    @Override
    public boolean pause() {
        return false;
    }

    @Override
    public boolean teardown() {
        return false;
    }

    @Override
    public void describe() {

    }

    @Override
    public void options() {

    }

    @Override
    public void send_RTSP_request(String request_type) {

    }
}
