.PHONY: all dependencies clean

SRC_FILES=$(shell find ../src -type f)
CORE_LOCATION=../../../../core/cpp
CORE_LIB=$(CORE_LOCATION)/lib/core.o

all: dependencies solve

dependencies:
	make -C $(CORE_LOCATION) all

solve: $(CORE_LIB) $(SRC_FILES)
	g++ -std=c++11 -o solve ../src/main.cpp $(CORE_LIB)

clean:
	rm -f solve
