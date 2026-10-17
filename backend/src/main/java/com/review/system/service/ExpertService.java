package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.review.system.common.BusinessException;
import com.review.system.config.ExpertLoginAccountSyncer;
import com.review.system.dto.ExpertQuery;
import com.review.system.dto.ExpertSaveRequest;
import com.review.system.dto.PageResult;
import com.review.system.entity.Expert;
import com.review.system.entity.SysUser;
import com.review.system.mapper.ExpertMapper;
import com.review.system.mapper.SysUserMapper;
import com.review.system.util.MaskUtil;
import com.review.system.util.PiiCryptoUtil;
import com.review.system.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertService {

    private final ExpertMapper expertMapper;
    private final SysUserMapper sysUserMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final ExpertLoginAccountSyncer expertLoginAccountSyncer;

    @org.springframework.beans.factory.annotation.Value("${app.jwt.secret}")
    private String jwtSecret;

    @org.springframework.beans.factory.annotation.Value("${app.pii.encryption-key:}")
    private String piiEncryptionKey;

    public PageResult<Expert> page(ExpertQuery query) {
        LambdaQueryWrapper<Expert> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Expert::getName, query.getName());
        }
        if (StringUtils.hasText(query.getOrganization())) {
            wrapper.like(Expert::getOrganization, query.getOrganization());
        }
        if (StringUtils.hasText(query.getRegion())) {
            wrapper.eq(Expert::getRegion, query.getRegion());
        }
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(Expert::getTitle, query.getTitle());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Expert::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getField())) {
            wrapper.apply("JSON_CONTAINS(fields, {0})", "\"" + query.getField() + "\"");
        }
        if (StringUtils.hasText(query.getTag())) {
            wrapper.apply("JSON_CONTAINS(tags, {0})", "\"" + query.getTag() + "\"");
        }
        wrapper.orderByDesc(Expert::getUpdatedAt);
        Page<Expert> page = expertMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        page.getRecords().forEach(this::maskSensitive);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public Expert detail(Long id) {
        Expert expert = expertMapper.selectById(id);
        if (expert == null) {
            throw new BusinessException("专家不存在");
        }
        if (com.review.system.security.RoleUtils.isSystemAdmin()) {
            expert.setIdCard(PiiCryptoUtil.decrypt(expert.getIdCard(), piiSecret()));
            expert.setBankAccount(PiiCryptoUtil.decrypt(expert.getBankAccount(), piiSecret()));
        }
        maskSensitive(expert);
        return expert;
    }

    @Transactional
    public Expert create(ExpertSaveRequest req) {
        Expert expert = toEntity(req, new Expert());
        expert.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "ACTIVE");
        expert.setReviewCount(0);
        expert.setOnTimeRate(new BigDecimal("100.00"));
        expertMapper.insert(expert);
        auditService.log("CREATE_EXPERT", "EXPERT", expert.getId(), "新增专家: " + expert.getName());
        maskSensitive(expert);
        return expert;
    }

    @Transactional
    public Expert update(Long id, ExpertSaveRequest req) {
        Expert expert = expertMapper.selectById(id);
        if (expert == null) {
            throw new BusinessException("专家不存在");
        }
        toEntity(req, expert);
        expertMapper.updateById(expert);
        auditService.log("UPDATE_EXPERT", "EXPERT", id, "更新专家信息");
        maskSensitive(expert);
        return expert;
    }

    @Transactional
    public void delete(Long id) {
        expertMapper.deleteById(id);
        auditService.log("DELETE_EXPERT", "EXPERT", id, "删除专家");
    }

    @Transactional
    public Expert approve(Long id, String remark) {
        Expert expert = requireExpert(id);
        if (!"PENDING".equals(expert.getStatus())) {
            throw new BusinessException("仅待审核专家可通过审核");
        }
        expert.setStatus("ACTIVE");
        if (StringUtils.hasText(remark)) {
            expert.setRemark(remark);
        }
        expertMapper.updateById(expert);
        auditService.log("APPROVE_EXPERT", "EXPERT", id, "审核通过专家: " + expert.getName());
        maskSensitive(expert);
        return expert;
    }

    @Transactional
    public Expert reject(Long id, String remark) {
        Expert expert = requireExpert(id);
        if (!"PENDING".equals(expert.getStatus())) {
            throw new BusinessException("仅待审核专家可驳回");
        }
        expert.setStatus("DISABLED");
        if (StringUtils.hasText(remark)) {
            expert.setRemark(remark);
        }
        expertMapper.updateById(expert);
        auditService.log("REJECT_EXPERT", "EXPERT", id, "驳回专家: " + expert.getName());
        maskSensitive(expert);
        return expert;
    }

    private Expert requireExpert(Long id) {
        Expert expert = expertMapper.selectById(id);
        if (expert == null) {
            throw new BusinessException("专家不存在");
        }
        return expert;
    }

    private String piiSecret() {
        return StringUtils.hasText(piiEncryptionKey) ? piiEncryptionKey : jwtSecret;
    }

    @Transactional
    public void createAccount(Long expertId, String username, String password) {
        Expert expert = expertMapper.selectById(expertId);
        if (expert == null) {
            throw new BusinessException("专家不存在");
        }
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(expert.getName());
        user.setEmail(expert.getEmail());
        user.setPhone(expert.getPhone());
        user.setStatus(1);
        user.setExpertId(expertId);
        sysUserMapper.insert(user);
        jdbcTemplate.update("""
                INSERT INTO sys_user_role (user_id, role_id)
                SELECT ?, id FROM sys_role WHERE code = 'EXPERT'
                """, user.getId());
        auditService.log("CREATE_EXPERT_ACCOUNT", "EXPERT", expertId, "创建专家账号: " + username);
    }

    public byte[] exportExcel(ExpertQuery query) {
        query.setPage(1);
        query.setSize(10000);
        List<Expert> list = page(query).getRecords();
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("专家库");
            String[] headers = {"姓名", "性别", "单位", "职称", "学历", "手机", "邮箱", "地区", "单位性质", "专业领域", "标签", "状态"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (Expert e : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(StringUtil.nullToEmpty(e.getName()));
                row.createCell(1).setCellValue(StringUtil.nullToEmpty(e.getGender()));
                row.createCell(2).setCellValue(StringUtil.nullToEmpty(e.getOrganization()));
                row.createCell(3).setCellValue(StringUtil.nullToEmpty(e.getTitle()));
                row.createCell(4).setCellValue(StringUtil.nullToEmpty(e.getEducation()));
                row.createCell(5).setCellValue(StringUtil.nullToEmpty(e.getPhone()));
                row.createCell(6).setCellValue(StringUtil.nullToEmpty(e.getEmail()));
                row.createCell(7).setCellValue(StringUtil.nullToEmpty(e.getRegion()));
                row.createCell(8).setCellValue(StringUtil.nullToEmpty(e.getOrgType()));
                row.createCell(9).setCellValue(joinList(e.getFields()));
                row.createCell(10).setCellValue(joinList(e.getTags()));
                row.createCell(11).setCellValue(StringUtil.nullToEmpty(e.getStatus()));
            }
            wb.write(out);
            auditService.log("EXPORT_EXPERT", "EXPERT", null, "导出专家 " + list.size() + " 条");
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Transactional
    public List<String> importExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name = cellStr(row.getCell(0));
                if (!StringUtils.hasText(name)) continue;
                try {
                    ExpertSaveRequest req = new ExpertSaveRequest();
                    req.setName(name);
                    req.setGender(cellStr(row.getCell(1)));
                    req.setOrganization(cellStr(row.getCell(2)));
                    req.setTitle(cellStr(row.getCell(3)));
                    req.setEducation(cellStr(row.getCell(4)));
                    req.setPhone(cellStr(row.getCell(5)));
                    req.setEmail(cellStr(row.getCell(6)));
                    req.setRegion(cellStr(row.getCell(7)));
                    req.setOrgType(cellStr(row.getCell(8)));
                    req.setFields(splitList(cellStr(row.getCell(9))));
                    req.setTags(splitList(cellStr(row.getCell(10))));
                    req.setStatus(StringUtils.hasText(cellStr(row.getCell(11))) ? cellStr(row.getCell(11)) : "PENDING");
                    create(req);
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
            auditService.log("IMPORT_EXPERT", "EXPERT", null, "导入专家，错误 " + errors.size() + " 条");
            expertLoginAccountSyncer.sync();
        } catch (Exception e) {
            throw new BusinessException("导入失败: " + e.getMessage());
        }
        return errors;
    }

    public byte[] template() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("专家导入模板");
            Row row = sheet.createRow(0);
            String[] headers = {"姓名*", "性别", "单位", "职称", "学历", "手机", "邮箱", "地区", "单位性质", "专业领域(逗号分隔)", "标签(逗号分隔)", "状态"};
            for (int i = 0; i < headers.length; i++) {
                row.createCell(i).setCellValue(headers[i]);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("模板生成失败");
        }
    }

    private Expert toEntity(ExpertSaveRequest req, Expert expert) {
        expert.setName(req.getName());
        expert.setGender(req.getGender());
        expert.setOrganization(req.getOrganization());
        expert.setTitle(req.getTitle());
        expert.setEducation(req.getEducation());
        expert.setPhone(req.getPhone());
        expert.setEmail(req.getEmail());
        if (StringUtils.hasText(req.getIdCard()) && !req.getIdCard().contains("*")) {
            expert.setIdCard(PiiCryptoUtil.encrypt(req.getIdCard(), piiSecret()));
        }
        if (StringUtils.hasText(req.getBankAccount()) && !req.getBankAccount().contains("*")) {
            expert.setBankAccount(PiiCryptoUtil.encrypt(req.getBankAccount(), piiSecret()));
        }
        expert.setRegion(req.getRegion());
        expert.setOrgType(req.getOrgType());
        expert.setFields(req.getFields());
        expert.setTags(req.getTags());
        if (StringUtils.hasText(req.getStatus())) {
            expert.setStatus(req.getStatus());
        }
        expert.setRemark(req.getRemark());
        return expert;
    }

    private void maskSensitive(Expert expert) {
        String idCard = PiiCryptoUtil.decrypt(expert.getIdCard(), piiSecret());
        String bank = PiiCryptoUtil.decrypt(expert.getBankAccount(), piiSecret());
        expert.setIdCard(MaskUtil.maskIdCard(idCard));
        expert.setBankAccount(MaskUtil.maskBankAccount(bank));
        expert.setPhone(MaskUtil.maskPhone(expert.getPhone()));
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        return new DataFormatter().formatCellValue(cell).trim();
    }

    private String joinList(List<String> list) {
        if (list == null) return "";
        return String.join(",", list);
    }

    private List<String> splitList(String s) {
        if (!StringUtils.hasText(s)) return List.of();
        return Arrays.stream(s.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}
