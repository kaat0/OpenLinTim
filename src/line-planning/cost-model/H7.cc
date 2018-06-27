/*H7.cc
*lineplanning
*
*implements heuristic 6 for (LP1) NEGLECTING upper frequencies
*uses
*-basis/Load.giv
*-basis/Pool.giv
*-Pool-cost.giv
*
*/

#include <iostream>
#include <fstream>
#include <limits.h>
#include <vector>
#include "../../essentials/config/config.h"

using namespace std;

int main(void)
{

    config::from_file("basis/Config.cnf", false);

/* Definition der Variablen
   In dieser Fassung darf es 500 Kanten und 3000 Linien geben.*/

   int fk=0;
   int lk=0;
   int temp;
   bool test;
   
    vector<int> f_min, length, order, Solution;
    vector<double> cost, g;
    
    vector<vector<int> > L;

   size_t pos;
   string text;

   /*read in minimum frequencies*/
    ifstream load (config::get_string_value("default_loads_file").c_str());

    while (!load.eof())
    {
       getline(load,text);
       if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57){
//       cout<<text<<endl;
       for(int j=0; j<=1; j++){
       pos=text.find(";");
       text=text.substr(pos+1);}
       f_min.push_back(atoi(text.c_str()));
//       cout<<f_min[fk]<<endl;
       fk++;}
    }

    cout<<"Read in "<<fk<<" edges."<<endl;

    load.close();


    /*read in pool*/
    ifstream pool (config::get_string_value("default_pool_file").c_str());
    int old_line_id = -1;
    while(!pool.eof())
    {
       getline(pool,text);
//       cout<<text<<endl;
       if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57){
       lk=atoi(text.c_str());
       pos=text.find(";");
       text=text.substr(pos+1);
       temp=atoi(text.c_str());
       pos=text.find(";");
       text=text.substr(pos+1);
       if (lk!=old_line_id)
       {
            old_line_id = lk;
            L.push_back(vector<int>());
            L[lk-1].push_back(atoi(text.c_str()));
       }
       else
            L[lk-1].push_back(atoi(text.c_str()));}
    }

    pool.close();

    cout<<"Read in "<<lk<<" lines."<<endl;

    /*EINLESEN DER KOSTEN UND L�NGEN*/

    /*read in costs and lengths*/
    ifstream poolcost(config::get_string_value("default_pool_cost_file").c_str());
    while(!poolcost.eof())
    {
       getline(poolcost,text);
       if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57){
//       cout<<text<<endl;
       temp=atoi(text.c_str());
       pos=text.find(";");
       text=text.substr(pos+1);
       length.push_back(atof(text.c_str()));
       pos=text.find(";");
       text=text.substr(pos+1);
       cost.push_back(atof(text.c_str()));}
    }

    cout<<"Read in costs and lengths for "<<temp<<" lines."<<endl;

    poolcost.close();

    cout<<"Finished reading."<<endl;


    /*IMPLEMENTIERUNG DER HEURISTIK 7*/

	
    //STEP1
    Solution.resize(lk);
    g.resize(lk);
    fill(Solution.begin(), Solution.end(), 0);
  
	while(true)
	{  
    //STEP2
 
    int max = 0;

    for (int i=0; i<=fk-1; i++) 
	if (f_min[i]>f_min[max])
	    max=i;

//	cout<<"Maximaler Bedarf auf:"<<max<<endl;
		
    if(f_min[max]==0)
    {
	cout<<"Found a solution by heuristic 7:"<<endl;
	for (int i=0; i<=lk-1; i++)
	    cout<<"Line "<<i+1<<": "<<Solution[i]<<endl;
	cout<<endl;

	double ergebnis=0;
	for (int i=0; i<=lk-1;i++)
	    ergebnis+=Solution[i]*cost[i];

	cout<<"KOSTEN INSGESAMT: "<<ergebnis<<endl;

    cout<<"Total costs:"<<endl;

    ofstream out(config::get_string_value("default_lines_file").c_str());

    cout<<"# feasible line concept"<<endl;
    out<<"# feasible line concept"<<endl;

    cout<<"# won by heuristic 7"<<endl;
    out<<"# won by heuristic 7"<<endl;


    cout<<"# line-id; edge-order; edge-id; frequency"<<endl;
    out<<"# line-id; edge-order; edge-id; frequency"<<endl;

    int number=1;

    for (int i=0; i<=lk-1; i++)
    {
	//if (Solution[i]>0)
	//{
	    int j=0;
	    while(j < L[i].size())
	    {
//		cout<<number<<";"<<j+1<<";"<<L[i][j]<<";"<<Solution[i]<<endl;
		out<<number<<";"<<j+1<<";"<<L[i][j]<<";"<<Solution[i]<<endl;
		j++;
	    }
	    number++;
	//}
    }

    out.close();


	return 0;
    }
    //STEP3

    for (int i=0; i<=lk-1; i++)
    {
	bool ok=false;
	int tempsum=0;
	int j=0;

	while(j < L[i].size())
	{
	    if(L[i][j]-1==max) ok=true;
	    j++;
	}
	j=0;
	if(ok)
	{
	    while(j < L[i].size())
	    {
		if(f_min[L[i][j]-1]>0)
		    tempsum++;
		j++;
	    }
	    g[i]= double(cost[i])/tempsum;
//		cout<<"g["<<i<<"] ist "<<g[i]<<endl;
	}
	else
	    {g[i]=-1;
//		cout<<"g["<<i<<"] ist "<<g[i]<<endl;
		}
    }
	
    int min=-1;
    for (int i=0; i<=lk-1; i++)
    {
	if (g[i]!=-1)
		{
		if (min==-1) 
			min=i;
		else
			if (g[min]>g[i])
				min=i;
		}
	}
	
	bool ok=false;
	for (int i=0; i<=lk-1; i++)
		if (g[i]!=-1) ok=true;
		
	if (!ok)
		{
		cout<<"Problem is infeasible!"<<endl;
		return 0;
		}
	
//	cout<<"Beste Linie ist "<<min<<endl;
	
	//STEP 4
	
	int fmin=INT_MAX;

//	cout<<"fmin DEBUG: "<<fmin<<endl;

	int j=0;
	while (j < L[min].size())
	{
		if (fmin>f_min[L[min][j]-1]&&f_min[L[min][j]-1]>0)
			fmin=f_min[L[min][j]-1];
		j++;
//		cout<<"fmin DEBUG: "<<f_min[L[min][j]-1]<<endl;
		}
	
	Solution[min]+=fmin;
//	cout<<"Belege sie mit "<<fmin<<endl;
	
	j=0;
	
	while (j < L[min].size())
		{
		if (fmin>f_min[L[min][j]-1])
			f_min[L[min][j]-1]=0;
		else
			f_min[L[min][j]-1]-=fmin;
		j++;
		}
	
//	for (int i=0; i<=fk-1; i++)
//		cout<<"Neuer Bedarf auf Kante "<<i<<": "<<f_min[i]<<endl;
		
	}




    return -1;    
}

