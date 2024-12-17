#!/usr/local/bin/gnuplot --persist

set title "Ideale Bildverlustwahrscheinlichkeit in Abhängigkeit von der Kanalverlustrate"
set xlabel "P_e (Kanalverlustwahrscheinlichkeit)"
set ylabel "P_e (Bildverlustwahrscheinlichkeit)"
set grid
set key right bottom

plot "data_file_2.txt" with lines title "2 RTP Pakete/Bild", \
     "data_file_5.txt" with lines title "5 RTP Pakete/Bild", \
     "data_file_10.txt" with lines title "10 RTP Pakete/Bild", \
     "data_file_20.txt" with lines title "20 RTP Pakete/Bild"

pause -1
