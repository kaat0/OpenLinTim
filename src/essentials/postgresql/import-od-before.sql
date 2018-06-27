BEGIN;

SET SEARCH_PATH TO lintim,public;

CREATE TEMPORARY TABLE _tmp_od (LIKE od INCLUDING DEFAULTS) ON COMMIT DROP;
ALTER TABLE _tmp_od DROP COLUMN dataset;

\copy _tmp_od (left_stop, right_stop, customers) FROM STDIN WITH (FORMAT text, DELIMITER ';');
