package com.history.creation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.history.creation.entity.FavoriteGroup;
import com.history.creation.entity.MaterialFavorite;
import com.history.creation.mapper.FavoriteGroupMapper;
import com.history.creation.mapper.MaterialFavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FavoriteGroupService {
    private final FavoriteGroupMapper favoriteGroupMapper;
    private final MaterialFavoriteMapper materialFavoriteMapper;

    public FavoriteGroupService(FavoriteGroupMapper favoriteGroupMapper, MaterialFavoriteMapper materialFavoriteMapper) {
        this.favoriteGroupMapper = favoriteGroupMapper;
        this.materialFavoriteMapper = materialFavoriteMapper;
    }

    public List<FavoriteGroup> listByUser(Long userId) {
        return favoriteGroupMapper.selectList(
                new LambdaQueryWrapper<FavoriteGroup>()
                        .eq(FavoriteGroup::getUserId, userId)
                        .orderByDesc(FavoriteGroup::getCreatedAt));
    }

    @Transactional
    public FavoriteGroup create(Long userId, String name) {
        if (name == null || name.isBlank()) throw new RuntimeException("分组名称不能为空");
        FavoriteGroup existing = favoriteGroupMapper.selectOne(
                new LambdaQueryWrapper<FavoriteGroup>()
                        .eq(FavoriteGroup::getUserId, userId)
                        .eq(FavoriteGroup::getName, name.trim()));
        if (existing != null) throw new RuntimeException("分组已存在");
        FavoriteGroup group = new FavoriteGroup();
        group.setUserId(userId);
        group.setName(name.trim());
        favoriteGroupMapper.insert(group);
        return group;
    }

    @Transactional
    public FavoriteGroup rename(Long userId, Long groupId, String newName) {
        if (newName == null || newName.isBlank()) throw new RuntimeException("分组名称不能为空");
        FavoriteGroup group = favoriteGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) throw new RuntimeException("分组不存在");
        FavoriteGroup existing = favoriteGroupMapper.selectOne(
                new LambdaQueryWrapper<FavoriteGroup>()
                        .eq(FavoriteGroup::getUserId, userId)
                        .eq(FavoriteGroup::getName, newName.trim())
                        .ne(FavoriteGroup::getId, groupId));
        if (existing != null) throw new RuntimeException("分组名称已存在");
        String oldName = group.getName();
        group.setName(newName.trim());
        favoriteGroupMapper.updateById(group);
        materialFavoriteMapper.update(null,
                new LambdaUpdateWrapper<MaterialFavorite>()
                        .eq(MaterialFavorite::getUserId, userId)
                        .eq(MaterialFavorite::getGroupName, oldName)
                        .set(MaterialFavorite::getGroupName, newName.trim()));
        return group;
    }

    @Transactional
    public void delete(Long userId, Long groupId) {
        FavoriteGroup group = favoriteGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) throw new RuntimeException("分组不存在");
        materialFavoriteMapper.update(null,
                new LambdaUpdateWrapper<MaterialFavorite>()
                        .eq(MaterialFavorite::getUserId, userId)
                        .eq(MaterialFavorite::getGroupName, group.getName())
                        .set(MaterialFavorite::getGroupName, "未定义"));
        favoriteGroupMapper.deleteById(groupId);
    }
}
