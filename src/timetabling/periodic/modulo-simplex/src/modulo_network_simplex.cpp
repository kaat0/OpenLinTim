/*
 * modulo_network_simplex.cpp
 */
//stl stuff
#include <vector>
#include <map>
#include <iostream>
#include <algorithm>
#include <fstream>
#include <string>
#include <limits>
#include <ctime>
#include <cstdlib>
#include <cmath>

//personal header
#include "modulo_network_simplex.h"

//boost stuff
#include "boost/graph/dijkstra_shortest_paths.hpp"
#include "boost/graph/kruskal_min_spanning_tree.hpp"
#include "boost/graph/breadth_first_search.hpp"
#include "boost/graph/visitors.hpp"
#include "boost/property_map/property_map.hpp"
#include "boost/graph/graph_utility.hpp"
#include <boost/pending/disjoint_sets.hpp>
#include <boost/graph/incremental_components.hpp>
#include <boost/graph/connected_components.hpp>
#include "boost/bimap/bimap.hpp"

//goblin stuff
#include "goblin.h"
#include "networkSimplex.h"

//defines unfeasible pivot changes
#define coeff_not_feasible std::numeric_limits<int>::max()

#include <termios.h>

//config
#include "../../../../essentials/config/config.h"

using namespace boost;
using namespace modulosimplex;

static goblinController *CT_NW_Simplex;

int getch() {
    static int ch = -1, fd = 0;
    struct termios neu, alt;
    fd = fileno(stdin);
    tcgetattr(fd, &alt);
    neu = alt;
    neu.c_lflag &= ~(ICANON|ECHO);
    tcsetattr(fd, TCSANOW, &neu);
	ch = getchar();
    tcsetattr(fd, TCSANOW, &alt);
    return ch;
}

int kbhit(void) {
        struct termios term, oterm;
        int fd = 0;
        int c = 0;
        tcgetattr(fd, &oterm);
        memcpy(&term, &oterm, sizeof(term));
        term.c_lflag = term.c_lflag & (!ICANON);
        term.c_cc[VMIN] = 0;
        term.c_cc[VTIME] = 1;
        tcsetattr(fd, TCSANOW, &term);
		c = getchar();
        tcsetattr(fd, TCSANOW, &oterm);
        if (c != -1)
        ungetc(c, stdin);
        return ((c != -1) ? 1 : 0);
}

//constructor
//pointers are set to 0 and parameters are initialised with standard values
simplex::simplex(int seed)
{
	g = 0;
	spanning_tree = 0;

	nr_of_nodes = 0;
	nr_of_edges = 0;

	search = TAB_FULL;
	search_impro = FIXED;
	dynamic_pivot_factor = 0.1;
	local_search = SINGLE_NODE_CUT;
	min_pivot_improvement = 0;
	min_cut_improvement = 0;
	percentage_improvement = 80;
	sa_temperature = 10000;
	sa_cooling_factor = 0.80;
	loc_number_of_nodes = 10;
	loc_number_of_tries = 3;

	ts_memory_length = 10;
	ts_max_iterations = 100;
	best_objective = 0;

	improved_last_time = true;
	if (seed ==0)
	   srand(time(NULL));
	else
	   srand(seed);

	sa_hybrid_active = false;
	loc_current_tries = 0;
	limit = 0;

 //	verbose = OUT_DEFAULT;
 //	verbose = OUT_DEBUG;

	non_solver = SOLVER_GOBLIN;

	nr_col_ops = 0;
	nr_piv_ops = 0;
	nr_calc_ops = 0;
	timelimit = 0;
// 	system("pwd");
	current_robustness = 0;
	allow_keylistener = config::get_bool_value("allow_keylistener");
	min_robustness = config::get_double_value("tim_nws_min_robustness");


	cout<<"Min robustness is "<<min_robustness<<"\n";
	dyn_rob_penalty = 1e8;

	goblin_initial = false;
	best_feasible_obj = std::numeric_limits<double>::max();
}

//destructor
//deletes the graphs
simplex::~simplex()
{
	if (g != 0)
		delete g;
	if (spanning_tree != 0)
		delete spanning_tree;
}

//initialises the algorithm with given feasible timetable
void simplex::init(std::string activities_file, std::string events_file, int given_period, std::string timetable)
{
	property_map<Graph,edge_maxtime_t>::type max = get(edge_maxtime, *g);
	property_map<Graph,edge_mintime_t>::type min = get(edge_mintime, *g);

	if (verbose == OUT_DEBUG)
	{
		std::cout<<"Verbose mode is ON.\n"<<std::flush;
		std::cout<<"\n*** Initializing algorithm. ***\n\n"<<std::flush;
		std::cout<<"Activities file: "<<activities_file<<".\n"<<std::flush;
		std::cout<<"Events file: "<<events_file<<".\n"<<std::flush;
	}

	std::cout<<"Using "<<activities_file<<" as activities and "<<events_file<<" as events.\n"<<std::flush;
	std::cout<<"Using "<<timetable<<" as timetable.\n"<<std::flush;

	//set_unexpected (myunexpected);
    std::string line;
	period = given_period;

	//read in the events
    std::ifstream load (events_file.c_str());
    while (!load.eof())
    {
       getline(load,line);

       if(line!=""&&(line.c_str())[0]>=48&&(line.c_str())[0]<=57)
       {
    	   nr_of_nodes=atoi(line.c_str());
       }
    }
    load.close();

	//creategraph
    if (g!=0)
    	delete g;
	g = new Graph(nr_of_nodes);
	edge_types.reserve(nr_of_nodes);

    typedef adjacency_list <vecS, vecS, undirectedS> CGraph;
//     typedef edge_list <vecS, vecS, undirectedS> CGraph;
    CGraph G(nr_of_nodes);

    vector<pair<int,int> > edgevec;
    vector<pair<int,int> > minvec;

	//read in the activities
	std::string headway("eadway");
    std::ifstream edgefile (activities_file.c_str());
    while (!edgefile.eof())
    {
       getline(edgefile,line);
       if(line!=""&&(line.c_str())[0]>=48&&(line.c_str())[0]<=57)
       {
    	   size_t pos = line.find(";");
    	   line=line.substr(pos+1);
     	   pos = line.find(";");

    	   std::string name(line,0,pos);
    	   line=line.substr(pos+1);

    	   int tail, head, min, max, weight;

    	   tail=atoi(line.c_str());
    	   pos = line.find(";");
    	   line=line.substr(pos+1);

    	   head=atoi(line.c_str());
    	   pos = line.find(";");
    	   line=line.substr(pos+1);

    	   min=atoi(line.c_str());
    	   pos = line.find(";");
    	   line=line.substr(pos+1);

    	   max=atoi(line.c_str());
// 	   max*=1.1;
    	   pos = line.find(";");
    	   line=line.substr(pos+1);

    	   weight=atoi(line.c_str());

	if (use_headways)
	{
    	   if (max-min >= period)
    		  max = min +period - 1;

		if (name.find(headway) != std::string::npos)
		{
			//uint k = rand() % 100;
			//if (k<100)
			//{
			while (min<0)
			{
				min+=period;
				max+=period;
			}

			//weight = 1;

			add_edge(tail-1, head-1, Edge_props(weight,Mintime(min,Maxtime(max))), *g);
			add_edge(tail-1,head-1,G);
			edge_types.push_back(name);
			edgevec.push_back(make_pair(tail-1,head-1));
			minvec.push_back(make_pair(min,max));
			//}
		}
		else
		{
		  //TEMP
		  if (name.find("change") != std::string::npos && max-min == period - 1 && weight == 0)
		  {

		  }
		  else
		  {
// 		    	if (weight == 0)
// 			  weight = 1;
// 			else
// 			  weight = 10;

// 			cout<<"Weight is "<<weight<<"\n";

			add_edge(tail-1, head-1, Edge_props(weight,Mintime(min,Maxtime(max))), *g);
			add_edge(tail-1,head-1,G);
			edge_types.push_back(name);
			edgevec.push_back(make_pair(tail-1,head-1));
			minvec.push_back(make_pair(min,max));
		  }
		}
	}
	else
	{
		std::cout<<"Ignoring headways not possible in connection with constraint propagation!\n"<<std::flush;
		std::cout<<"Aborting.\n"<<std::flush;
		exit(0);
	}
       }
    }
    edgefile.close();

	//connect the graph components
	std::vector<int> component(num_vertices(G));
	int num_comp = connected_components(G, &component[0]);
	if (num_comp > 1)
	{
		std::cout<<"WARNING: "<<num_comp<<" connected components found. Connecting them.\n"<<std::flush;
//		abort();

		std::vector<int> representative(num_comp);
		for (uint i=0; i<nr_of_nodes; ++i)
			representative[component[i]] = i;
		for (uint i=0; i<representative.size()-1; ++i)
		{
 			add_edge(representative[i], representative[i+1], Edge_props(1,Mintime(0,Maxtime(0))), *g);
 			add_edge(representative[i], representative[i+1],G);
 			edge_types.push_back(std::string("help_edge"));
//  			cout<<num_edges(*g)<<";\"help_edge\";"<<representative[i]+1<<";"<<representative[i+1]+1<<";0;0;1\n";
//			 cout<<"Size of connected component "<<i<<" is "<<representative[i].size()<<".\n";
		}

		//hack hack hack hack hack
/*		map<int,int> newnodes;
		int nodecounter = 1;
		for (set<int>::iterator it = representative[1].begin(); it!=representative[1].end(); ++it)
		{
		  newnodes[*it] = nodecounter;
		  cout<<nodecounter<<";\"x\";0;0;0\n";
		  ++nodecounter;
		}

		cout<<"\n";

			std::ifstream timefile (timetable.c_str());
			string line;
	while (!timefile.eof())
	{
		getline(timefile,line);
		if(line!=""&&(line.c_str())[0]>=48&&(line.c_str())[0]<=57)
		{
			int node_nr=atoi(line.c_str());
			size_t pos = line.find(";");
			line=line.substr(pos+1);

			int node_pot=atoi(line.c_str());
// 			node_potentials[node_nr-1] = node_pot;

		  if (representative[1].count(node_nr-1) != 0)
		    cout<<newnodes[node_nr-1]<<";"<<node_pot<<"\n";

			//std::cout<<"Node "<<node_nr<<" has "<<node_pot<<"\n";
		}
	}
	timefile.close();

		cout<<"\n";
		nodecounter=1;
property_map<Graph,edge_weight_t>::type w = get(edge_weight, *g);
		typedef graph_traits<Graph>::edge_iterator eite;
		for (std::pair<eite,eite> p = edges(*g); p.first != p.second; ++p.first)
		{
// 			graph_edges.insert( boost::bimap<int,Edge>::value_type(i, *p.first ) );
// 			++i;
			if (representative[1].count(source(*p.first,*g)) != 0 && representative[1].count(target(*p.first,*g)) != 0)
			{
			  cout<<nodecounter<<";\"x\";"<<newnodes[source(*p.first,*g)]<<";"<<newnodes[target(*p.first,*g)]<<";"<<min[*p.first]<<";"<<max[*p.first]<<";"<<w[*p.first]<<"\n";
			  ++nodecounter;
			}
		}

		*/

	}

//     cout<<"NUM EDGES: "<<nr_of_edges<<"\nNUM VERTICES: "<<nr_of_nodes<<"\n";

	nr_of_edges = num_edges(*g);
	graph_edges_left.resize(nr_of_edges);


	//create the bimaps
	int i = 0;
	typedef graph_traits<Graph>::vertex_iterator vite;
	for (std::pair<vite,vite> p = vertices(*g); p.first != p.second; ++p.first)
	{
		graph_vertices.insert( boost::bimap<int,Vertex>::value_type(i, *p.first ) );
		++i;
	}

	i = 0;
	typedef graph_traits<Graph>::edge_iterator eite;
	for (std::pair<eite,eite> p = edges(*g); p.first != p.second; ++p.first)
	{
		graph_edges.insert( boost::bimap<int,Edge>::value_type(i, *p.first ) );
// 	cout<<"Integer: "<<i<<", Edge: "<<(int)*p.first<<"\n";
// 	vector<Edge> testvec(10);
		graph_edges_left[i] = *p.first;
		++i;

	}

	//allocate memory
	//allocating on demand would slow down the program a lot!
	m_rhs.resize(nr_of_edges);
	m_slack.resize(nr_of_edges);
	in_tree_edges.reserve(nr_of_edges);
	out_tree_edges.reserve(nr_of_edges);
	columns.resize(nr_of_edges);
	modulo_param.resize(nr_of_edges);
	span.reserve(nr_of_nodes);
	if (countpercentages)
	{
		distribution.resize(nr_of_edges+1);
		for (uint i=0; i<nr_of_edges+1; ++i)
			distribution[i] = 0;
	}

	property_map<Graph,edge_weight_t>::type w = get(edge_weight, *g);

	//initialize feasible spanning tree structure
	std::cout<<"Finding modulo parameters.\n"<<std::flush;

	best_feasible.resize(nr_of_nodes);

	std::vector<int> node_potentials(nr_of_nodes);
	int node_nr = 0;
	int node_pot = 0;
	std::ifstream timefile (timetable.c_str());
	while (!timefile.eof())
	{
		getline(timefile,line);
		if(line!=""&&(line.c_str())[0]>=48&&(line.c_str())[0]<=57)
		{
			node_nr=atoi(line.c_str());
			size_t pos = line.find(";");
			line=line.substr(pos+1);

			node_pot=atoi(line.c_str());
			node_potentials[node_nr-1] = node_pot;
			//std::cout<<"Node "<<node_nr<<" has "<<node_pot<<"\n";
		}
	}
	timefile.close();

	double target_val_loop, source_val_loop;
	int max_loop, min_loop;
	Edge e_loop;
	int loop_percentage = 0;


	if (verbose == OUT_DEBUG)
	    cout<<"Searched 0%"<<std::flush;
	int numedges = num_edges(*g);

	if (verbose == OUT_DEBUG)
	    cout<<"Searched 0%"<<std::flush;
	for (uint i=0; i<numedges; i++)
	{
	  		if (verbose == OUT_DEBUG)
			{
			  if ((i+1.0)*100/numedges > loop_percentage+1)
			  {
			    ++loop_percentage;
			    std::cout<<"\rSearched "<<loop_percentage<<"%   "<<std::flush;
			  }
			}
// 		e_loop = graph_edges.left.at(i);
		modulo_param[i] = 0;

		if (edge_types[i] == "help_edge")
			continue;

		min_loop = minvec[i].first;
		max_loop = minvec[i].second;

//  		modulo_param[i] += min_loop/period;
//  		max_loop -= (min_loop/period)*period;
//  		min_loop %= period;

		target_val_loop = node_potentials[edgevec[i].second];
		source_val_loop = node_potentials[edgevec[i].first];
// 		std::cout<<target_val_loop<<" - "<<source_val_loop<<"\n"<<std::flush;
		while (target_val_loop - source_val_loop > max_loop)
		{
			target_val_loop-=period;
			--modulo_param[i];
// 			cout<<"Changed+."<<std::flush;
		}
		while (target_val_loop - source_val_loop < min_loop)
		{
			target_val_loop+=period;
			++modulo_param[i];
// 			cout<<"Changed-."<<std::flush;
		}

		if (target_val_loop - source_val_loop > max_loop)
		{
// 		 cout<<"GIVEN TIMETABLE IS NOT FEASIBLE: "<< graph_vertices.right.at(source(e_loop,*g))+1<<" - "<<graph_vertices.right.at(target(e_loop,*g))+1<<"\n";
 		 cout<<"GIVEN TIMETABLE IS NOT FEASIBLE.\n"<<source_val_loop<<" - "<<target_val_loop<<": ("<<minvec[i].first<<","<<minvec[i].second<<")\n";
		 exit(1);
		}
	}

	cout<<"Done.\nCalculating starting structure.\n"<<flush;
	/*
	//test: create an aperiodic problem instance
	std::ofstream apac("Activities.giv");
	std::ofstream apev("Events.giv");
	for (uint i=0; i<num_edges(*g); ++i)
	{
		Edge e = graph_edges.left.at(i);
		apac<<i+1<<"; "<<edge_types[i]<<"; "<<graph_vertices.right.at(source(e,*g))+1<<"; "<<graph_vertices.right.at(target(e,*g))+1<<"; "<<min[e]-modulo_param[i]*period<<"; "<<max[e] - modulo_param[i]*period<<"; "<<w[e]<<"\n";
	}
	for (uint i=0; i<nr_of_nodes; ++i)
	{
		apev<<i<<"; TYP; 0; 0; 0\n";
	}
	apac.close();
	apev.close();
	std::cout<<"WROTE TO APERIODIC TIMETABLE FILE\n";
	*/

	if (config::get_bool_value("tim_nws_use_robustness"))
	{
	  cout<<"NEW: Reading robustness.\n"<<flush;
	  ifstream rob( (config::get_string_value("default_activity_buffer_file")).c_str() );
	  int robline = 1;
	  while (!rob.eof())
	  {
		  getline(rob,line);
		  if(line!=""&&(line.c_str())[0]>=48&&(line.c_str())[0]<=57)
		  {
			  size_t pos = line.find(";");
			  line=line.substr(pos+1);

			  robustness.push_back(atof(line.c_str()));
  // 			cout<<robline<<" "<<robustness[robline-1]<<" "<<line<<"\n";
  // 			++robline;
			  //cout<<robustness.back()<<"\n";
		  }
	  }
	  rob.close();
	  cout<<"Done.\n"<<flush;
	}
	else
	{
	  cout<<"Robustness not used.\n"<<flush;
	  for (int i=0; i<numedges; ++i)
	    robustness.push_back(0);
	}

	for (int i=0; i<num_comp; ++i)
		robustness.push_back(0);


	if (search != TAB_NOTHING)
	{
	  non_periodic();


	  //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//	  write_result(config::get_string_value("default_timetable_periodic_file"));

//	  exit(0);

	  build();
	}



}

