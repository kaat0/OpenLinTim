#include <fstream>
#include <iostream>
#include "evaluation.h"
#include "../../essentials/config/config.h"
#include "../../essentials/statistic/statistic.h"

int main (void)
{
	config::from_file("basis/Config.cnf", false);
	string filename_statistic = config::get_string_value("default_statistic_file");
	statistic::from_file(filename_statistic);
	evaluation eval;
	
	eval.init(config::get_string_value("default_lines_file"), config::get_string_value("default_loads_file"), config::get_string_value("default_od_file"), config::get_string_value("default_edges_file"), config::get_string_value("default_pool_cost_file"));

	eval.evaluate();
	
	eval.print_results(config::get_string_value("default_evaluation_lines_file"));

	eval.results_to_statistic();
	statistic::to_file(filename_statistic);
}
