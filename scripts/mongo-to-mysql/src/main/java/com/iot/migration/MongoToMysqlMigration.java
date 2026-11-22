package com.iot.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 一次性数据迁移：admin_service 的 orgs/roles/menus 与 auth_service 的 users
 * → MySQL `iot-platform` 的 sys_org/sys_role/sys_menu/sys_user。
 *
 * <p>幂等：按主键或唯一键 INSERT ... ON DUPLICATE KEY UPDATE；重复执行不会新增记录。
 * 只输出迁移计数与失败记录标识，不输出密码哈希或连接口令。
 */
public final class MongoToMysqlMigration {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String ORG_UPSERT =
            "INSERT INTO `sys_org` (`id`,`code`,`parent_code`,`name`,`leader`,`phone`,`sort`,`status`,`created_at`) "
                    + "VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                    + "`code`=VALUES(`code`),`parent_code`=VALUES(`parent_code`),`name`=VALUES(`name`),"
                    + "`leader`=VALUES(`leader`),`phone`=VALUES(`phone`),`sort`=VALUES(`sort`),"
                    + "`status`=VALUES(`status`),`created_at`=VALUES(`created_at`)";

    private static final String ROLE_UPSERT =
            "INSERT INTO `sys_role` (`id`,`code`,`name`,`menu_ids`,`data_scopes`,`built_in`,`status`,`created_at`) "
                    + "VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                    + "`code`=VALUES(`code`),`name`=VALUES(`name`),`menu_ids`=VALUES(`menu_ids`),"
                    + "`data_scopes`=VALUES(`data_scopes`),`built_in`=VALUES(`built_in`),"
                    + "`status`=VALUES(`status`),`created_at`=VALUES(`created_at`)";

    private static final String MENU_UPSERT =
            "INSERT INTO `sys_menu` (`id`,`parent_id`,`name`,`type`,`path`,`component`,`perm`,`icon`,`sort`,`status`,`created_at`) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                    + "`parent_id`=VALUES(`parent_id`),`name`=VALUES(`name`),`type`=VALUES(`type`),"
                    + "`path`=VALUES(`path`),`component`=VALUES(`component`),`perm`=VALUES(`perm`),"
                    + "`icon`=VALUES(`icon`),`sort`=VALUES(`sort`),`status`=VALUES(`status`),"
                    + "`created_at`=VALUES(`created_at`)";

    private static final String USER_UPSERT =
            "INSERT INTO `sys_user` (`id`,`username`,`password_hash`,`openid`,`unionid`,`phone`,`roles`,"
                    + "`org_id`,`org_code`,`data_scopes`,`token_version`,`status`,`created_at`,`last_login_at`) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                    + "`username`=VALUES(`username`),`password_hash`=VALUES(`password_hash`),"
                    + "`openid`=VALUES(`openid`),`unionid`=VALUES(`unionid`),`phone`=VALUES(`phone`),"
                    + "`roles`=VALUES(`roles`),`org_id`=VALUES(`org_id`),`org_code`=VALUES(`org_code`),"
                    + "`data_scopes`=VALUES(`data_scopes`),`token_version`=VALUES(`token_version`),"
                    + "`status`=VALUES(`status`),`created_at`=VALUES(`created_at`),"
                    + "`last_login_at`=VALUES(`last_login_at`)";

