#!/usr/bin/env bash
set -e # exit if an error occurs during the execution of this script

# directory with source code, relative to from where this script is called
SRC_DIR="../../src"



# check whether config command line interface can be accessed
config="${SRC_DIR}/essentials/config/config_cmd"
if [[ ! -f "${config}" ]]; then
	echo "${config} does not exist" >&2
	exit 1
fi
if [[ ! -x "${config}" ]]; then
	echo "cannot execute ${config}" >&2
	exit 1
fi



# test whether directory with plots exists
animation_dir=$("${config}" -c basis/Config.cnf -s plot_delays_animation_output_dir -t string -u)
animation_nr=$("${config}" -c basis/Config.cnf -s plot_delays_number_of_steps_in_animation -t string -u)
animation_file=$("${config}" -c basis/Config.cnf -s default_delay_graph_file -t string -u)

if [[ -z "${animation_dir}" || ! -d "${animation_dir}" ]]; then
	echo "${animation_dir} does not exist" >&2
	exit 1
fi

if [[ "${animation_nr}" == "1" ]]; then
	neato -n -Tps "${animation_file}" -o "${animation_file}".ps
	animate "${animation_file}".ps
else
	for file in "${animation_dir}"/*
	do
		case "${file}" in
		*.dot) neato  -n -Tps "${file}" -o "${file}".ps
		esac
	done
	# open plots as slideshow with gqview ...
	animate -delay 10 "${animation_dir}"/delayedstops_*.dot.ps
	# ... and delete plots afterwards
	# rm -f "${animation_dir}"/*
fi





