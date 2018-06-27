.PHONY: all

all: config statistic evaluation

config:
	make -C ../../essentials/config all

statistic:
	make -C ../../essentials/statistic statistic_cmd

evaluation: evaluation.cpp evaluation.h main.cpp
	make -C ../../essentials/shortest-paths/
	g++ -o evaluation evaluation.cpp main.cpp ../../essentials/config/config.o \
	../../essentials/statistic/statistic.o ../../essentials/string-helper/string_helper.o \
	../../essentials/shortest-paths/fheap.o ../../essentials/shortest-paths/shortest_paths.o -O3

clean:
	rm -f evaluation.o main.o