    private MongoToMysqlMigration() {
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps, Document doc) throws SQLException;
    }

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        if (options.help) {
            Options.printUsage();
            return;
        }
        long started = System.currentTimeMillis();
        try (MongoClient mongo = MongoClients.create(options.mongoUri);
             Connection mysql = DriverManager.getConnection(
                     options.mysqlUrl, options.mysqlUser, options.mysqlPassword)) {
            mysql.setAutoCommit(false);
            MongoDatabase admin = mongo.getDatabase(options.adminDb);
            MongoDatabase auth = mongo.getDatabase(options.authDb);

            int orgs = migrate(mysql, admin.getCollection("orgs"), ORG_UPSERT, options.batchSize,
                    (ps, doc) -> {
                        int i = 1;
                        ps.setString(i++, idOf(doc));
                        ps.setString(i++, text(doc, "code", ""));
                        ps.setString(i++, text(doc, "parentCode", null));
                        ps.setString(i++, text(doc, "name", ""));
                        ps.setString(i++, text(doc, "leader", null));
                        ps.setString(i++, text(doc, "phone", null));
                        ps.setString(i++, text(doc, "sort", "0"));
                        ps.setString(i++, text(doc, "status", "ENABLED"));
                        ps.setTimestamp(i, timestamp(doc, "createdAt"));
                    });
            int roles = migrate(mysql, admin.getCollection("roles"), ROLE_UPSERT, options.batchSize,
                    (ps, doc) -> {
                        int i = 1;
                        ps.setString(i++, idOf(doc));
                        ps.setString(i++, text(doc, "code", ""));
                        ps.setString(i++, text(doc, "name", ""));
                        ps.setString(i++, jsonList(doc.get("menuIds")));
                        ps.setString(i++, jsonMap(doc.get("dataScopes")));
                        ps.setBoolean(i++, Boolean.TRUE.equals(doc.get("builtIn")));
                        ps.setString(i++, text(doc, "status", "ENABLED"));
                        ps.setTimestamp(i, timestamp(doc, "createdAt"));
                    });
            int menus = migrate(mysql, admin.getCollection("menus"), MENU_UPSERT, options.batchSize,
                    (ps, doc) -> {
                        int i = 1;
                        ps.setString(i++, idOf(doc));
                        ps.setString(i++, text(doc, "parentId", null));
                        ps.setString(i++, text(doc, "name", ""));
                        ps.setString(i++, text(doc, "type", ""));
                        ps.setString(i++, text(doc, "path", null));
                        ps.setString(i++, text(doc, "component", null));
                        ps.setString(i++, text(doc, "perm", null));
                        ps.setString(i++, text(doc, "icon", null));
                        ps.setString(i++, text(doc, "sort", "0"));
                        ps.setString(i++, text(doc, "status", "ENABLED"));
                        ps.setTimestamp(i, timestamp(doc, "createdAt"));
                    });
            int users = migrate(mysql, auth.getCollection("users"), USER_UPSERT, options.batchSize,
                    (ps, doc) -> {
                        int i = 1;
                        ps.setString(i++, idOf(doc));
                        ps.setString(i++, text(doc, "username", null));
                        ps.setString(i++, text(doc, "passwordHash", null));
                        ps.setString(i++, text(doc, "openid", null));
                        ps.setString(i++, text(doc, "unionid", null));
                        ps.setString(i++, text(doc, "phone", null));
                        ps.setString(i++, jsonList(doc.get("roles")));
                        ps.setString(i++, text(doc, "orgId", null));
                        ps.setString(i++, text(doc, "orgCode", null));
                        ps.setString(i++, jsonMap(doc.get("dataScopes")));
                        ps.setLong(i++, number(doc, "tokenVersion", 0L));
                        ps.setString(i++, text(doc, "status", "ENABLED"));
                        ps.setTimestamp(i++, timestamp(doc, "createdAt"));
                        ps.setTimestamp(i, timestamp(doc, "lastLoginAt"));
                    });

            System.out.println("Migration done: sys_org=" + orgs + " sys_role=" + roles
                    + " sys_menu=" + menus + " sys_user=" + users
                    + " elapsed=" + (System.currentTimeMillis() - started) + "ms");
        }
    }

    private static int migrate(Connection mysql, MongoCollection<Document> collection,
                               String upsertSql, int batchSize, Binder binder) throws SQLException {
        int count = 0;
        try (PreparedStatement ps = mysql.prepareStatement(upsertSql)) {
            for (Document doc : collection.find()) {
                try {
                    binder.bind(ps, doc);
                    ps.addBatch();
                } catch (SQLException e) {
                    throw new SQLException("migrate " + collection.getNamespace().getCollectionName()
                            + " failed at id=" + idOf(doc), e);
                }
                count++;
                if (count % batchSize == 0) {
                    executeBatch(ps, mysql);
                }
            }
            executeBatch(ps, mysql);
        }
        return count;
    }

    private static void executeBatch(PreparedStatement ps, Connection mysql) throws SQLException {
        try {
            ps.executeBatch();
            mysql.commit();
        } catch (SQLException e) {
            mysql.rollback();
            throw e;
        }
    }

    private static String idOf(Document doc) {
        Object id = doc.get("_id");
        if (id instanceof ObjectId objectId) {
            return objectId.toHexString();
        }
        return id == null ? UUID.randomUUID().toString() : String.valueOf(id);
    }

    private static String text(Document doc, String key, String fallback) {
        Object value = doc.get(key);
        if (value == null) {
            return fallback;
        }
        String s = String.valueOf(value);
        return s.isEmpty() ? fallback : s;
    }

    private static long number(Document doc, String key, long fallback) {
        Object value = doc.get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }

    private static Timestamp timestamp(Document doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Date date) {
            return new Timestamp(date.getTime());
        }
        return new Timestamp(System.currentTimeMillis());
    }

    private static String jsonList(Object value) {
        try {
            return JSON.writeValueAsString(value instanceof List<?> list ? list : new ArrayList<>());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("serialize JSON list failed", e);
        }
    }

    private static String jsonMap(Object value) {
        try {
            return JSON.writeValueAsString(value instanceof Map<?, ?> map ? map : new HashMap<>());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("serialize JSON map failed", e);
        }
    }

    private static final class Options {
        private boolean help;
        private String mongoUri = env("MONGO_URI", "mongodb://localhost:27017");
        private String adminDb = env("ADMIN_MONGO_DB", "admin_service");
        private String authDb = env("AUTH_MONGO_DB", "auth_service");
        private String mysqlUrl = env("MYSQL_URL",
                "jdbc:mysql://localhost:3306/iot-platform?useUnicode=true&characterEncoding=utf8"
                        + "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
        private String mysqlUser = env("MYSQL_USER", "root");
        private String mysqlPassword = env("MYSQL_PASSWORD", "");
        private int batchSize = 200;

        static Options parse(String[] args) {
            Options o = new Options();
            for (int i = 0; i < args.length; i++) {
                String key = args[i];
                if ("--help".equals(key) || "-h".equals(key)) {
                    o.help = true;
                    continue;
                }
                String value;
                int eq = key.indexOf('=');
                if (eq > 0) {
                    value = key.substring(eq + 1);
                    key = key.substring(0, eq);
                } else if (i + 1 < args.length) {
                    value = args[++i];
                } else {
                    throw new IllegalArgumentException("missing value for " + key);
                }
                switch (key) {
                    case "--mongo-uri" -> o.mongoUri = value;
                    case "--admin-db" -> o.adminDb = value;
                    case "--auth-db" -> o.authDb = value;
                    case "--mysql-url" -> o.mysqlUrl = value;
                    case "--mysql-user" -> o.mysqlUser = value;
                    case "--mysql-password" -> o.mysqlPassword = value;
                    case "--batch-size" -> o.batchSize = Integer.parseInt(value);
                    default -> throw new IllegalArgumentException("unknown option: " + key);
                }
            }
            return o;
        }

        static void printUsage() {
            System.out.println("Usage: java -jar mongo-to-mysql.jar [options]");
            System.out.println("  --mongo-uri URI       MongoDB URI (env MONGO_URI)");
            System.out.println("  --admin-db NAME       源库：机构/角色/菜单 (env ADMIN_MONGO_DB, default admin_service)");
            System.out.println("  --auth-db NAME        源库：用户 (env AUTH_MONGO_DB, default auth_service)");
            System.out.println("  --mysql-url URL       MySQL JDBC URL (env MYSQL_URL)");
            System.out.println("  --mysql-user USER     MySQL user (env MYSQL_USER, default root)");
            System.out.println("  --mysql-password PWD  MySQL password (env MYSQL_PASSWORD)");
            System.out.println("  --batch-size N        每批提交记录数 (default 200)");
        }

        private static String env(String key, String fallback) {
            String value = System.getenv(key);
            return value == null || value.isEmpty() ? fallback : value;
        }
    }
}
