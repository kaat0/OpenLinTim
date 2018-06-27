\set datasetquoted '\'' :dataset '\''
CREATE TEMPORARY TABLE t AS
SELECT id, short_name, long_name, /*x, y,*/
  ST_X(ST_Transform(ST_SetSRID(ST_Point(y, x), 4326), :srid)),
  ST_Y(ST_Transform(ST_SetSRID(ST_Point(y, x), 4326), :srid))
FROM lintim.stop
WHERE dataset = :datasetquoted
ORDER BY id
;
\copy (SELECT * FROM t) TO STDOUT WITH (FORMAT text, DELIMITER ';');
