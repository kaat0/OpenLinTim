CREATE SCHEMA lintim;
SET SEARCH_PATH TO lintim,public;

CREATE TABLE dataset
(
      id TEXT PRIMARY KEY NOT NULL
    , info TEXT
);

CREATE TABLE demand
(
      dataset TEXT NOT NULL
    , id integer NOT NULL
    , short_name text
    , long_name text
    , x double precision
    , y double precision
    , customers double precision
    , CONSTRAINT demand_pk
        PRIMARY KEY (dataset, id)
    , CONSTRAINT demand_dataset_fk
        FOREIGN KEY (dataset)
        REFERENCES dataset (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE stop
(
      dataset TEXT NOT NULL
    , id integer NOT NULL
    , short_name text
    , long_name text
    , x double precision
    , y double precision
    , CONSTRAINT stop_pk
        PRIMARY KEY (dataset, id)
    , CONSTRAINT stop_dataset_fk
        FOREIGN KEY (dataset)
        REFERENCES dataset (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE edge
(
      dataset TEXT NOT NULL
    , id integer NOT NULL
    , left_stop integer NOT NULL
    , right_stop INTEGER NOT NULL
    , length double precision
    , min_travel_time interval
    , max_travel_time interval
    , CONSTRAINT edge_pk
        PRIMARY KEY (dataset, id)
    , CONSTRAINT edge_left_stop_fk
        FOREIGN KEY (dataset, left_stop)
        REFERENCES stop (dataset, id)
        ON DELETE CASCADE ON UPDATE CASCADE
    , CONSTRAINT edge_right_stop_fk
        FOREIGN KEY (dataset, right_stop)
        REFERENCES stop (dataset, id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE od
(
      dataset TEXT NOT NULL
    , left_stop integer NOT NULL
    , right_stop INTEGER NOT NULL
    , customers double precision
    , CONSTRAINT od_left_stop_fk
        FOREIGN KEY (dataset, left_stop)
        REFERENCES stop (dataset, id)
        ON DELETE CASCADE ON UPDATE CASCADE
    , CONSTRAINT od_right_stop_fk
        FOREIGN KEY (dataset, right_stop)
        REFERENCES stop (dataset, id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE traffic_line
(
      dataset TEXT NOT NULL
    , id integer NOT NULL
    , short_name text
    , long_name text
    , edges integer[] NOT NULL
    , length double precision
    , cost double precision
    , frequency double precision
    , CONSTRAINT traffic_line_pk
        PRIMARY KEY (dataset, id)
    , CONSTRAINT traffic_line_dataset_fk
        FOREIGN KEY (dataset)
        REFERENCES dataset (id)
        ON DELETE CASCADE ON UPDATE CASCADE
    -- TO DO referential integrity for edges in array
    -- Idea: only allow traffic lines to be inserted when
    -- the underlying network is somehow marked as "fixed"
    -- (and prevent UPDATE and DELETE on those tables then)
);
