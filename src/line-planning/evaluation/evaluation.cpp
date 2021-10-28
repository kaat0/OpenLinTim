#include <iostream>
#include <cstdlib>
#include <cassert>
#include <algorithm>
#include <sstream>
#include "evaluation.h"
#include "../../essentials/statistic/statistic.h"
#include "../../essentials/config/config.h"

evaluation::evaluation()
{

}

void evaluation::init(std::string linefile, std::string loadfile, std::string odfile, std::string edgefile, std::string poolfile)
{
	config::from_file("basis/Config.cnf", false);
	directed = !config::get_bool_value("ptn_is_undirected");
	lc_extended_statistic = config::get_bool_value("lc_eval_extended");
	gen_conversion_length = config::get_double_value("gen_conversion_length");
	edge_length_type length_type = process_edge_length_type(config::get_string_value("ean_model_weight_drive"));

	std::string text;

	std::ifstream edgestream(edgefile.c_str());
    while (!edgestream.eof())
    {
		getline(edgestream,text);
		if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
			edgedata e;
			e.nr = atoi(text.c_str());
			size_t pos = text.find(";");
			text=text.substr(pos+1);

			e.from = atoi(text.c_str());
			pos = text.find(";");
			text=text.substr(pos+1);

			e.to = atoi(text.c_str());
			pos = text.find(";");
			text=text.substr(pos+1);

			e.length = gen_conversion_length * atof(text.c_str());
			pos = text.find(";");
			text=text.substr(pos+1);

			e.lower = atoi(text.c_str());
			pos = text.find(";");
			text=text.substr(pos+1);

			e.upper = atoi(text.c_str());
			pos = text.find(";");
			text=text.substr(pos+1);

			e.duration = compute_edge_length(length_type, e.length, e.lower, e.upper);

			e.used = false;

			edges[e.nr] = e;
		}
	}
	edgestream.close();

	//Read in line concept
	std::ifstream linestream (linefile.c_str());

	int current_id = 0;
    while (!linestream.eof())
    {
       getline(linestream,text);
       if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
       {
			int id = atoi(text.c_str());
			size_t pos = text.find(";");
			text=text.substr(pos+1);

			int edge_order = atoi(text.c_str());
			pos = text.find(";");
			text=text.substr(pos+1);

			int edge_id = atoi(text.c_str());
			pos = text.find(";");
			text=text.substr(pos+1);

			int frequency = atoi(text.c_str());

			if (frequency > 0) edges[edge_id].used = true;

		   if (id != current_id)
		   {
				current_id = id;
				std::vector<int> new_edge;
				new_edge.push_back(edge_id);
				line new_line(new_edge,frequency, id);
				new_line.nodes.insert(edges[edge_id].from);
				new_line.nodes.insert(edges[edge_id].to);
				lineconcept.push_back(new_line);
			}
			else
			{
				lineconcept[id-1].edges.push_back(edge_id);
				lineconcept[id-1].nodes.insert(edges[edge_id].from);
				lineconcept[id-1].nodes.insert(edges[edge_id].to);
			}
       }
	else
		comments.push_back(text);
    }
    linestream.close();

	std::ifstream loadstream(loadfile.c_str());
    while (!loadstream.eof())
    {
		getline(loadstream,text);
		if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
			int id = atoi(text.c_str());
			size_t pos = text.find(";");
			text=text.substr(pos+1);
			pos = text.find(";");
			text=text.substr(pos+1);

			int min = atoi(text.c_str());
			pos = text.find(";");
			text=text.substr(pos+1);

			int max = atoi(text.c_str());

			loads.push_back(std::pair<int,int>(min,max));
		}
	}
	loadstream.close();

	std::ifstream odstream(odfile.c_str());
    while (!odstream.eof())
    {
		getline(odstream,text);
		if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
			int from = atoi(text.c_str());
			size_t pos = text.find(";");
			text=text.substr(pos+1);

			int to = atoi(text.c_str());
			pos = text.find(";");
			text=text.substr(pos+1);

			int num = atoi(text.c_str());

			od_matrix[pair<int,int>(from,to)] = num;
		}
	}
	odstream.close();

	std::ifstream poolstream(poolfile.c_str());
    while (!poolstream.eof())
    {
		getline(poolstream,text);
		if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
			int id = atoi(text.c_str());
			size_t pos = text.find(";");
			text=text.substr(pos+1);

			pos = text.find(";");
			text=text.substr(pos+1);

			double cost = atof(text.c_str());
			costs[id] = cost;
		}
	}
	poolstream.close();

	//create shortest paths object
	//find number of nodes

	int nr_of_nodes=0;
	for (unsigned int i=0; i<lineconcept.size(); ++i)
	{
		int comp = *(lineconcept[i].nodes.rbegin());
		if (comp > nr_of_nodes)
			nr_of_nodes = comp;
	}

        cout<<"Number of nodes: "<<nr_of_nodes<<"\n";

        path.set_directed(directed);

	for (int i=0; i<nr_of_nodes; ++i)
		path.add_node(i);

	for (std::map<int,edgedata>::iterator it = edges.begin(); it!=edges.end(); ++it)
		if (it->second.used)
			path.add_edge(it->first-1, it->second.from-1, it->second.to-1, it->second.duration);

 	cout<<"Computing shortest paths..."<<"\n";
	path.compute_pairwise();
 	cout<<"Done."<<"\n";
}

