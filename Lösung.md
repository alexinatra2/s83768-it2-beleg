# Lösung

## 1. Client Methoden

[RtpPacket](src/rtp/RtpPacket.java) 

## 2. SDP-Protokoll 

[RtpPacket](src/rtp/RtpPacket.java) 

## 3. RTP-Protokoll

[RtpPacket](src/rtp/RtpPacket.java) 

## 4. Auswertung der Fehlerstatistiken ohne Fehlerkorrektur

In [Statistiken](statistics/plot-4.gp) wird die Paketverlustwahrscheinlichkeit
in Abhängigkeit von der Kanalverlustrate mittels `gnuplot` berechnet.

```bash
gnuplot ./statistics/plot-4.gp
```

## 5. Implementierung des FEC-Schutzes

[FecHandler](src/rtp/FecHandler.java)

## 6. Analyse der Leistungsfähigkeit des implementierten FEC-Verfahrens

### 6.1. Parameterwahl

Bei einer Kanalverlustrate von 10% beim Einsatz von FEC mit k=2 wurden nach etwa 10 Sekunden
223 RTP-Pakete verloren bei RTP-Index 2229. Die reale Kanalverlustrate war also folgende

```
P = 223 / 2229 = 0.100044863
```

Von diesen 223 verlorenen Paketen wurden 152 korrigiert und 71 nicht korrigiert. 
Die Verlustrate der RTP-Pakete errechnet sich damit, wie folgt:

```
P = 71 / 2229 = 0,031852849
```

Die Korrektureffizienz beträgt damit:

```
Korrektureffizienz = 152 / 223 = 0,68161435
```

### 6.2 Bestimmung der Verlustraten mittels Simulation

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

