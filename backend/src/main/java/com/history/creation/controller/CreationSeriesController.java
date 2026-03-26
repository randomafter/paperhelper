package com.history.creation.controller;

import com.history.creation.common.Result;
import com.history.creation.dto.CreationSeriesDTO;
import com.history.creation.security.SecurityUtils;
import com.history.creation.service.CreationSeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class CreationSeriesController {

    private final CreationSeriesService seriesService;

    /** 获取某组的系列档案 */
    @GetMapping
    public Result<CreationSeriesDTO> get(@RequestParam String groupName) {
        Long userId = SecurityUtils.getCurrentUserId();
        CreationSeriesDTO dto = seriesService.getByGroup(userId, groupName);
        return Result.ok(dto);
    }

    /** 保存/更新系列档案 */
    @PutMapping
    public Result<CreationSeriesDTO> save(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String groupName      = body.get("groupName");
        String pinnedOutline  = body.get("pinnedOutline");
        String charProfiles   = body.get("charProfiles");
        String charProfilesJson = body.get("charProfilesJson");
        String worldSetting   = body.get("worldSetting");
        String plotHooks      = body.get("plotHooks");
        try {
            return Result.ok(seriesService.save(userId, groupName,
                    pinnedOutline, charProfiles,
                    charProfilesJson, worldSetting, plotHooks));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
