package com.proxy.persistence.h2;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.proxy.persistence.h2.model.GatewayTokenRequestRecord;

@Repository
public interface GatewayTokenRequestRecordRespository
		extends PagingAndSortingRepository<GatewayTokenRequestRecord, Long> {

	List<GatewayTokenRequestRecord> findByDeviceIdOrderByRequestTimeDesc(String deviceId, Pageable pageable);

	long countByDeviceId(String deviceId);
}
