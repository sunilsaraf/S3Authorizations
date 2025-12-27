package com.example.s3auth.repository;

import com.example.s3auth.model.AccessKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessKeyRepository extends CassandraRepository<AccessKey, String> {
    List<AccessKey> findByUserId(String userId);
}