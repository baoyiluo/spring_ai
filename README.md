# spring_ai
CREATE INDEX IF NOT EXISTS index_vs_source_checksum
ON public.rag_vector_store ((metadata->>'source'), (metadata->>'checksum'))

CREATE INDEX IF NOT EXISTS rag_vector_store_index
    ON public.rag_vector_store USING hnsw
    (Embedding vector_cosine_ops)
    TABLESPACE pg_default;
