#!/usr/bin/env python

import numpy as np
import sys
from scipy import stats

# I used the lee.cor and similarities0-1.txt files from https://github.com/piskvorky/gensim/tree/develop/gensim/test/test_data

sim_matrix_original = np.loadtxt("similarities0-1.txt")
gesa_matrix = np.loadtxt(str(sys.argv[1]))

sim_matrix_original_list = []
sim_matrix_list = []

sim_matrix_original_list = []
sim_matrix_list = []
for i in range(50):
  for j in range(50):
    if(i <= j):
      sim_matrix_original_list.append(sim_matrix_original[i][j])
      sim_matrix_list.append( pow(gesa_matrix[i][j], 1) )

print "Pearson's linear correlation coefficient:", stats.pearsonr(sim_matrix_original_list, sim_matrix_list)
print "Spearman rank-order correlation coefficient:", stats.spearmanr(sim_matrix_original_list, sim_matrix_list)

