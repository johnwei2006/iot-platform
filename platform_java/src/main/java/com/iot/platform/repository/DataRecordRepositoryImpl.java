package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.DataRecordMapper;
import com.iot.platform.model.DataRecord;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DataRecordRepositoryImpl implements DataRecordRepository {

    private final DataRecordMapper mapper;

    public DataRecordRepositoryImpl(DataRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<DataRecord> findByDeviceIdOrderByCreatedAtDesc(String deviceId) {
        return mapper.selectList(Wrappers.<DataRecord>lambdaQuery()
                .eq(DataRecord::getDeviceId, deviceId)
                .orderByDesc(DataRecord::getCreatedAt));
    }

    @Override
    public List<DataRecord> findByDeviceIdAndCreatedAtBetween(String deviceId, Date from, Date to) {
        return mapper.selectList(Wrappers.<DataRecord>lambdaQuery()
                .eq(DataRecord::getDeviceId, deviceId)
                .ge(DataRecord::getCreatedAt, from)
                .le(DataRecord::getCreatedAt, to)
                .orderByDesc(DataRecord::getCreatedAt));
    }

    @Override
    public Optional<DataRecord> findFirstByDeviceIdOrderByCreatedAtDesc(String deviceId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<DataRecord>lambdaQuery()
                .eq(DataRecord::getDeviceId, deviceId)
                .orderByDesc(DataRecord::getCreatedAt)
                .last("limit 1")));
    }

    @Override
    public DataRecord save(DataRecord record) {
        if (record.getId() == null || record.getId().isEmpty()) {
            record.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(record.getId()) == null) {
            mapper.insert(record);
        } else {
            mapper.updateById(record);
        }
        return record;
    }
}
