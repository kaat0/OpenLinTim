#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include "../../essentials/config/config.h"

using namespace std;

int main(int argc, char** argv)
{
	config::from_file("basis/Config.cnf", false);

	if (argc != 2)
	{
		cout<<"Usage: ./change_upper_bounds FAKTOR\n";
		abort();
	}

	double faktor = atof(argv[1]);
	
	ifstream load (config::get_string_value("default_loads_file").c_str());
	ofstream newload ("basis/newload.giv");

	string text;
	int edgeid, iload, l,u;
	size_t pos;
	while (!load.eof())
	{
		getline(load,text);
		if(text!=""&&(text.c_str())[0]>=48&&(text.c_str())[0]<=57)
		{
		stringstream s;

		edgeid = atoi(text.c_str());
		pos=text.find(";");
		text=text.substr(pos+1);

		iload = atoi(text.c_str());
		pos=text.find(";");
		text=text.substr(pos+1);

		l = atoi(text.c_str());
		pos=text.find(";");
		text=text.substr(pos+1);

		u = atoi(text.c_str());
		u *= faktor;
		
		s<<edgeid<<"; "<<iload<<"; "<<l<<"; "<<u;
		newload<<s.str()<<"\n";
		}
		else
		{
			newload<<text<<"\n";
		}
	}
	load.close();
	newload.close();
}
