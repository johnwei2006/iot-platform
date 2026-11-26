package com.iot.platform.service;

import com.iot.platform.model.Node;
import com.iot.platform.repository.NodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class NodeServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @InjectMocks
    private NodeService nodeService;

    @Test
    public void updatePartialFieldsKeepsUpdateByWhenMissing() {
        Node existing = new Node();
        existing.setId("node-1");
        existing.setNodeCode("30200001183");
        existing.setNodeName("原名称");
        existing.setUpdateBy("creator-1");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(existing));
        when(nodeRepository.save(any(Node.class))).thenAnswer(inv -> inv.getArgument(0));

        Node patch = new Node();
        patch.setNodeName("新名称");
        patch.setLongitude("120.15");
        patch.setLatitude("30.28");
        patch.setAreaId("area-1");
        patch.setAddress("小日光温室配电柜");

        Node updated = nodeService.update("node-1", patch);

        assertEquals("新名称", updated.getNodeName());
        assertEquals("120.15", updated.getLongitude());
        assertEquals("30.28", updated.getLatitude());
        assertEquals("小日光温室配电柜", updated.getAddress());
        assertEquals("area-1", updated.getAreaId());
        assertEquals("creator-1", updated.getUpdateBy());
        assertNotNull(updated.getUpdateTime());
    }

    @Test
    public void updateAppliesExplicitUpdateBy() {
        Node existing = new Node();
        existing.setId("node-2");
        existing.setUpdateBy("creator-2");
        when(nodeRepository.findById("node-2")).thenReturn(Optional.of(existing));
        when(nodeRepository.save(any(Node.class))).thenAnswer(inv -> inv.getArgument(0));

        Node patch = new Node();
        patch.setUpdateBy("editor-9");

        Node updated = nodeService.update("node-2", patch);

        assertEquals("editor-9", updated.getUpdateBy());
    }
}
