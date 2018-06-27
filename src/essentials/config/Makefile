-include ../settings/Makefile.env

.PHONY: clean all

../string-helper/string_helper.o:
	make -C ../string-helper string_helper.o

all: config_cmd

config_cmd: ../string-helper/string_helper.o config.o

config_java:
	ant -q

clean: 
	rm -f config.o config_cmd Config.class
