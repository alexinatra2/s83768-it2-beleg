#!/usr/local/bin/gnuplot --persist

# Achsenbezeichner und Titel
set title "Bilddefektwahrscheinlichkeit bei verschiedenen RTPs/Bild"
set xlabel "Kanalfehlerrate (P)"
set ylabel "Bilddefektwahrscheinlichkeit"

# Legende
set key left top

# Gruppengrößen (1, 5, 20 RTPs/Bild)
k_values = "1 5 20"

# Setze das Bereich für P
set xrange [0:1]
set yrange [0:1]

# Funktion zur Berechnung der Bilddefektwahrscheinlichkeit
# Bilddefekt tritt auf, wenn mehr als 1 Paket verloren geht (k > 1)
image_defect(k, P) = (1 - (1 - P)**k) ** (k > 1)

# Plotte die Bilddefektwahrscheinlichkeiten für verschiedene k-Werte
plot for [k in k_values] image_defect(k, x) title sprintf("RTPs/Bild = %s", k) with lines lw 2

pause -1
