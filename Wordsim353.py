#!/usr/bin/python

from scipy import stats
import sys

## Word1	Word2	Human_(mean)	Score
esa_353 = open(str(sys.argv[1]))

x = []
y = []

for line in esa_353:
  # print float(e[i].split("\t")[2]), float(e[i].split("\t")[3])
  x.append(float(line.split("\t")[2]))
  y.append(float(line.split("\t")[3]))
  #print float(line.split("\t")[3])
print "Spearman rank-order correlation coefficient:", stats.spearmanr(x, y)
print "Pearson's linear correlation coefficient:", stats.pearsonr(x, y)
