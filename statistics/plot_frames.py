import numpy as np

# Parameters
packet_loss_rates = np.linspace(0, 1, 101)  # Channel loss rates from 0 to 1
num_packets_per_frame = [1, 2, 5, 10, 20]  # RTP packets per frame

# File to store results for Gnuplot
output_file = "probabilities.txt"

# Calculate probabilities and write to file
with open(output_file, "w") as f:
    # Write header
    f.write("P_e\t" + "\t".join([f"n={n}" for n in num_packets_per_frame]) + "\n")
    for P_e in packet_loss_rates:
        probabilities = [1 - (1 - P_e) ** n for n in num_packets_per_frame]
        f.write(f"{P_e:.4f}\t" + "\t".join([f"{p:.4f}" for p in probabilities]) + "\n")
