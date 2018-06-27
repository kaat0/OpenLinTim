\.
\set datasetquoted '\'' :dataset '\''

DELETE FROM stop WHERE dataset = :datasetquoted;

INSERT INTO stop (dataset, id, short_name, long_name, x, y) SELECT :datasetquoted, id,  trim(short_name), trim(long_name), x, y FROM _tmp_stop;

COMMIT;
