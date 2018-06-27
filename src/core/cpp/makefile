.PHONY: all
all:
	mkdir -p lib
	make lib/core.o
	
lib/core.o: $(shell find include -type f)
	g++ -c -std=c++11 -o lib/core.o src/core.cpp

.PHONY: clean
clean:
	rm -f lib/core.o