//calculate node potentials recursively
void simplex::set_time()
{
	//delete all already set times
	m_pi.clear();

	//set time of a single vertex to be zero
	Vertex start =  source(span.front(),*g);
	m_pi[graph_vertices.right.at(start)]=0;
	//std::cout<<"Time at "<<start<<" is 0.\n"<<std::flush;

	//begin recursion
	set_time(start);
	if (verbose == OUT_DEBUG) std::cout<<"Setting time for node "<<graph_vertices.right.at(start)<<" to 0.\n"<<std::flush;
}

void simplex::set_time(Vertex where)
{
	property_map<Graph,edge_mintime_t>::type min = get(edge_mintime, *g);
	property_map<Graph,edge_maxtime_t>::type max = get(edge_maxtime, *g);


	for (uint i=0; i<in_tree_edges.size(); ++i)
	{
// 		Edge e = graph_edges.left.at(in_tree_edges[i]);
		Edge e = graph_edges_left[in_tree_edges[i]];
		if (source(e,*g) == where)
		{
			if (m_pi.find(graph_vertices.right.at(target(e, *g))) == m_pi.end())
			{
				if (lower_tree_edges[i])
				{
					m_pi[graph_vertices.right.at(target(e,*g))] = m_pi[graph_vertices.right.at(where)] + min[e];
					//std::cout<<"Using "<<e<<"as lower out edge.\n";
				}
				else
				{
					m_pi[graph_vertices.right.at(target(e,*g))] = m_pi[graph_vertices.right.at(where)] + max[e];
					//std::cout<<"Using "<<e<<"as upper out edge.\n";
				}

				//std::cout<<"Time at "<<target(e,*g)<<" is "<<m_pi[graph_vertices.right.at(target(e,*g))]<<"\n";
				set_time(target(e,*g));
			}
		}

		if (target(e,*g) == where)
		{
			if (m_pi.find(graph_vertices.right.at(source(e, *g))) == m_pi.end())
			{
				if (lower_tree_edges[i])
				{
					m_pi[graph_vertices.right.at(source(e,*g))] = m_pi[graph_vertices.right.at(where)] - min[e];
					//std::cout<<"Using "<<e<<"as lower in edge.\n";
				}
				else
				{
					m_pi[graph_vertices.right.at(source(e,*g))] = m_pi[graph_vertices.right.at(where)] - max[e];
					//std::cout<<"Using "<<e<<"as upper in edge.\n";
				}
				//std::cout<<"Time at "<<source(e,*g)<<" is "<<m_pi[graph_vertices.right.at(source(e,*g))]<<"\n";
				set_time(source(e,*g));
			}
		}
	}



}

