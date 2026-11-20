package com.proxy.persistence.h2;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.proxy.persistence.h2.model.SendFailureMessage;

@Repository
public interface SendFailureMessageRespository extends CrudRepository<SendFailureMessage,Long>{

	@Query(value = "select v from SendFailureMessage v where v.deviceId = :deviceId and v.messageId = :messageId")
	SendFailureMessage querySendFailureMessageByMessageId(@Param("deviceId") String deviceId, @Param("messageId") long messageId);
	
	@Transactional
	@Modifying
	@Query(value = "delete SendFailureMessage v where v.deviceId = :deviceId and v.messageId = :messageId")
	int deleteSendFailureMessageByMessageId(@Param("deviceId") String deviceId, @Param("messageId") long messageId);
	
	
	@Query(value = "select v from SendFailureMessage v")
	List<SendFailureMessage> queryAllSendFailureMessages();
}