void evaluation::evaluate()
{
	number_lines_result = number_lines();
	max_frequency_result = max_frequency();

	feasible_result = feasible();

	nr_all_sp_direct_travellers_result = nr_all_sp_direct_travellers();
	pool_costs_result = pool_costs();
	if(lc_extended_statistic){
		nr_of_changes_sp_result = nr_of_changes_sp();
	}
	sum_squared_edge_frequencies_result = sum_squared_edge_frequencies();

	//average_frequency_result = average_frequency();
	average_line_length_result = average_line_length();
	//variance_line_length_result = variance_line_length();
	//nr_possible_direct_travellers_result = nr_possible_direct_travellers();
	//nr_sp_direct_travellers_result = nr_sp_direct_travellers();
	//max_frequency_edge_result = max_frequency_edge();
	//max_frequency_edge_value_result = max_frequency_edge_value();
	//edge_freq_result = edge_frequency_distribution();
	//variance_frequency_result = variance_frequency();

// 	reachable(14);
// 	reachable_lc(14);
}

void evaluation::print_results()
{

	std::cout<<"Comments:\n";

	for (std::list<std::string>::iterator it = comments.begin(); it != comments.end(); ++it)
		std::cout<<*it<<"\n";

	std::cout<<"Feasibility: ";
	if (feasible_result) std::cout<<"TRUE\n"; else std::cout<<"FALSE\n";

	std::cout<<"Number of lines: "<<number_lines_result<<"\n";

	//std::cout<<"Average line frequency: "<<average_frequency_result<<"\n";

	std::cout<<"Maximum line frequency: "<<max_frequency_result<<"\n";

	//std::cout<<"Variance of line frequencies: "<<variance_frequency_result<<"\n";

	std::cout<<"Sum of squared edge frequencies: "<<sum_squared_edge_frequencies_result<<"\n";
}

void evaluation::print_results(std::string outputfile)
{
	std::ofstream out (outputfile.c_str());

	out<<"**Lineplanning Evaluation**\n\n";

	out<<"Comments:\n";

	for (std::list<std::string>::iterator it = comments.begin(); it != comments.end(); ++it)
		out<<*it<<"\n";

	out<<"Feasibility: ";
	if (feasible_result) out<<"TRUE\n"; else out<<"FALSE\n";

	out<<"Number of lines: "<<number_lines_result<<"\n";

	out<<"Line costs: "<<pool_costs_result<<"\n";

	out<<"Maximum line frequency: "<<max_frequency_result<<"\n";

	out<<"Number of direct travellers (on all shortest paths): "<<nr_all_sp_direct_travellers_result<<"\n";

	if(lc_extended_statistic)
		out<<"Number of changes * passengers on shortest paths: "<<nr_of_changes_sp_result<<"\n";

	out<<"Sum of squared edge frequencies: "<<sum_squared_edge_frequencies_result<<"\n";

	//out<<"Average line frequency: "<<average_frequency_result<<"\n";

	//out<<"Variance of line frequencies: "<<variance_frequency_result<<"\n";

	//out<<"Average line length: "<<average_line_length_result<<"\n";

	//out<<"Variance line length: "<<variance_line_length_result<<"\n";

	//out<<"Number of direct travellers (neglecting shortest paths): "<<nr_possible_direct_travellers_result<<"\n";

	//out<<"Number of direct travellers (on shortest paths): "<<nr_sp_direct_travellers_result<<"\n";

	out.close();
}

