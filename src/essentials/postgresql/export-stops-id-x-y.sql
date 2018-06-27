\set datasetquoted '\'' :dataset '\''
CREATE TEMPORARY TABLE t AS
SELECT id, x, y
FROM lintim.stop
WHERE dataset = :datasetquoted
ORDER BY id
;
\copy (SELECT * FROM t) TO STDOUT WITH (FORMAT text, DELIMITER ';');
