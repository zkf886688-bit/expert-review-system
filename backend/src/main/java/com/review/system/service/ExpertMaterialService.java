package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.common.BusinessException;
import com.review.system.entity.Expert;
import com.review.system.entity.ExpertMaterial;
import com.review.system.mapper.ExpertMapper;
import com.review.system.mapper.ExpertMaterialMapper;
import com.review.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpertMaterialService {

    private final ExpertMaterialMapper materialMapper;
    private final ExpertMapper expertMapper;
    private final FileService fileService;
    private final AuditService auditService;

    public List<ExpertMaterial> list(Long expertId) {
        requireExpert(expertId);
        return materialMapper.selectList(new LambdaQueryWrapper<ExpertMaterial>()
                .eq(ExpertMaterial::getExpertId, expertId)
                .orderByDesc(ExpertMaterial::getCreatedAt));
    }

    @Transactional
    public ExpertMaterial upload(Long expertId, MultipartFile file, String materialType) {
        requireExpert(expertId);
        String path = fileService.save(file, "expert/" + expertId);
        ExpertMaterial material = new ExpertMaterial();
        material.setExpertId(expertId);
        material.setFileName(file.getOriginalFilename());
        material.setFilePath(path);
        material.setFileSize(file.getSize());
        material.setMaterialType(StringUtils.hasText(materialType) ? materialType.trim() : "其他");
        material.setUploadedBy(SecurityUtils.currentUserId());
        material.setCreatedAt(LocalDateTime.now());
        materialMapper.insert(material);
        auditService.log("UPLOAD_EXPERT_MATERIAL", "EXPERT", expertId,
                "上传资质材料: " + material.getFileName());
        return material;
    }

    @Transactional
    public void delete(Long expertId, Long materialId) {
        requireExpert(expertId);
        ExpertMaterial material = materialMapper.selectById(materialId);
        if (material == null || !expertId.equals(material.getExpertId())) {
            throw new BusinessException("材料不存在");
        }
        materialMapper.deleteById(materialId);
        auditService.log("DELETE_EXPERT_MATERIAL", "EXPERT", expertId,
                "删除资质材料: " + material.getFileName());
    }

    private Expert requireExpert(Long expertId) {
        Expert expert = expertMapper.selectById(expertId);
        if (expert == null) {
            throw new BusinessException("专家不存在");
        }
        return expert;
    }
}
