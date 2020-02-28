import os, sys, math
from robtim import Dataset as Dataset
from robtim.eval import *

from core.util.config import Config
from core.io.config import ConfigReader
from core.util.statistic import Statistic
from core.io.statistic import StatisticWriter
from core.io.statistic import StatisticReader

cwd = os.getcwd()

config = Config();

ConfigReader.read(cwd+'/basis/Config.cnf', True, config)
#print(cwd+'/basis/Config.cnf')
localconf = {
	"lc_solver": config.getStringValue('lc_solver'),
	"DM_solver": config.getStringValue('DM_solver'),
	"DM_method" : config.getStringValue('DM_method'),
	"tim_solver": config.getStringValue('tim_solver'),
}


d = Dataset(cwd.split('/')[-1], cwd.rsplit('/', 1)[0] , os.environ.copy() , silent=True)
d.resetConfig()
d.applyConfig(localconf)

evalIterations = config.getIntegerValue('dm_robustness_iterations')
binomial = config.getDoubleValue('dm_robustness_treeOnTrack_binomial')
beta = config.getIntegerValue('dm_robustness_treeOnTrack_beta')

evaluator = RobustnessEvaluator(
	scenarios = ScenarioScheduler(
		TreeOnTrackScenarioGenerator(binomial, beta),
		iterations=[evalIterations]),
	statistician = MatrixStatistician([], ["dm_time_average"]))
	
nominal, delayed = evaluator.evaluate(d);

stat = StatisticReader.read('./statistic/statistic.sta')
stat.setValue('tim_rob_nominal_time_avg', nominal[0])
delay_max = -1
delay_min = math.inf
delay_sum = 0
for d in delayed:
	delay_max = max(delay_max, d[0]);
	delay_min = min(delay_min, d[0]);
	delay_sum += d[0];
stat.setValue('tim_rob_delayed_avg_avg', delay_sum/evalIterations);
stat.setValue('tim_rob_delayed_avg_min', delay_min);
stat.setValue('tim_rob_delayed_avg_max', delay_max);

StatisticWriter.write(stat, './statistic/statistic.sta')


#tim_rob_nominal_time_average, tim_rob_delayed_time_average_average, time_rob_delayed_time_average_max min
print("Nominal: ", nominal)
print("Delayed: ", delayed)
