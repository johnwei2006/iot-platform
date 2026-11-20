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

import com.proxy.persistence.h2.model.DeviceInfo;

@Repository
public interface DeviceInfoRespository extends CrudRepository<DeviceInfo,Long>{

	@Query(value = "select distinct v from DeviceInfo v where v.deviceId = :deviceId")
	DeviceInfo queryDeviceInfoByDevice(@Param("deviceId") String deviceId);
	
	@Query(value = "select distinct v from DeviceInfo v where v.ip = :ip and v.port = :port")
	DeviceInfo getDeviceInfoByIp(@Param("ip") String ip, @Param("port") int port);
	
	@Transactional
	@Modifying
	@Query(value = "update DeviceInfo v set v.ip=:ip,v.port=:port,v.transportProtocol=:transportProtocol,v.transportProtocolVersion=:transportProtocolVersion,v.lastConnectedTime=:lastConnectedTime where v.deviceId = :deviceId")
	int updateDeviceInfo(@Param("deviceId") String deviceId, @Param("ip") String ip, @Param("port") int port, @Param("transportProtocol") short transportProtocol, @Param("transportProtocolVersion") short transportProtocolVersion, @Param("lastConnectedTime") Date lastConnectedTime);
	
	@Query(value = "select distinct v from DeviceInfo v where v.lastConnectedTime > :lastConnectedTime")
	public List<DeviceInfo> queryDeviceInfoNotLaterThan(@Param("lastConnectedTime") Date lastConnectedTime);

	@Transactional
	@Modifying
	@Query(value = "delete DeviceInfo v where v.deviceId = :deviceId")
	int deleteDeviceInfo(@Param("deviceId") String deviceId);
}
