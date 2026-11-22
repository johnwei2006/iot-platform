package com.proxy.persistence.h2;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.proxy.persistence.h2.model.GatewayReconnectRecord;

@Repository
public interface GatewayReconnectRecordRespository
		extends PagingAndSortingRepository<GatewayReconnectRecord, Long>,
		org.springframework.data.repository.CrudRepository<GatewayReconnectRecord, Long> {

	List<GatewayReconnectRecord> findByDeviceIdOrderByConnectTimeDesc(String deviceId, Pageable pageable);

	long countByDeviceId(String deviceId);
}
