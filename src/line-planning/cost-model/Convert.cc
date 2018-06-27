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

//test

#include <iostream>
#include <fstream>
#include <cstdlib>
#include <vector>
#include <set>

#include "../../essentials/config/config.h"

using namespace std;

int main(void)
{

    config::from_file("basis/Config.cnf", false);

	int temp;
	string text;
	int Linienzahl = -1;
	int Kantenzahl;
// 	int L[3000][600];
	vector<set<int> > L;
// 	int Min[3000];
	vector<int> Min;
// 	int Max[3000];
	vector<int> Max;
// 	double cost[3000];
	vector<double> cost;
	size_t pos;


// 	    cout<<"Initialisiere."<<endl;
	//Matrix mit 0 initialisieren
// 	for (int i=0; i<=2999; i++)
// 		for(int j=0; j<=599; j++)
// 			L[i][j]=0;
	
	ifstream pool (config::get_string_value("default_pool_file").c_str());
	
	//Linien einlesen
	    cout<<"Lese Linien ein...";
	int consistency_nr = 0;
	while(!pool.eof())
	{
		getline(pool,text);
		    if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
		if (Linienzahl != atoi(text.c_str()))
		{
			Linienzahl=atoi(text.c_str());
			consistency_nr = 1;
			L.resize(L.size()+1);
		}
		else
			consistency_nr++;   
		pos=text.find(";");
		text=text.substr(pos+1);
		temp=atoi(text.c_str());
		if (temp != consistency_nr)
		{
			cout<<"ERROR: Inconsistent numbering of line "<<Linienzahl<<"\n";
			exit(1);
		}
		
		pos=text.find(";");
		text=text.substr(pos+1);
		int edge = atoi(text.c_str());
		(L.back()).insert(edge-1);
// 		L[Linienzahl-1][edge-1]=1;
		
//  		cout<<Linienzahl<<"; "<<consistency_nr<<"; "<<edge<<"\n";
		}
	}
	
	pool.close();
	    cout<<"fertig."<<endl<<flush;
	
	ifstream load (config::get_string_value("default_loads_file").c_str());

	//Kanten einlesen
	    cout<<"Lese Kanten ein...";
	    
	consistency_nr = 1;
    while (!load.eof())
    {
		getline(load,text);
		    if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
		Kantenzahl=atoi(text.c_str());
		
		if (Kantenzahl != consistency_nr)
		{
			cout<<"ERROR: Inconsistent numbering of edges at "<<Kantenzahl<<"\n";
			exit(1);
		}
		else
			++consistency_nr;
		
		pos=text.find(";");
		text=text.substr(pos+1);
		
		pos=text.find(";");
		text=text.substr(pos+1);

//  		Min[Kantenzahl-1]=atoi(text.c_str());
		Min.push_back(atoi(text.c_str()));
		pos=text.find(";");
		text=text.substr(pos+1);
// 		Max[Kantenzahl-1]=atoi(text.c_str());
		Max.push_back(atoi(text.c_str()));
		}
    }	   
	
	load.close();
	    cout<<"fertig."<<endl;
	
	
	ifstream costfile(config::get_string_value("default_pool_cost_file").c_str());
	
	//Kosten einlesen
	    cout<<"Lese Kosten ein...";
	    consistency_nr = 1;
	while(!costfile.eof())
	{
		getline(costfile,text);
		    if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
		temp=atoi(text.c_str());
		
		if (temp != consistency_nr)
		{
			cout<<"ERROR: Inconsistent numbering of line costs at "<<temp<<"\n";
			exit(1);
		}
		else
			++consistency_nr;		
		
		pos=text.find(";");
		text=text.substr(pos+1);
		
		pos=text.find(";");
		text=text.substr(pos+1);
// 		cost[temp-1]=atof(text.c_str());
		cost.push_back(atof(text.c_str()));
		}
	}
	
	costfile.close();
	    cout<<"fertig."<<endl;
	
	//Alle Daten fertig eingelesen!
	//Beginne jetzt, die Datei zu schreiben
	
		cout<<"Daten eingelesen."<<endl;
		cout<<"Schreibe:"<<endl;
	ofstream out("line-planning/Moseldaten");
	
	out<<"Linienzahl: "<<Linienzahl<<endl;
	out<<"Kantenzahl: "<<Kantenzahl<<endl;
	
	    cout<<"Schreibe Matrix...";
	out<<"A:[";
	for (int i=0; i<Linienzahl; i++)
		{
		for (int j=0; j<Kantenzahl; j++)
		{
			if (L[i].count(j) != 0)
			  out<<"1";
			else
			  out<<"0";
// 			out<<L[i][j];
			if (j!=Kantenzahl-1||i!=Linienzahl-1)
				out<<",";
// 			if (L[i][j] == 1)
// 				cout<<i+1<<"; "<<j+1<<"\n";
		}
		if (i!=Linienzahl-1) out<<endl;
		}
	out<<"]"<<endl;
	
	    cout<<"fertig."<<endl;
	out<<"cost:[";
	for (int i=0; i<Linienzahl; i++)
		{
		out<<cost[i];
		if (i!=Linienzahl-1) out<<",";
		}
	out<<"]"<<endl;
	
	out<<"Min:[";
	for (int i=0; i<Kantenzahl; i++)
		{
		out<<Min[i];
		if (i!=Kantenzahl-1) out<<",";
		}
	out<<"]"<<endl;	
	
	out<<"Max:[";
	for (int i=0; i<Kantenzahl; i++)
		{
		out<<Max[i];
		if (i!=Kantenzahl-1) out<<",";
		}
	out<<"]"<<endl;		
	
	out.close();
	
	return 0;
}
	