void evaluation::results_to_statistic()
{
    statistic::set_double_value("lc_cost", pool_costs_result);
    statistic::set_bool_value("lc_feasible", feasible_result);
    statistic::set_integer_value("lc_prop_directed_lines", number_lines_result); //former lc_lines
    statistic::set_integer_value("lc_prop_freq_max", max_frequency_result);//former lc_line_frequency_maximum
    statistic::set_integer_value("lc_obj_direct_travellers_sp", nr_all_sp_direct_travellers_result); // former lc_direct_travelers_all_shortest_paths
    if(lc_extended_statistic)
		statistic::set_integer_value("lc_number_changes_sp", nr_of_changes_sp_result);
    statistic::set_integer_value("lc_obj_game", sum_squared_edge_frequencies_result); // former lc_edge_frequency_squares_sum

    // From previous version of evaluation
    //statistic::set_integer_value("lc_edge_frequency_maximum", max_frequency_edge_value_result);
    //statistic::set_integer_value("lc_edge_frequency_maximum_id", max_frequency_edge_result);
    //statistic::set_string_value("lc_edge_frequency_distribution", edge_freq_result); Distributions are coming from somewhere else
    //statistic::set_double_value("lc_line_frequency_variance", variance_frequency_result);
    //statistic::set_double_value("lc_line_length_average", average_line_length_result);
    //statistic::set_double_value("lc_line_length_variance", variance_line_length_result);
    //statistic::set_integer_value("lc_direct_travelers", nr_possible_direct_travellers_result);
    //statistic::set_integer_value("lc_direct_travelers_shortest_paths", nr_sp_direct_travellers_result);
    //statistic::set_double_value("lc_line_frequency_average", average_frequency_result);
}

int evaluation::number_lines()
{
        int counter = 0;
        for (int i=0; i<lineconcept.size(); ++i)
            if (lineconcept[i].frequency > 0)
				++counter;
	if(directed)
		return counter;
	else
		return counter*2;
}

double evaluation::average_frequency()
{
	unsigned long sum=0;
	unsigned int number = lineconcept.size();
	for (unsigned int i=0; i<number; ++i)
		sum+=lineconcept[i].frequency;
	if (number != 0)
		return (double) sum / number_lines();
	else
		return 0;
}

int evaluation::max_frequency()
{
	int max = 0;
	for (unsigned int i=0; i<lineconcept.size(); ++i)
		if (lineconcept[i].frequency > max)
			max = lineconcept[i].frequency;

	return max;
}

int evaluation::max_frequency_edge()
{
	std::vector<int> edgeloads(loads.size());
	for (unsigned int i=0; i<loads.size(); ++i)
		edgeloads[i] = 0;

	for (unsigned int i=0; i<lineconcept.size(); ++i)
		for(unsigned int j=0; j<lineconcept[i].edges.size(); ++j)
			edgeloads[lineconcept[i].edges[j]-1] += lineconcept[i].frequency;

	int max = 0;
	for (unsigned int i=0; i<edgeloads.size(); ++i)
		if (edgeloads[i] > edgeloads[max])
			max = i;

	return max+1;
}

std::string evaluation::edge_frequency_distribution()
{
	std::vector<int> edgeloads(loads.size());
        for (unsigned int i=0; i<loads.size(); ++i)
                edgeloads[i] = 0;

        for (unsigned int i=0; i<lineconcept.size(); ++i)
                for(unsigned int j=0; j<lineconcept[i].edges.size(); ++j)
                        edgeloads[lineconcept[i].edges[j]-1] += lineconcept[i].frequency;

        int max = 0;
        for (unsigned int i=0; i<edgeloads.size(); ++i)
                if (edgeloads[i] > edgeloads[max])
                        max = i;

	std::vector<int> frequencies(edgeloads[max]+1);
	for (unsigned int i=0; i<frequencies.size(); ++i)
				frequencies[i] = 0;
	for (unsigned int i=0; i<edgeloads.size(); ++i)
		frequencies[edgeloads[i]]++;
	std::stringstream result;
	for (unsigned int i=0; i<=edgeloads[max]; ++i)
		result << frequencies[i] << ",";

    return result.str();
}

