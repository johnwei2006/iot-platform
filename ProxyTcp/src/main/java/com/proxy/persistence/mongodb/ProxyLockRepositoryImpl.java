package com.proxy.persistence.mongodb;

import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;
import com.proxy.persistence.mongodb.model.ProxyLock;

@Service
public class ProxyLockRepositoryImpl implements ProxyLockRepositoryCustom{
	
    @Resource
    private MongoOperations mongoTemplate;

	public List<ProxyLock> queryLockByType(String lockType) {
        Query query = new Query(Criteria.where("lockType").is(lockType));
        return mongoTemplate.find(query, ProxyLock.class);      
	}
	
	public int updateOrSaveLock(ProxyLock lock) {
        Query query = new Query(Criteria.where("lockType").is(lock.getLockType()));
        List<ProxyLock> locks = mongoTemplate.find(query, ProxyLock.class);
        if (locks == null || locks.size() == 0) {
        	mongoTemplate.save(lock);
        	return 1;
        } else {
			Update update = new Update();
			update.set("isLocked", lock.isLocked());
			update.set("lockedTime", lock.getLockedTime());
			update.set("locker", lock.getLocker());
	
			UpdateResult result = mongoTemplate.updateMulti(query, update, ProxyLock.class);
			return (int)result.getModifiedCount();
        }
	}
	
	public boolean lock(ProxyLock lock) {
        Query query = new Query(Criteria.where("lockType").is(lock.getLockType()));
        List<ProxyLock> locks = mongoTemplate.find(query, ProxyLock.class);
        if (locks == null || locks.size() == 0) {
        	mongoTemplate.save(lock);
        	return true;
        } else {
        	boolean needFinalLock = true;
        	if (locks.get(0).isLocked()) {
        		Date expiredDate = new Date();
        		expiredDate.setTime(expiredDate.getTime() - 10*60*1000);
        		if (locks.get(0).getLockedTime().after(expiredDate)) {
        			needFinalLock = false;
        		} 
        	}
        	if (needFinalLock) {
    			Update update = new Update();
    			update.set("isLocked", true);
    			update.set("lockedTime", new Date());
    			update.set("locker", lock.getLocker());
    	
    			UpdateResult result = mongoTemplate.updateMulti(query, update, ProxyLock.class);
    			if (result.getModifiedCount() > 0)
    				return true;
    			else 
    				return false;
        	} else {
        		return false;
        	}
        }
	}
	
	public boolean unLock(ProxyLock lock) {
        Query query = new Query(Criteria.where("lockType").is(lock.getLockType()));
        List<ProxyLock> locks = mongoTemplate.find(query, ProxyLock.class);
        if (locks == null || locks.size() == 0) {
        	lock.setLocked(false);
        	mongoTemplate.save(lock);
        	return true;
        } else {
        	if (locks.get(0).isLocked()) {
    			Update update = new Update();
    			update.set("isLocked", false);
    			update.set("lockedTime", lock.getLockedTime());
    			update.set("locker", lock.getLocker());
    	
    			UpdateResult result = mongoTemplate.updateMulti(query, update, ProxyLock.class);
    			if (result.getModifiedCount() > 0)
    				return true;
    			else 
    				return false;
        	} else {
    			return true;
        	}
        }
	}
}
