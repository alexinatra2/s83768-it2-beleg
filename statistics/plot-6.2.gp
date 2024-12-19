#!/usr/local/bin/gnuplot --persist

# Define axis labels and title
set xlabel "Kanalfehler (Fehlerwahrscheinlichkeit)"
set ylabel "Fehlerquote (Restfehler und Gesamtfehler)"
set title "Vergleich von Restfehler und Gesamtfehler für verschiedene Gruppengrößen (k)"

# Configure grid and style
set grid
set style data linespoints
set key right bottom

# Plot total and residual error rates for all k values
plot \
    "k2_data.dat" using 1:1 with linespoints title "Gesamtfehlerquote (k=2)" lc rgb "red", \
    "k2_data.dat" using 1:($4/$2) with linespoints title "Restfehlerquote (k=2)" lc rgb "pink", \
    "k6_data.dat" using 1:1 with linespoints title "Gesamtfehlerquote (k=6)" lc rgb "blue", \
    "k6_data.dat" using 1:($4/$2) with linespoints title "Restfehlerquote (k=6)" lc rgb "cyan", \
    "k12_data.dat" using 1:1 with linespoints title "Gesamtfehlerquote (k=12)" lc rgb "green", \
    "k12_data.dat" using 1:($4/$2) with linespoints title "Restfehlerquote (k=12)" lc rgb "light-green", \
    "k48_data.dat" using 1:1 with linespoints title "Gesamtfehlerquote (k=48)" lc rgb "orange", \
    "k48_data.dat" using 1:($4/$2) with linespoints title "Restfehlerquote (k=48)" lc rgb "yellow"
    
pause -1

