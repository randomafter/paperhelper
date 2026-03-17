package com.history.creation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.history.creation.dto.UserMaterialDTO;
import com.history.creation.dto.UserMaterialRequest;
import com.history.creation.entity.Material;
import com.history.creation.entity.UserMaterial;
import com.history.creation.mapper.MaterialMapper;
import com.history.creation.mapper.UserMaterialMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserMaterialService {

    private final UserMaterialMapper userMaterialMapper;
    private final MaterialMapper materialMapper;
    private final NotificationService notificationService;

    public UserMaterialService(UserMaterialMapper userMaterialMapper,
                               MaterialMapper materialMapper,
                               NotificationService notificationService) {
        this.userMaterialMapper = userMaterialMapper;
        this.materialMapper = materialMapper;
        this.notificationService = notificationService;
    }

    /** 当前用户的素材列表 */
    public List<UserMaterialDTO> listByUser(Long userId) {
        List<UserMaterial> list = userMaterialMapper.selectList(
                new LambdaQueryWrapper<UserMaterial>()
                        .eq(UserMaterial::getUserId, userId)
                        .orderByDesc(UserMaterial::getCreatedAt));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** 管理员：待审核列表 */
    public List<UserMaterialDTO> listPending() {
        List<UserMaterial> list = userMaterialMapper.selectList(
                new LambdaQueryWrapper<UserMaterial>()
                        .eq(UserMaterial::getStatus, "pending")
                        .orderByAsc(UserMaterial::getCreatedAt));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** 创建素材（默认自用状态） */
    @Transactional
    public UserMaterialDTO create(Long userId, UserMaterialRequest req) {
        UserMaterial um = new UserMaterial();
        um.setUserId(userId);
        um.setCategory(req.getCategory());
        um.setTitle(req.getTitle());
        um.setContent(req.getContent());
        um.setTags(req.getTags());
        um.setStatus("approved");  // 新建时直接为已通过（自用），不需审核
        userMaterialMapper.insert(um);
        return toDTO(um);
    }

    /** 编辑（仅限草稿/被拒绝状态） */
    @Transactional
    public UserMaterialDTO update(Long userId, Long id, UserMaterialRequest req) {
        UserMaterial um = getOwned(userId, id);
        if (!"draft".equals(um.getStatus()) && !"rejected".equals(um.getStatus())) {
            throw new RuntimeException("当前状态不允许编辑");
        }
        um.setCategory(req.getCategory());
        um.setTitle(req.getTitle());
        um.setContent(req.getContent());
        um.setTags(req.getTags());
        userMaterialMapper.updateById(um);
        return toDTO(um);
    }

    /** 提交审核 */
    @Transactional
    public UserMaterialDTO submit(Long userId, Long id) {
        UserMaterial um = getOwned(userId, id);
        if (!"draft".equals(um.getStatus()) && !"rejected".equals(um.getStatus())) {
            throw new RuntimeException("当前状态不允许提交");
        }
        um.setStatus("pending");
        um.setAdminComment(null);
        userMaterialMapper.updateById(um);
        return toDTO(um);
    }

    /** 删除（自用素材可随时删除） */
    @Transactional
    public void delete(Long userId, Long id) {
        UserMaterial um = getOwned(userId, id);
        userMaterialMapper.deleteById(id);
    }

    /** 管理员：审核通过 → 写入素材库 */
    @Transactional
    public void approve(Long adminId, Long id, String comment) {
        UserMaterial um = userMaterialMapper.selectById(id);
        if (um == null) throw new RuntimeException("素材不存在");
        if (!"pending".equals(um.getStatus())) throw new RuntimeException("非待审核状态");

        um.setStatus("approved");
        um.setAdminComment(comment);
        userMaterialMapper.updateById(um);

        // 写入公共素材库
        Material material = new Material();
        material.setCategory(um.getCategory());
        material.setTitle(um.getTitle());
        material.setContent(um.getContent());
        materialMapper.insert(material);

        // 发送通知给用户
        notificationService.sendNotification(
                um.getUserId(),
                "material_approved",
                "素材审核通过",
                "你的素材《" + um.getTitle() + "》已审核通过，现已收录至素材检索库！",
                um.getId()
        );
    }

    /** 管理员：审核拒绝 */
    @Transactional
    public void reject(Long adminId, Long id, String comment) {
        UserMaterial um = userMaterialMapper.selectById(id);
        if (um == null) throw new RuntimeException("素材不存在");
        if (!"pending".equals(um.getStatus())) throw new RuntimeException("非待审核状态");

        um.setStatus("rejected");
        um.setAdminComment(comment);
        userMaterialMapper.updateById(um);

        // 发送通知给用户
        notificationService.sendNotification(
                um.getUserId(),
                "material_rejected",
                "素材审核未通过",
                "你的素材《" + um.getTitle() + "》未通过审核。审核意见：" + (comment != null ? comment : "无"),
                um.getId()
        );
    }

    private UserMaterial getOwned(Long userId, Long id) {
        UserMaterial um = userMaterialMapper.selectById(id);
        if (um == null || !um.getUserId().equals(userId)) {
            throw new RuntimeException("素材不存在");
        }
        return um;
    }

    private UserMaterialDTO toDTO(UserMaterial um) {
        UserMaterialDTO dto = new UserMaterialDTO();
        dto.setId(um.getId());
        dto.setUserId(um.getUserId());
        dto.setCategory(um.getCategory());
        dto.setTitle(um.getTitle());
        dto.setContent(um.getContent());
        dto.setTags(um.getTags());
        dto.setStatus(um.getStatus());
        dto.setAdminComment(um.getAdminComment());
        dto.setCreatedAt(um.getCreatedAt());
        dto.setUpdatedAt(um.getUpdatedAt());
        return dto;
    }
}
