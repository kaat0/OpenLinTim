/* Convert.cc
*Hilfsprogramm
*
*Bereitet die gegebenen Daten in eine
*f�r xpress geeignete Form auf
*
*Ben�tigt
*-basis/Load.giv		(Min und Maxfrequenzen + Kanten)
*-basis/Pool.giv		(Linienpool)
*-Pool-cost.giv	(Kosten)
*
*Ausgabe
*"moseldaten" in der Form
*
*Linienzahl: ?
*Kantenzahl: ?
*A:[?
*?
*?]
*cost:[?]
*Min:[?]
*Max:[?]
*/

#include <iostream>
#include <fstream>
#include <cstdlib>
#include <vector>
#include <set>

#include "../../essentials/config/config.h"

using namespace std;

struct od_entry
{
	od_entry(unsigned short a, unsigned short b, int value)
	{
		i = a;
		j = b;
		val = value;
	}
	unsigned short i;
	unsigned short j;
	int val;
};

struct edge
{
	edge(unsigned short i, unsigned short j)
	{
		s = i;
		t = j;
	};
	unsigned short s;
	unsigned short t;
};


int main(void)
{

    config::from_file("basis/Config.cnf", false);

	int temp;
	string text;
	int Linienzahl;
	int Kantenzahl;
	int L[3000][500];
	vector<edge> edges;
	vector<set<unsigned short> > line_knots;
	int Min[3000];
	int Max[3000];
	double cost[3000];
	vector<od_entry> OD;
	size_t pos;


	    cout<<"Initializing."<<endl<<flush;
	//Empty matrices
	for (int i=0; i<=2999; i++)
		for(int j=0; j<=499; j++)
			{
			L[i][j]=0;
			//OD[i][j]=0;
			}

	cout<<"Reading in edges..."<<flush;

	ifstream edgef (config::get_string_value("default_edges_file").c_str());
	int source, sink;
    while (!edgef.eof())
    {
		getline(edgef,text);
		    if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
		Kantenzahl=atoi(text.c_str());

		pos=text.find(";");
		text=text.substr(pos+1);
		source = atoi(text.c_str());

		pos=text.find(";");
		text=text.substr(pos+1);
		sink = atoi(text.c_str());
		
		edge e(source,sink);
		edges.push_back(e);

		/*
		pos=text.find(";");
		text=text.substr(pos+1);
		cost[Kantenzahl-1] = atof(text.c_str());

		pos=text.find(";");
		text=text.substr(pos+1);
		Min[Kantenzahl-1]=atoi(text.c_str());

		pos=text.find(";");
		text=text.substr(pos+1);
		Max[Kantenzahl-1]=atoi(text.c_str());
		*/
		}
    }	   
	
	edgef.close();

	ifstream load(config::get_string_value("default_loads_file").c_str());
    while (!load.eof())
    {
		getline(load,text);
		    if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
		Kantenzahl=atoi(text.c_str());

		pos=text.find(";");
		text=text.substr(pos+1);

		pos=text.find(";");
		text=text.substr(pos+1);
		Min[Kantenzahl-1]=atoi(text.c_str());

		pos=text.find(";");
		text=text.substr(pos+1);
		Max[Kantenzahl-1]=atoi(text.c_str());
		}
    }

	load.close();
	cout<<"done."<<endl<<flush;


	ifstream pool (config::get_string_value("default_pool_file").c_str());
	
	int last_line_number = 0;
	int control = 1;
	cout<<"Reading in lines..."<<flush;
	while(!pool.eof())
	{
		getline(pool,text);
		    if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
		Linienzahl=atoi(text.c_str());
		if (last_line_number != Linienzahl)
		{
			last_line_number = Linienzahl;
			set<unsigned short> temp_set;
			line_knots.push_back(temp_set);
			control = 1;
		}
		pos=text.find(";");
		text=text.substr(pos+1);
		
		if (atoi(text.c_str()) != control)
		{
			cout<<"ERROR: No consistent numbering in line "<<last_line_number<<"\n";
			exit(1);
		}
		else
			++control;
		  
		pos=text.find(";");
		text=text.substr(pos+1);

		temp=atoi(text.c_str());
		edge e = edges[temp-1];
		line_knots[Linienzahl-1].insert(e.s);
		line_knots[Linienzahl-1].insert(e.t);

		L[Linienzahl-1][temp-1]=1;
		}
	}
	
	pool.close();
	cout<<"done.\nReading in line costs..."<<flush;

	ifstream poolcost(config::get_string_value("default_pool_cost_file").c_str());
	while(!poolcost.eof())
	{
		getline(poolcost,text);
		if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
			Linienzahl=atoi(text.c_str());
			pos=text.find(";");
			text=text.substr(pos+1);
			pos=text.find(";");
			text=text.substr(pos+1);
			cost[Linienzahl-1] = atof(text.c_str());
		}
	}
	poolcost.close();
	cout<<"done.\n"<<flush;	
	
	
	ifstream od_file (config::get_string_value("default_od_file").c_str());

	int i_pos;
	int j_pos;

	cout<<"Reading in OD matrix..."<<flush;
	while (!od_file.eof())
	{
		getline(od_file,text);
		if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
		i_pos=atoi(text.c_str());
		pos=text.find(";");
		text=text.substr(pos+1);
		
		j_pos = atoi(text.c_str());
		pos=text.find(";");
		text=text.substr(pos+1);

		od_entry e(i_pos-1,j_pos-1,atoi(text.c_str()));
		OD.push_back(e);
		}
    	}		

	od_file.close();
	cout<<"done.\n"<<flush;

	/*
	ifstream costfile(config::get_string_value("default_pool_cost_file").c_str());
	
	//Kosten einlesen
	    cout<<"Reading in costs..."<<flush;
	while(!costfile.eof())
	{
		getline(costfile,text);
		    if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
		temp=atoi(text.c_str());
		for (int j=0; j<=1; j++){
		pos=text.find(";");
		text=text.substr(pos+1);}
		cost[temp-1]=atoi(text.c_str());
		}
	}
	
	costfile.close();
	    cout<<"done."<<endl<<flush;
	*/

	//Alle Daten fertig eingelesen!
	//Beginne jetzt, die Datei zu schreiben
	
		cout<<"All data read in."<<endl;
		cout<<"Writing...\n";
	ofstream out("line-planning/Moseldaten");
	
	out<<"Linienzahl: "<<Linienzahl<<endl;
	out<<"Kantenzahl: "<<Kantenzahl<<endl;
	out<<"ODsize: "<<OD.size()<<endl;
	
	    cout<<"Edge-path incidence matrix...";
	out<<"H:[";
	for (int i=0; i<Linienzahl; i++)
		{
		for (int j=0; j<Kantenzahl; j++)
		{
			out<<L[i][j];
			if (j!=Kantenzahl-1||i!=Kantenzahl-1)
				out<<",";
		}
		if (i!=Linienzahl-1) out<<endl;
		}
	out<<"]"<<endl;

	cout<<"done.\nCosts...";
	out<<"cost:[";
	for (int i=0; i<Linienzahl; i++)
		{
		out<<cost[i];
		if (i!=Linienzahl-1) out<<",";
		}
	out<<"]"<<endl;

	cout<<"done.\nMin values...";
	
	out<<"Min:[";
	for (int i=0; i<Kantenzahl; i++)
		{
		out<<Min[i];
		if (i!=Kantenzahl-1) out<<",";
		}
	out<<"]"<<endl;	
	cout<<"done.\nMax values...";
	
	out<<"Max:[";
	for (int i=0; i<Kantenzahl; i++)
		{
		out<<Max[i];
		if (i!=Kantenzahl-1) out<<",";
		}
	out<<"]"<<endl;		

	cout<<"done.\nOD-pair-path matrix...";
	vector<bool> feasible(OD.size());

	out<<"K:[";
	for (unsigned int i=0; i<OD.size(); i++)
		{
		feasible[i] = false;
		for (int j=0; j<Linienzahl; j++)
		{

			if ( line_knots[j].find((OD[i]).i)!=line_knots[j].end() && line_knots[j].find((OD[i]).j) != line_knots[j].end())
			{
				out<<"1";
				feasible[i] = true;
			}
			else
				out<<"0";
			if (j!=Linienzahl-1||i!=OD.size()-1)
				out<<",";
		}
		if (i!=OD.size()-1) out<<endl;
		}
	out<<"]"<<endl;

	cout<<"done.\nOD min values...";

	//search for largest OD-Value
	int max_od = 0;
	for (unsigned int i=0; i<OD.size(); ++i)
		if (max_od < OD[i].val)
			max_od = OD[i].val;

	out<<"OD:[";
	for (unsigned int i=0; i<OD.size(); ++i)
	{
		//only consider the most important values
		if (/*OD[i].val >= 0.5 * max_od && */feasible[i])
			out<<(OD[i].val-1)/atoi(config::get_string_value("gen_passengers_per_vehicle").c_str()) + 1;
		else
			out<<"0";

		if (i!=OD.size()-1) out<<",";
	}
	out<<"]\n";

	cout<<"done.\nLP formulation is ready to go!\n";	


	out.close();
	
	return 0;
}
	
