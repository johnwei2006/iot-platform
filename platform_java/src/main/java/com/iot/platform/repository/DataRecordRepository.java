package com.iot.platform.repository;

import com.iot.platform.model.DataRecord;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/** 数据记录访问接口（MyBatis-Plus 适配器实现）。 */
public interface DataRecordRepository {

    List<DataRecord> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    List<DataRecord> findByDeviceIdAndCreatedAtBetween(String deviceId, Date from, Date to);

    Optional<DataRecord> findFirstByDeviceIdOrderByCreatedAtDesc(String deviceId);

    DataRecord save(DataRecord record);
}
