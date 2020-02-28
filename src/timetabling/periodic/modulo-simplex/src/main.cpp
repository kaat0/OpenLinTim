/*
 * main.cpp
 */
#include "modulo_network_simplex.h"
#include "../../../../essentials/config/config.h"

using namespace std;
using namespace modulosimplex;

int main(int argc, char** argv)
{
      
      cout<<"**************************\n**************************\n**************************\n**************************\n";
  
	config::from_file("basis/Config.cnf", false);
	
	//create algorithm object
	simplex solver(config::get_integer_value("tim_nws_seed"));

	solver.set_limit(config::get_integer_value("tim_nws_limit"));
	
	solver.set_timelimit(config::get_integer_value("tim_nws_timelimit"));
	
	//set usage of headways
	solver.set_headways(config::get_bool_value("tim_nws_headways"));

	//choose the local cut method
	std::string loc_method(config::get_string_value("tim_nws_loc_search"));
	if (loc_method == "SINGLE_NODE_CUT")
		solver.set_loc_improvement(SINGLE_NODE_CUT);
	else if (loc_method == "RANDOM_CUT")
		solver.set_loc_improvement(RANDOM_CUT);
	else if (loc_method == "WAITING_CUT")
		solver.set_loc_improvement(WAITING_CUT);
	else if (loc_method == "CONNECTED_CUT")
		solver.set_loc_improvement(CONNECTED_CUT);
	else if (loc_method == "NO_CUT")
		solver.set_loc_improvement(NO_CUT);	  
	else
	{
		cout<<"Unknown local search method: "<<loc_method<<".\nAborting algorithm.\n";
		exit(1);
	}
	
	//choose the fundamental cut method
	std::string tab_method(config::get_string_value("tim_nws_tab_search"));
	if (tab_method == "TAB_FULL")
	{
		solver.set_tab_search(TAB_FULL);
		cout<<"Using tab full search method.\n";
	}
	else if (tab_method == "TAB_SIMPLE_TABU_SEARCH")
	{
		solver.set_tab_search(TAB_SIMPLE_TABU_SEARCH);
		solver.set_ts_memory(config::get_integer_value("tim_nws_ts_memory"));
		solver.set_ts_max_iterations(config::get_integer_value("tim_nws_ts_max_iterations"));
		cout<<"Using tabu search method.\n";
	}
	else if (tab_method == "TAB_SIMULATED_ANNEALING")
	{
		solver.set_tab_search(TAB_SIMULATED_ANNEALING);
		solver.set_sa_init_temperature(config::get_integer_value("tim_nws_sa_init"));
		solver.set_sa_coolness_factor(config::get_double_value("tim_nws_sa_cooldown"));
		cout<<"Using simulated annealing search method.\n";
	}
	else if (tab_method == "TAB_STEEPEST_SA_HYBRID")
	{
		solver.set_tab_search(TAB_STEEPEST_SA_HYBRID);
		solver.set_sa_init_temperature(config::get_integer_value("tim_nws_sa_init"));
		solver.set_sa_coolness_factor(config::get_double_value("tim_nws_sa_cooldown"));
		cout<<"Using steepest descend simulated annealing hybrid search method.\n";
	}
	else if (tab_method == "TAB_PERCENTAGE")
	{
		solver.set_tab_search(TAB_PERCENTAGE);
		solver.set_percentage_improvement(config::get_integer_value("tim_nws_percentage"));	
		cout<<"Using tab percentage search method.\n";
	}
	else if (tab_method == "TAB_FASTEST")
	{
		solver.set_tab_search(TAB_FASTEST);
		solver.set_min_pivot_improvement(config::get_double_value("tim_nws_min_pivot"));
		solver.set_dynamic_pivot_factor(config::get_double_value("tim_nws_dyn_pivot"));
		solver.set_tab_min_improvement(DYNAMIC);
		cout<<"Using tab fastest search method.\n";
	}
	else if (tab_method == "TAB_NOTHING")
	{
		solver.set_tab_search(TAB_NOTHING);
		cout<<"Using tab nothing search method.\n";
	}
	else
	{
		cout<<"Unknown fundamental search method: "<<tab_method<<".\nAborting algorithm.\n";
		exit(1);
	}	
	
	//initialise with EAN and period
	if (config::get_string_value("tim_model") == "con_ns" || config::get_string_value("tim_model") == "ns_improve" || config::get_string_value("tim_model") == "csp_ns")
	solver.init(config::get_string_value("default_activities_periodic_file"), config::get_string_value("default_events_periodic_file"), config::get_integer_value("period_length"), config::get_string_value("default_timetable_periodic_file"));
	else{
        std::cout << "Called network_simplex with invalid tim_model " << config::get_string_value("tim_model") << "\n" << std::flush;
        exit(-1);
	}
	//start the algorithm
	solver.solve();
	
	//write out the timetable
	solver.write_result(config::get_string_value("default_timetable_periodic_file"));

	return 0;

}