//search for a local cut
void simplex::improvable()
{

	graph_traits<Graph>::out_edge_iterator out_i, out_end;
	graph_traits<Graph>::in_edge_iterator in_i, in_end;

	property_map<Graph,edge_maxtime_t>::type max = get(edge_maxtime, *g);
	property_map<Graph,edge_mintime_t>::type min = get(edge_mintime, *g);
	property_map<Graph,vertex_index_t>::type index = get(vertex_index, *g);
	property_map<Graph,edge_weight_t>::type w = get(edge_weight, *g);

	bool found = false;
	cut.active = false;
	cut.directions.reserve(nr_of_edges);

	if (local_search == NO_CUT)
	  return;

	//search for single node cuts
	if (local_search == SINGLE_NODE_CUT)
	{
		Edge e;
		std::vector<int> omega_sum(period);
		std::pair<Vertex_Iterator,Vertex_Iterator> p = vertices(*g);
		while (p.first != p.second && !found)
		{
			Vertex v = *p.first;

			for (int delta = 0; delta<period; ++delta)
			{
				bool feasible = true;
				omega_sum[delta] = 0;
				for (tie(in_i, in_end) = in_edges(v, *g);
				in_i != in_end; ++in_i)
				{
					if (feasible)
					{
						e = *in_i;
						int time = m_slack[graph_edges.right.at(e)] + min[e];
						time += delta;
						if (time>max[e])
							time-=period;
						if (time <min[e])
						{
							feasible = false;
							omega_sum[delta]=0;
						}
						else
							omega_sum[delta]+=w[e]*(time - min[e] - m_slack[graph_edges.right.at(e)]);
					}
				}

				for (tie(out_i, out_end) = out_edges(v, *g);
				out_i != out_end; ++out_i)
				{
					if (feasible)
					{
						e = *out_i;
						int time = m_slack[graph_edges.right.at(e)] + min[e];
						time -= delta;
						if (time < min[e])
							time+=period;
						if (time > max[e])
						{
							feasible = false;
							omega_sum[delta] = 0;
						}
						else
							omega_sum[delta]+=w[e]*(time - min[e] - m_slack[graph_edges.right.at(e)]);
					}
				}
			}

			int minimum=0;
			for (int i = 1; i<period; i++)
				if (omega_sum[i]<omega_sum[minimum])
					minimum =i;

			if (omega_sum[minimum] < 0 && (min_cut_improvement==0 || (min_cut_improvement != 0 && min_cut_improvement*get_objective() <= -omega_sum[minimum])))
			{
				found = true;
				cut.where = graph_vertices.right.at(v);
				cut.active = true;
				cut.delta = minimum;
				for (uint i=0; i<nr_of_edges;++i)
					cut.directions[i]=0;
				for (tie(in_i, in_end) = in_edges(v, *g);
				in_i != in_end; ++in_i)
				{
					e = *in_i;
					cut.directions[graph_edges.right.at(e)] = -1;
				}

				for (tie(out_i, out_end) = out_edges(v, *g);
				out_i != out_end; ++out_i)
				{
					e = *out_i;
					cut.directions[graph_edges.right.at(e)] = 1;
				}

				if (verbose == OUT_DEBUG)
				{
					std::cout<<"Cut found at "<<cut.where<<" with delta "<<cut.delta<<".\n"<<std::flush;
					std::cout<<"Calculated change: "<<omega_sum[minimum]<<"\n";
				}

			}

			++p.first;
		}
	}//search for random node cuts
	else if (local_search == RANDOM_CUT)
	{
		if(loc_current_tries < loc_number_of_tries)
		{
		++loc_current_tries;
		Edge e;
		std::vector<int> omega_sum(period);

		//create a random sort of vertices
		std::vector<int> vertices_vector(nr_of_nodes);
		for (uint i=0; i<nr_of_nodes; ++i)
			vertices_vector[i] = i;

		for (uint i=0; i<nr_of_nodes; ++i)
		{
			uint k = rand() % (nr_of_nodes - i);
			uint helper = vertices_vector[k];
			vertices_vector[k] = vertices_vector[nr_of_nodes-1-i];
			vertices_vector[nr_of_nodes-1-i] = helper;
		}

		uint p_ite=0;
		while (p_ite < nr_of_nodes && !found)
		{
			Vertex v = graph_vertices.left.at(vertices_vector[p_ite]);;

			for (int delta = 0; delta<period; ++delta)
			{
				bool feasible = true;
				omega_sum[delta] = 0;
				bool modulochange = false;
				for (tie(in_i, in_end) = in_edges(v, *g);
				in_i != in_end; ++in_i)
				{
					if (feasible)
					{
						e = *in_i;
						int time = m_slack[graph_edges.right.at(e)] + min[e];
						time += delta;
						if (time>max[e])
						{
							time-=period;
							modulochange = true;
						}
						if (time <min[e])
						{
							feasible = false;
							omega_sum[delta]=0;
						}
						else
							omega_sum[delta]+=w[e]*(time - min[e] - m_slack[graph_edges.right.at(e)]);
					}
				}

				for (tie(out_i, out_end) = out_edges(v, *g);
				out_i != out_end; ++out_i)
				{
					if (feasible)
					{
						e = *out_i;
						int time = m_slack[graph_edges.right.at(e)] + min[e];
						time -= delta;
						if (time < min[e])
						{
							time+=period;
							modulochange = true;
						}
						if (time > max[e])
						{
							feasible = false;
							omega_sum[delta] = 0;
						}
						else
							omega_sum[delta]+=w[e]*(time - min[e] - m_slack[graph_edges.right.at(e)]);
					}
				}
				if (!modulochange)
					omega_sum[delta] = 0;
			}

			int minimum=0;
			for (int i = 1; i<period; i++)
				if (omega_sum[i]!=0)
					minimum =i;

			if (omega_sum[minimum] != 0)
			{
				found = true;
				cut.where = graph_vertices.right.at(v);
				cut.active = true;
				cut.delta = minimum;
				for (uint i=0; i<nr_of_edges;++i)
					cut.directions[i]=0;
				for (tie(in_i, in_end) = in_edges(v, *g);
				in_i != in_end; ++in_i)
				{
					e = *in_i;
					cut.directions[graph_edges.right.at(e)] = -1;
				}

				for (tie(out_i, out_end) = out_edges(v, *g);
				out_i != out_end; ++out_i)
				{
					e = *out_i;
					cut.directions[graph_edges.right.at(e)] = 1;
				}

				if (verbose == OUT_DEBUG)
				{
					std::cout<<"Cut found at "<<cut.where<<" with delta "<<cut.delta<<".\n"<<std::flush;
					std::cout<<"Calculated change: "<<omega_sum[minimum]<<"\n";
				}
			}

			++p_ite;
		}
		}
	}//search for waiting edge cuts
	else if (local_search == WAITING_CUT)
	{
		std::vector<int> omega_sum(period);
		std::string waitword("wait");
		uint waitedge = 0;
		Edge e;
		//std::cout<<"\n";
		while (waitedge < nr_of_edges && found == false)
		{
			if (edge_types[waitedge].find(waitword) == std::string::npos)
				++waitedge;
			else
			{
			//std::cout<<"\rWaitedge found: "<<waitedge<<"\n";
// 			Vertex s = source(graph_edges.left.at(waitedge),*g);
			Vertex s = source(graph_edges_left[waitedge],*g);
// 			Vertex t = target(graph_edges.left.at(waitedge),*g);
			Vertex t = target(graph_edges_left[waitedge],*g);
			for (int delta = 0; delta<period; ++delta)
			{
				bool feasible = true;
				omega_sum[delta] = 0;
				for (tie(in_i, in_end) = in_edges(s, *g);
				in_i != in_end; ++in_i)
				{
					if (feasible)
					{
						e = *in_i;
						int time = m_slack[graph_edges.right.at(e)] + min[e];
						time += delta;
						if (time>max[e])
							time-=period;
						if (time <min[e])
						{
							feasible = false;
							omega_sum[delta]=0;
						}
						else
							omega_sum[delta]+=w[e]*(time - min[e] - m_slack[graph_edges.right.at(e)]);
					}
				}

				for (tie(out_i, out_end) = out_edges(s, *g);
				out_i != out_end; ++out_i)
				{
					if (feasible && graph_edges.right.at(*out_i) != waitedge)
					{
						e = *out_i;
						int time = m_slack[graph_edges.right.at(e)] + min[e];
						time -= delta;
						if (time < min[e])
							time+=period;
						if (time > max[e])
						{
							feasible = false;
							omega_sum[delta] = 0;
						}
						else
							omega_sum[delta]+=w[e]*(time - min[e] - m_slack[graph_edges.right.at(e)]);
					}
				}

				for (tie(in_i, in_end) = in_edges(t, *g);
				in_i != in_end; ++in_i)
				{
					if (feasible && graph_edges.right.at(*in_i) != waitedge)
					{
						e = *in_i;
						int time = m_slack[graph_edges.right.at(e)] + min[e];
						time += delta;
						if (time>max[e])
							time-=period;
						if (time <min[e])
						{
							feasible = false;
							omega_sum[delta]=0;
						}
						else
							omega_sum[delta]+=w[e]*(time - min[e] - m_slack[graph_edges.right.at(e)]);
					}
				}

				for (tie(out_i, out_end) = out_edges(t, *g);
				out_i != out_end; ++out_i)
				{
					if (feasible && graph_edges.right.at(*out_i) != waitedge)
					{
						e = *out_i;
						int time = m_slack[graph_edges.right.at(e)] + min[e];
						time -= delta;
						if (time < min[e])
							time+=period;
						if (time > max[e])
						{
							feasible = false;
							omega_sum[delta] = 0;
						}
						else
							omega_sum[delta]+=w[e]*(time - min[e] - m_slack[graph_edges.right.at(e)]);
					}
				}
			}
			int minimum=0;
			for (int i = 1; i<period; i++)
				if (omega_sum[i]<omega_sum[minimum])
					minimum =i;

			if (omega_sum[minimum] < 0 && (min_cut_improvement==0 || (min_cut_improvement != 0 && min_cut_improvement*get_objective() <= -omega_sum[minimum])))
			{
				found = true;
				cut.where = 0;
				cut.active = true;
				cut.delta = minimum;
				for (uint i=0; i<nr_of_edges;++i)
					cut.directions[i]=0;
				for (tie(in_i, in_end) = in_edges(s, *g);
				in_i != in_end; ++in_i)
				{
					e = *in_i;
					cut.directions[graph_edges.right.at(e)] = -1;
				}

				for (tie(out_i, out_end) = out_edges(s, *g);
				out_i != out_end; ++out_i)
				{
					e = *out_i;
					cut.directions[graph_edges.right.at(e)] = 1;
				}

				for (tie(in_i, in_end) = in_edges(t, *g);
				in_i != in_end; ++in_i)
				{
					e = *in_i;
					cut.directions[graph_edges.right.at(e)] = -1;
				}

				for (tie(out_i, out_end) = out_edges(t, *g);
				out_i != out_end; ++out_i)
				{
					e = *out_i;
					cut.directions[graph_edges.right.at(e)] = 1;
				}

				cut.directions[waitedge] = 0;

				if (verbose == OUT_DEBUG)
				{
					std::cout<<"Cut found with delta "<<cut.delta<<".\n"<<std::flush;
					std::cout<<"Calculated change: "<<omega_sum[minimum]<<"\n";
				}
			}
			else
				++waitedge;
			}
		}
	}//search for connected cut
	else if (local_search == CONNECTED_CUT)
	{
		for (int n=0; n<loc_number_of_nodes && !found; ++n)
		{
			uint k = rand() % nr_of_nodes;

			if (verbose == OUT_DEBUG)
				std::cout<<"Using node "<<k<<".\n"<<std::flush;
			//choose a random node k in V
			//uint k = rand() % (nr_of_nodes);

			//set S:={k}
			std::set<uint> S;
			S.insert(k);

			std::cout<<"Vertex "<<k<<"/"<<nr_of_nodes<<"\n"<<std::flush;

			std::pair<std::set<Edge>,std::set<Edge> > cut_edges;
			do
			{
				if (verbose == OUT_DEBUG)
					std::cout<<"Set size: "<<S.size()<<".\n"<<std::flush;

				//std::cout<<"Cut partition size is "<<S.size()<<"\n"<<std::flush;
				//identify cut edges
				cut_edges.first.clear();
				cut_edges.second.clear();

				for (std::set<uint>::iterator it=S.begin(); it != S.end(); ++it)
				{
					for (tie(out_i, out_end) = out_edges(graph_vertices.left.at(*it) , *g); out_i != out_end; ++out_i)
						if (S.count(graph_vertices.right.at(target(*out_i,*g))) == 0)
							cut_edges.first.insert(*out_i);
					for (tie(in_i, in_end) = in_edges(graph_vertices.left.at(*it) , *g); in_i != in_end; ++in_i)
						if (S.count(graph_vertices.right.at(source(*in_i,*g))) == 0)
							cut_edges.second.insert(*in_i);
				}

// 				for (uint i=0; i<nr_of_edges; ++i)
// 				{
// 					Edge e = graph_edges.left.at(i);
// 					if (S.find(graph_vertices.right.at(source(e,*g))) == S.end()
// 					  && S.find(graph_vertices.right.at(target(e,*g))) != S.end() )
// 						cut_edges.first.insert(e);
// 					else if (S.find(graph_vertices.right.at(source(e,*g))) != S.end()
// 					  && S.find(graph_vertices.right.at(target(e,*g))) == S.end() )
// 						cut_edges.second.insert(e);
// 				}

				if (verbose == OUT_DEBUG)
					 std::cout<<"Cut size: "<<cut_edges.first.size() + cut_edges.second.size()<<".\n"<<std::flush;

				//check if this is a feasible + improving cut
				std::vector<int> omega_sum(period);
				for (int delta = 0; delta<period; ++delta)
				{
					bool feasible = true;
					omega_sum[delta] = 0;

					//incoming edges
					for (std::set<Edge>::iterator it=cut_edges.first.begin(); it!=cut_edges.first.end() && feasible; ++it)
					{
						int time = m_slack[graph_edges.right.at(*it)] + min[*it];
						time += delta;
						if (time > max[*it])
							time -= period;
						if (time < min[*it])
						{
							feasible = false;
							omega_sum[delta] = 0;
						}
						else
							omega_sum[delta] += w[*it]*(time - min[*it] - m_slack[graph_edges.right.at(*it)]);;
					}

					//outgoing edges
					if (feasible)
					{
						for (std::set<Edge>::iterator it=cut_edges.second.begin(); it!=cut_edges.second.end() && feasible; ++it)
						{
							int time = m_slack[graph_edges.right.at(*it)] + min[*it];
							time -= delta;
							if (time < min[*it])
								time += period;
							if (time > max[*it])
							{
								feasible = false;
								omega_sum[delta] = 0;
							}
							else
								omega_sum[delta] += w[*it]*(time - min[*it] - m_slack[graph_edges.right.at(*it)]);;
						}
					}
				}

				int minimum=0;
				for (int i = 1; i<period; i++)
					if (omega_sum[i]<omega_sum[minimum])
						minimum =i;

				//cut is improving
				if (omega_sum[minimum] < 0 && (min_cut_improvement==0 || (min_cut_improvement != 0 && min_cut_improvement*get_objective() <= -omega_sum[minimum])))
				{
					found = true;
					cut.where = 0;
					cut.active = true;
					cut.delta = minimum;
					for (uint i=0; i<nr_of_edges;++i)
						cut.directions[i]=0;

					for (std::set<Edge>::iterator it=cut_edges.first.begin(); it != cut_edges.first.end(); ++it)
					{
						cut.directions[graph_edges.right.at(*it)] = -1;
					}

					for (std::set<Edge>::iterator it=cut_edges.second.begin(); it != cut_edges.second.end(); ++it)
					{
						cut.directions[graph_edges.right.at(*it)] = 1;
					}

					if (verbose == OUT_DEBUG)
					{
						std::cout<<"Connected cut found with delta "<<cut.delta<<".\n"<<std::flush;
						std::cout<<"Calculated change: "<<omega_sum[minimum]<<"\n";
					}
				}//cut is not improving
				else
				{
					//sort cut edges according to u - l
					std::list<std::pair<int, Edge> > edgelist;
					for (std::set<Edge>::iterator it = cut_edges.first.begin(); it!= cut_edges.first.end(); ++it)
						edgelist.push_back(make_pair(max[*it] - min[*it],*it));
					for (std::set<Edge>::iterator it = cut_edges.second.begin(); it!= cut_edges.second.end(); ++it)
						edgelist.push_back(make_pair(max[*it] - min[*it],*it));
					edgelist.sort();

					//in this version: ignore if the cut is really connected. just let it grow.
					//find first edge that can be added
					Edge e = (edgelist.front()).second;

					//update S
					if ( cut_edges.first.find(e) != cut_edges.first.end() )
						S.insert(graph_vertices.right.at(target(e, *g)));
					else
						S.insert(graph_vertices.right.at(source(e, *g)));

				}

			}while(!found && S.size() < nr_of_nodes);
		}
	}
	else
	{
	  std::cout<<"Unknown local improvement procedure.\nAborting.";
	  exit(1);
	}

	if (verbose == OUT_DEBUG && !(cut.active))
		std::cout<<"No cut found.\n"<<std::flush;
}

