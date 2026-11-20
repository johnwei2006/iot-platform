package com.proxy.persistence.mongodb;

import java.util.List;

import com.proxy.persistence.mongodb.model.ProxyLock;

public interface ProxyLockRepositoryCustom{
	public List<ProxyLock> queryLockByType(String lockType);
	public int updateOrSaveLock(ProxyLock lock);
	public boolean lock(ProxyLock lock);
	public boolean unLock(ProxyLock lock);
}
