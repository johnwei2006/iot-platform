package com.iot.platform.repository.node;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.NodeMapper;
import com.iot.platform.model.Node;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NodeRepositoryImpl implements NodeRepository {

    private final NodeMapper mapper;

    public NodeRepositoryImpl(NodeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Node> findByNodeCode(String nodeCode) {
        return Optional.ofNullable(mapper.selectOne(
                Wrappers.<Node>lambdaQuery().eq(Node::getNodeCode, nodeCode)));
    }

    @Override
    public List<Node> findAll() {
        return mapper.selectList(Wrappers.<Node>lambdaQuery().eq(Node::getIsDeleted, 0));
    }

    @Override
    public List<Node> findAll(Wrapper<Node> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public Optional<Node> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Node save(Node node) {
        if (node.getId() == null || node.getId().isEmpty()) {
            node.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(node.getId()) == null) {
            mapper.insert(node);
        } else {
            mapper.updateById(node);
        }
        return node;
    }

    @Override
    public void deleteById(String id) {
        mapper.update(null, new LambdaUpdateWrapper<Node>()
                .eq(Node::getId, id).set(Node::getIsDeleted, 1));
    }
}