//search for a fundamental cut
//returns true when found
bool simplex::pivot()
{
	/*
	std::cout<<"Current tree: \n";
	for (uint i=0; i<in_tree_edges.size(); i++)
		std::cout<<in_tree_edges[i]<<"\n";
	std::cout<<"As lower: \n";
	for (uint i=0; i<in_tree_edges.size(); i++)
			std::cout<<lower_tree_edges[i]<<"\n";
	 */

	if (search == TAB_NOTHING)
	  return false;

	int max_change = 0;
	bool impro = false;
	int current_objective = get_objective();

	int change_in_edge=0;
	int change_out_edge=0;
	bool as_lower = true;

	uint loop_percentage=0;
	int loop_counter=0;

	//keep tabu list up to date
	if (search == TAB_SIMPLE_TABU_SEARCH && (best_objective == 0 || current_objective < best_objective) )
	{
		ts_memory q_member;
		q_member.lower_checksum = 0;
		q_member.upper_checksum = 0;

		for (uint i=0; i<in_tree_edges.size(); ++i)
		{
			if (lower_tree_edges[i])
			{
				q_member.lower_tree.insert(in_tree_edges[i]);
				q_member.lower_checksum+= in_tree_edges[i];
			}
			else
			{
				q_member.upper_tree.insert(in_tree_edges[i]);
				q_member.upper_checksum+=in_tree_edges[i];
			}
		}

		if (best_objective != 0)
		{
			ts_memory_deque.push_back(q_member);

			if (ts_memory_deque.size() > (uint) ts_memory_length)
				ts_memory_deque.pop_front();
		}

		best_objective = current_objective;
		best_solution = q_member;

	}


	if (search != TAB_SIMULATED_ANNEALING && verbose == OUT_DEBUG)
		std::cout<<"Searched 0%"<<std::flush;
	//std::cout<<coeff.size()<<"\n"<<std::flush;

	//steepest descent
	if (search == TAB_FULL || (search == TAB_STEEPEST_SA_HYBRID && sa_hybrid_active == false))
	{
		nr_col_ops+=in_tree_edges.size();
		for (std::map<std::pair<int, int>,bool>::iterator it = coeff.begin(); it!=coeff.end(); ++it)
			for (int temp_lower_int = 0; temp_lower_int<=1; temp_lower_int++)
			{
				++loop_counter;
				if ((100*loop_counter)/(coeff.size()*2) != loop_percentage)
				{
					loop_percentage = (100*loop_counter)/(coeff.size()*2);
					if (verbose == OUT_DEBUG) std::cout<<"\rSearched "<<loop_percentage<<"%"<<std::flush;
				}

					int tempchange;
					bool temp_as_lower;
					if (temp_lower_int == 0)
						temp_as_lower = true;
					else
						temp_as_lower = false;

					tempchange = get_obj_change(((*it).first).first,((*it).first).second, temp_as_lower);
					if (tempchange < max_change)
					{
						max_change = tempchange;
						change_in_edge = ((*it).first).first;
						change_out_edge = ((*it).first).second;
						impro = true;
						as_lower = temp_as_lower;
					}
			}
	}//fastest
	else if (search == TAB_FASTEST)
	{
		//std::vector<tableau_column> in_tree_cols(in_tree_edges.size());

		std::vector<std::pair<int,int> > column_sizes(in_tree_edges.size());
		std::map<int, std::set<int> > columns;

		for (std::map<std::pair<int, int>,bool>::iterator it = coeff.begin(); it!=coeff.end(); ++it)
		{
			columns[((*it).first).second].insert(((*it).first).first);
		}

		for (uint i=0; i<in_tree_edges.size(); ++i)
			column_sizes[i] = std::make_pair(columns[in_tree_edges[i]].size(),in_tree_edges[i]);

		std::sort(column_sizes.begin(), column_sizes.end());
		int tempchange = 0;

		uint i=0;
		while(current_objective*min_pivot_improvement > -max_change && i<column_sizes.size())
		{
		  ++nr_col_ops;
			++loop_counter;
			if ((100*loop_counter)/in_tree_edges.size() != loop_percentage)
			{
				loop_percentage = (100*loop_counter)/(in_tree_edges.size());
				if (verbose == OUT_DEBUG) std::cout<<"\rSearched "<<loop_percentage<<"%"<<std::flush;
			}

			//std::cout<<column_sizes[i].first<<"\n";

			std::set<int>::iterator it;
			for (it = columns[column_sizes[i].second].begin(); it!=columns[column_sizes[i].second].end(); ++it)
			{
				tempchange = get_obj_change(*it,column_sizes[i].second,true);
				if (tempchange < max_change)
				{
					max_change = tempchange;
					change_in_edge = *it;
					change_out_edge = column_sizes[i].second;
					impro = true;
					as_lower = true;
				}

				tempchange = get_obj_change(*it,column_sizes[i].second,false);
				if (tempchange < max_change)
				{
					max_change = tempchange;
					change_in_edge = *it;
					change_out_edge = column_sizes[i].second;
					impro = true;
					as_lower = false;
				}
			}
			++i;
		}
	}//percentage
	else if (search == TAB_PERCENTAGE)
	{
		std::vector<std::pair<int,int> > column_sizes(in_tree_edges.size());
		std::map<int, std::set<int> > columns;

		for (std::map<std::pair<int, int>,bool>::iterator it = coeff.begin(); it!=coeff.end(); ++it)
		{
			columns[((*it).first).second].insert(((*it).first).first);
		}

		for (uint i=0; i<in_tree_edges.size(); ++i)
			column_sizes[i] = std::make_pair(columns[in_tree_edges[i]].size(),in_tree_edges[i]);

		std::sort(column_sizes.begin(), column_sizes.end());
		int tempchange = 0;

		uint i=0;
		while(loop_percentage < percentage_improvement)
		{
		  ++nr_col_ops;
			++loop_counter;
			if ((100*loop_counter)/in_tree_edges.size() != loop_percentage)
			{
				loop_percentage = (100*loop_counter)/(in_tree_edges.size());
				if (verbose == OUT_DEBUG) std::cout<<"\rSearched "<<loop_percentage<<"%"<<std::flush;
			}

			//std::cout<<column_sizes[i].first<<"\n";

			std::set<int>::iterator it;
			for (it = columns[column_sizes[i].second].begin(); it!=columns[column_sizes[i].second].end(); ++it)
			{
				tempchange = get_obj_change(*it,column_sizes[i].second,true);
				if (tempchange < max_change)
				{
					max_change = tempchange;
					change_in_edge = *it;
					change_out_edge = column_sizes[i].second;
					impro = true;
					as_lower = true;
				}

				tempchange = get_obj_change(*it,column_sizes[i].second,false);
				if (tempchange < max_change)
				{
					max_change = tempchange;
					change_in_edge = *it;
					change_out_edge = column_sizes[i].second;
					impro = true;
					as_lower = false;
				}
			}
			++i;
		}

		if (!impro)
		{
			while(i < in_tree_edges.size())
			{
			  ++nr_col_ops;
				++loop_counter;
				if ((100*loop_counter)/in_tree_edges.size() != loop_percentage)
				{
					loop_percentage = (100*loop_counter)/(in_tree_edges.size());
					if (verbose == OUT_DEBUG) std::cout<<"\rSearched "<<loop_percentage<<"%"<<std::flush;
				}

				//std::cout<<column_sizes[i].first<<"\n";

				std::set<int>::iterator it;
				for (it = columns[column_sizes[i].second].begin(); it!=columns[column_sizes[i].second].end(); ++it)
				{
					tempchange = get_obj_change(*it,column_sizes[i].second,true);
					if (tempchange < max_change)
					{
						max_change = tempchange;
						change_in_edge = *it;
						change_out_edge = column_sizes[i].second;
						impro = true;
						as_lower = true;
					}

					tempchange = get_obj_change(*it,column_sizes[i].second,false);
					if (tempchange < max_change)
					{
						max_change = tempchange;
						change_in_edge = *it;
						change_out_edge = column_sizes[i].second;
						impro = true;
						as_lower = false;set_time();
					}
				}
				++i;
			}
		}

	}//simulated annealing
	else if (search == TAB_SIMULATED_ANNEALING || (search == TAB_STEEPEST_SA_HYBRID && sa_hybrid_active == true))
	{
		if (countpercentages)
		{
			std::map<int, std::set<int> > columns;

			for (std::map<std::pair<int, int>,bool>::iterator it = coeff.begin(); it!=coeff.end(); ++it)
			{
				columns[((*it).first).second].insert(((*it).first).first);
			}

			for (uint i=0; i<in_tree_edges.size(); ++i)
				++distribution[columns[in_tree_edges[i]].size()];
		}



		if (verbose == OUT_DEBUG) std::cout<<"\nSearching.\n"<<std::flush;
		typedef std::pair< std::pair<int, int>, bool> tab_value;
		std::vector< tab_value > possibilities;
		possibilities.reserve(coeff.size());

		for (std::map<std::pair<int, int>,bool>::iterator it = coeff.begin(); it!=coeff.end(); ++it)
		{
			possibilities.push_back(std::make_pair(std::make_pair(((*it).first).first, ((*it).first).second),true));
			possibilities.push_back(std::make_pair(std::make_pair(((*it).first).first, ((*it).first).second),false));
		}

		int left_size = possibilities.size();

		while (!impro && left_size != 0)
		{
			int position = rand()%left_size;
			tab_value it = possibilities[position];
			int tempchange = get_obj_change((it.first).first, (it.first).second, it.second);
			if (tempchange == coeff_not_feasible)
			{
				//possibilities.erase(it);
				tab_value helper = it;
				possibilities[position] = possibilities[left_size-1];
				possibilities[left_size-1] = helper;
				--left_size;
			}
			else if (tempchange < 0)
			{
				max_change = tempchange;
				change_in_edge = (it.first).first;
				change_out_edge = (it.first).second;
				impro = true;
				as_lower = it.second;
				sa_temperature*=sa_cooling_factor;
				if (verbose == OUT_DEBUG) std::cout<<"Current temperature: "<<sa_temperature<<"\n"<<std::flush;
			}
			else
			{
				double p;
				tempchange == 0 ? p = 0 : p = exp(-tempchange/sa_temperature);
				//std::cout<<"p: "<<p<<"\n";
				double rval = double(rand())/RAND_MAX;
				//std::cout<<"rval: "<<rval<<"\n";
				if (rval < p)
				{
					max_change = tempchange;
					change_in_edge = (it.first).first;
					change_out_edge = (it.first).second;
					impro = true;
					as_lower = it.second;
					sa_temperature*=sa_cooling_factor;
					if (verbose == OUT_DEBUG) std::cout<<"Current temperature: "<<sa_temperature<<"\n"<<std::flush;
				}
				else
				{
					tab_value helper = it;
					possibilities[position] = possibilities[left_size-1];
					possibilities[left_size-1] = helper;
					--left_size;
				}
			}
		}

	}//tabu search
	else if (search == TAB_SIMPLE_TABU_SEARCH)
	{
		max_change = std::numeric_limits<int>::max();
		//std::cout<<"Max change: "<<max_change<<"\n";
		//construct sets similiar to ts_memory for comparison
		std::set<int> upper_tree_set;
		std::set<int> lower_tree_set;

		uint upper_tree_checksum = 0;
		uint lower_tree_checksum = 0;

		for (uint i=0; i<in_tree_edges.size(); ++i)
		{
			if (lower_tree_edges[i])
			{
				lower_tree_set.insert(in_tree_edges[i]);
				lower_tree_checksum+=in_tree_edges[i];
			}
			else
			{
				upper_tree_set.insert(in_tree_edges[i]);
				upper_tree_checksum+=in_tree_edges[i];
			}
		}

		for (std::map<std::pair<int, int>,bool>::iterator it = coeff.begin(); it!=coeff.end(); ++it)
			for (int temp_lower_int = 0; temp_lower_int<=1; temp_lower_int++)
			{
				++loop_counter;
				if ((100*loop_counter)/(coeff.size()*2) != loop_percentage)
				{
					loop_percentage = (100*loop_counter)/(coeff.size()*2);
					if (verbose == OUT_DEBUG) std::cout<<"\rSearched "<<loop_percentage<<"%"<<std::flush;
				}

					int tempchange;
					bool temp_as_lower;
					if (temp_lower_int == 0)
						temp_as_lower = true;
					else
						temp_as_lower = false;

					tempchange = get_obj_change(((*it).first).first,((*it).first).second, temp_as_lower);
					//std::cout<<"Tempchange "<<tempchange<<"\n";
					if (tempchange != coeff_not_feasible && tempchange != 0 && tempchange < max_change)
					{
						//check if change is forbidden
						bool forbidden = false;
						std::deque<ts_memory>::iterator qit;

						for (qit = ts_memory_deque.begin(); qit!=ts_memory_deque.end(); ++qit)
						{
							if (!forbidden)
							{
								bool was_lower;
								std::set<int>::iterator pos = lower_tree_set.find(((*it).first).second);
								if (pos != lower_tree_set.end())
								{
									was_lower = true;
									lower_tree_set.erase(pos);
									lower_tree_checksum-= *pos;
								}
								else
								{
									pos = upper_tree_set.find(((*it).first).second);
									upper_tree_set.erase(pos);
									upper_tree_checksum-= *pos;
									was_lower = false;
								}

								if (temp_as_lower)
								{
									lower_tree_set.insert(((*it).first).first);
									lower_tree_checksum+= ((*it).first).first;
								}
								else
								{
									upper_tree_set.insert(((*it).first).first);
									upper_tree_checksum+= ((*it).first).first;
								}


								if ((*qit).lower_checksum == lower_tree_checksum && (*qit).upper_checksum == upper_tree_checksum && (*qit).lower_tree == lower_tree_set && (*qit).upper_tree == upper_tree_set)
									forbidden = true;

								if (temp_as_lower)
								{
									lower_tree_set.erase(lower_tree_set.find(((*it).first).first));
									lower_tree_checksum-= ((*it).first).first;
								}
								else
								{
									upper_tree_set.erase(upper_tree_set.find(((*it).first).first));
									upper_tree_checksum-= ((*it).first).first;
								}

								if (was_lower)
								{
									lower_tree_set.insert(((*it).first).second);
									lower_tree_checksum+= ((*it).first).second;
								}
								else
								{
									upper_tree_set.insert(((*it).first).second);
									upper_tree_checksum+= ((*it).first).second;
								}

							}
						}

						if (!forbidden)
						{
							max_change = tempchange;
							change_in_edge = ((*it).first).first;
							change_out_edge = ((*it).first).second;
							impro = true;
							as_lower = temp_as_lower;
						}

					}
			}
	}

	//searching is done. evaluating results

	if (search != TAB_SIMULATED_ANNEALING && verbose == OUT_DEBUG)
		std::cout<<"\n";

	if (impro && (min_pivot_improvement == 0 || (min_pivot_improvement != 0 && min_pivot_improvement*current_objective <= -max_change)))
	{//fundamental cut found
		if (verbose == OUT_DEBUG)
		{
			if (as_lower)
				std::cout<<"Pivoting "<<change_in_edge<<" <-> "<<change_out_edge<<" as lower.\n"<<std::flush;
			else
				std::cout<<"Pivoting "<<change_in_edge<<" <-> "<<change_out_edge<<" as upper.\n"<<std::flush;
			std::cout<<"Old objective value: "<<current_objective<<".\n"<<std::flush;
			std::cout<<"New objective value: "<<current_objective+max_change<<".\n"<<std::flush;
		}
		else
		{
			//if (as_lower)
			//	std::cout<<change_in_edge<<" <-> "<<change_out_edge<<" as lower.\n"<<std::flush;
			//else
			//	std::cout<<change_in_edge<<" <-> "<<change_out_edge<<" as upper.\n"<<std::flush;
			//std::cout<<current_objective+max_change<<"\n"<<std::flush;
		}

		//keep tabu list up to date
		if (search == TAB_SIMPLE_TABU_SEARCH)
		{
			//make this one forbidden
			ts_memory q_member;
			q_member.lower_checksum = 0;
			q_member.upper_checksum = 0;

			for (uint i=0; i<in_tree_edges.size(); ++i)
			{
				if (lower_tree_edges[i])
				{
					q_member.lower_tree.insert(in_tree_edges[i]);
					q_member.lower_checksum+= in_tree_edges[i];
				}
				else
				{
					q_member.upper_tree.insert(in_tree_edges[i]);
					q_member.upper_checksum+= in_tree_edges[i];
				}
			}

			std::set<int>::iterator it;
			it = q_member.lower_tree.find(change_out_edge);
			if (it!=q_member.lower_tree.end())
			{
				q_member.lower_tree.erase(it);
				q_member.lower_checksum-= change_out_edge;
			}
			else
			{
				q_member.upper_tree.erase(q_member.upper_tree.find(change_out_edge));
				q_member.upper_checksum-=change_out_edge;
			}

			if (as_lower)
			{
				q_member.lower_tree.insert(change_in_edge);
				q_member.lower_checksum+= change_in_edge;
			}
			else
			{
				q_member.upper_tree.insert(change_in_edge);
				q_member.upper_checksum+=change_in_edge;
			}


			ts_memory_deque.push_back(q_member);

			if (ts_memory_deque.size() > (uint) ts_memory_length)
				ts_memory_deque.pop_front();

			if (best_objective > current_objective+max_change)
			{
				best_objective = current_objective+max_change;
				best_solution = q_member;
			}
		}


		swap_edges(change_in_edge, change_out_edge, as_lower);
		improved_last_time = true;

		if (presentation || allow_keylistener) getchar();

		return true;
	}
	else
	{//no fundamental cut found
		if (verbose == OUT_DEBUG)
			std::cout<<"No improving pivot found.\n"<<std::flush;

		if (search == TAB_SIMPLE_TABU_SEARCH)
		{
			ts_recreate_best();
		}
		else if (search_impro == DYNAMIC && min_pivot_improvement != 0)
		{
			if (verbose == OUT_DEBUG) std::cout<<"Adjusting minimum pivot improvement to "<<min_pivot_improvement*dynamic_pivot_factor<<".\n";
			min_pivot_improvement*=dynamic_pivot_factor;
			if (improved_last_time)
			{
				improved_last_time = false;
				return pivot();
			}
		}
	}

	if (presentation || allow_keylistener) getchar();
	return false;

}

