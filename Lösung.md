# Lösung

## 0. Allgemeines Vorgehen

## 1. Client Methoden

Der Code für diese Aufgabe ist in der Klasse [`rtsp.Rtsp`](src/rtsp/Rtsp.java) zu finden.

Die Methoden implementieren die grundlegende RTSP-Funktionalität. Die Methoden `play`, 
`pause` und `teardown` verwalten die Zustandsübergänge und senden die entsprechenden 
RTSP-Anfragen. Die Zustandsübergänge sind wie folgt implementiert:

- Von READY zu PLAYING nach erfolgreichem PLAY-Request.
- Von PLAYING zu READY nach erfolgreichem PAUSE-Request.
- Von READY oder PLAYING zu INIT nach erfolgreichem TEARDOWN-Request.
 
Die Methoden `describe` und `options` fragen Informationen vom Server ab. 
Die Methode `send_RTSP_request` erstellt und sendet RTSP-Anfragen verarbeitet 
dabei Header wie `CSeq`, `Transport` und `Session`.

## 2. SDP-Protokoll 

Der Code für diese Aufgabe ist wie in Aufgabe 1 in der Klasse 
[`rtsp.Rtsp`](src/rtsp/Rtsp.java) zu finden.

Die Methode `getDescribe()` wurde so implementiert, dass sie eine statische SDP-Antwort 
(Session Description Protocol) für einen MJPEG-Stream zurückgibt. Die wichtigsten Parameter, 
wie die Framerate des Videos und die Dauer (falls vorhanden), werden aus den Metadaten des Videos übernommen. 
Die Antwort wird als String aufgebaut, der die notwendigen Felder für das SDP-Protokoll enthält, 
einschließlich der Protokollversion, Origin-Informationen, Session-Details und der Medienbeschreibung 
für den MJPEG-Stream. 
Eine logische Darstellung dieser Antwort wird dann in der Konsole ausgegeben.

## 3. RTP-Protokoll

Der Code für diese Aufgabe befindet sich in der Klass [`RtpPacket`](src/rtp/RtpPacket.java).

Die Methode `setRtpHeader()` setzt die RTP-Header-Felder gemäß der RTP-Spezifikation. 
Sie arbeitet mit den vordefinierten Feldern wie Version, Padding, Marker, Payload Type, Sequence Number, Timestamp und SSRC. 
Der Header wird byteweise aufgebaut, wobei jedes Byte entsprechend seiner Position und Bedeutung gesetzt wird.

- Byte 0 setzt die Version (2), Padding (0), Extension (0) und CC (0) entsprechend der RTP-Spezifikation.
- Byte 1 kombiniert den Marker (1 Bit) und den Payload Type (7 Bits).
- Byte 2-3 setzen die Sequence Number als 16-Bit-Wert.
- Byte 4-7 setzen den Timestamp als 32-Bit-Wert.
- Byte 8-11 setzen die SSRC (Synchronization Source Identifier) als 32-Bit-Wert.

## 4. Auswertung der Fehlerstatistiken ohne Fehlerkorrektur

In [Statistiken](statistics/plot-4.gp) wird die Paketverlustwahrscheinlichkeit
in Abhängigkeit von der Kanalverlustrate mittels `gnuplot` berechnet.

```bash
gnuplot ./statistics/plot-4.gp
```

## 5. Implementierung des FEC-Schutzes

Für die Lösung dieser Aufgabe wurden in [`FecHandler`](src/rtp/FecHandler.java) `checkCorrection` und `correctRtp`
überschrieben. 

Damit ein Paket korrigiert mit FEC korrigiert werden kann, muss die Anzahl der Fehler in einer FEC-Gruppe genau 
1 betragen. Dies wird in der `checkCorrection` Methode ermittelt. Sollte dies der Fall sein wird `correctRtp`
aufgerufen und mittels XOR, das Paket wiederhergestellt. Der XOR Call verbirgt sich hierbei hinter der `addRtp`
Methode, welche iterativ für alle anwesenden Gruppenpakete aufgerufen wird. 

## 6. Analyse der Leistungsfähigkeit des implementierten FEC-Verfahrens

### 6.1. Parameterwahl

Bei einer Kanalverlustrate von 10 % beim Einsatz von FEC mit k=2 wurden nach etwa 10 Sekunden
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

### 6.2. Bestimmung der Verlustraten mittels Simulation

