/* SolConv.cc
*Hilfsprogramm
*
*Bereitet die von xpress ausgegeben Daten in die Standardform auf
*
*Ben�tigt
*-line-planning/xpresssol
*-basis/Load.giv
*-basis/Pool.giv
*
*Ausgabe
*-line-planning/Line-Concept.lin
*
*/

#include <iostream>
#include <fstream>
#include <cstdlib>
#include <cmath>
#include <vector>

#include "../../essentials/config/config.h"

using namespace std;

int main(void)
{
    config::from_file("basis/Config.cnf", false);

	int temp;
	string text;
	int Linienzahl;
	int Kantenzahl;
	vector<vector<int> > L;
	//int L[6000][300];
	size_t pos;
	vector<int> Min;
//	int Min[300];
	vector<int> Max;
//	int Max[300];
	vector<int> freq;
//	int freq[6000];

	//Matrix mit 0 initialisieren
//	for (int i=0; i<=5999; i++)
//		for(int j=0; j<=299; j++)
//			L[i][j]=-1;

//	for (int i=0; i<=5999; i++)
//	    freq[i]=0;

//	cout<<"Initialisierung abgeschlossen."<<endl;
	
	ifstream pool (config::get_string_value("default_pool_file").c_str());
	
	//Linien einlesen
	while(!pool.eof())
	{
		getline(pool,text);
		if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
		//Linienzahl=atoi(text.c_str());
        if (Linienzahl != atoi(text.c_str()))
        {
            Linienzahl=atoi(text.c_str());
            L.resize(L.size()+1);
        }
		pos=text.find(";");
		text=text.substr(pos+1);
		temp=atoi(text.c_str());
		pos=text.find(";");
		text=text.substr(pos+1);
        int edge = atoi(text.c_str());
        (L.back()).push_back(edge-1);
		//L[Linienzahl-1][temp-1]=atoi(text.c_str());
		}
	}
	
	pool.close();
	
	cout<<"Pool eingelesen."<<endl;
	
	ifstream load (config::get_string_value("default_loads_file").c_str());

	//Kanten einlesen
    while (!load.eof())
    {
		getline(load,text);
		if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
		Kantenzahl=atoi(text.c_str());
		for (int j=0; j<=1; j++){
		pos=text.find(";");
		text=text.substr(pos+1);}
		//Min[Kantenzahl-1]=atoi(text.c_str());
        Min.push_back(atoi(text.c_str()));
		pos=text.find(";");
		text=text.substr(pos+1);
        Max.push_back(atoi(text.c_str()));
		//Max[Kantenzahl-1]=atoi(text.c_str());
		}
    }	   
	
	load.close();
	
	cout<<"Kanten eingelesen."<<endl;

	ifstream optfile("line-planning/xpresssol");

	    while(!optfile.eof())
	    {
		getline(optfile,text);
		if(text!="")
		{
		    temp=atoi(text.c_str());
		    pos=text.find(";");
		    text=text.substr(pos+1);
		    double val = atof(text.c_str());
		    //freq[temp]=floor(val + 0.5);
            freq.push_back(floor(val+0.5));
		}
	    }

	optfile.close();

	cout<<"Optimale Loesung eingelesen."<<endl;

	//Alle Daten fertig eingelesen!
	//Beginne jetzt, die Datei zu schreiben

	cout<<"Schreibe in Datei..."<<endl;
	
	ofstream out(config::get_string_value("default_lines_file").c_str());
	
	out<<"# feasible line concept"<<endl;
	out<<"# optimal solution won by xpress"<<endl;

	out<<"# line-id; edge-order; edge-id; frequency"<<endl;

        /* 
        // old version:
        // only lines with freq>0 are printed
         
	int number =1;

	for (int i=0; i<Linienzahl; i++)
	{
	    if (freq[i+1]>0)
	    {
		int j=0;
		while(L[i][j]!=-1)
		{
		    out<<number<<";"<<j+1<<";"<<L[i][j]<<";"<<freq[i+1]<<endl;
		    j++;
		}
		number++;
	    }
	}

        */
        
        // new version:
        // all lines are printed
        
    /*    for (int i=0; i<Linienzahl; ++i)
	{
	   int j=0;
	   while(L[i][j]!=-1)
	   {
                out<<i+1<<";"<<j+1<<";"<<L[i][j]<<";"<<freq[i+1]<<endl;
                j++;
	   }

	}*/

    int edge_id;
    for (int i=0; i<Linienzahl; i++) {
        edge_id=1;
        for (int j = 0; j < L[i].size(); j++) {
            out<<i+1<<";"<<edge_id<<";"<<L[i][j]+1<<";"<<freq[i]<<endl;
            edge_id++;
        }
    }

	out.close();
    cout<<"Ausgabedatei geschrieben"<<endl;
	return 0;
	}
	