//use found local cut
void simplex::transform()
{
	//find the current modulo parameters
	find_modulo();

	property_map<Graph,edge_maxtime_t>::type max = get(edge_maxtime, *g);
	property_map<Graph,edge_mintime_t>::type min = get(edge_mintime, *g);

	//change slack and modulo parameters
	int time;
	for (uint i = 0; i<nr_of_edges; i++)
	{
		switch (cut.directions[i])
		{
		case 0: break;
		case -1:
// 			time = m_slack[i] + min[graph_edges.left.at(i)] + cut.delta;
			time = m_slack[i] + min[graph_edges_left[i]] + cut.delta;
// 			if(time > max[graph_edges.left.at(i)])
			if(time > max[graph_edges_left[i]])
			{
				time-=period;
				--modulo_param[i];
				if (verbose == OUT_DEBUG)
					std::cout<<"Changing modulo parameter of "<<i<<" by -1.\n"<<std::flush;
// 				m_slack[i] = time - min[graph_edges.left.at(i)];
				m_slack[i] = time - min[graph_edges_left[i]];
			}
			break;
		case 1:
// 			time = m_slack[i] + min[graph_edges.left.at(i)]- cut.delta;
			time = m_slack[i] + min[graph_edges_left[i]]- cut.delta;
// 			if(time<min[graph_edges.left.at(i)])
			if(time<min[graph_edges_left[i]])
			{
				time+=period;
				++modulo_param[i];
				if (verbose == OUT_DEBUG)
					std::cout<<"Changing modulo parameter of "<<i<<" by 1.\n"<<std::flush;
// 				m_slack[i] = time - min[graph_edges.left.at(i)];
				m_slack[i] = time - min[graph_edges_left[i]];
			}
			break;

		default: throw std::exception();
		}
	}
	if (verbose == OUT_DEBUG) std::cout<<"Transformation succeeded.\n"<<std::flush;

}

