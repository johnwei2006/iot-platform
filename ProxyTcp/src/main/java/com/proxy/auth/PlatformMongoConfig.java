package com.proxy.auth;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

/**
 * 平台库（iot_platform）只读 MongoTemplate 配置。
 * 网关认证需要读取 platform_java 维护的 devices 集合校验预共享密钥；
 * 与 ProxyTcp 自身的 proxy 库相互独立。
 *
 * <p>迁移说明：Mongo 驱动 4.x 移除了 com.mongodb.MongoClient/MongoClientURI 同步旧类，
 * Spring Data MongoDB 3+ 移除了 SimpleMongoDbFactory，分别改用
 * MongoClients/ConnectionString 与 SimpleMongoClientDatabaseFactory。
 */
@Configuration
@Profile("mongo")
public class PlatformMongoConfig {

	@Bean(name = "platformMongoTemplate")
	public MongoTemplate platformMongoTemplate(
			@Value("${platformMongoUri:mongodb://127.0.0.1:27017/iot_platform}") String platformMongoUri) {
		MongoClient client = MongoClients.create(new ConnectionString(platformMongoUri));
		String database = new ConnectionString(platformMongoUri).getDatabase();
		if (database == null || database.isEmpty()) {
			database = "iot_platform";
		}
		return new MongoTemplate(new SimpleMongoClientDatabaseFactory(client, database));
	}
}
