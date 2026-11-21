package com.proxy.auth;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

/**
 * 平台库（iot_platform）只读 MongoTemplate 配置。
 * 网关认证需要读取 platform_java 维护的 devices 集合校验预共享密钥；
 * 与 ProxyTcp 自身的 proxy 库相互独立。
 */
@Configuration
@Profile("mongo")
public class PlatformMongoConfig {

	@Bean(name = "platformMongoTemplate")
	public MongoTemplate platformMongoTemplate(
			@Value("${platformMongoUri:mongodb://127.0.0.1:27017/iot_platform}") String platformMongoUri) {
		String serverUri = stripDatabase(platformMongoUri);
		MongoClient client = new MongoClient(new MongoClientURI(serverUri));
		String database = new MongoClientURI(platformMongoUri).getDatabase();
		if (database == null || database.isEmpty()) {
			database = "iot_platform";
		}
		return new MongoTemplate(new SimpleMongoDbFactory(client, database));
	}

	private String stripDatabase(String uri) {
		int idx = uri.lastIndexOf('/');
		if (idx > uri.indexOf("://")) {
			return uri.substring(0, idx);
		}
		return uri;
	}
}
