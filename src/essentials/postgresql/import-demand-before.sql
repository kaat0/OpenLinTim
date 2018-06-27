BEGIN;

SET SEARCH_PATH TO lintim,public;

CREATE TEMPORARY TABLE _tmp_demand (LIKE demand INCLUDING DEFAULTS) ON COMMIT DROP;
ALTER TABLE _tmp_demand DROP COLUMN dataset;

\copy _tmp_demand (id, short_name, long_name, x, y, customers) FROM STDIN WITH (FORMAT text, DELIMITER ';');
