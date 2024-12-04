#!/usr/local/bin/gnuplot --persist

set title "Frame Loss Probability vs. Channel Loss Rate"
set xlabel "Channel Loss Rate (P_e)"
set ylabel "Frame Loss Probability (P_frame)"
set grid
set xrange [0:1]
set yrange [0:1]
set key left top

# Input file
plot "probabilities.txt" using 1:2 with lines title "1 RTP/frame" linewidth 2, \
     "probabilities.txt" using 1:3 with lines title "2 RTP/frame" linewidth 2, \
     "probabilities.txt" using 1:4 with lines title "5 RTP/frame" linewidth 2, \
     "probabilities.txt" using 1:5 with lines title "10 RTP/frame" linewidth 2, \
     "probabilities.txt" using 1:6 with lines title "20 RTP/frame" linewidth 2
pause -1
