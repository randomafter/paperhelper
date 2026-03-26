package com.history.creation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.history.creation.dto.CreationSeriesDTO;
import com.history.creation.entity.CreationSeries;
import com.history.creation.mapper.CreationSeriesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CreationSeriesService {

    private final CreationSeriesMapper seriesMapper;

    /** 获取用户某组的系列档案，不存在则返回 null */
    public CreationSeriesDTO getByGroup(Long userId, String groupName) {
        if (groupName == null || groupName.isBlank()) return null;
        CreationSeries s = seriesMapper.selectOne(
            new LambdaQueryWrapper<CreationSeries>()
                .eq(CreationSeries::getUserId, userId)
                .eq(CreationSeries::getGroupName, groupName)
        );
        return s == null ? null : toDTO(s);
    }

    /** 保存/更新系列档案（upsert） */
    @Transactional
    public CreationSeriesDTO save(Long userId, String groupName,
                                  String pinnedOutline, String charProfiles,
                                  String charProfilesJson, String worldSetting,
                                  String plotHooks) {
        if (groupName == null || groupName.isBlank())
            throw new RuntimeException("系列名称不能为空");

        CreationSeries existing = seriesMapper.selectOne(
            new LambdaQueryWrapper<CreationSeries>()
                .eq(CreationSeries::getUserId, userId)
                .eq(CreationSeries::getGroupName, groupName)
        );

        if (existing == null) {
            existing = new CreationSeries();
            existing.setUserId(userId);
            existing.setGroupName(groupName);
            existing.setCreatedAt(LocalDateTime.now());
        }
        if (pinnedOutline  != null) existing.setPinnedOutline(pinnedOutline);
        if (charProfiles   != null) existing.setCharProfiles(charProfiles);
        if (charProfilesJson != null) existing.setCharProfilesJson(charProfilesJson);
        if (worldSetting   != null) existing.setWorldSetting(worldSetting);
        if (plotHooks      != null) existing.setPlotHooks(plotHooks);
        existing.setUpdatedAt(LocalDateTime.now());

        if (existing.getId() == null) seriesMapper.insert(existing);
        else seriesMapper.updateById(existing);
        return toDTO(existing);
    }

    private CreationSeriesDTO toDTO(CreationSeries s) {
        CreationSeriesDTO dto = new CreationSeriesDTO();
        dto.setId(s.getId());
        dto.setUserId(s.getUserId());
        dto.setGroupName(s.getGroupName());
        dto.setPinnedOutline(s.getPinnedOutline());
        dto.setCharProfiles(s.getCharProfiles());
        dto.setCharProfilesJson(s.getCharProfilesJson());
        dto.setWorldSetting(s.getWorldSetting());
        dto.setPlotHooks(s.getPlotHooks());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }
}
