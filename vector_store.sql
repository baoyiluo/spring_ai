CREATE EXTENSION IF NOT EXISTS vector;
drop table vector_store;
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(3072)
);