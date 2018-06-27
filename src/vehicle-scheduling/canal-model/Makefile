PROGRAMPATH=.

all: data_mdm1 data_mdm2 data_am data_tm data_nm
 

*.class: *.java
	javac -Xlint:all -classpath .:../../essentials/shortest-paths/src:../../essentials/config *.java

compile: *.class
	
data_mdm1: *.class

data_mdm2: *.class

data_am: *.class 

data_tm: *.class

data_nm: *.class 

TF-mdm1: *.class 
	java CalculateVS mdm1 
	./vehicle-schedules.sh mdm1
	
TF-mdm2: *.class 
	java CalculateVS mdm2 
	./vehicle-schedules.sh mdm2

TF-am: *.class 
	java CalculateVS am
	./vehicle-schedules.sh am

TF-tm: *.class 
	java CalculateVS tm
	./vehicle-schedules.sh tm

TF-nm: *.class 
	java CalculateVS nm
	./vehicle-schedules.sh nm

tf-mdm1:
	./vehicle-schedules.sh mdm1

tf-mdm2: 
	./vehicle-schedules.sh mdm2

tf-am:
	./vehicle-schedules.sh am

tf-tm:
	./vehicle-schedules.sh tm

tf-nm:
	./vehicle-schedules.sh nm

tf:
	./calculateVS.sh

clean:
	rm -vf *.class
	rm -vf data_mdm1
	rm -vf data_mdm2
	rm -vf data_am
	rm -vf data_tm
	rm -vf data_nm
	rm -vf data_vehicle_flow
	rm -vf mdm1_generated.mos
	rm -vf mdm2_generated.mos
