\.
\set datasetquoted '\'' :dataset '\''

DELETE FROM demand WHERE dataset = :datasetquoted;

INSERT INTO demand (dataset, id, short_name, long_name, x, y, customers) SELECT :datasetquoted, id,  trim(short_name), trim(long_name), x, y, customers FROM _tmp_demand;

COMMIT;