//solve the dual problem
void simplex::non_periodic()
{
	property_map<Graph,edge_maxtime_t>::type max = get(edge_maxtime, *g);
	property_map<Graph,edge_mintime_t>::type min = get(edge_mintime, *g);
	property_map<Graph,edge_weight_t>::type w = get(edge_weight, *g);

	if (non_solver == SOLVER_GOBLIN)
	{
		//create goblin solver
		if (verbose == OUT_DEBUG)
			std::cout<<"Creating Goblin Controller.\n"<<std::flush;
		CT_NW_Simplex = new goblinController();
	    CT_NW_Simplex -> traceLevel = 0;
	    CT_NW_Simplex -> methMCF = abstractMixedGraph::MCF_BF_SIMPLEX;

	    //CT->logEventHandler = &myLogEventHandler;

	    if (verbose == OUT_DEBUG)
		std::cout<<"Creating dual problem instance: GOBLIN.\n"<<std::flush;

		//create goblin graph
		sparseDiGraph *goblin_graph = new sparseDiGraph((TNode)0, *CT_NW_Simplex);
		sparseRepresentation* goblin_rep = static_cast<sparseRepresentation*> (goblin_graph->Representation());

	    graph_traits<Graph>::out_edge_iterator out_i, out_end;
	    graph_traits<Graph>::in_edge_iterator in_i, in_end;

	    int count_edges=0;
		int test_demand=0;

// 	    ofstream mcf("mcf-netherlands.net");
// 	    mcf<<"c GOGO Netherlands!\n";
// 	    mcf<<"c nodes, links\nc\n";
// 	    mcf<<"p min "<<nr_of_nodes<<" "<<2*nr_of_edges<<"\n";
// 	    mcf<<"c\nc Suppy and demand\n";

		int numvertices = num_vertices(*g);
	    for (uint i=0; i<numvertices; i++)
	    {
			goblin_graph->InsertNode();
			int demand=0;

if (!goblin_initial && current_robustness >= min_robustness - 0.0001)
{
			for (tie(out_i, out_end) = out_edges(graph_vertices.left.at(i) , *g);
							out_i != out_end; ++out_i)
 				demand-=w[*out_i];

			for (tie(in_i, in_end) = in_edges(graph_vertices.left.at(i) , *g);
									in_i != in_end; ++in_i)
 				demand+=w[*in_i];
}
else
{
			for (tie(out_i, out_end) = out_edges(graph_vertices.left.at(i) , *g);
							out_i != out_end; ++out_i)
					demand+=(int)(1e8*robustness[graph_edges.right.at(*out_i)]);


			for (tie(in_i, in_end) = in_edges(graph_vertices.left.at(i) , *g);
									in_i != in_end; ++in_i)
					demand-=(int)(1e8*robustness[graph_edges.right.at(*in_i)]);
}


//cout<<demand<<"\n";

			goblin_rep->SetDemand(i,demand);

// 			mcf<<"n "<<i+1<<" "<<demand<<"\n";

			test_demand += demand;
	    }

		goblin_initial = false;

		//if (verbose == OUT_DEBUG)
			//std::cout<<"TEST DEMAND: "<<test_demand<<"\n"<<flush;


// 		mcf<<"c\nc Arcs: from, to, min, max, cost\n";

		int numedges = nr_of_edges;
	    for (uint i=0; i<numedges; ++i)
	    {
// 		Edge e = graph_edges.left.at(i);
		Edge e = graph_edges_left[i];
		int source_v = graph_vertices.right.at(source(e,*g));
		int target_v = graph_vertices.right.at(target(e,*g));

		goblin_graph->InsertArc(source_v, target_v, InfCap, -(min[e]-modulo_param[graph_edges.right.at(e)]*period),0);
		goblin_graph->InsertArc(target_v, source_v, InfCap, max[e]-modulo_param[graph_edges.right.at(e)]*period,0);

// 		mcf<<"a "<<graph_vertices.right.at(source(e,*g))+1<<" "<<graph_vertices.right.at(target(e,*g))+1<<" 0 -1 "<<-(min[e]-modulo_param[graph_edges.right.at(e)]*period)<<"\n";
// 		mcf<<"a "<<graph_vertices.right.at(target(e,*g))+1<<" "<<graph_vertices.right.at(source(e,*g))+1<<" 0 -1 "<<max[e]-modulo_param[graph_edges.right.at(e)]*period<<"\n";
	    }

// 	    mcf<<"c\nc End.";
// 	    mcf.close();

		//start goblin solver
	    if (verbose == OUT_DEBUG) std::cout<<"Invoking goblin solver.\n"<<std::flush;
	    double return_val = goblin_graph->MinCostBFlow();
	    if (verbose == OUT_DEBUG) std::cout<<"Objective value: "<<return_val<<"\n"<<std::flush;

	    if (verbose == OUT_DEBUG) std::cout<<"Organizing edges..."<<std::flush;

	    in_tree_edges.clear();
	    out_tree_edges.clear();
	    lower_tree_edges.clear();

	    //record the spanning tree as a graph
	    if (spanning_tree != 0)
		delete spanning_tree;
	    spanning_tree = new Tree(nr_of_nodes);
		spanning_set.clear();

	    count_edges = 0;
	    int count_span=0;

	    // ofstream out("new-timetable.tim");

	    for (uint i=0; i<numvertices; i++)
	    {
		int pre = goblin_graph->Pred(i);
		pre/=2;

	// 	cout<<pre<<"\n";

		// out<<i+1<<";"<<-goblin_graph->Pi(i)<<"\n"<<flush;

		if (pre <= numedges*2-1)
		{
// 			Edge e = graph_edges.left.at(pre/2);
			Edge e = graph_edges_left[pre/2];
			span[count_span]=e;
			spanning_set.insert(pre/2);
	//     		std::cout<<"Spanning tree edge: "<<graph_edges.left.at(pre/2);
			++count_span;
			in_tree_edges.push_back(graph_edges.right.at(e));
			if (pre%2 ==1)
			{
				lower_tree_edges.push_back(false);
				//std::cout<<"Edge "<<graph_edges.left.at(pre/2)<<" as upper.\n";
			}
			else
			{
				lower_tree_edges.push_back(true);
				//std::cout<<"Edge "<<graph_edges.left.at(pre/2)<<" as lower.\n";
			}
		}
	    }

	    // out.close();

		//is there something wrong?
	    if (count_span !=  numvertices-1)
	    {
		std::cout<<"Number of edges: "<<count_span<<"\n";
		std::cout<<"Number of nodes: "<<nr_of_nodes<<"\n";
		//HACK!
	// 	abort();
		// int count_adds = 0;
		// for (int i=0; i<nr_of_edges; ++i)
		// {
		// if (edge_types[i].find("help_edge") != std::string::npos)
		// {
		  // ++count_adds;
		  // lower_tree_edges.push_back(true);
		  // span[count_span] =graph_edges.left.at(i);
		  // ++count_span;
		  // spanning_set.insert(i);
		  // in_tree_edges.push_back(i);
		// }
		// }
		// cout<<"ADDED "<<count_adds<<" help edges to ST.\n";

		// if (count_span != (int)nr_of_nodes-1)
		  abort();
	    }
		else
		// cout<<"Tree found!\n";

	    for (int i=0; i<count_span; i++)
	    {
		//if (verbose == OUT_DEBUG)
		//	std::cout<<"Adding to tree: "<<source(span[i],*g)<<" <-> "<<target(span[i],*g)<<"\n"<<std::flush;
		add_edge(source(span[i],*g), target(span[i],*g), *spanning_tree);
	    }

	    delete CT_NW_Simplex;

	    // cout<<"Added and deleted.\n"<<flush;

		//sort if in or out
		typedef graph_traits<Graph>::edge_iterator eite;
		for (std::pair<eite,eite> p = edges(*g); p.first != p.second; ++p.first)
		{
			bool in = false;
			for (uint i = 0; i<count_span; ++i)
				if (span[i] == *p.first )
					in = true;

			if (!in)
			{
			// cout<<*p.first<<"\n";
				out_tree_edges.push_back(graph_edges.right.at(*p.first));
				// if (verbose == OUT_DEBUG) std::cout<<"Out tree edge found: "<<graph_edges.right.at(*p.first)<<"\n"<<std::flush;
			}
		}
		if (verbose == OUT_DEBUG) std::cout<<"done.\n"<<std::flush;

		if (presentation || allow_keylistener)
		{
			std::cout<<"Spanning tree:\n";
			for (uint i=0; i<in_tree_edges.size(); ++i)
				std::cout<<in_tree_edges[i]<<"\n";
			getchar();
		}
	}
	else if (non_solver == SOLVER_MCF)
	{

	  if (verbose == OUT_DEBUG)
		std::cout<<"Creating dual problem instance: MCF.\n"<<std::flush;

	    graph_traits<Graph>::out_edge_iterator out_i, out_end;
	    graph_traits<Graph>::in_edge_iterator in_i, in_end;

	    int count_edges=0;
	    int test_demand=0;

	    ofstream mcf("mcf-temp.net");
	    mcf<<"c GOGO Netherlands!\n";
	    mcf<<"c nodes, links\nc\n";
	    mcf<<"p min "<<nr_of_nodes<<" "<<2*nr_of_edges<<"\n";
	    mcf<<"c\nc Suppy and demand\n";

	    for (uint i=0; i<nr_of_nodes; i++)
	    {
			int demand=0;
			for (tie(out_i, out_end) = out_edges(graph_vertices.left.at(i) , *g);
							out_i != out_end; ++out_i)
				demand-=w[*out_i];
			for (tie(in_i, in_end) = in_edges(graph_vertices.left.at(i) , *g);
									in_i != in_end; ++in_i)
				demand+=w[*in_i];

			mcf<<"n "<<i+1<<" "<<demand<<"\n";

			test_demand += demand;
	    }

		if (verbose == OUT_DEBUG)
			std::cout<<"TEST DEMAND: "<<test_demand<<"\n";


		mcf<<"c\nc Arcs: from, to, min, max, cost\n";

	    for (uint i=0; i<nr_of_edges; i++)
	    {
// 		Edge e = graph_edges.left.at(i);
		Edge e = graph_edges_left[i];
		mcf<<"a "<<graph_vertices.right.at(source(e,*g))+1<<" "<<graph_vertices.right.at(target(e,*g))+1<<" 0 999999 "<<-(min[e]-modulo_param[graph_edges.right.at(e)]*period)<<"\n";
		mcf<<"a "<<graph_vertices.right.at(target(e,*g))+1<<" "<<graph_vertices.right.at(source(e,*g))+1<<" 0 999999 "<<max[e]-modulo_param[graph_edges.right.at(e)]*period<<"\n";
	    }

	    mcf<<"c\nc End.";
	    mcf.close();

	    if (verbose == OUT_DEBUG) std::cout<<"Invoking MCF solver.\n"<<std::flush;
	    int mcf_return = system("../../src/timetabling/networksimplex/thirdparty/mcf/bin/mcflight mcf-temp.net");


	}
	else
	{
		cout<<"Unknown nonperiodic solver.\n";
		exit(1);
	}
}

//main algorithm
void simplex::solve()
{
        config::from_file("basis/Config.cnf", false);

	int limit_counter = 1;
	time_t rawtime;
	struct tm * timeinfo;
	time ( &rawtime );
	timeinfo = localtime ( &rawtime );
	std::cout<<asctime(timeinfo)<<"\n";

	bool sa_hybrid_return = false;

	//int global_count =1;

	//begin without local cut
	cut.active = false;
	string cuttype;
	switch (local_search)
	{
		case SINGLE_NODE_CUT: cuttype = "single node cut"; break;
		case RANDOM_CUT: cuttype = "random node cut"; break;
		case WAITING_CUT: cuttype = "waiting edge cut"; break;
		case CONNECTED_CUT: cuttype = "connected cut"; break;
		default: cuttype = "unknown cut"; break;
	}

	save_best_feasible();

	clock_t begin = clock();
	do
	{
		//if local cut is found, apply it
		if (cut.active)
		{
			++limit_counter;

			if (verbose == OUT_DEBUG) std::cout<<"\n*** Cut found, applying it. ***\n\n"<<std::flush;
			transform();
			if (presentation || allow_keylistener) getchar();

			if (search != TAB_NOTHING)
			{
			  if (verbose == OUT_DEBUG) std::cout<<"\n*** Solving non-periodic flow. ***\n\n"<<std::flush;
			  non_periodic();

			  if (verbose == OUT_DEBUG) std::cout<<"\n*** Building simplex tableau. ***\n\n"<<std::flush;
			  build();
			  if (presentation || allow_keylistener) getchar();

			  if (current_robustness >= min_robustness - 0.0001)
				std::cout<<limit_counter<<";"<<get_objective()<<";"<<1.0* (clock() - begin) / CLOCKS_PER_SEC<<";"<<nr_col_ops<<";"<<nr_piv_ops<<";"<<nr_calc_ops<<";"<<current_robustness<<";"<<cuttype<<";FEAS\n"<<std::flush;
			else
				std::cout<<limit_counter<<";"<<get_objective()<<";"<<1.0* (clock() - begin) / CLOCKS_PER_SEC<<";"<<nr_col_ops<<";"<<nr_piv_ops<<";"<<nr_calc_ops<<";"<<current_robustness<<";"<<cuttype<<";INFEAS\n"<<std::flush;

				if (get_objective() < best_feasible_obj && current_robustness >= min_robustness - 0.0001)
				{
				//cout<<"*\n"<<flush;
					best_feasible_obj = get_objective();
					save_best_feasible();
				}
			}
		}
		else if(sa_hybrid_return && !sa_hybrid_active)
		{
			sa_hybrid_return = false;
			sa_hybrid_active = true;
		}

		//pivot
		if (verbose == OUT_DEBUG) std::cout<<"\n*** Pivoting. ***\n\n"<<std::flush;
		int pivot_count = 1;
		if (verbose != OUT_DEBUG && limit_counter == 1)
			std::cout<<limit_counter<<";"<<get_objective()<<";"<<1.0* (clock() - begin) / CLOCKS_PER_SEC<<";"<<nr_col_ops<<";"<<nr_piv_ops<<";"<<nr_calc_ops<<";"<<current_robustness<<";input solution\n"<<std::flush;

		while(pivot() && (search != TAB_SIMPLE_TABU_SEARCH || pivot_count < ts_max_iterations) && (!countpercentages || pivot_count<100) && (timelimit > 1.0* (clock() - begin) / CLOCKS_PER_SEC || timelimit == 0) )
		{
			++limit_counter;

			if (verbose == OUT_DEBUG) {
			std::cout<<"*** "<<pivot_count<<" Pivot. ***\n"<<std::flush;
			}
			else
			{
			if (current_robustness >= min_robustness - 0.0001)
				std::cout<<limit_counter<<";"<<get_objective()<<";"<<1.0* (clock() - begin) / CLOCKS_PER_SEC<<";"<<nr_col_ops<<";"<<nr_piv_ops<<";"<<nr_calc_ops<<";"<<current_robustness<<";pivot;FEAS\n"<<std::flush;
			else
				std::cout<<limit_counter<<";"<<get_objective()<<";"<<1.0* (clock() - begin) / CLOCKS_PER_SEC<<";"<<nr_col_ops<<";"<<nr_piv_ops<<";"<<nr_calc_ops<<";"<<current_robustness<<";pivot;INFFEAS\n"<<std::flush;
			}

			++pivot_count;
			if (get_objective() < best_feasible_obj && current_robustness >= min_robustness - 0.0001)
			{
			//cout<<"*\n"<<flush;
				best_feasible_obj = get_objective();
				save_best_feasible();
			}

			if (limit !=0 && limit_counter>limit)
				return;
			if (allow_keylistener && kbhit())
			{
				char c=getch();
				if (c == 'q')
				{
					std::cout<<"Aborting...\n";
					time ( &rawtime );
					timeinfo = localtime ( &rawtime );
					std::cout<<asctime(timeinfo)<<"\n";
					write_result(config::get_string_value("default_timetable_periodic_file"));
					return;
				}
			}


                        if (pivot_count%10 == 0)
                        {
				    if (verbose == OUT_DEBUG)
					std::cout<<"Saving current timetable...\n";
                                    write_result(config::get_string_value("default_timetable_periodic_file"));
                        }
			//char bla[50];
			//sprintf(bla, "%d", global_count);
			//write_result("timetable" + std::string(bla));
			//++global_count;

		};
		//recreate best tabu search solution
		if (search == TAB_SIMPLE_TABU_SEARCH && pivot_count == ts_max_iterations)
			ts_recreate_best();

		//if the distribution was to be counted, this will be the end of the algorithm
		if (countpercentages)
		{
			for (uint i=0; i<nr_of_edges+1; ++i)
				std::cout<<distribution[i]<<"\n";

			uint sum=0;
			for (uint i=0; i<nr_of_edges+1; ++i)
				sum+=distribution[i];
			std::cout<<"Sum: "<<sum<<"\n";

			//calculate distribution
			std::vector<ulong> dis(10);
			for (uint i=0; i<10; ++i)
				dis[i] = 0;
			int pos=0;
			for (uint i=0; i<sum; ++i)
			{
				while(distribution[pos] ==0)
					++pos;
				--distribution[pos];
				dis[10*i/sum]+=pos;
			}
			for (uint i=0; i<10; ++i)
				std::cout<<dis[i]<<"\n";

			exit(0);
		}
		if (verbose == OUT_DEBUG)
			std::cout<<"\n*** Searching for an improving cut. ***\n\n"<<std::flush;
		improvable();
		if (presentation || allow_keylistener) getchar();

		if (cut.active == false && (search == TAB_STEEPEST_SA_HYBRID && !sa_hybrid_active))
			sa_hybrid_return = true;
	}
	while ((cut.active || sa_hybrid_return) && (timelimit > 1.0* (clock() - begin) / CLOCKS_PER_SEC || timelimit==0) );

	if (timelimit < 1.0* (clock() - begin) / CLOCKS_PER_SEC && timelimit != 0)
	  std::cout<<"Timelimit hit!\n";

	time ( &rawtime );
	timeinfo = localtime ( &rawtime );
	std::cout<<asctime(timeinfo)<<"\n";

}

