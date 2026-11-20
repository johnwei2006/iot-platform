package com.proxy.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.proxy.persistence.mongodb.model.SendFailureMessage;

@Repository
public interface SendFailureMessageRepository extends MongoRepository<SendFailureMessage, String>, SendFailureMessageRepositoryCustom{
}