int evaluation::max_frequency_edge_value()
{
	std::vector<int> edgeloads(loads.size());
	for (unsigned int i=0; i<loads.size(); ++i)
		edgeloads[i] = 0;

	for (unsigned int i=0; i<lineconcept.size(); ++i)
		for(unsigned int j=0; j<lineconcept[i].edges.size(); ++j)
			edgeloads[lineconcept[i].edges[j]-1] += lineconcept[i].frequency;

	int max = 0;
	for (unsigned int i=0; i<edgeloads.size(); ++i)
		if (edgeloads[i] > edgeloads[max])
			max = i;

	return edgeloads[max];
}

int evaluation::sum_squared_edge_frequencies()
{
	std::vector<int> edgeloads(loads.size());
	for (unsigned int i=0; i<loads.size(); ++i)
		edgeloads[i] = 0;

	for (unsigned int i=0; i<lineconcept.size(); ++i)
		for(unsigned int j=0; j<lineconcept[i].edges.size(); ++j)
			edgeloads[lineconcept[i].edges[j]-1] += lineconcept[i].frequency;

	int sum_of_squares = 0;
	for (unsigned int i=0; i<edgeloads.size(); ++i)
		sum_of_squares += edgeloads[i] * edgeloads[i];

	return sum_of_squares;
}

double evaluation::variance_frequency()
{
	double sum=0;
	unsigned int number = lineconcept.size();
	double average = average_frequency();

	for (unsigned int i=0; i<number; ++i)
	{
	       if (lineconcept[i].frequency > 0)
	       {
		  double temp = lineconcept[i].frequency - average;
		  temp*=temp;
		  sum+=temp;
	       }
	}
	if (number != 0)
		return (double) sum / number_lines();
	else
		return 0;
}

bool evaluation::feasible()
{
	bool result = true;
	std::vector<int> edgeloads(loads.size());
	for (unsigned int i=0; i<loads.size(); ++i)
		edgeloads[i] = 0;

	for (unsigned int i=0; i<lineconcept.size(); ++i)
		for(unsigned int j=0; j<lineconcept[i].edges.size(); ++j)
			edgeloads[lineconcept[i].edges[j]-1] += lineconcept[i].frequency;

	for (unsigned int i=0; i<edgeloads.size(); ++i)
		if (edgeloads[i] < loads[i].first || edgeloads[i] > loads[i].second)
		{
			result = false;
			cout<<"Lineconcept infeasible: Edge "<<i+1<<" has load "<<edgeloads[i]<<", ";
			cout<<"Min: "<<loads[i].first<<", Max: "<<loads[i].second<<"\n";
		}

	return result;
}

int evaluation::nr_possible_direct_travellers()
{
	int result = 0;
	OD::iterator it;
	bool direct;
	int linenumber = 0;
	for (it = od_matrix.begin(); it != od_matrix.end(); ++it)
	{
		direct = false;
		for (unsigned int i=0; i<lineconcept.size() && (direct == false); ++i)
		{
			if (lineconcept[i].frequency > 0 && lineconcept[i].nodes.find(it->first.first) != lineconcept[i].nodes.end() && lineconcept[i].nodes.find(it->first.second) != lineconcept[i].nodes.end())

				direct = true;
				linenumber  = i;
		}
		if (direct)
		{
			result += it->second;
			//cout<<"Line: "<<lineconcept[linenumber].id<<" Direct traveller: "<<it->second<<": "<<it->first.first<<" - "<<it->first.second<<" Result: "<<result<<"\n";
		}
	}
	//for(set<int>::iterator i= lineconcept[4].nodes.begin(); i!=lineconcept[4].nodes.end(); i++)cout<<"Node: "<<*i<<"\n";
	return result;
}