//returns the current objective value
int simplex::get_objective()
{
	property_map<Graph,edge_maxtime_t>::type max = get(edge_maxtime, *g);
	property_map<Graph,edge_mintime_t>::type min = get(edge_mintime, *g);
	property_map<Graph,edge_weight_t>::type w = get(edge_weight, *g);

	int obj = 0;
	for (uint i=0; i<out_tree_edges.size(); ++i)
	{
// 		obj += w[graph_edges.left.at(out_tree_edges[i])] * m_rhs[out_tree_edges[i]];
		obj += w[graph_edges_left[out_tree_edges[i]]] * m_rhs[out_tree_edges[i]];
		//std::cout<<out_tree_edges[i]<<" has slack "<<m_rhs[out_tree_edges[i]]<<"\n";
		//std::cout<<out_tree_edges[i]<<" has weight "<<w[graph_edges.left.at(out_tree_edges[i])]<<"\n";
	}

	for (uint i=0; i<in_tree_edges.size(); ++i)
		if (!lower_tree_edges[i])
		{
// 			Edge e = graph_edges.left.at(in_tree_edges[i]);
			Edge e = graph_edges_left[in_tree_edges[i]];
			obj += w[e] * (max[e]-min[e]);
		}

	return obj;
}

//calculates the change of a pivot
int simplex::get_obj_change(int in_edge, int out_edge, bool as_lower)
{
	//std::cout<<"In_edge: "<<in_edge<<"\n";
	//std::cout<<"Out_edge: "<<out_edge<<"\n";
	++nr_piv_ops;

	property_map<Graph,edge_maxtime_t>::type max = get(edge_maxtime, *g);
	property_map<Graph,edge_mintime_t>::type min = get(edge_mintime, *g);
	property_map<Graph,edge_weight_t>::type w = get(edge_weight, *g);

	if (coeff.find(std::pair<int,int>(in_edge,out_edge))==coeff.end())
	{
		return coeff_not_feasible;
	}

	int change = 0;
	double robchange = 0;

	std::set<int>::iterator it;
	for (it = columns[out_edge].begin(); it!=columns[out_edge].end(); ++it)
	{
		if (*it != in_edge)
		{
			++nr_calc_ops;
			int mod;
			int coeff_i_out, coeff_in_out;
			coeff_i_out = get_coeff(*it, out_edge);
			coeff_in_out = get_coeff(in_edge,out_edge);

			if (as_lower)
				mod = m_rhs[*it] - coeff_i_out * m_rhs[in_edge] / coeff_in_out;
			else
				mod = m_rhs[*it] - coeff_i_out * (m_rhs[in_edge] - (max[graph_edges_left[in_edge]] - min[graph_edges_left[in_edge]])) / coeff_in_out;

			while (mod < 0) mod += period;
//   if (mod < 0)
//     mod = (mod%period) + period;
//   else
			mod %= period;
// 			if (mod>max[graph_edges.left.at(*it)]-min[graph_edges.left.at(*it)])
			if (mod>max[graph_edges_left[*it]]-min[graph_edges_left[*it]])
			{
				return coeff_not_feasible;
			}
// 			change += (mod - m_rhs[*it]) * w[graph_edges.left.at(*it)];
			change += (mod - m_rhs[*it]) * w[graph_edges_left[*it]];
			robchange += (mod -m_rhs[*it]) * robustness[*it];
//			std::cout<<"Adding "<<(mod - m_rhs[*it]) * w[graph_edges.left.at(*it)]<<"\n";
		}
	}

	int coeff_in_out;
	coeff_in_out = get_coeff(in_edge,out_edge);

	int mod;
	if (as_lower)
		mod = m_rhs[in_edge] / coeff_in_out;
	else
// 		mod = (m_rhs[in_edge] - (max[graph_edges.left.at(in_edge)] - min[graph_edges.left.at(in_edge)])) / coeff_in_out;
		mod = (m_rhs[in_edge] - (max[graph_edges_left[in_edge]] - min[graph_edges_left[in_edge]])) / coeff_in_out;
	//while (mod < 0) mod += period;
	//mod %= period;
	//if (mod>max[graph_edges.left.at(out_edge)]-min[graph_edges.left.at(out_edge)])
	//{
//		return coeff_not_feasible;
//	}

	mod += m_rhs[out_edge];
	while (mod < 0 ) mod+= period;
	mod%=period;

// 	if (mod> max[graph_edges.left.at(out_edge)]-min[graph_edges.left.at(out_edge)])
	if (mod> max[graph_edges_left[out_edge]]-min[graph_edges_left[out_edge]])
		return coeff_not_feasible;

	//change += w[graph_edges.left.at(out_edge)]*(mod-m_rhs[out_edge]);
	change += w[graph_edges_left[out_edge]]*(mod-m_rhs[out_edge]);
	//std::cout<<"Adding afterwards "<<w[graph_edges.left.at(out_edge)]*(mod-m_rhs[out_edge])<<"\n";
	//std::cout<<"Adding afterwards "<<w[graph_edges.left.at(out_edge)]*mod<<"\n";

	robchange += robustness[out_edge] * (mod-m_rhs[out_edge]);

	if (as_lower)
	{
		change -= w[graph_edges_left[in_edge]]*m_rhs[in_edge];
		robchange -= robustness[in_edge]*m_rhs[in_edge];
	}
	else
	{
		change -= w[graph_edges_left[in_edge]]*(m_rhs[in_edge]-(max[graph_edges_left[in_edge]] - min[graph_edges_left[in_edge]]));
		robchange -= robustness[in_edge] * (m_rhs[in_edge]-(max[graph_edges_left[in_edge]] - min[graph_edges_left[in_edge]]));
	}

	//do not enter forbidden domain
	if ( robchange < 0 && robchange + current_robustness < min_robustness )
		return coeff_not_feasible;


	//please leave forbidden domain
	if (robchange > 0 && current_robustness < min_robustness)
		change += (-robchange) * dyn_rob_penalty;

	// if ( (robchange < 0 && robchange + current_robustness < min_robustness) || (robchange > 0 && current_robustness < min_robustness))
	// {
	  // return coeff_not_feasible;
	  // change += (min_robustness - (robchange + current_robustness)) * 1e4;
	  // change += (-robchange) * dyn_rob_penalty;
	// }

	/*
	if (as_lower)
		std::cout<<"Adding afterwards"<<-w[graph_edges.left.at(in_edge)]*m_rhs[in_edge]<<"\n";
	else
		std::cout<<"Adding afterwards"<<-w[graph_edges.left.at(in_edge)]*(m_rhs[in_edge]-(max[graph_edges.left.at(in_edge)] - min[graph_edges.left.at(in_edge)]));

	std::cout<<"Sum is "<<change<<"\n";
*/
	return change;

//   return -robchange*1e6;

}

//tableau setter
void simplex::set_coeff(int out_edge, int in_edge, int value)
{
	switch (value)
	{
	case 1: coeff[std::pair<int,int>(out_edge,in_edge)] = true; break;
	case -1: coeff[std::pair<int,int>(out_edge,in_edge)] = false; break;
	case 0: if (coeff.find(std::pair<int,int>(out_edge,in_edge))!=coeff.end())
				coeff.erase(coeff.find(std::pair<int,int>(out_edge,in_edge)));
			break;
	default: throw;
	}
}

//tableau getter
int simplex::get_coeff(int out_edge, int in_edge)
{
	int ret_val;
	typedef std::pair<int, int> p;

	if (coeff.find(p(out_edge,in_edge))!=coeff.end())
		ret_val = coeff[p(out_edge,in_edge)] ? 1: -1;
	else
		ret_val = 0;

	return ret_val;
}

//deprecated
void simplex::set_coeff_temp(int out_edge, int in_edge, int value)
{
	switch (value)
	{
	case 1: coeff_temp[std::pair<int,int>(out_edge,in_edge)] = true; break;
	case -1: coeff_temp[std::pair<int,int>(out_edge,in_edge)] = false; break;
	case 0: if (coeff_temp.find(std::pair<int,int>(out_edge,in_edge))!=coeff_temp.end())
				coeff_temp.erase(coeff_temp.find(std::pair<int,int>(out_edge,in_edge)));
			break;
	default: throw;
	}
}

//deprecated
void simplex::update_coeffs()
{
	for (uint i=0; i<nr_of_edges; ++i)
		columns[i].clear();

	coeff.clear();
	std::map<std::pair<int, int>,bool>::iterator it;
	for (it = coeff_temp.begin(); it!=coeff_temp.end(); ++it)
	{
		int value;
		if ((*it).second)
			value = 1;
		else
			value = -1;

		set_coeff( ((*it).first).first , ((*it).first).second, value);
		columns[((*it).first).second].insert(((*it).first).first);
	}
	coeff_temp.clear();
}

//update the tableau when using a pivot
void simplex::swap_edges(int into_edge, int out_edge, bool as_lower)
{
	if (verbose == OUT_DEBUG) std::cout<<"Swapping edges, new version.\n";

	//find in in_tree edges, delete, and vice versa
	for (uint i = 0; i< in_tree_edges.size(); i++)
		if (in_tree_edges[i] == out_edge)
		{
			//std::cout<<lower_tree_edges[i]<<"\n";
			lower_tree_edges[i] = as_lower;
		}

	int swap_temp = into_edge;
	int i = 0;
	bool found = false;
	while(!found)
	{
		if (out_tree_edges[i]==into_edge)
		{
			out_tree_edges[i]=out_edge;
			found=true;
		}
		else
			++i;
	}
	found = false;
	i=0;
	while(!found)
	{
		if (in_tree_edges[i]==out_edge)
		{
			in_tree_edges[i]=swap_temp;
			found=true;
		}
		else
			++i;
	}

	//record the spanning tree as a graph
	if (spanning_tree != 0)
	   	delete spanning_tree;
	spanning_tree = new Tree(nr_of_nodes);
	for (uint i=0; i<in_tree_edges.size(); i++)
		add_edge(source(graph_edges_left[in_tree_edges[i]],*g), target(graph_edges_left[in_tree_edges[i]],*g), *spanning_tree);

	//rebuild!
	build();
}

