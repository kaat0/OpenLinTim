BEGIN;

SET SEARCH_PATH TO lintim,public;

CREATE TEMPORARY TABLE _tmp_edge (LIKE edge INCLUDING DEFAULTS) ON COMMIT DROP;
ALTER TABLE _tmp_edge DROP COLUMN dataset;

\copy _tmp_edge (id, left_stop, right_stop, length, min_travel_time, max_travel_time) FROM STDIN WITH (FORMAT text, DELIMITER ';');
