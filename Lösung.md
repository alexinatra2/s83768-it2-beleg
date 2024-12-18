# Lösung

## 1. Client Methoden

[RtpPacket](src/rtp/RtpPacket.java) 

## 2. SDP-Protokoll 

[RtpPacket](src/rtp/RtpPacket.java) 

## 3. RTP-Protokoll

[RtpPacket](src/rtp/RtpPacket.java) 

## 4. Auswertung der Fehlerstatistiken ohne Fehlerkorrektur

In [Statistiken](statistics/rtp-ideal.gp) wird die Paketverlustwahrscheinlichkeit
in Abhängigkeit von der Kanalverlustrate mittels `gnuplot` berechnet.

```bash
gnuplot ./statistics/rtp-ideal.gp
```

## 5. Implementierung des FEC-Schutzes

[FecHandler](src/rtp/FecHandler.java)

## 6. 

## 7. Generierung von Restart-Markern

In [restart-markers](images/restart-markers/) befinden sich alle Bilder für diese
Aufgabe. Folgende Dateien sind im Directory enthalten:

- [das Original](images/restart-markers/htw-0080.jpeg)
- [mit Restart-Markern](images/restart-markers/htw-0080-restart.jpeg)
- [das Original nach Hex-Editor Manipulation](images/restart-markers/htw-0080-broken.jpeg)
- [mit Restart-Markern nach Hex-Editor Manipulation](images/restart-markers/htw-0080-restart-broken.jpeg)

Es fällt deutlich auf, dass nach Manipulation im Hex-Editor das JPEG Bild mit Restart-Markern 
im Vergleich zum Original intakt bleibt. Das Original ist vollständig zerstört.

## 8. Fehlerkaschierung

[JpegDisplay](src/JpegDisplay.java)

## 11. Dokumentation

