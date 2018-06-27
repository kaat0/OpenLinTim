\.
\set datasetquoted '\'' :dataset '\''

DELETE FROM od WHERE dataset = :datasetquoted;

INSERT INTO od (dataset, left_stop, right_stop, customers) SELECT :datasetquoted, left_stop, right_stop, customers FROM _tmp_od;

COMMIT;
