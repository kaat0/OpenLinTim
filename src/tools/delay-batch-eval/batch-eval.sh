#!/usr/bin/env bash

### BATCH-EVALUATION of delay-management, e.g. to test the robustness of a timetable
### heavily based on ../delay-simulation/simulation.sh

set -e # exit if an error occurs during the execution of this script

# directory with source code, relative to from where this script is called
SRC_DIR="../../src"

source ${SRC_DIR}/base.sh


if [ ! -d delay-management/batch-eval ]; then
	mkdir delay-management/batch-eval
fi

#statparams="dm_objective_value dm_delayed_events dm_total_events dm_delayed_events_percentage dm_total_delay dm_average_delay_among_all dm_average_delay_among_delayed dm_weighted_total_delay dm_increase_of_travel_time dm_badly_delayed_events dm_badly_delayed_events_percentage dm_total_bad_delay dm_weighted_bad_delay dm_missed_connections dm_total_connections dm_missed_connections_percentage dm_missed_used_connections dm_missed_connections_delay dm_swapped_headways dm_total_headways dm_swapped_headways_percentage dm_delayed_trips dm_total_trips dm_delayed_trips_percentage dm_badly_delayed_trips dm_badly_delayed_trips_percentage";



# compile all necessary stuff
make --quiet -C "${SRC_DIR}"/essentials/config config_cmd
"${SRC_DIR}"/delay-management/ip-based/Solve/update_symlink.sh
ant -q -f "${SRC_DIR}"/delay-management/ip-based/build.xml
ant -q -f "${SRC_DIR}"/delay-management/delay-generator/build.xml



# check whether config command line interface can be accessed
config="${SRC_DIR}"/essentials/config/config_cmd
if [[ ! -f "${config}" ]]; then
	echo "${config} does not exist" >&2
	exit 1
fi
if [[ ! -x "${config}" ]]; then
	echo "cannot execute ${config}" >&2
	exit 1
fi



# control parameters for the plots
simulations=$("${config}" -c basis/Config.cnf -s plot_delays_number_of_simulations -t integer -u)
# steps=$("${config}" -c basis/Config.cnf -s plot_delays_number_of_steps_in_animation -t integer -u)
delayed_stops=$("${config}" -c basis/Config.cnf -s default_delayed_stops_file -t string -u)
# dot_output=$("${config}" -c basis/Config.cnf -s default_delay_graph_file -t string -u)
# graph_output=$(echo "${dot_output}" | sed 's/\.dot$/\.gif/')
# animation_dir=$("${config}" -c basis/Config.cnf -s plot_delays_animation_output_dir -t string -u)
debug=$("${config}" -c basis/Config.cnf -s DM_debug -u -t bool)
verbose=$("${config}" -c basis/Config.cnf -s DM_verbose -u -t bool)
reuse_delays=$("${config}" -c basis/Config.cnf -s DM_reuse_batch_delays -u -t bool)



# delay-generator details for saving and re-using batch delays
delays_generator=$("${config}" -c basis/Config.cnf -s delays_generator -t string -u)
delays_events=$("${config}" -c basis/Config.cnf -s delays_events -t bool -u)
delays_activities=$("${config}" -c basis/Config.cnf -s delays_activities -t bool -u)
delays_count=$("${config}" -c basis/Config.cnf -s delays_count -t string -u)
delays_count_is_absolute=$("${config}" -c basis/Config.cnf -s delays_count_is_absolute -t bool -u)
delays_station_id_for_delays=$("${config}" -c basis/Config.cnf -s delays_station_id_for_delays -t integer -u)
delays_edge_id_for_delays=$("${config}" -c basis/Config.cnf -s delays_edge_id_for_delays -t integer -u)
delays_min_delay=$("${config}" -c basis/Config.cnf -s delays_min_delay -t string -u)
delays_max_delay=$("${config}" -c basis/Config.cnf -s delays_max_delay -t string -u)
delays_min_time=$("${config}" -c basis/Config.cnf -s delays_min_time -t string -u)
delays_max_time=$("${config}" -c basis/Config.cnf -s delays_max_time -t string -u)
delays_absolute_numbers=$("${config}" -c basis/Config.cnf -s delays_absolute_numbers -t bool -u)
delays_batch_comment=$("${config}" -c basis/Config.cnf -s DM_batch_delays_comment -t string -u)
foldermd5=$(echo ${delays_generator}--${delays_events}--${delays_activities}--${delays_count}--${delays_count_is_absolute}--${delays_station_id_for_delays}--${delays_edge_id_for_delays}--${delays_min_delay}--${delays_max_delay}--${delays_min_time}--${delays_max_time}--${delays_absolute_numbers} | md5sum | cut -f 1 -d " ")
foldername="${delays_batch_comment}--${foldermd5}"

if [[ -d delay-management/batch-eval/${foldername} ]]
then
  echo "found existing batch delays folder"
