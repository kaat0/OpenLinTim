# Statistical Data

This directory contains numpy data files collected using the `RobustnessEvaluator` or the `RobustnessOptimizer`. Graphs generated from this data can be found in the related report. The files contain the results of the following numerical experiments:

* Optimizing robustness using different EAN generators: `adding1.mat` increasing slack, `weighted.mat` constant weighted slack, `unweighted.mat` constant unweighted slack.
* Iterating the evaluation 5000 times: `bahn-01.mat` information about the delayed timetables, `bahn-01-nominal.mat` information about the nominal timetable

More detailed information about these experiments can be found in the `figure_XX.py` files in the main directory.
