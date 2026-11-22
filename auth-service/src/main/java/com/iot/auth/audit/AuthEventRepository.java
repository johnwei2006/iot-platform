package com.iot.auth.audit;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuthEventRepository extends MongoRepository<AuthEvent, String> {
}
