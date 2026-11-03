"""
Draws the plots for the README from the CSV files in the "results" folder.

Usage (from the project root):
    pip install matplotlib
    python plots/make_plots.py
"""
import csv
from pathlib import Path

import matplotlib
matplotlib.use("Agg")  # draw to files, no window needed
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parent.parent
RESULTS = ROOT / "results"
PLOTS = ROOT / "plots"


def read_csv(name):
    with open(RESULTS / name, newline="") as f:
        return list(csv.DictReader(f))


def save(fig, name):
    fig.tight_layout()
    fig.savefig(PLOTS / name, dpi=130)
    plt.close(fig)
    print("saved", PLOTS / name)


# 1) LCS and Edit Distance: time vs input size
rows = read_csv("dp.csv")
fig, ax = plt.subplots(figsize=(7, 4.5))
for algo, marker in [("LCS", "o"), ("EditDistance", "s")]:
    data = [r for r in rows if r["algorithm"] == algo]
    ax.plot([int(r["n"]) for r in data], [float(r["avg_time_ms"]) for r in data],
            marker=marker, label=algo)
ax.set_title("LCS and Edit Distance: time vs string length (O(n*m))")
ax.set_xlabel("length n of each string")
ax.set_ylabel("average time (ms), 5 runs")
ax.grid(alpha=0.3)
ax.legend()
save(fig, "dp_time.png")


# 2) Naive vs KMP vs Rabin-Karp: time vs text length (random text)
rows = read_csv("matching.csv")
fig, axes = plt.subplots(1, 2, figsize=(11, 4.5), sharey=True)
for ax, case in zip(axes, ["present", "absent"]):
    for algo, marker in [("Naive", "o"), ("KMP", "s"), ("Rabin-Karp", "^")]:
        data = [r for r in rows if r["algorithm"] == algo and r["case"] == case]
        ax.plot([int(r["n"]) for r in data], [float(r["avg_time_ms"]) for r in data],
                marker=marker, label=algo)
    ax.set_title(f"Random text, pattern {case} (m = 20)")
    ax.set_xlabel("text length n")
    ax.grid(alpha=0.3)
    ax.legend()
axes[0].set_ylabel("average time (ms), 5 runs")
save(fig, "matching_time.png")


# 3) Extra: worst case for Naive
rows = read_csv("matching_worst_case.csv")
fig, ax = plt.subplots(figsize=(7, 4.5))
for algo, marker in [("Naive", "o"), ("KMP", "s"), ("Rabin-Karp", "^")]:
    data = [r for r in rows if r["algorithm"] == algo]
    ax.plot([int(r["n"]) for r in data], [float(r["avg_time_ms"]) for r in data],
            marker=marker, label=algo)
ax.set_title("Worst case for Naive: text A...A, pattern A...AT (m = 50)")
ax.set_xlabel("text length n")
ax.set_ylabel("average time (ms), 5 runs")
ax.grid(alpha=0.3)
ax.legend()
save(fig, "matching_worst_case_time.png")


# 4) Kasai: time and comparisons vs n
rows = read_csv("kasai.csv")
n = [int(r["n"]) for r in rows]
fig, ax1 = plt.subplots(figsize=(7, 4.5))
ax1.plot(n, [float(r["avg_time_ms"]) for r in rows], marker="o", color="tab:blue", label="time (ms)")
ax1.set_xlabel("string length n")
ax1.set_ylabel("average time (ms), 5 runs", color="tab:blue")
ax2 = ax1.twinx()
ax2.plot(n, [int(r["comparisons"]) for r in rows], marker="s", linestyle="--",
         color="tab:orange", label="char comparisons")
ax2.plot(n, [2 * x for x in n], linestyle=":", color="gray", label="2n (reference line)")
ax2.set_ylabel("character comparisons", color="tab:orange")
ax1.set_title("Kasai's LCP algorithm (O(n))")
ax1.grid(alpha=0.3)
lines = ax1.get_lines() + ax2.get_lines()
ax1.legend(lines, [l.get_label() for l in lines], loc="upper left")
save(fig, "kasai_time.png")
