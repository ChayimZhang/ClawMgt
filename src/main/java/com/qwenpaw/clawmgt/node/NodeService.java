package com.qwenpaw.clawmgt.node;

import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.enums.NodeStatus;
import com.qwenpaw.clawmgt.domain.repository.NodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NodeService {
    private final ChannelService channelService;
    private final NodeRepository nodeRepository;

    public NodeService(ChannelService channelService, NodeRepository nodeRepository) {
        this.channelService = channelService;
        this.nodeRepository = nodeRepository;
    }

    @Transactional
    public NodeEntity registerNode(Long channelId, String nodeKey, String hostname, String ipAddress, String clawVersion, String metadata) {
        channelService.requireChannel(channelId);
        LocalDateTime now = LocalDateTime.now();

        NodeEntity node = new NodeEntity();
        node.setChannelId(channelId);
        node.setNodeKey(nodeKey);
        node.setHostname(hostname);
        node.setIpAddress(ipAddress);
        node.setClawVersion(clawVersion);
        node.setStatus(NodeStatus.ONLINE);
        node.setMetadata(metadata);
        node.setLastHeartbeatAt(now);
        node.setCreatedAt(now);
        node.setUpdatedAt(now);
        return nodeRepository.save(node);
    }

    @Transactional
    public NodeEntity updateHeartbeat(Long nodeId) {
        NodeEntity node = requireNode(nodeId);
        LocalDateTime now = LocalDateTime.now();
        node.setStatus(NodeStatus.ONLINE);
        node.setLastHeartbeatAt(now);
        node.setUpdatedAt(now);
        return nodeRepository.save(node);
    }

    @Transactional(readOnly = true)
    public NodeEntity requireNode(Long nodeId) {
        return nodeRepository.findById(nodeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND", "Node not found: " + nodeId));
    }

    @Transactional(readOnly = true)
    public NodeEntity requireNodeInChannel(Long channelId, Long nodeId) {
        NodeEntity node = requireNode(nodeId);
        if (!node.getChannelId().equals(channelId)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "NODE_CHANNEL_MISMATCH", "Node does not belong to channel");
        }
        return node;
    }

    @Transactional(readOnly = true)
    public boolean exists(Long nodeId) {
        return nodeRepository.existsById(nodeId);
    }

    @Transactional(readOnly = true)
    public List<NodeEntity> findOnlineNodes(Long channelId) {
        channelService.requireChannel(channelId);
        return nodeRepository.findByChannelIdAndStatus(channelId, NodeStatus.ONLINE);
    }
}
