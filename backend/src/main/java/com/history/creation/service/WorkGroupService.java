package com.history.creation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.history.creation.entity.CreationWork;
import com.history.creation.entity.WorkGroup;
import com.history.creation.mapper.CreationWorkMapper;
import com.history.creation.mapper.WorkGroupMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class WorkGroupService {
    private final WorkGroupMapper workGroupMapper;
    private final CreationWorkMapper creationWorkMapper;

    public WorkGroupService(WorkGroupMapper workGroupMapper, CreationWorkMapper creationWorkMapper) {
        this.workGroupMapper = workGroupMapper;
        this.creationWorkMapper = creationWorkMapper;
    }

    public List<WorkGroup> listByUser(Long userId) {
        return workGroupMapper.selectList(
                new LambdaQueryWrapper<WorkGroup>()
                        .eq(WorkGroup::getUserId, userId)
                        .orderByDesc(WorkGroup::getCreatedAt));
    }

    @Transactional
    public WorkGroup create(Long userId, String name) {
        if (name == null || name.isBlank()) throw new RuntimeException("分组名称不能为空");
        WorkGroup existing = workGroupMapper.selectOne(
                new LambdaQueryWrapper<WorkGroup>()
                        .eq(WorkGroup::getUserId, userId)
                        .eq(WorkGroup::getName, name.trim()));
        if (existing != null) throw new RuntimeException("分组已存在");
        WorkGroup group = new WorkGroup();
        group.setUserId(userId);
        group.setName(name.trim());
        workGroupMapper.insert(group);
        return group;
    }

    @Transactional
    public WorkGroup rename(Long userId, Long groupId, String newName) {
        if (newName == null || newName.isBlank()) throw new RuntimeException("分组名称不能为空");
        WorkGroup group = workGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) throw new RuntimeException("分组不存在");
        WorkGroup existing = workGroupMapper.selectOne(
                new LambdaQueryWrapper<WorkGroup>()
                        .eq(WorkGroup::getUserId, userId)
                        .eq(WorkGroup::getName, newName.trim())
                        .ne(WorkGroup::getId, groupId));
        if (existing != null) throw new RuntimeException("分组名称已存在");
        String oldName = group.getName();
        group.setName(newName.trim());
        workGroupMapper.updateById(group);
        creationWorkMapper.update(null,
                new LambdaUpdateWrapper<CreationWork>()
                        .eq(CreationWork::getUserId, userId)
                        .eq(CreationWork::getGroupName, oldName)
                        .set(CreationWork::getGroupName, newName.trim()));
        return group;
    }

    @Transactional
    public void delete(Long userId, Long groupId) {
        WorkGroup group = workGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) throw new RuntimeException("分组不存在");
        // 删除该分组下的所有作品
        creationWorkMapper.delete(
                new LambdaQueryWrapper<CreationWork>()
                        .eq(CreationWork::getUserId, userId)
                        .eq(CreationWork::getGroupName, group.getName()));
        workGroupMapper.deleteById(groupId);
    }
}
