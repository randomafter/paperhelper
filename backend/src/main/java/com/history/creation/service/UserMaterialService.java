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

    /** 创建素材（默认草稿状态） */
    @Transactional
    public UserMaterialDTO create(Long userId, UserMaterialRequest req) {
        UserMaterial um = new UserMaterial();
        um.setUserId(userId);
        um.setCategory(req.getCategory());
        um.setTitle(req.getTitle());
        um.setContent(req.getContent());
        um.setTags(req.getTags());
        um.setStatus("draft");  // 新建时为草稿，用户可选择提交审核或自用
        userMaterialMapper.insert(um);
        return toDTO(um);
    }

    /** 编辑（仅限草稿/被拒绝/自用已通过状态） */
    @Transactional
    public UserMaterialDTO update(Long userId, Long id, UserMaterialRequest req) {
        UserMaterial um = getOwned(userId, id);
        if (!"draft".equals(um.getStatus()) && !"rejected".equals(um.getStatus()) && !"approved".equals(um.getStatus())) {
            throw new RuntimeException("当前状态不允许编辑");
        }
        um.setCategory(req.getCategory());
        um.setTitle(req.getTitle());
        um.setContent(req.getContent());
        um.setTags(req.getTags());
        // 已通过的素材被编辑后，重置为草稿，需重新提交审核
        if ("approved".equals(um.getStatus())) {
            um.setStatus("draft");
        }
        userMaterialMapper.updateById(um);
        return toDTO(um);
    }

    /** 提交审核 */
    @Transactional
    public UserMaterialDTO submit(Long userId, Long id) {
        UserMaterial um = getOwned(userId, id);
        if (!"draft".equals(um.getStatus()) && !"rejected".equals(um.getStatus()) && !"approved".equals(um.getStatus())) {
            throw new RuntimeException("当前状态不允许提交");
        }
        um.setStatus("pending");
        um.setAdminComment(null);
        userMaterialMapper.updateById(um);
        return toDTO(um);
    }

    /** 更新自建素材的收藏分组 */
    @Transactional
    public void updateGroup(Long userId, Long id, String groupName) {
        UserMaterial um = getOwned(userId, id);
        um.setFavoriteGroup(groupName);
        userMaterialMapper.updateById(um);
    }

    /** 删除（审核中不允许删除；已通过则同步删除公共素材库对应记录） */
    @Transactional
    public void delete(Long userId, Long id) {
        UserMaterial um = getOwned(userId, id);
        if ("pending".equals(um.getStatus())) {
            throw new RuntimeException("素材审核中，暂不允许删除");
        }
        // 若已通过审核，同步删除公共素材库中由该用户提交的对应记录
        if ("approved".equals(um.getStatus())) {
            materialMapper.delete(new LambdaQueryWrapper<Material>()
                    .eq(Material::getSourceUserId, userId)
                    .eq(Material::getTitle, um.getTitle()));
        }
        userMaterialMapper.deleteById(id);
    }

    /** 管理员：审核通过 → 写入或更新素材库 */
    @Transactional
    public void approve(Long adminId, Long id, String comment) {
        UserMaterial um = userMaterialMapper.selectById(id);
        if (um == null) throw new RuntimeException("素材不存在");
        if (!"pending".equals(um.getStatus())) throw new RuntimeException("非待审核状态");

        um.setStatus("approved");
        um.setAdminComment(comment);
        userMaterialMapper.updateById(um);

        // 查找公共素材库中是否已有该用户的同名素材（修改重审场景）
        Material existing = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getSourceUserId, um.getUserId())
                .eq(Material::getTitle, um.getTitle())
                .last("LIMIT 1"));
        if (existing != null) {
            // 更新已有记录
            existing.setCategory(um.getCategory());
            existing.setContent(um.getContent());
            existing.setStatus("PUBLISHED");
            materialMapper.updateById(existing);
        } else {
            // 首次通过：新增记录
            Material material = new Material();
            material.setCategory(um.getCategory());
            material.setTitle(um.getTitle());
            material.setContent(um.getContent());
            material.setSourceUserId(um.getUserId());
            material.setStatus("PUBLISHED");
            materialMapper.insert(material);
        }

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
        dto.setFavoriteGroup(um.getFavoriteGroup() != null ? um.getFavoriteGroup() : "我的灵感");
        dto.setCreatedAt(um.getCreatedAt());
        dto.setUpdatedAt(um.getUpdatedAt());
        return dto;
    }
}
