\set datasetquoted '\'' :dataset '\''
INSERT INTO lintim.dataset(id, info) SELECT :datasetquoted, 'auto-inserted at ' || now();
