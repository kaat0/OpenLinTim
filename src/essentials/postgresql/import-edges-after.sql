\.
\set datasetquoted '\'' :dataset '\''

DELETE FROM edge WHERE dataset = :datasetquoted;

INSERT INTO edge (dataset, id, left_stop, right_stop, length, min_travel_time, max_travel_time) SELECT :datasetquoted, id,  left_stop, right_stop, length, min_travel_time, max_travel_time FROM _tmp_edge;

COMMIT;
