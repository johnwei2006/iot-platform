package com.proxy.auth;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

/**
 * ProxyTcp 主库（proxy 库）MongoTemplate。
 *
 * <p>必须显式声明名为 {@code mongoTemplate} 的 bean：Spring Data MongoDB 的 repository
 * （如 GatewayTokenRepository）按名字解析该 bean，而多个 {@code MongoOperations} 类型 bean
 * 会让 {@code MongoDataAutoConfiguration} 的默认 mongoTemplate 因
 * {@code @ConditionalOnMissingBean(MongoOperations.class)} 不生效。
 * 本配置与 {@link PlatformMongoConfig} 的 platformMongoTemplate 并存，分别连 proxy / iot_platform 两个库。
 */
@Configuration
@Profile("mongo")
public class ProxyMongoConfig {

	@Bean(name = "mongoTemplate")
	@Primary
	public MongoTemplate mongoTemplate(
			@Value("${spring.data.mongodb.uri:mongodb://127.0.0.1:27017/proxy}") String mongoUri) {
		ConnectionString cs = new ConnectionString(mongoUri);
		MongoClient client = MongoClients.create(cs);
		String database = cs.getDatabase();
		if (database == null || database.isEmpty()) {
			database = "proxy";
		}
		return new MongoTemplate(new SimpleMongoClientDatabaseFactory(client, database));
	}
}
