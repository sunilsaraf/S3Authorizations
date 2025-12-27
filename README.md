# AWS S3 Authorization Service (Java / Spring Boot)

Overview
- This project provides an authorization microservice that can create and manage AWS S3-style Access Keys (Access Key ID + Secret Access Key), persist metadata to Apache Cassandra, and return the secret to the caller exactly once (AWS-style).
- Secrets are stored encrypted. The project supports two storage-encryption modes:
  - Envelope encryption using AWS KMS (recommended for production).
  - Local AES-GCM master key (development / edge cases).
- REST API built with Spring Boot. Data layer uses Spring Data Cassandra.

Highlights / design goals
- Return secret only on creation; secret is never returned again.
- Secrets encrypted at rest (KMS preferred).
- Idempotent and auditable operations; basic validation and error handling.
- Production readiness notes in README (securing keys, KMS, monitoring, backups).

Quick start (development using local Cassandra)
1. Start Cassandra (or use the docker-compose provided):
   - docker compose up -d cassandra
2. Configure `application.yml` (see `src/main/resources/application.yml`).
   - For local development with AES fallback, set `APP_MASTER_KEY` env var to a base64-encoded 32-byte key.
   - For KMS, provide AWS credentials and KMS key ARN, and set `encryption.kms.enabled: true`.
3. Build and run:
   - mvn -DskipTests package
   - java -jar target/s3auth-0.0.1-SNAPSHOT.jar
4. Example request to create a key:
   - POST /api/v1/keys
   - Body:
     {
       "userId": "alice",
       "description": "app-service key"
     }
   - Response returns `accessKeyId` and `secretAccessKey` (secret shown once).

Security & production recommendations
- Always enable AWS KMS (`encryption.kms.enabled: true`) and provide a KMS key for envelope encryption.
- Use IAM roles for EKS / EC2 or workload identity for the service instead of static AWS credentials.
- Enable network-level restrictions (VPC, private subnets).
- Use secrets manager to store `APP_MASTER_KEY` if AES fallback is used.
- Enable logging, structured tracing, metrics (Prometheus) and rate-limiting (API GW) in front of the service.
- Rotate master keys and KMS keys according to policy; implement key rotation for stored secrets by re-wrapping.
- Back up Cassandra and enable multi-AZ clusters.

API summary
- POST /api/v1/keys
  - Create a new access key. Returns accessKeyId and secretAccessKey (secret only here).
- GET /api/v1/keys/{accessKeyId}
  - Retrieve metadata (userId, createdAt, status) — secret is not returned.
- DELETE /api/v1/keys/{accessKeyId}
  - Revoke/delete key.
- GET /api/v1/keys?userId=...
  - List keys for a user (metadata only).

Cassandra table (CQL)
- A minimal table is created via Spring Data mapping. Example schema backing:
  CREATE TABLE IF NOT EXISTS s3auth.access_keys (
    access_key_id text PRIMARY KEY,
    user_id text,
    secret_ciphertext blob,
    encryption_metadata text,
    description text,
    created_at timestamp,
    status text
  );

Notes
- This repo is a scaffold and includes a local AES fallback for ease of development. For production, enable KMS and secure all credentials.
- See code comments for extension points (rotation, audit logging, HSM/KMS integration).
