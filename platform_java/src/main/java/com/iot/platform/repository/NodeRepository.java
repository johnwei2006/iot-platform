package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.Node;

import java.util.List;
import java.util.Optional;

/** 节点数据访问接口。 */
public interface NodeRepository {

    Optional<Node> findByNodeCode(String nodeCode);

    List<Node> findAll();

    List<Node> findAll(Wrapper<Node> wrapper);

    Optional<Node> findById(String id);

    Node save(Node node);

    void deleteById(String id);
}
