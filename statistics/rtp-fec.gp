#!/usr/local/bin/gnuplot --persist

set title "Bildverlustwahrscheinlichkeit in Abhängigkeit von der Kanalverlustrate"
set xlabel "P_e (Kanalverlustwahrscheinlichkeit)"
set ylabel "P_e (Bildverlustwahrscheinlichkeit)"
set grid
set key right bottom

# Funktion für die Berechnung des Bildverlusts
P_v(x, N) = 1 - (1 - x)**N

# Einstellen des Bereichs für die x- und y-Achse
set xrange [0:1]
set yrange [0:1]

# Plotten der Bildverlustwahrscheinlichkeit für unterschiedliche Anzahlen von RTP-Paketen pro Bild
plot P_v(x, 1) lw 2 title "1 RTP-Pakete/Bild", \
     P_v(x, 2) lw 2 title "2 RTP-Pakete/Bild", \
     P_v(x, 5) lw 2 title "5 RTP-Pakete/Bild", \
     P_v(x, 10) lw 2 title "10 RTP-Pakete/Bild", \
     P_v(x, 20) lw 2 title "20 RTP-Pakete/Bild"

pause -1
