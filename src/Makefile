.PHONY: clean

all:
	make -C timetabling ptn2ean
	ant -q

clean:
	make -C config clean
	make -C string-helper clean
	make -C timetabling clean
	make -C line-planning clean
	make -C roll_out clean
	make -C Delay_Management clean
	make -C Delay_Generator clean
	make -C Trips clean
	make -C ShortestPaths clean
	make -C Evaluation/Delay_Management/ clean
	make -C javalib clean
	make -C statistic clean