In [diesem Diagram](./statistics/plot-6.2.gp) werden die Messungen aus
für [k=2](./statistics/k2_data.dat), [k=6](./statistics/k6_data.dat), 
[k=12](./statistics/k12_data.dat) und [k=48](./statistics/k48_data.dat) ausgewertet.

Dabei wurden in den Messungsdaten je die Kanalverlustrate, der Paketindex, die Anzahl
an verlorenen RTP-Paketen, die Anzahl an korrigierbaren RTP-Paketen und die Restfehlerwahrscheinlichkeit
erfasst.

```bash
cd statistics
gnuplot ./statistics/plot-6.2.gp
```

### 6.3. Abschätzung der zu erwartenden Verlustraten mittels theoretischer Betrachtung

Die vereinfachte Formel für die Paketverlustwahrscheinlichkeit kann genutzt werden, 
weil sie auf der Annahme basiert, dass Fehlerereignisse unabhängig sind und die Wahrscheinlichkeit
für den Verlust von Paketen in einem FEC-geschützten Block direkt von der Kanalfehlerrate abhängt. 
Für geringe Fehlerwahrscheinlichkeiten ist die Annahme, dass ein Paketverlust nur dann relevant ist, 
wenn mehr als ein Paket in einem FEC-Block verloren geht, eine gute Näherung, da die Wahrscheinlichkeit 
für mehrere Fehler in einem Block bei niedrigen Fehlerraten sehr gering ist.

Für das Gnuplot Skript wurde daher die aus der Vorlesung vereinfachte Variante der Formel verwendet:

`P = P_e^k`

```bash
gnuplot ./statistics/plot-6.3.gp
```

### 6.4. Abschätzung der Bilddefektwahrscheinlichkeit

Die Bilddefektwahrscheinlichkeit hängt von der Anzahl der RTP-Pakete pro Bild und der Kanalfehlerrate ab. 
Für ein Bild mit nur einem RTP-Paket tritt ein Bilddefekt auf, wenn dieses eine Paket verloren geht, 
was die Wahrscheinlichkeit direkt auf die Kanalfehlerrate setzt. Bei mehreren RTP-Paketen pro Bild (z. B. 5 oder 20) 
tritt ein Bilddefekt auf, wenn mehr als ein Paket verloren geht, und die Wahrscheinlichkeit lässt sich durch 
die Berechnung der Verlustwahrscheinlichkeit für mehr als ein Paket in einem Block ermitteln.

Dazu wurde im Gnuplot Skript die folgende Formel verwendet:

`image_defect(k, P) = (1 - (1 - P)**k) ** (k > 1)`

Hierbei ist resultiert die Expression `k > 1` bei `k = 1` in einem Wert 0, was zur Folge hat, dass die
Defektwahrscheinlichkeit einfach als 1 angenommen wird. Für allgemeine Fälle (5, 20) resultiert also
nur:

`image_defect(k, P) = 1 - (1 - P)**k`

```bash 
gnuplot ./statistics/plot-6.4.gp
```

## 7. Generierung von Restart-Markern

In [restart-markers](images/restart-markers) befinden sich alle Bilder für diese
Aufgabe. Folgende Dateien sind im Directory enthalten:

- [das Original](images/restart-markers/htw-0080.jpeg)
- [mit Restart-Markern](images/restart-markers/htw-0080-restart.jpeg)
- [das Original nach Hex-Editor Manipulation](images/restart-markers/htw-0080-broken.jpeg)
- [mit Restart-Markern nach Hex-Editor Manipulation](images/restart-markers/htw-0080-restart-broken.jpeg)

Es fällt deutlich auf, dass nach Manipulation im Hex-Editor das JPEG Bild mit Restart-Markern 
im Vergleich zum Original intakt bleibt. Das Original ist vollständig zerstört.

## 8. Fehlerkaschierung

Der Code für diese Aufgabe befindet sich in der Klasse [`JpegDisplay`](src/JpegDisplay.java). 

Die Liste im Argument von `setTransparency` enthält die Indices der verlorenen JPEG Restart-Slices.
Diese haben eine Höhe von 16 Pixeln, da eine MCU (Minimum Coded Unit) in unserem Beispiel 16x16 Pixel groß ist. 
An diesen Stellen wird der das Vordergrundbild auf diesem Slice transparent gesetzt.
Das finale Bild setzt sich dann aus Vorder- und Hintergrundbild zusammen, wobei das Hintergrundbild das Bild des
vorangegangenen Frames ist.
