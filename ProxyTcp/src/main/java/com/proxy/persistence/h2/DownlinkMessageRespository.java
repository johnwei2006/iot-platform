package com.proxy.persistence.h2;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.proxy.Constants.Status;
import com.proxy.persistence.h2.model.DownlinkMessage;


@Repository
public interface DownlinkMessageRespository extends CrudRepository<DownlinkMessage,Long>{

	@Query(value = "select v from DownlinkMessage v where v.deviceId = :deviceId and v.messageId = :messageId")
	DownlinkMessage queryDownlinkMessageByMessageId(@Param("deviceId") String deviceId, @Param("messageId") long messageId);
	
	@Transactional
	@Modifying
	@Query(value = "delete DownlinkMessage v where v.deviceId = :deviceId and v.messageId = :messageId")
	int deleteDownlinkMessageByMessageId(@Param("deviceId") String deviceId, @Param("messageId") long messageId);
	
	@Transactional
	@Modifying
	@Query(value = "delete DownlinkMessage v where v.status = :status")// + Status.ACKNOWLEDGED)
	int deleteAllAckDownlinkMessage(@Param("status") Status status);
	
	@Query(value = "select v from DownlinkMessage v where v.status = :status")
	List<DownlinkMessage> queryAllNotAckRecords(@Param("status") Status status);
	
	@Transactional
	@Modifying
	@Query(value = "delete DownlinkMessage v where v.resendCount >= :resendCount")
	int deleteOverResendMessages(@Param("resendCount") int resendCount);
	
	@Query(value = "select v from DownlinkMessage v where v.resendCount >= :resendCount")
	List<DownlinkMessage> queryOverResendMessages(@Param("resendCount") int resendCount);
	
	@Query(value = "select v from DownlinkMessage v")
	List<DownlinkMessage> queryAllDownlinkMessages();
	
	@Query(value = "select v from DownlinkMessage v where v.status != :status")
	List<DownlinkMessage> queryAllDownlinkMessagesExceptStatus(@Param("status") Status status);

	@Query(value = "select v from DownlinkMessage v where v.status != :status and v.messageTime >= :since")
	List<DownlinkMessage> queryAllDownlinkMessagesExceptStatusSince(@Param("status") Status status, @Param("since") Date since);

	@Transactional
	@Modifying
	@Query(value = "update DownlinkMessage v set v.status = :failed where v.status != :failed and v.messageTime < :expireBefore")
	int updateExpiredDownlinkMessagesFailed(@Param("failed") Status failed, @Param("expireBefore") Date expireBefore);

	@Query(value = "select v from DownlinkMessage v where v.deviceId = :deviceId and v.status != :status")
	List<DownlinkMessage> queryDeviceDownlinkMessagesExceptStatus(@Param("deviceId") String deviceId, @Param("status") Status status);

	@Query(value = "select v from DownlinkMessage v where v.status = :status")
	List<DownlinkMessage> queryAllDownlinkMessagesWithStatus(@Param("status") Status status);

	@Transactional
	@Modifying
	@Query(value = "delete DownlinkMessage v where v.resendCount >= :resendCount")
	int deleteMessage(@Param("resendCount") int resendCount);
}
