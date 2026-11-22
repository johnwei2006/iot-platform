package com.iot.admin.org;

import com.iot.admin.common.AppException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrgServiceTest {

    @Mock
    private OrgRepository orgRepository;
    @Mock
    private OrgService.UserOrgChecker userOrgChecker;

    private OrgService orgService;

    @Before
    public void setUp() {
        orgService = new OrgService(orgRepository);
    }

    private Org org(String code, String name) {
        Org o = new Org();
        o.setId("id-" + code);
        o.setCode(code);
        o.setName(name);
        o.setParentCode(orgService.parentCodeOf(code));
        return o;
    }

    @Test
    public void codeValidationAndHierarchy() {
        orgService.validateCode("0001");
        orgService.validateCode("000101");
        orgService.validateCode("00010103");
        orgService.validateCode("0001010305");

        assertEquals(1, orgService.levelOf("0001"));
        assertEquals(2, orgService.levelOf("000101"));
        assertEquals(3, orgService.levelOf("00010103"));
        assertEquals(4, orgService.levelOf("0001010305"));
        assertNull(orgService.parentCodeOf("0001"));
        assertEquals("0001", orgService.parentCodeOf("000101"));
        assertEquals("000101", orgService.parentCodeOf("00010103"));

        try {
            orgService.validateCode("001");
            fail("too short should be rejected");
        } catch (AppException e) { /* expected */ }
        // 格式合法但父级未创建的情况由 create 流程校验（见 createRejectsMissingParent）
        orgService.validateCode("000112");
        try {
            orgService.validateCode("000101030507");
            fail("exceeds max depth should be rejected");
        } catch (AppException e) { /* expected */ }
        try {
            orgService.validateCode("000a01");
            fail("non-digit should be rejected");
        } catch (AppException e) { /* expected */ }
    }

    @Test
    public void createRequiresParentExistence() {
        when(orgRepository.findByCode("0001"))
                .thenReturn(Optional.of(org("0001", "总部")));
        when(orgRepository.findByCode("000102")).thenReturn(Optional.empty());
        when(orgRepository.save(any(Org.class))).thenAnswer(inv -> inv.getArgument(0));

        Org created = orgService.create("000102", "研发部", null, null, null);

        assertEquals("000102", created.getCode());
        assertEquals("0001", created.getParentCode());
        verify(orgRepository).save(any(Org.class));
    }

    @Test
    public void createRejectsMissingParent() {
        when(orgRepository.findByCode("999901")).thenReturn(Optional.empty());
        when(orgRepository.findByCode("9999")).thenReturn(Optional.empty());

        try {
            orgService.create("999901", "幽灵部门", null, null, null);
            fail("missing parent should be rejected");
        } catch (AppException e) {
            assertEquals(400, e.getResultCode().getCode());
        }
        verify(orgRepository, never()).save(any(Org.class));
    }

    @Test
    public void createRejectsDuplicateCode() {
        when(orgRepository.findByCode("0001")).thenReturn(Optional.of(org("0001", "总部")));

        try {
            orgService.create("0001", "重复编码", null, null, null);
            fail("duplicate code should be rejected");
        } catch (AppException e) {
            assertEquals(409, e.getResultCode().getCode());
        }
    }

    @Test
    public void deleteRejectsWhenChildOrgsExist() {
        Org root = org("0001", "总部");
        when(orgRepository.findById("id-0001")).thenReturn(Optional.of(root));
        when(orgRepository.findByParentCode("0001")).thenReturn(Arrays.asList(org("000101", "研发部")));

        try {
            orgService.delete("id-0001", userOrgChecker);
            fail("org with children should not be deletable");
        } catch (AppException e) {
            assertEquals(409, e.getResultCode().getCode());
        }
    }

    @Test
    public void deleteRejectsWhenUsersBound() {
        Org leaf = org("000101", "研发部");
        when(orgRepository.findById("id-000101")).thenReturn(Optional.of(leaf));
        when(orgRepository.findByParentCode("000101")).thenReturn(new ArrayList<Org>());
        List<Org> descendants = new ArrayList<Org>();
        descendants.add(leaf);
        when(orgRepository.findByCodeStartingWith("000101")).thenReturn(descendants);
        when(userOrgChecker.hasUsersUnder("000101")).thenReturn(true);

        try {
            orgService.delete("id-000101", userOrgChecker);
            fail("org with bound users should not be deletable");
        } catch (AppException e) {
            assertEquals(409, e.getResultCode().getCode());
        }
    }

    @Test
    public void deleteLeafWithoutUsersSucceeds() {
        Org leaf = org("000101", "研发部");
        when(orgRepository.findById("id-000101")).thenReturn(Optional.of(leaf));
        when(orgRepository.findByParentCode("000101")).thenReturn(new ArrayList<Org>());
        List<Org> descendants = new ArrayList<Org>();
        descendants.add(leaf);
        when(orgRepository.findByCodeStartingWith("000101")).thenReturn(descendants);
        when(userOrgChecker.hasUsersUnder("000101")).thenReturn(false);

        orgService.delete("id-000101", userOrgChecker);

        verify(orgRepository).delete(leaf);
    }
}
