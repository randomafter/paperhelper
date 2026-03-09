package com.history.creation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.history.creation.dto.ExcelImportResult;
import com.history.creation.dto.MaterialCreateRequest;
import com.history.creation.dto.MaterialDTO;
import com.history.creation.dto.MaterialSearchRequest;
import com.history.creation.dto.MaterialUpdateRequest;
import com.history.creation.entity.Material;
import com.history.creation.entity.MaterialFavorite;
import com.history.creation.entity.MaterialTag;
import com.history.creation.entity.Tag;
import com.history.creation.mapper.MaterialFavoriteMapper;
import com.history.creation.mapper.MaterialMapper;
import com.history.creation.mapper.MaterialTagMapper;
import com.history.creation.mapper.TagMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialService {

    private final MaterialMapper materialMapper;
    private final TagMapper tagMapper;
    private final MaterialTagMapper materialTagMapper;
    private final MaterialFavoriteMapper materialFavoriteMapper;

    public MaterialService(MaterialMapper materialMapper, TagMapper tagMapper,
                       MaterialTagMapper materialTagMapper, MaterialFavoriteMapper materialFavoriteMapper) {
        this.materialMapper = materialMapper;
        this.tagMapper = tagMapper;
        this.materialTagMapper = materialTagMapper;
        this.materialFavoriteMapper = materialFavoriteMapper;
    }

    public Page<MaterialDTO> searchMaterials(MaterialSearchRequest req, Long userId) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        
        if (req.getDynasty() != null && !req.getDynasty().isEmpty()) {
            wrapper.eq(Material::getDynasty, req.getDynasty());
        }
        if (req.getCategory() != null && !req.getCategory().isEmpty()) {
            wrapper.eq(Material::getCategory, req.getCategory());
        }
        if (req.getKeyword() != null && !req.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(Material::getTitle, req.getKeyword())
                    .or().like(Material::getContent, req.getKeyword()));
        }
        if (req.getTag() != null && !req.getTag().isEmpty()) {
            wrapper.exists("SELECT 1 FROM material_tag mt, tag t " +
                    "WHERE mt.material_id = material.id " +
                    "AND mt.tag_id = t.id " +
                    "AND t.name = {0}", req.getTag());
        }
        
        wrapper.orderByDesc(Material::getCreatedAt);
        
        Page<Material> page = new Page<>(req.getPage(), req.getSize());
        Page<Material> resultPage = materialMapper.selectPage(page, wrapper);
        
        Page<MaterialDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<MaterialDTO> dtoList = resultPage.getRecords().stream()
                .map(material -> convertToDTO(material, userId))
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }

    public MaterialDTO getMaterialById(Long id, Long userId) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new RuntimeException("素材不存在");
        }
        return convertToDTO(material, userId);
    }

    @Transactional
    public MaterialDTO createMaterial(MaterialCreateRequest req) {
        Material material = new Material();
        material.setDynasty(req.getDynasty());
        material.setCategory(req.getCategory());
        material.setTitle(req.getTitle());
        material.setContent(req.getContent());
        material.setSourceUrl(req.getSourceUrl());
        materialMapper.insert(material);
        
        if (req.getTags() != null && !req.getTags().isEmpty()) {
            for (String tagName : req.getTags()) {
                Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getName, tagName));
                if (tag == null) {
                    tag = new Tag();
                    tag.setName(tagName);
                    tagMapper.insert(tag);
                }
                
                MaterialTag materialTag = new MaterialTag();
                materialTag.setMaterialId(material.getId());
                materialTag.setTagId(tag.getId());
                materialTagMapper.insert(materialTag);
            }
        }
        
        return convertToDTO(material, null);
    }

    @Transactional
    public MaterialDTO updateMaterial(MaterialUpdateRequest req) {
        Material material = materialMapper.selectById(req.getId());
        if (material == null) {
            throw new RuntimeException("素材不存在");
        }
        
        material.setDynasty(req.getDynasty());
        material.setCategory(req.getCategory());
        material.setTitle(req.getTitle());
        material.setContent(req.getContent());
        material.setSourceUrl(req.getSourceUrl());
        materialMapper.updateById(material);
        
        materialTagMapper.delete(new LambdaQueryWrapper<MaterialTag>()
                .eq(MaterialTag::getMaterialId, req.getId()));
        
        if (req.getTags() != null && !req.getTags().isEmpty()) {
            for (String tagName : req.getTags()) {
                Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getName, tagName));
                if (tag == null) {
                    tag = new Tag();
                    tag.setName(tagName);
                    tagMapper.insert(tag);
                }
                
                MaterialTag materialTag = new MaterialTag();
                materialTag.setMaterialId(material.getId());
                materialTag.setTagId(tag.getId());
                materialTagMapper.insert(materialTag);
            }
        }
        
        return convertToDTO(material, null);
    }

    @Transactional
    public void deleteMaterial(Long id) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new RuntimeException("素材不存在");
        }
        materialMapper.deleteById(id);
    }

    @Transactional
    public void toggleFavorite(Long userId, Long materialId) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new RuntimeException("素材不存在");
        }
        
        MaterialFavorite favorite = materialFavoriteMapper.selectOne(
                new LambdaQueryWrapper<MaterialFavorite>()
                        .eq(MaterialFavorite::getUserId, userId)
                        .eq(MaterialFavorite::getMaterialId, materialId));
        
        if (favorite != null) {
            materialFavoriteMapper.deleteById(favorite.getId());
        } else {
            favorite = new MaterialFavorite();
            favorite.setUserId(userId);
            favorite.setMaterialId(materialId);
            materialFavoriteMapper.insert(favorite);
        }
    }

    public List<MaterialDTO> getFavorites(Long userId) {
        List<MaterialFavorite> favorites = materialFavoriteMapper.selectList(
                new LambdaQueryWrapper<MaterialFavorite>()
                        .eq(MaterialFavorite::getUserId, userId)
                        .orderByDesc(MaterialFavorite::getCreatedAt));
        
        List<MaterialDTO> result = new ArrayList<>();
        for (MaterialFavorite favorite : favorites) {
            Material material = materialMapper.selectById(favorite.getMaterialId());
            if (material != null) {
                result.add(convertToDTO(material, userId));
            }
        }
        return result;
    }

    public List<String> getAllTags() {
        List<Tag> tags = tagMapper.selectList(null);
        return tags.stream().map(Tag::getName).collect(Collectors.toList());
    }

    private MaterialDTO convertToDTO(Material material, Long userId) {
        MaterialDTO dto = new MaterialDTO();
        dto.setId(material.getId());
        dto.setDynasty(material.getDynasty());
        dto.setCategory(material.getCategory());
        dto.setTitle(material.getTitle());
        dto.setContent(material.getContent());
        dto.setSourceUrl(material.getSourceUrl());
        dto.setCreatedAt(material.getCreatedAt());
        dto.setUpdatedAt(material.getUpdatedAt());
        
        List<MaterialTag> materialTags = materialTagMapper.selectList(
                new LambdaQueryWrapper<MaterialTag>()
                        .eq(MaterialTag::getMaterialId, material.getId()));
        List<String> tagNames = new ArrayList<>();
        for (MaterialTag mt : materialTags) {
            Tag tag = tagMapper.selectById(mt.getTagId());
            if (tag != null) {
                tagNames.add(tag.getName());
            }
        }
        dto.setTags(tagNames);
        
        if (userId != null) {
            MaterialFavorite favorite = materialFavoriteMapper.selectOne(
                    new LambdaQueryWrapper<MaterialFavorite>()
                            .eq(MaterialFavorite::getUserId, userId)
                            .eq(MaterialFavorite::getMaterialId, material.getId()));
            dto.setIsFavorite(favorite != null);
        }
        
        return dto;
    }

    @Transactional
    public ExcelImportResult importFromExcel(MultipartFile file) {
        ExcelImportResult result = new ExcelImportResult();
        result.setSuccessCount(0);
        result.setFailCount(0);
        result.setErrors(new ArrayList<>());
        
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    String dynasty = getCellValueAsString(row.getCell(0));
                    String category = getCellValueAsString(row.getCell(1));
                    String title = getCellValueAsString(row.getCell(2));
                    String content = getCellValueAsString(row.getCell(3));
                    String sourceUrl = getCellValueAsString(row.getCell(4));
                    String tagsStr = getCellValueAsString(row.getCell(5));
                    
                    if (dynasty == null || dynasty.trim().isEmpty() ||
                        category == null || category.trim().isEmpty() ||
                        title == null || title.trim().isEmpty() ||
                        content == null || content.trim().isEmpty()) {
                        result.setFailCount(result.getFailCount() + 1);
                        result.getErrors().add("第" + (i + 1) + "行：必填字段为空");
                        continue;
                    }
                    
                    Material material = new Material();
                    material.setDynasty(dynasty.trim());
                    material.setCategory(category.trim());
                    material.setTitle(title.trim());
                    material.setContent(content.trim());
                    if (sourceUrl != null && !sourceUrl.trim().isEmpty()) {
                        material.setSourceUrl(sourceUrl.trim());
                    }
                    materialMapper.insert(material);
                    
                    if (tagsStr != null && !tagsStr.trim().isEmpty()) {
                        String[] tags = tagsStr.split(",");
                        for (String tagName : tags) {
                            String trimmedTag = tagName.trim();
                            if (trimmedTag.isEmpty()) continue;
                            
                            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                                    .eq(Tag::getName, trimmedTag));
                            if (tag == null) {
                                tag = new Tag();
                                tag.setName(trimmedTag);
                                tagMapper.insert(tag);
                            }
                            
                            MaterialTag materialTag = new MaterialTag();
                            materialTag.setMaterialId(material.getId());
                            materialTag.setTagId(tag.getId());
                            materialTagMapper.insert(materialTag);
                        }
                    }
                    
                    result.setSuccessCount(result.getSuccessCount() + 1);
                    
                } catch (Exception e) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.getErrors().add("第" + (i + 1) + "行：" + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            result.getErrors().add("文件处理失败：" + e.getMessage());
        }
        
        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> String.valueOf(cell.getNumericCellValue());
            default -> "";
        };
    }
}
