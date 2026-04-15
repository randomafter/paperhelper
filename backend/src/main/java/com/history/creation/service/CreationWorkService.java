package com.history.creation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.history.creation.dto.CreationWorkDTO;
import com.history.creation.entity.CreationSeries;
import com.history.creation.entity.CreationWork;
import com.history.creation.mapper.CreationSeriesMapper;
import com.history.creation.mapper.CreationWorkMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CreationWorkService {

    private final CreationWorkMapper workMapper;
    private final CreationSeriesMapper seriesMapper;

    public CreationWorkService(CreationWorkMapper workMapper, CreationSeriesMapper seriesMapper) {
        this.workMapper = workMapper;
        this.seriesMapper = seriesMapper;
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

        String normalizedGroup = normalizeGroupName(groupName);
        if (normalizedGroup != null) {
            applyGroupSettings(userId, work, normalizedGroup);
        }

        workMapper.insert(work);
        return toDTO(work);
    }

    /** 保存/更新作品内容（明确更新updatedAt） */
    @Transactional
    public CreationWorkDTO save(Long userId, Long id, String title, String content,
                                String pinnedOutline, String charProfiles, String outlineData,
                                String charProfilesJson, String worldSetting, String plotHooks) {
        if (id == null) {
            return create(userId, title, content, null);
        }
        CreationWork work = workMapper.selectById(id);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new RuntimeException("作品不存在");
        }
        work.setTitle(title != null && !title.isBlank() ? title : "未命名");
        work.setContent(content != null ? content : "");
        if (pinnedOutline != null) work.setPinnedOutline(pinnedOutline);
        if (charProfiles != null) work.setCharProfiles(charProfiles);
        if (outlineData != null) work.setOutlineData(outlineData);
        if (charProfilesJson != null) work.setCharProfilesJson(charProfilesJson);
        if (worldSetting != null) work.setWorldSetting(worldSetting);
        if (plotHooks != null) work.setPlotHooks(plotHooks);
        work.setUpdatedAt(LocalDateTime.now());
        workMapper.updateById(work);
        return toDTO(work);
    }

    /** 更新分组 */
    @Transactional
    public CreationWorkDTO updateGroup(Long userId, Long id, String groupName, Boolean syncSeriesSettings) {
        CreationWork work = workMapper.selectById(id);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new RuntimeException("作品不存在");
        }
        work.setGroupName(groupName);

        if (Boolean.TRUE.equals(syncSeriesSettings)) {
            applyGroupSettings(userId, work, groupName);
        }

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

    private void applyGroupSettings(Long userId, CreationWork work, String groupName) {
        String normalizedGroup = normalizeGroupName(groupName);
        if (normalizedGroup == null) return;

        CreationSeries series = null;
        try {
            series = seriesMapper.selectOne(
                    new LambdaQueryWrapper<CreationSeries>()
                            .eq(CreationSeries::getUserId, userId)
                            .eq(CreationSeries::getGroupName, normalizedGroup)
            );
        } catch (Exception ignored) {
            // 兼容旧库缺少 creation_series 表的场景
        }

        if (series != null) {
            work.setPinnedOutline(series.getPinnedOutline());
            work.setCharProfiles(series.getCharProfiles());
            work.setCharProfilesJson(series.getCharProfilesJson());
            work.setWorldSetting(series.getWorldSetting());
            work.setPlotHooks(series.getPlotHooks());
            return;
        }

        List<CreationWork> sameGroupWorks = workMapper.selectList(
                new LambdaQueryWrapper<CreationWork>()
                        .eq(CreationWork::getUserId, userId)
                        .eq(CreationWork::getGroupName, normalizedGroup)
                        .ne(work.getId() != null, CreationWork::getId, work.getId())
                        .orderByDesc(CreationWork::getUpdatedAt)
                        .orderByDesc(CreationWork::getId)
                        .last("limit 1")
        );
        if (sameGroupWorks.isEmpty()) return;

        CreationWork latestWork = sameGroupWorks.get(0);
        work.setPinnedOutline(latestWork.getPinnedOutline());
        work.setCharProfiles(latestWork.getCharProfiles());
        work.setCharProfilesJson(latestWork.getCharProfilesJson());
        work.setWorldSetting(latestWork.getWorldSetting());
        work.setPlotHooks(latestWork.getPlotHooks());
        work.setOutlineData(latestWork.getOutlineData());
    }

    private String normalizeGroupName(String groupName) {
        if (groupName == null) return null;
        String normalized = groupName.trim();
        if (normalized.isEmpty() || "未分组".equals(normalized) || "全部".equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private CreationWorkDTO toDTO(CreationWork work) {
        CreationWorkDTO dto = new CreationWorkDTO();
        dto.setId(work.getId());
        dto.setUserId(work.getUserId());
        dto.setTitle(work.getTitle());
        dto.setContent(work.getContent());
        dto.setGroupName(work.getGroupName());
        dto.setPinnedOutline(work.getPinnedOutline());
        dto.setCharProfiles(work.getCharProfiles());
        dto.setOutlineData(work.getOutlineData());
        dto.setCharProfilesJson(work.getCharProfilesJson());
        dto.setWorldSetting(work.getWorldSetting());
        dto.setPlotHooks(work.getPlotHooks());
        dto.setLastOpenedAt(work.getLastOpenedAt());
        dto.setCreatedAt(work.getCreatedAt());
        dto.setUpdatedAt(work.getUpdatedAt());
        return dto;
    }
}
