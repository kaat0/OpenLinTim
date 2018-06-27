/*H6.cc
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
#include <vector>
#include <algorithm>

#include "../../essentials/config/config.h"

using namespace std;

void sort(vector<double> *wert, vector<int> *order, double length);

int main(void)
{
   config::from_file("basis/Config.cnf", false);

       
   int fk=0;
   int lk=0;

   int temp;
   vector<vector<int> > L;
   vector<double> cost, length, g;
   vector<int> f_min, order, Solution;
   
   bool test;
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


    /*implementation of heuristic 6*/

    //STEP1
    Solution.resize(lk);
    fill(Solution.begin(), Solution.end(), 0);
    
    //STEP2
    order.resize(lk);
    g.resize(lk);
    for (int i=0; i<=lk-1; i++) 
    {
       g[i]=(double)cost[i]/length[i];
       order[i]=i+1;
    }
    
    //turn around order of equal elements
    //for consistency with lecture notes
    for (int i=0; i<=(lk-1)/2; i++)
    {
       double help;
       help=g[i];
       g[i]=g[lk-1-i];
       g[lk-1-i]=help;

       help=order[i];
       order[i]=order[lk-1-i];
       order[lk-1-i]=help;
    }


    sort(&g,&order,lk);
/*
    for (int i=0; i<=lk-1; i++)
	cout<<order[i]<<" ";
    cout<<endl;
*/
    //STEP3+4+5

    bool repeat=true;

    while(repeat)
    {
       test=false;
    for (int i=0; i<=fk-1; i++) if(f_min[i]!=0) test=true;

    if (test)
    {
       int max=0;
       int i=0;
       int j=0;
       int Kante=0;
       bool found=false;
       while(i<lk&&!found)
       {
	  j=0;
	  while(j < L[order[i]-1].size())
	  {
	     if (f_min[L[order[i]-1][j]-1]>max)
	     {
		max=f_min[L[order[i]-1][j]-1]; 
		Kante=L[order[i]-1][j];
	     }
	     j++;
	     if(max!=0) found=true;
	  }
	  i++;
       }
       if (!found) {cout<<"NO FEASIBLE SET POSSIBLE"<<endl;return -1;}
       i--;
       //     cout<<max<<" Bedarf auf Kante "<<Kante<<" gedeckt durch Linie "<<order[i]<<endl;
       Solution[order[i]-1]+=max;
       j=0;
       while(j < L[order[i]-1].size())
       {
	  f_min[L[order[i]-1][j]-1]-=max;
	  if (f_min[L[order[i]-1][j]-1]<0) f_min[L[order[i]-1][j]-1]=0;
	  /* for (int z=0; z<=fk-1; z++) cout<<f_min[z]<<"-";
	     cout<<endl;*/
	  j++;
       }
    }
    else repeat=false;
    }
    cout<<"Found a solution by heuristic 6:"<<endl;
    for (int i=0; i<=lk-1; i++)
       cout<<"Line "<<i+1<<": "<<Solution[i]<<endl;
    cout<<endl;

    double ergebnis=0;
    for (int i=0; i<=lk-1; i++)
	ergebnis+= Solution[i]*cost[i];
    cout<<"Total cost: "<<ergebnis<<endl;

    cout<<"Writing to file:"<<endl;

    ofstream out(config::get_string_value("default_lines_file").c_str());

    cout<<"# feasible line concept"<<endl;
    out<<"# feasible line concept"<<endl;

    cout<<"# won by heuristic 6"<<endl;
    out<<"# won by heuristic 6"<<endl;

    cout<<"# line-id; edge-order; edge-id; frequency"<<endl;
    out<<"# line-id; edge-order; edge-id; frequency"<<endl;

    int number=1;

    for (int i=0; i<=lk-1; i++)
    {
	    int j=0;
	    while(j < L[i].size())
	    {
//		cout<<number<<";"<<j+1<<";"<<L[i][j]<<";"<<Solution[i]<<endl;
		out<<number<<";"<<j+1<<";"<<L[i][j]<<";"<<Solution[i]<<endl;
		j++;
	    }
	    number++;
    }

    out.close();

    return 0;
}

//Bubble-Sort-Hilfsfunktion
void sort(vector<double> *wert, vector<int> *order, double length)
{

   int swapi;
   double swapd;
   bool ok=false;
   while(!ok)
   {
      ok=true;
      for (int i=0; i<=length-2; i++)
      if ((*wert)[i]>(*wert)[i+1])
      {
	 swapd=(*wert)[i];
	 (*wert)[i]=(*wert)[i+1];
	 (*wert)[i+1]=swapd;

	 swapi=(*order)[i];
	 (*order)[i]=(*order)[i+1];
	 (*order)[i+1]=swapi;
	 ok=false;
      }
   }
}
