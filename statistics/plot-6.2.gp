#!/usr/local/bin/gnuplot --persist

# Define axis labels and title
set xlabel "Kanalfehler (Fehlerwahrscheinlichkeit)"
set ylabel "Fehlerquote (Restfehler und Gesamtfehler)"
set title "Vergleich von Restfehler und Gesamtfehler für verschiedene Gruppengrößen (k)"

# Configure grid and style
set grid
set style data linespoints
set key right bottom

# Wir definieren die Gruppengrößen (k = 2, 6, 12, 48)
k_values = "2 6 12 48"

# Plot total and residual error rates for all k values
plot for [k in k_values] sprintf("k%s_data.dat", k) using 1:5 title sprintf("k = %s", k) with lines lw 2
    
pause -1