double evaluation::average_line_length()
{
	double result = 0;
	for (unsigned int i=0; i<lineconcept.size(); ++i)
	   if (lineconcept[i].frequency > 0)
		for (int j = 0; j < lineconcept[i].edges.size(); ++j)
			result += edges[lineconcept[i].edges[j]].length;

	result /= number_lines();
	return result;
}

double evaluation::variance_line_length()
{
	double result =0;
	double average = average_line_length_result;

	for (unsigned int i=0; i<lineconcept.size(); ++i)
	{
		double line_length = 0;
		for (int j = 0; j < lineconcept[i].edges.size(); ++j)
		  if(lineconcept[i].frequency > 0)
			line_length += edges[lineconcept[i].edges[j]].length;
		line_length -= average;
		line_length *= line_length;
		result += line_length;
	}

	if (lineconcept.size() != 0)
		return result / number_lines();
	else
		return 0;
}

int evaluation::nr_of_changes_sp()
{
	int result = 0;
	for (OD::iterator it = od_matrix.begin(); it != od_matrix.end(); ++it)
	{
		std::vector<unsigned int> path_edges = path.get_path_edges(it->first.first-1, it->first.second-1);
		//std::cout<<"From  "<<it->first.first<<" to "<<it->first.second<<": ";
		//for (int k=0; k<path_edges.size(); ++k)
		//  std::cout<<(path_edges[k]+1)<<" ";
		//std::cout<<"\n";

		int start = 0;
		//bug corrected. Due to possible earilier bug (no clue where this came from) path_edges where double as high as supposed.
		//Hence the following if-else is redundand.

		int end;
		if (directed)
			end = path_edges.size();
		else
			end = path_edges.size();

		int nr_changes = -1;
		while (start != end)
		{
		  std::pair<int,int> longest = std::make_pair<int,int>(-1,0);
		  //search for the line concept that covers most edges
		  for (unsigned int i=0; i<lineconcept.size(); ++i)
		  {
		      if (lineconcept[i].frequency == 0)
		          continue;

		      //search for beginning edge
		      bool found = false;
		      for (unsigned int j=0; j<lineconcept[i].edges.size() && found==false; ++j)
		      {
		          if (lineconcept[i].edges[j] == path_edges[start]+1)
		          {
		              found = true;
		          }
		      }

		      //if edge is available, search for continuing edges
		      if (found)
		      {
		          int start_temp = start;
		          do
		          {
                              found = false;
		              for (unsigned int j=0; j<lineconcept[i].edges.size() && found==false; ++j)
		              {
		                  if (lineconcept[i].edges[j] == path_edges[start_temp]+1)
		                  {
		                      found = true;
		                      ++start_temp;
		                  }
		              }
		          }while(found && start_temp < end);

		          if (start_temp-start > longest.second)
		          {
		              longest.first = i;
		              longest.second = start_temp - start;
		          }
		      }
		  }

		  //line concept determined. update position.
		  if (longest.second == 0)
		  {
		      std::cout<<"WARNING: Edge "<<path_edges[start]+1<<" is used on shortest paths, but not covered by line concept. Parameter lc_number_changes_sp will be set to -1.\n";
		      return -1;
		  }

		  if (directed)
		  {
			std::cout<<"WARNING: Directed PTNs are not supported for lc_number_changes_sp. Parameter will be set to -1.\n";
			return -1;
		  }
		  else
			assert(longest.second != 0);
		  //cout<<"On path "<< it->first.first <<" - "<< it->first.second <<" driving with line "<<longest.first+1<<" along edges "<<path_edges[start]+1<<" to "<<path_edges[start+longest.second]+1<<"\n";
		  start += longest.second;
		  ++nr_changes;
		}
		//cout<<"Nr of changes on path "<< it->first.first <<" - "<< it->first.second <<" is "<<nr_changes<<" with passengers "<< it->second<<"\n";
		if(nr_changes>0)
			result += it->second * nr_changes;
	}

	return result;
}

