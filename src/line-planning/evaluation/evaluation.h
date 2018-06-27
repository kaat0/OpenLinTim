#ifndef EVALUATION_H_
#define EVALUATION_H_

#include "../../essentials/shortest-paths/shortest_paths.h"
#include <vector>
#include <fstream>
#include <list>
#include <set>

typedef std::map<std::pair<int,int>, int> OD;

enum edge_length_type {
	AVERAGE_DRIVING_TIME,
	MINIMAL_DRIVING_TIME,
	MAXIMAL_DRIVING_TIME,
	EDGE_LENGTH
};

struct edgedata
{
	int nr;
	int from;
	int to;
	double length;
	int lower;
	int upper;
	bool used;
	double duration;
};

struct line
{
public:
	line (std::vector<int> e, int f, int _id)
	{
		edges = e;
		frequency = f;
		id = _id;
	}

	int id;
	std::vector<int> edges;
	int frequency;
	std::set<int> nodes;
};

class evaluation
{
public:
	evaluation();
	void init(std::string linefile, std::string loadfile, std::string odfile, std::string edgefile, std::string poolfile);
	void evaluate();
	void print_results();
	void print_results(std::string outputfile);
	void results_to_statistic();
	void print_to_statistic();

private:
	bool feasible();
	int number_lines();
	double average_frequency();
	int max_frequency();
	double variance_frequency();
	double average_line_length();
        std::string edge_frequency_distribution();
	double variance_line_length();
	int nr_of_changes_sp();
	int nr_possible_direct_travellers();
	int nr_sp_direct_travellers();
	int nr_all_sp_direct_travellers();
	double pool_costs();
	int max_frequency_edge();
	int max_frequency_edge_value();
	int sum_squared_edge_frequencies();

	void reachable(int from);
	void reachable_lc(int from);

	std::vector<line> lineconcept;
	std::vector<std::pair<int,int> > loads;

	std::list<std::string> comments;

	OD od_matrix;
	std::map<int,edgedata> edges;
	std::map<int, double> costs;

	shortest_paths path;

	bool feasible_result;
	int number_lines_result;
	double average_frequency_result;
	int max_frequency_result;
	int max_frequency_edge_result;
	int max_frequency_edge_value_result;
	double variance_frequency_result;
	int nr_possible_direct_travellers_result;
	double average_line_length_result;
	double variance_line_length_result;
	int nr_sp_direct_travellers_result;
	int nr_all_sp_direct_travellers_result;
	double pool_costs_result;
	int nr_of_changes_sp_result;
	int sum_squared_edge_frequencies_result;
	std::string edge_freq_result;
	double gen_conversion_length;
	
	bool directed;
	bool lc_extended_statistic;

	// Methods to determine the length of an edge based on the appropriate config file entry ("ean_model_weight_drive")
	edge_length_type process_edge_length_type(std::string config_value);
	double compute_edge_length(edge_length_type length_type, double length, double min_travel_time, double max_travel_time);
};

#endif
