package com.history.creation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.history.creation.dto.CreationSeriesDTO;
import com.history.creation.entity.CreationSeries;
import com.history.creation.entity.CreationWork;
import com.history.creation.mapper.CreationSeriesMapper;
import com.history.creation.mapper.CreationWorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreationSeriesService {

    private final CreationSeriesMapper seriesMapper;
    private final CreationWorkMapper workMapper;

    /** 获取用户某组的系列档案，不存在则回退到同组作品设定 */
    public CreationSeriesDTO getByGroup(Long userId, String groupName) {
        if (groupName == null || groupName.isBlank()) return null;
        try {
            CreationSeries s = seriesMapper.selectOne(
                new LambdaQueryWrapper<CreationSeries>()
                    .eq(CreationSeries::getUserId, userId)
                    .eq(CreationSeries::getGroupName, groupName)
            );
            if (s != null) return toDTO(s);
        } catch (Exception ignored) {
            // 兼容旧库缺少 creation_series 表的场景
        }
        return buildFromLatestWork(userId, groupName);
    }

    /** 保存/更新系列档案（upsert） */
    @Transactional
    public CreationSeriesDTO save(Long userId, String groupName,
                                  String pinnedOutline, String charProfiles,
                                  String outlineData, String charProfilesJson,
                                  String worldSetting, String plotHooks) {
        if (groupName == null || groupName.isBlank()) {
            throw new RuntimeException("系列名称不能为空");
        }

        CreationSeries existing;
        try {
            existing = seriesMapper.selectOne(
                new LambdaQueryWrapper<CreationSeries>()
                    .eq(CreationSeries::getUserId, userId)
                    .eq(CreationSeries::getGroupName, groupName)
            );
        } catch (Exception e) {
            throw new RuntimeException("当前数据库尚未初始化系列档案表，请先执行建表脚本");
        }

        if (existing == null) {
            existing = new CreationSeries();
            existing.setUserId(userId);
            existing.setGroupName(groupName);
            existing.setCreatedAt(LocalDateTime.now());
        }
        if (pinnedOutline != null) existing.setPinnedOutline(pinnedOutline);
        if (charProfiles != null) existing.setCharProfiles(charProfiles);
        if (outlineData != null) existing.setOutlineData(outlineData);
        if (charProfilesJson != null) existing.setCharProfilesJson(charProfilesJson);
        if (worldSetting != null) existing.setWorldSetting(worldSetting);
        if (plotHooks != null) existing.setPlotHooks(plotHooks);
        existing.setUpdatedAt(LocalDateTime.now());

        if (existing.getId() == null) seriesMapper.insert(existing);
        else seriesMapper.updateById(existing);
        return toDTO(existing);
    }

    private CreationSeriesDTO buildFromLatestWork(Long userId, String groupName) {
        List<CreationWork> works = workMapper.selectList(
            new LambdaQueryWrapper<CreationWork>()
                .eq(CreationWork::getUserId, userId)
                .eq(CreationWork::getGroupName, groupName)
                .orderByDesc(CreationWork::getUpdatedAt)
                .orderByDesc(CreationWork::getId)
                .last("limit 1")
        );
        if (works.isEmpty()) return null;
        CreationWork work = works.get(0);
        CreationSeriesDTO dto = new CreationSeriesDTO();
        dto.setUserId(userId);
        dto.setGroupName(groupName);
        dto.setPinnedOutline(work.getPinnedOutline());
        dto.setCharProfiles(work.getCharProfiles());
        dto.setOutlineData(work.getOutlineData());
        dto.setCharProfilesJson(work.getCharProfilesJson());
        dto.setWorldSetting(work.getWorldSetting());
        dto.setPlotHooks(work.getPlotHooks());
        dto.setCreatedAt(work.getCreatedAt());
        dto.setUpdatedAt(work.getUpdatedAt());
        return dto;
    }

    private CreationSeriesDTO toDTO(CreationSeries s) {
        CreationSeriesDTO dto = new CreationSeriesDTO();
        dto.setId(s.getId());
        dto.setUserId(s.getUserId());
        dto.setGroupName(s.getGroupName());
        dto.setPinnedOutline(s.getPinnedOutline());
        dto.setCharProfiles(s.getCharProfiles());
        dto.setOutlineData(s.getOutlineData());
        dto.setCharProfilesJson(s.getCharProfilesJson());
        dto.setWorldSetting(s.getWorldSetting());
        dto.setPlotHooks(s.getPlotHooks());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }
}