int evaluation::nr_sp_direct_travellers()
{
	int result = 0;
        int linenumber = 0;
ofstream myfile;
  myfile.open ("example.txt");
	for (OD::iterator it = od_matrix.begin(); it != od_matrix.end(); ++it)
	{
		vector<unsigned int> path_edges = path.get_path_edges(it->first.first-1, it->first.second-1);
		bool direct = false;
		for (unsigned int i=0; i<lineconcept.size() && direct == false; ++i)
		{
		      if(lineconcept[i].frequency == 0)
		          continue;

			bool all_edges_found = true;

				for(unsigned int j = 0; j<path_edges.size(); ++j){
					bool found_edge = false;
					for (unsigned int k=0; k<lineconcept[i].edges.size() && found_edge == false; ++k)
					{

						if (lineconcept[i].edges[k] == path_edges[j]+1)
							found_edge = true;
					}

					if (found_edge == false)
						all_edges_found = false;
				}
				if (all_edges_found == true){
				direct = true;

			}


		}
		if (direct == true)
		{
			result += it->second;
		}
	}
	return result;
}


int evaluation::nr_all_sp_direct_travellers()
{
	int result = 0;
        int linenumber = 0;
	// Loop over all od-pairs
	int count = 1;
	for (OD::iterator it = od_matrix.begin(); it != od_matrix.end(); ++it)
	{	
		if (count % 1000 == 1) {
			std::cout << "OD pair " << count << " of " << od_matrix.size() << std::endl;
		}
		count += 1;
		if (it->second == 0) {
			continue;
		}
		// Get weight of path
		double weight = path.get_distance(it->first.first-1, it->first.second-1);
		bool direct = false;
		// Loop over all lines in lineconcept
		for (unsigned int i=0; i<lineconcept.size() && direct == false; ++i)
		{
		      if(lineconcept[i].frequency == 0)
		          continue;

			bool all_edges_found = true;

					bool found_start_edge = false;
					bool found_end_edge = false;
					bool found_undir_start_edge = false;
					bool found_undir_end_edge = false;
					double compareweight = 0;
					double undirectedcompareweight = 0;
					if(directed){
					// Loop over all edges in one line
					for (unsigned int k=0; k<lineconcept[i].edges.size() && found_end_edge == false; ++k)
					{

						if (edges[lineconcept[i].edges[k]].from == it->first.first)
							found_start_edge = true;
						if(edges[lineconcept[i].edges[k]].to  == it->first.second)
							found_end_edge = true;
						if(found_start_edge == true)
							compareweight += edges[lineconcept[i].edges[k]].duration;
					}
					} else {
					for (unsigned int k=0; k<lineconcept[i].edges.size() && (found_start_edge==false || found_end_edge == false); ++k)
					{
						// Check for undirected version if OD-start and end are on line in forward direction
						if((edges[lineconcept[i].edges[k]].from == it->first.first || edges[lineconcept[i].edges[k]].to == it->first.first) && (edges[lineconcept[i].edges[k]].to  == it->first.second || edges[lineconcept[i].edges[k]].from  == it->first.second)){
							found_start_edge = true;
							found_end_edge = true;
						}
						else if ((edges[lineconcept[i].edges[k]].from == it->first.first  || edges[lineconcept[i].edges[k]].to == it->first.first) && k < lineconcept[i].edges.size()-1 && edges[lineconcept[i].edges[k+1]].from != it->first.first && edges[lineconcept[i].edges[k+1]].to != it->first.first)
							found_start_edge = true;
						else if(edges[lineconcept[i].edges[k]].to  == it->first.second || edges[lineconcept[i].edges[k]].from  == it->first.second)
							found_end_edge = true;
						if(found_start_edge == true)
							compareweight += edges[lineconcept[i].edges[k]].duration;
					}
					for (unsigned int k=lineconcept[i].edges.size(); k>0 && (found_undir_start_edge==false || found_undir_end_edge == false); --k)
					{

							if((edges[lineconcept[i].edges[k]].from == it->first.first || edges[lineconcept[i].edges[k]].to == it->first.first) && (edges[lineconcept[i].edges[k]].to  == it->first.second || edges[lineconcept[i].edges[k]].from  == it->first.second)){
							found_undir_start_edge = true;
							found_undir_end_edge = true;
						}
						else if ((edges[lineconcept[i].edges[k-1]].from == it->first.first || edges[lineconcept[i].edges[k-1]].to == it->first.first) && k!=1 && edges[lineconcept[i].edges[k-2]].from != it->first.first && edges[lineconcept[i].edges[k-2]].to != it->first.first)
							found_undir_start_edge = true;
						else if(edges[lineconcept[i].edges[k-1]].to  == it->first.second || edges[lineconcept[i].edges[k-1]].from  == it->first.second)
							found_undir_end_edge = true;
						if(found_undir_start_edge == true)
							undirectedcompareweight += edges[lineconcept[i].edges[k-1]].duration;
	}
					}
					// If either start_edge or end_edge is not found or the respective weight does not equal the minimal weight, then this is not the line
					if ((found_start_edge  == false || found_end_edge == false || weight != compareweight) && (directed || found_undir_start_edge == false || found_undir_end_edge == false || weight != undirectedcompareweight))
						all_edges_found = false;
			// Only if upper conditions are fulfilled at least once there is a direct connection
			if (all_edges_found == true){
				direct = true;
			}
		}
		// If there is a direct connection with correct weight, add passengers
		if (direct == true)
		{
			result += it->second;
		}
	}
	return result;
}

