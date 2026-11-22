package com.iot.admin.org;

import com.iot.admin.common.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 机构服务：4-2-2-2 编码规则与层级树。
 * 编码规则：一级 4 位（^\d{4}$），逐级 +2 位（6/8/10 位），最深 4 级；
 * 子编码必须以父编码为前缀。层级查询用编码前缀匹配，免递归。
 */
@Service
public class OrgService {

    /** 一级编码长度；每降一级 +2；最深 4 级 */
    public static final int ROOT_CODE_LENGTH = 4;
    public static final int MAX_LEVEL = 4;
    public static final int MAX_CODE_LENGTH = ROOT_CODE_LENGTH + 2 * (MAX_LEVEL - 1);

    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{4}([0-9]{2}){0,3}$");

    private final OrgRepository orgRepository;

    @Autowired
    public OrgService(OrgRepository orgRepository) {
        this.orgRepository = orgRepository;
    }

    /** 校验编码格式与层级；不校验唯一性/父级存在性（由创建/更新流程负责）。 */
    public void validateCode(String code) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw AppException.badRequest("org code must be 4-10 digits in 4-2-2-2 pattern: " + code);
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw AppException.badRequest("org code exceeds max depth " + MAX_LEVEL + ": " + code);
        }
    }

    /** 由编码推导层级（1-4）。 */
    public int levelOf(String code) {
        return (code.length() - ROOT_CODE_LENGTH) / 2 + 1;
    }

    /** 由编码推导父编码；一级返回 null。 */
    public String parentCodeOf(String code) {
        validateCode(code);
        if (code.length() == ROOT_CODE_LENGTH) {
            return null;
        }
        return code.substring(0, code.length() - 2);
    }

    public Org create(String code, String name, String leader, String phone, String sort) {
        validateCode(code);
        if (orgRepository.findByCode(code).isPresent()) {
            throw AppException.conflict("org code already exists: " + code);
        }
        String parentCode = parentCodeOf(code);
        if (parentCode != null && !orgRepository.findByCode(parentCode).isPresent()) {
            throw AppException.badRequest("parent org not found: " + parentCode);
        }
        Org org = new Org();
        org.setCode(code);
        org.setName(name);
        org.setParentCode(parentCode);
        org.setLeader(leader);
        org.setPhone(phone);
        org.setSort(sort == null ? "0" : sort);
        return orgRepository.save(org);
    }

    /** 更新机构（编码不可修改，层级语义绑定编码；改名/联系方式/排序/启停）。 */
    public Org update(String id, String name, String leader, String phone, String sort, String status) {
        Org org = orgRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("org not found: " + id));
        if (name != null) {
            org.setName(name);
        }
        if (leader != null) {
            org.setLeader(leader);
        }
        if (phone != null) {
            org.setPhone(phone);
        }
        if (sort != null) {
            org.setSort(sort);
        }
        if (status != null && ("ENABLED".equals(status) || "DISABLED".equals(status))) {
            org.setStatus(status);
        }
        return orgRepository.save(org);
    }

    /** 删除机构：无子机构且无用户关联（经 AuthInternalClient 检查）方可删除。 */
    public void delete(String id, UserOrgChecker userOrgChecker) {
        Org org = orgRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("org not found: " + id));
        String code = org.getCode();
        List<Org> children = orgRepository.findByParentCode(code);
        if (!children.isEmpty()) {
            throw AppException.conflict("org has child orgs: " + code);
        }
        // 同前缀子孙（防御性，正常编码树下一级即子机构）
        List<Org> descendants = orgRepository.findByCodeStartingWith(code);
        if (descendants.size() > 1) {
            throw AppException.conflict("org has descendant orgs: " + code);
        }
        if (userOrgChecker != null && userOrgChecker.hasUsersUnder(code)) {
            throw AppException.conflict("org has bound users: " + code);
        }
        orgRepository.delete(org);
    }

    public List<Org> listAll() {
        List<Org> orgs = orgRepository.findAll();
        orgs.sort(Comparator.comparing(Org::getCode));
        return orgs;
    }

    /** 构建机构树（enabled 优先全量；节点含 children）。 */
    public List<Map<String, Object>> tree() {
        Map<String, Map<String, Object>> nodeByCode = new LinkedHashMap<String, Map<String, Object>>();
        for (Org org : listAll()) {
            Map<String, Object> node = new LinkedHashMap<String, Object>();
            node.put("id", org.getId());
            node.put("code", org.getCode());
            node.put("name", org.getName());
            node.put("parentCode", org.getParentCode());
            node.put("leader", org.getLeader());
            node.put("phone", org.getPhone());
            node.put("sort", org.getSort());
            node.put("status", org.getStatus());
            node.put("level", levelOf(org.getCode()));
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeByCode.put(org.getCode(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> node : nodeByCode.values()) {
            String parentCode = (String) node.get("parentCode");
            Map<String, Object> parent = parentCode == null ? null : nodeByCode.get(parentCode);
            if (parent == null) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(node);
            }
        }
        return roots;
    }

    /** 删除守护：检查某机构（含子机构前缀）下是否仍有用户。 */
    public interface UserOrgChecker {
        boolean hasUsersUnder(String orgCode);
    }
}