//create a simplex tableau
void simplex::build()
{
  	//find times recursively
	//if (verbose) std::cout<<"Calculating pi..."<<std::flush;
  	//set_time();
  	//if (verbose) std::cout<<"done.\n"<<std::flush;
	find_modulo();

	graph_traits<Graph>::edge_iterator ei;
	property_map<Graph,edge_maxtime_t>::type max = get(edge_maxtime, *g);
	property_map<Graph,edge_mintime_t>::type min = get(edge_mintime, *g);
	property_map<Graph,vertex_index_t>::type index = get(vertex_index, *g);
	property_map<Graph,edge_weight_t>::type w = get(edge_weight, *g);

	/*
	if (verbose)
	{
		std::cout<<"In tree edges:\n"<<std::flush;
		for (uint k = 0; k<in_tree_edges.size(); k++)
			std::cout<<in_tree_edges[k]<<" ";
		std::cout<<"\n"<<std::flush;
		for (uint k = 0; k<in_tree_edges.size(); k++)
			std::cout<<lower_tree_edges[k]<<" ";
		std::cout<<"\n"<<std::flush;
		std::cout<<"Out tree edges:\n"<<std::flush;
		for (uint k = 0; k<out_tree_edges.size(); k++)
				std::cout<<out_tree_edges[k]<<" ";
		std::cout<<"\n"<<std::flush;
	}
	*/

	//calculate right hand side
	if (verbose == OUT_DEBUG)
		std::cout<<"Calculating slack for tree edges.\n"<<std::flush;
	//calculate slack: in edges
	for (uint k = 0; k<in_tree_edges.size(); k++)
		if (lower_tree_edges[k])
			m_slack[in_tree_edges[k]] = 0;
		else
			m_slack[in_tree_edges[k]] = max[graph_edges_left[in_tree_edges[k]]] - min[graph_edges_left[in_tree_edges[k]]];

	if (verbose == OUT_DEBUG)
		std::cout<<"Calculating slack for non-tree edges.\n"<<std::flush;
	//calculate slack: out edges
	int slack, min_int;
	for (uint k = 0; k<out_tree_edges.size(); k++)
	{
		slack = m_pi[graph_vertices.right.at(target(graph_edges_left[out_tree_edges[k]], *g))] - m_pi[graph_vertices.right.at(source(graph_edges_left[out_tree_edges[k]], *g))];
		//while (slack > max[graph_edges.left.at(out_tree_edges[k])])
		//	slack-=period;
		slack%=period;
		//while (slack < min[graph_edges.left.at(out_tree_edges[k])])
		//	slack+=period;
		if (slack<0) slack+=period;
		min_int = min[graph_edges_left[out_tree_edges[k]]];
		slack+=period*(min_int/period);
		if (slack < min_int)
			slack+=period;
		slack-=min_int;
		//slack-=min[graph_edges.left.at(out_tree_edges[k])];
		m_slack[out_tree_edges[k]] = slack;
	}

	coeff.clear();
	for (uint i=0; i<nr_of_edges; i++)
		columns[i].clear();
	columns.clear();

	for (uint i = 0; i<nr_of_edges; i++)
		m_rhs[i]=m_slack[i];

	current_robustness=0;
	for (uint i = 0; i<nr_of_edges; i++)
		current_robustness+=robustness[i]*m_slack[i];
// 	cout<<"Robustness: "<<rob<<"\n"<<flush;
	// if (current_robustness < min_robustness)
	  // dyn_rob_penalty*=2;
	// else
	  // dyn_rob_penalty*=0.95;

// 	cout<<dyn_rob_penalty<<"\n"<<flush;

	if (verbose == OUT_DEBUG)
		std::cout<<"Calculating tableau coefficients.\n"<<std::flush;
	//set coefficients:
	//calculate cycles
	for (uint i=0; i<out_tree_edges.size(); i++)
	{
		std::vector<Vertex> parent(nr_of_nodes);
		for (unsigned int k = 0; k < parent.size(); ++k)
			parent[k] = k;
		breadth_first_search(*spanning_tree, source(graph_edges_left[out_tree_edges[i]],*g),
			visitor(make_bfs_visitor(record_predecessors(&parent[0], on_tree_edge())) ));

		std::list<int> path;
		int position = graph_vertices.right.at(target(graph_edges_left[out_tree_edges[i]],*g));
		path.push_back(position);

		while (position != graph_vertices.right.at(source(graph_edges_left[out_tree_edges[i]],*g)))
		{
			position = graph_vertices.right.at(parent[graph_vertices.left.at(position)]);
			path.push_back(position);
		}

		std::vector<int> v_path;
		v_path.reserve(path.size());
		for (std::list<int>::iterator it = path.begin(); it!=path.end(); it++)
			v_path.push_back(*it);

		//set the right coefficients
		for (uint k=0; k<v_path.size()-1; k++)
		{
			graph_traits<Graph>::out_edge_iterator out_i, out_end;
        		for (tie(out_i, out_end) = out_edges(graph_vertices.left.at(v_path[k]) , *g);
        					out_i != out_end; ++out_i)
        		{
				if (v_path[k+1] == graph_vertices.right.at(target(*out_i,*g)))
				{
					bool check_in=true;
					//for (uint l=0; l<out_tree_edges.size(); l++)
					//	if (graph_edges.left.at(out_tree_edges[l]) == *out_i)
					//		check_in = false;
					int edgenum = graph_edges.right.at(*out_i);
					if (spanning_set.find(edgenum) == spanning_set.end())
						check_in = false;
					if (check_in)
					{
						set_coeff(out_tree_edges[i],graph_edges.right.at(*out_i), 1);
						columns[graph_edges.right.at(*out_i)].insert(out_tree_edges[i]);
						//std::cout<<"Coeff "<<out_tree_edges[i]<<" , "<<graph_edges.right.at(*out_i)<<" = 1\n";
						break;
					}
				}
			}
			graph_traits<Graph>::in_edge_iterator in_i, in_end;
        		for (tie(in_i, in_end) = in_edges(graph_vertices.left.at(v_path[k]) , *g);
        					in_i != in_end; ++in_i)
			{
				if (v_path[k+1] == graph_vertices.right.at(source(*in_i,*g)))
				{
					bool check_in=true;
					//for (uint l=0; l<out_tree_edges.size(); l++)
					//	if (graph_edges.left.at(out_tree_edges[l]) == *in_i)
					//		check_in = false;
					int edgenum = graph_edges.right.at(*in_i);
					if (spanning_set.find(edgenum) == spanning_set.end())
						check_in = false;
					if (check_in)
					{
						set_coeff(out_tree_edges[i],graph_edges.right.at(*in_i), -1);
						columns[graph_edges.right.at(*in_i)].insert(out_tree_edges[i]);
						//std::cout<<"Coeff "<<out_tree_edges[i]<<" , "<<graph_edges.right.at(*in_i)<<" = -1\n";
						break;
					}
				}
			}
        	}
	}
}

//calculate the current modulo parameters
void simplex::find_modulo()
{
  if (search == TAB_NOTHING)
    return;

	property_map<Graph,edge_maxtime_t>::type max = get(edge_maxtime, *g);
	property_map<Graph,edge_mintime_t>::type min = get(edge_mintime, *g);
	property_map<Graph,edge_weight_t>::type w = get(edge_weight, *g);

	if (verbose == OUT_DEBUG)
		std::cout<<"Calculating times.\n";
	set_time();

	if (verbose == OUT_DEBUG)
		std::cout<<"Calculating modulo parameters.\n";
	for (uint i = 0; i<in_tree_edges.size(); i++)
		modulo_param[in_tree_edges[i]] = 0;

	for (uint i = 0; i<out_tree_edges.size(); i++)
	{
		modulo_param[out_tree_edges[i]] = 0;
		int difftime = m_pi[graph_vertices.right.at(target(graph_edges_left[out_tree_edges[i]], *g))];
		difftime -= m_pi[graph_vertices.right.at(source(graph_edges_left[out_tree_edges[i]], *g))];
		while (difftime > max[graph_edges_left[out_tree_edges[i]]])
		{
			difftime-=period;
			--modulo_param[out_tree_edges[i]];
		}
		while (difftime < min[graph_edges_left[out_tree_edges[i]]])
		{
			difftime+=period;
			++modulo_param[out_tree_edges[i]];
		}
		//std::cout<<"Edge "<<out_tree_edges[i]<<" has modulo "<<modulo_param[out_tree_edges[i]]<<"\n";
	}
}

//write the timetable to a file
void simplex::write_result(std::string filename)
{
	if (verbose == OUT_DEBUG)
		std::cout<<"Calculating times.\n";
 	//set_time();
	for (int i=0; i<nr_of_nodes; ++i)
		m_pi[i] = best_feasible[i];

	if (verbose == OUT_DEBUG)
		std::cout<<"Writing result to file.\n";

	std::ofstream output (filename.c_str());
	output<<"#timetable found by modulo network simplex algorithm\n";
	time_t rawtime;
	struct tm * timeinfo;
	time ( &rawtime );
	timeinfo = localtime ( &rawtime );
	output<<"#"<<asctime(timeinfo);

	for (uint i=0; i<nr_of_nodes; i++)
	{
		int tmp = m_pi[i];
		while (tmp<0)
			tmp+=period;
		if (tmp>period)
			tmp%=period;
		output<<i+1<<"; "<<tmp<<"\n";
	}

	output.close();

}

//setter methods
void simplex::set_tab_search(TABLEAU_SEARCH tab_search)
{
	search = tab_search;
}

void simplex::set_min_pivot_improvement(double min_percentage)
{
	min_pivot_improvement = min_percentage;
}

void simplex::set_min_cut_improvement(double min_percentage)
{
	min_cut_improvement = min_percentage;
}

void simplex::set_tab_min_improvement(TABLEAU_MIN_IMPROVEMENT tab_min)
{
	search_impro = tab_min;
}

void simplex::set_percentage_improvement(double percentage)
{
	percentage_improvement = percentage;
}

void simplex::set_sa_init_temperature(double temp)
{
	sa_temperature = temp;
}

void simplex::set_sa_coolness_factor(double fac)
{
	sa_cooling_factor = fac;
}

void simplex::set_loc_improvement(LOCAL_IMPROVEMENT loc)
{
	local_search = loc;
}

void simplex::set_loc_number_of_nodes(int number)
{
	loc_number_of_nodes = number;
}

void simplex::set_loc_number_of_tries(int number)
{
	loc_number_of_tries = number;
}

void simplex::set_ts_memory(int length)
{
	ts_memory_length = length;
}

//recreates the best solution of tabu search
void simplex::ts_recreate_best()
{
	std::cout<<"Recreate best\n"<<std::flush;

	std::set<int>in_tree_set;
	std::set<int>::iterator set_it;
	int tree_counter=0;
	for (set_it = best_solution.lower_tree.begin(); set_it!= best_solution.lower_tree.end(); ++set_it)
	{
		in_tree_edges[tree_counter] = *set_it;
		in_tree_set.insert(*set_it);
		lower_tree_edges[tree_counter] = true;
		++tree_counter;
	}

	for (set_it = best_solution.upper_tree.begin(); set_it != best_solution.upper_tree.end(); ++set_it)
	{
		in_tree_edges[tree_counter] = *set_it;
		in_tree_set.insert(*set_it);
		lower_tree_edges[tree_counter] = false;
		++tree_counter;
	}

	int counter =0;
	for (uint i=0; i<nr_of_edges; ++i)
		if (in_tree_set.find(i)==in_tree_set.end())
		{
			out_tree_edges[counter] = i;
			++counter;
		}

	std::cout<<"In tree size: "<<in_tree_edges.size()<<"\n"<<std::flush;
	std::cout<<"Out tree size: "<<out_tree_edges.size()<<"\n"<<std::flush;

	if (spanning_tree != 0)
		delete spanning_tree;
	spanning_tree = new Tree(nr_of_nodes);
	spanning_set.clear();

	for (uint i=0; i<nr_of_nodes-1; i++)
	{
		add_edge(source(graph_edges_left[in_tree_edges[i]],*g), target(graph_edges_left[in_tree_edges[i]],*g), *spanning_tree);
		spanning_set.insert(in_tree_edges[i]);
	}

	build();

}

//setter methods
void simplex::set_ts_max_iterations(int number)
{
	ts_max_iterations = number;
}

void simplex::set_dynamic_pivot_factor(double factor)
{
	dynamic_pivot_factor = factor;
}

void simplex::set_headways(bool what)
{
	use_headways = what;
}

void simplex::set_limit(int number)
{
	limit = number;
}

void simplex::set_timelimit(double time)
{
	timelimit = time;
}

void simplex::save_best_feasible()
{
	set_time();
	for (int i=0; i<nr_of_nodes; ++i)
		best_feasible[i] = m_pi[i];
}

void simplex::restore_best_feasible()
{
	for (int i=0; i<nr_of_nodes; ++i)
		best_feasible[i] = m_pi[i];

	std::ofstream output ( (config::get_string_value("default_timetable_periodic_file")).c_str());
	output<<"#timetable found by modulo network simplex algorithm\n";
	time_t rawtime;
	struct tm * timeinfo;
	time ( &rawtime );
	timeinfo = localtime ( &rawtime );
	output<<"#"<<asctime(timeinfo);

	for (uint i=0; i<nr_of_nodes; i++)
	{
		int tmp = m_pi[i];
		while (tmp<0)
			tmp+=period;
		if (tmp>period)
			tmp%=period;
		output<<i+1<<"; "<<tmp<<"\n";
	}

	output.close();
}