double evaluation::pool_costs()
{
    double result = 0;
    for (int i=0; i<lineconcept.size(); ++i)
        result += lineconcept[i].frequency * costs[lineconcept[i].id];


	//cout<<"Costs: "<<result<<"\n";
    return result;
}

void evaluation::reachable(int from)
{
  set<int> reach;
  reach.insert(from);
  bool change = true;
  while(change)
  {
    change = false;
    for (map<int,edgedata>::iterator it = edges.begin(); it!=edges.end(); ++it)
    {
      if (reach.count(it->second.from) > 0 && reach.count(it->second.to) == 0)
      {
	reach.insert(it->second.to);
	change = true;
      }
    }

  }

  for (set<int>::iterator it = reach.begin(); it!=reach.end(); ++it)
    cout<<*it<<"\n";

}

void evaluation::reachable_lc(int from)
{
  set<int> reach;
  reach.insert(from);
  bool change = true;

  	std::vector<int> edgeloads(loads.size());
	for (unsigned int i=0; i<loads.size(); ++i)
		edgeloads[i] = 0;

	for (unsigned int i=0; i<lineconcept.size(); ++i)
		for(unsigned int j=0; j<lineconcept[i].edges.size(); ++j)
			edgeloads[lineconcept[i].edges[j]-1] += lineconcept[i].frequency;

  while(change)
  {
    change = false;
    for (map<int,edgedata>::iterator it = edges.begin(); it!=edges.end(); ++it)
    {
      if (edgeloads[it->second.nr - 1] > 0 && reach.count(it->second.from) > 0 && reach.count(it->second.to) == 0)
      {
	reach.insert(it->second.to);
	change = true;
      }
    }

  }

  for (set<int>::iterator it = reach.begin(); it!=reach.end(); ++it)
    cout<<*it<<"\n";

}

edge_length_type evaluation::process_edge_length_type(std::string config_value) {
	edge_length_type result;
	if (config_value == "AVERAGE_DRIVING_TIME") {
		result = AVERAGE_DRIVING_TIME;
	}
	else if (config_value == "MINIMAL_DRIVING_TIME") {
		result = MINIMAL_DRIVING_TIME;
	}
	else if (config_value == "MAXIMAL_DRIVING_TIME") {
		result = MAXIMAL_DRIVING_TIME;
	}
	else if (config_value == "EDGE_LENGTH") {
		result = EDGE_LENGTH;
	}
	else {
		std::cerr << "Do not recognize edge length type " << config_value << std::endl;
		throw std::invalid_argument("Invalid ean_model_weight_drive");
	}
	return result;
}

double evaluation::compute_edge_length(edge_length_type length_type, double length, double min_travel_time, double max_travel_time) {
	double result;
	switch(length_type) {
		case AVERAGE_DRIVING_TIME: {
			result = (min_travel_time + max_travel_time) / 2.;
		} break;
		case MINIMAL_DRIVING_TIME: {
			result = min_travel_time;
		} break;
		case MAXIMAL_DRIVING_TIME: {
			result = max_travel_time;
		} break;
		case EDGE_LENGTH: {
			result = length;
		} break;
		default:{
			throw std::invalid_argument("Invalid edge_length_type");
		}
	}
	return result;
}
