# Lösung

## 0. Allgemeines Vorgehen

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
die Defektwahrscheinlichkeit einfach als 1 angenommen wird. Für allgemeine Fälle (5, 20) resultiert also
nur:

`image_defect(k, P) = 1 - (1 - P)**k`

```bash 
gnuplot ./statistics/plot-6.4.gp
```

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

