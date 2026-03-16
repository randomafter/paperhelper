package com.history.creation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.history.creation.dto.CreationWorkDTO;
import com.history.creation.entity.CreationWork;
import com.history.creation.mapper.CreationWorkMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CreationWorkService {

    private final CreationWorkMapper workMapper;

    public CreationWorkService(CreationWorkMapper workMapper) {
        this.workMapper = workMapper;
    }

    /**
     * 分页查询当前用户作品
     * 排序：最近打开的置顶（last_opened_at DESC），其次按更新时间
     */
    public Page<CreationWorkDTO> listByUser(Long userId, int page, int size) {
        Page<CreationWork> pageParam = new Page<>(page, size);
        Page<CreationWork> result = workMapper.selectPage(pageParam,
                new LambdaQueryWrapper<CreationWork>()
                        .eq(CreationWork::getUserId, userId)
                        .orderByDesc(CreationWork::getLastOpenedAt)
                        .orderByDesc(CreationWork::getUpdatedAt));
        Page<CreationWorkDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::toDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    /** 获取单个作品并更新最近打开时间 */
    @Transactional
    public CreationWorkDTO getByIdAndUser(Long id, Long userId) {
        CreationWork work = workMapper.selectById(id);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new RuntimeException("作品不存在");
        }
        // 记录最近打开时间（用于置顶）
        work.setLastOpenedAt(LocalDateTime.now());
        workMapper.updateById(work);
        return toDTO(work);
    }

    /** 创建作品 */
    @Transactional
    public CreationWorkDTO create(Long userId, String title, String content, String groupName) {
        CreationWork work = new CreationWork();
        work.setUserId(userId);
        work.setTitle(title != null && !title.isBlank() ? title : "未命名");
        work.setContent(content != null ? content : "");
        work.setGroupName(groupName);
        workMapper.insert(work);
        return toDTO(work);
    }

    /** 保存/更新作品内容（明确更新updatedAt） */
    @Transactional
    public CreationWorkDTO save(Long userId, Long id, String title, String content) {
        if (id == null) {
            return create(userId, title, content, null);
        }
        CreationWork work = workMapper.selectById(id);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new RuntimeException("作品不存在");
        }
        work.setTitle(title != null && !title.isBlank() ? title : "未命名");
        work.setContent(content != null ? content : "");
        work.setUpdatedAt(LocalDateTime.now());
        workMapper.updateById(work);
        return toDTO(work);
    }

    /** 更新分组 */
    @Transactional
    public CreationWorkDTO updateGroup(Long userId, Long id, String groupName) {
        CreationWork work = workMapper.selectById(id);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new RuntimeException("作品不存在");
        }
        work.setGroupName(groupName);
        workMapper.updateById(work);
        return toDTO(work);
    }

    /** 删除作品 */
    @Transactional
    public void delete(Long userId, Long id) {
        CreationWork work = workMapper.selectById(id);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new RuntimeException("作品不存在");
        }
        workMapper.deleteById(id);
    }

    private CreationWorkDTO toDTO(CreationWork work) {
        CreationWorkDTO dto = new CreationWorkDTO();
        dto.setId(work.getId());
        dto.setUserId(work.getUserId());
        dto.setTitle(work.getTitle());
        dto.setContent(work.getContent());
        dto.setGroupName(work.getGroupName());
        dto.setLastOpenedAt(work.getLastOpenedAt());
        dto.setCreatedAt(work.getCreatedAt());
        dto.setUpdatedAt(work.getUpdatedAt());
        return dto;
    }
}
