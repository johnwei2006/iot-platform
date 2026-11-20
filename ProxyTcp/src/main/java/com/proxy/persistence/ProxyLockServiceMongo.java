package com.proxy.persistence;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proxy.persistence.mongodb.ProxyLockRepository;
import com.proxy.persistence.mongodb.model.ProxyLock;

@org.springframework.context.annotation.Profile("mongo")
@Service
public class ProxyLockServiceMongo {
	@Autowired
	ProxyLockRepository proxyLockRepository;
	
	public List<ProxyLock> queryProxyLockByType(String lockType){
		return proxyLockRepository.queryLockByType(lockType);
	}
	
	@Transactional
	public int updateOrSaveLock(ProxyLock lock) {
		return proxyLockRepository.updateOrSaveLock(lock);
	}
	
	@Transactional
	public boolean lock(ProxyLock lock) {
		return proxyLockRepository.lock(lock);
	}

	@Transactional
	public boolean unLock(ProxyLock lock) {
		return proxyLockRepository.unLock(lock);
	}
}
