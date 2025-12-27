-- Run this CQL (Spring Boot can auto-create tables if schema-action is create_if_not_exists)
CREATE KEYSPACE IF NOT EXISTS s3auth WITH replication = {'class': 'SimpleStrategy', 'replication_factor':1};

CREATE TABLE IF NOT EXISTS s3auth.access_keys (
  access_key_id text PRIMARY KEY,
  user_id text,
  secret_ciphertext blob,
  encryption_metadata text,
  description text,
  created_at timestamp,
  status text
);