else
  echo "did not find existing batch delays folder, creating"
  mkdir delay-management/batch-eval/${foldername}
  echo 'delays_generator; "'${delays_generator}'"' >> delay-management/batch-eval/${foldername}/delays-info.txt
  echo "delays_events; ${delays_events}" >> delay-management/batch-eval/${foldername}/delays-info.txt
  echo "delays_activities; ${delays_activities}" >> delay-management/batch-eval/${foldername}/delays-info.txt
  echo "delays_count; ${delays_count}" >> delay-management/batch-eval/${foldername}/delays-info.txt
  echo "delays_count_is_absolute; ${delays_count_is_absolute}" >> delay-management/batch-eval/${foldername}/delays-info.txt
  echo "delays_station_id_for_delays; ${delays_station_id_for_delays}" >> delay-management/batch-eval/${foldername}/delays-info.txt
  echo "delays_edge_id_for_delays; ${delays_edge_id_for_delays}" >> delay-management/batch-eval/${foldername}/delays-info.txt
  echo "delays_min_delay; ${delays_min_delay}" >> delay-management/batch-eval/${foldername}/delays-info.txt
  echo "delays_max_delay; ${delays_max_delay}" >> delay-management/batch-eval/${foldername}/delays-info.txt
  echo "delays_min_time; ${delays_min_time}" >> delay-management/batch-eval/${foldername}/delays-info.txt
  echo "delays_max_time; ${delays_max_time}" >> delay-management/batch-eval/${foldername}/delays-info.txt
  echo "delays_absolute_numbers; ${delays_absolute_numbers}" >> delay-management/batch-eval/${foldername}/delays-info.txt
fi



# verify parameters
#if [[ ${simulations} -eq 1 ]]; then # we need delayed_stops and dot_output
#	if [[ -z "${delayed_stops}" ]]; then
#		echo "default_delayed_stops_file not set" >&2
#		exit 1
#	fi
#	if [[ -z "${dot_output}" ]]; then
#		echo "default_delay_graph_file not set" >&2
#		exit 1
#	fi
#else # we need animation_dir
#	if [[ -z "${animation_dir}" ]]; then
#		echo "plot_delays_animation_output_dir not set" >&2
#		exit 1
#	fi

#fi



# create temporary files
#if [[ ${steps} -eq 1 ]]; then
#	sum=$(mktemp) || { echo "$0: creation of temporary file failed!" >&2; exit 1; }
#else
#	for i in $(seq -w 1 ${steps}); do
#		sum[$((10#$i))]=$(mktemp) || { echo "$0: creation of temporary file failed!" >&2; exit 1; }
#	done
#fi
#tmpfile=$(mktemp) || { echo "$0: creation of temporary file failed!" >&2; exit 1; }



# roll out (UPDATE: rolling out is not done here, has to be done beforehands, in order not to destroy aperiodic timetables)
# java ${JFLAGS} -classpath "${CLASSPATH}":"${SRC_DIR}"/roll_out:"${SRC_DIR}"/config:"${SRC_DIR}"/javalib/Tools:"${SRC_DIR}"/javalib/EAN Rollout



# start simulation; sum up delays in each pass of the loop
for n in $(seq 1 ${simulations}); do
	# if debug and/or verbose is set, disable debug/verbose output
	# after the first iteration
	if [[ $n -eq 2 ]]; then
		if ((${debug})); then
			echo "DM_debug; false" >> basis/After-Config.cnf
		fi
		if ((${verbose})); then
			echo "DM_verbose; false" >> basis/After-Config.cnf
		fi
	fi

	# generate (or re-use existing) delays
	rm -f  delay-management/Delays-Events.giv delay-management/Delays-Activities.giv
	delayfileactivities="delay-management/batch-eval/${foldername}/Delays-Activities.${n}.giv"
	delayfileevents="delay-management/batch-eval/${foldername}/Delays-Events.${n}.giv"
	echo $delayfileactivities $delayfileevents
	if [[ reuse_delays && -e $delayfileactivities && -e $delayfileevents ]]; then
		cp $delayfileactivities delay-management/Delays-Activities.giv
		cp $delayfileevents delay-management/Delays-Events.giv
	else
		#java ${JFLAGS} -classpath "${CLASSPATH}":"${SRC_DIR}"/delay-management/delay-generator:"${SRC_DIR}"/essentials/config:"${SRC_DIR}"/essentials/dm-helper/Tools:"${SRC_DIR}"/essentials/dm-helper/EAN DelayGenerator
		make dm-delays
	fi
	if [[ reuse_delays && -e $delayfileactivities ]]; then
		cp $delayfileactivities delay-management/Delays-Activities.giv
	else
		cp delay-management/Delays-Activities.giv $delayfileactivities
	fi
	if [[ reuse_delays && -e $delayfileevents ]]; then
		cp $delayfileevents delay-management/Delays-Events.giv
	else
		cp delay-management/Delays-Events.giv $delayfileevents
	fi

	# apply delay management
	java "${JFLAGS[@]}" -classpath "${CLASSPATH}":"${SRC_DIR}"/delay-management/ip-based/Solve:"${SRC_DIR}"/essentials/config:"${SRC_DIR}"/essentials/dm-helper/Tools:"${SRC_DIR}"/essentials/dm-helper/EAN:"${SRC_DIR}"/essentials/statistic SolveDM
	make dm-disposition-timetable-evaluate

	# add results to batch-results file

	statparamsline="#"
	if [[ ${n} -eq 1 ]]; then
		statparams=`${config} -c statistic/statistic.sta -n`
		for statparam in ${statparams}; do
			statparamsline="${statparamsline}; ${statparam}"
		done
		echo ${statparamsline} > "delay-management/dm-batch-eval.csv";
	fi


	statline="${n}"
	for statparam in ${statparams}; do
		paramvalue=$("${config}" -c statistic/statistic.sta -s ${statparam} -u)
		statline="${statline}; ${paramvalue}";
	done
	echo ${statline} >> "delay-management/dm-batch-eval.csv";


	echo "finished simulation $n of ${simulations}"
done



# re-enable debug/verbose output
if ((${debug})); then
	echo "DM_debug; true" >> basis/After-Config.cnf
fi
if ((${verbose})); then
	echo "DM_verbose; true" >> basis/After-Config.cnf
fi
