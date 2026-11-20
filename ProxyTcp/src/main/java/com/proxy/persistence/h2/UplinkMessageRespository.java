package com.proxy.persistence.h2;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.proxy.persistence.h2.model.UplinkMessage;

@Repository
public interface UplinkMessageRespository extends CrudRepository<UplinkMessage,Long>{

	@Query(value = "select v from UplinkMessage v where v.messageId = :messageId and v.deviceId = :deviceId")
	UplinkMessage queryUplinkMessage(@Param("deviceId") String deviceId, @Param("messageId") long messageId);
	
	@Query(value = "select v from UplinkMessage v")
	List<UplinkMessage> queryAllUplinkMessages();

	@Transactional
	@Modifying
	@Query(value = "delete UplinkMessage v where v.deviceId = :deviceId and v.messageId = :messageId")
	int deleteUplinkMessage(@Param("deviceId") String deviceId, @Param("messageId") long messageId);

	@Transactional
	@Modifying
	@Query(value = "delete UplinkMessage v where v.deviceId = :deviceId")
	int deleteUplinkMessageByVehicle(@Param("deviceId") String deviceId);
	
	@Transactional
	@Modifying
	@Query(value = "delete UplinkMessage v where v.receivedTime <= :receivedTime")
	int deleteUplinkMessageLaterThanTime(@Param("receivedTime") Date receivedTime);

}
