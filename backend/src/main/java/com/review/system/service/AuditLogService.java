package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.review.system.common.BusinessException;
import com.review.system.util.StringUtil;
import com.review.system.dto.PageResult;
import com.review.system.entity.AuditLog;
import com.review.system.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public PageResult<AuditLog> page(int page, int size, String action, String keyword) {
        LambdaQueryWrapper<AuditLog> wrapper = buildQuery(action, keyword);
        Page<AuditLog> result = auditLogMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    public byte[] exportExcel(String action, String keyword) {
        LambdaQueryWrapper<AuditLog> wrapper = buildQuery(action, keyword);
        wrapper.last("LIMIT 5000");
        List<AuditLog> list = auditLogMapper.selectList(wrapper);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("审计日志");
            String[] headers = {"时间", "操作人", "操作", "对象类型", "对象ID", "详情", "IP"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (AuditLog log : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(log.getCreatedAt() != null ? log.getCreatedAt().toString() : "");
                row.createCell(1).setCellValue(StringUtil.nullToEmpty(log.getUsername()));
                row.createCell(2).setCellValue(StringUtil.nullToEmpty(log.getAction()));
                row.createCell(3).setCellValue(StringUtil.nullToEmpty(log.getTargetType()));
                row.createCell(4).setCellValue(log.getTargetId() != null ? log.getTargetId().doubleValue() : 0);
                row.createCell(5).setCellValue(StringUtil.nullToEmpty(log.getDetail()));
                row.createCell(6).setCellValue(StringUtil.nullToEmpty(log.getIp()));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private LambdaQueryWrapper<AuditLog> buildQuery(String action, String keyword) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(action)) {
            wrapper.eq(AuditLog::getAction, action);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AuditLog::getUsername, keyword)
                    .or().like(AuditLog::getDetail, keyword));
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return wrapper;
    }
}
