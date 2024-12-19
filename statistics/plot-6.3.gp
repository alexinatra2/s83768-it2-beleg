#!/usr/local/bin/gnuplot --persist

# Achsenbezeichner und Titel
set title "Paketverlustwahrscheinlichkeit mit FEC"
set xlabel "Kanalfehlerrate (Pe)"
set ylabel "Paketverlustwahrscheinlichkeit"

# Legende
set key left top

# Berechne die Verlustwahrscheinlichkeit: P_l(k, P_e) = P_e^k
# Wir definieren die Gruppengrößen (k = 2, 6, 12, 48)
k_values = "2 6 12 48"

# Setze das Bereich für Pe
set xrange [0:1]
set yrange [0:1]

# Funktion zur Berechnung der Paketverlustwahrscheinlichkeit
packet_loss(k, Pe) = Pe**k

# Plotte die Verlustwahrscheinlichkeiten für verschiedene k-Werte
plot for [k in k_values] packet_loss(k, x) title sprintf("k = %s", k) with lines lw 2

pause -1
