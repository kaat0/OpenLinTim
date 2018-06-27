BEGIN;

SET SEARCH_PATH TO lintim,public;

CREATE TEMPORARY TABLE _tmp_stop (LIKE stop INCLUDING DEFAULTS) ON COMMIT DROP;
ALTER TABLE _tmp_stop DROP COLUMN dataset;

\copy _tmp_stop (id, short_name, long_name, x, y) FROM STDIN WITH (FORMAT text, DELIMITER ';');
