package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.common.BusinessException;
import com.review.system.entity.*;
import com.review.system.mapper.*;
import com.review.system.config.ReviewTypeRegistry;
import com.review.system.security.DataScopeService;
import com.review.system.security.RoleUtils;
import com.review.system.util.StringUtil;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final ReviewTaskMapper taskMapper;
    private final ReviewProjectMapper projectMapper;
    private final ReviewSubmissionMapper submissionMapper;
    private final ReviewSubmissionArchiveMapper archiveMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final ExpertMapper expertMapper;
    private final AuditService auditService;
    private final DataScopeService dataScopeService;
    private final ReviewTypeRegistry reviewTypeRegistry;

    public List<ProjectSummaryVO> summarizeByTask(Long taskId) {
        return summarizeByTask(taskId, resolveMaskFlag(null));
    }

    public List<ProjectSummaryVO> summarizeByTask(Long taskId, Boolean maskOverride) {
        ReviewTask task = requireTask(taskId);
        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getTaskId, taskId));
        boolean mask = resolveMaskFlag(maskOverride);
        List<ProjectSummaryVO> list = new ArrayList<>();
        int expertSeq = 1;
        for (ReviewProject p : projects) {
            ProjectSummaryVO vo = summarizeProject(task, p, mask, expertSeq);
            expertSeq += vo.getOpinions() != null ? vo.getOpinions().size() : 0;
            list.add(vo);
        }
        list.sort(Comparator.comparing(
                (ProjectSummaryVO s) -> s.getAvgScore() != null ? s.getAvgScore() : BigDecimal.ZERO,
                Comparator.reverseOrder()));
        int rank = 1;
        for (ProjectSummaryVO vo : list) {
            vo.setRank(rank++);
        }
        return list;
    }

    public ProjectSummaryVO summarizeProject(Long projectId, boolean mask) {
        ReviewProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        ReviewTask task = requireTask(project.getTaskId());
        return summarizeProject(task, project, mask, 1);
    }

    public List<ProjectRoundComparisonVO> roundComparisonByTask(Long taskId) {
        requireTask(taskId);
        boolean mask = resolveMaskFlag(null);
        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getTaskId, taskId));
        return projects.stream().map(p -> buildRoundComparison(p, mask)).toList();
    }

    public List<DimensionSummaryVO> summarizeDimensions(Long taskId) {
        ReviewTask task = requireTask(taskId);
        List<Map<String, Object>> formConfig = task.getFormConfig() != null
                ? task.getFormConfig()
                : reviewTypeRegistry.defaultFormConfig(task.getReviewType());
        if (formConfig == null || formConfig.isEmpty()) {
            return List.of();
        }
        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getTaskId, taskId));
        Map<String, List<BigDecimal>> scoresByKey = new LinkedHashMap<>();
        for (ReviewProject project : projects) {
            List<ReviewSubmission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<ReviewSubmission>()
                    .eq(ReviewSubmission::getProjectId, project.getId())
                    .eq(ReviewSubmission::getStatus, "SUBMITTED"));
            for (ReviewSubmission submission : submissions) {
                if (submission.getScores() == null) {
                    continue;
                }
                for (Map<String, Object> item : formConfig) {
                    String key = String.valueOf(item.get("key"));
                    Object raw = submission.getScores().get(key);
                    BigDecimal score = toScore(raw);
                    if (score != null) {
                        scoresByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(score);
                    }
                }
            }
        }
        List<DimensionSummaryVO> result = new ArrayList<>();
        for (Map<String, Object> item : formConfig) {
            String key = String.valueOf(item.get("key"));
            String name = String.valueOf(item.getOrDefault("name", key));
            List<BigDecimal> values = scoresByKey.getOrDefault(key, List.of());
            if (values.isEmpty()) {
                result.add(DimensionSummaryVO.builder()
                        .key(key)
                        .name(name)
                        .sampleCount(0)
                        .build());
                continue;
            }
            BigDecimal avg = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
            BigDecimal max = values.stream().max(BigDecimal::compareTo).orElse(null);
            BigDecimal min = values.stream().min(BigDecimal::compareTo).orElse(null);
            result.add(DimensionSummaryVO.builder()
                    .key(key)
                    .name(name)
                    .avgScore(avg)
                    .maxScore(max)
                    .minScore(min)
                    .sampleCount(values.size())
                    .build());
        }
        return result;
    }

    private static BigDecimal toScore(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(raw.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public byte[] exportExcel(Long taskId, Boolean maskOverride) {
        requireTask(taskId);
        boolean mask = resolveMaskFlag(maskOverride);
        auditService.log("EXPORT_SUMMARY", "TASK", taskId,
                "导出 Excel 汇总" + (mask ? "（脱敏）" : ""));
        List<ProjectSummaryVO> summaries = summarizeByTask(taskId, mask);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("评审汇总");
            Row header = sheet.createRow(0);
            String[] headers = {"项目名称", "申请人", "申报单位", "已提交数", "最低要求", "平均分", "标准差", "最高分", "最低分", "推荐结论"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (ProjectSummaryVO s : summaries) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getProjectName());
                row.createCell(1).setCellValue(StringUtil.nullToEmpty(s.getApplicant()));
                row.createCell(2).setCellValue(StringUtil.nullToEmpty(s.getOrganization()));
                row.createCell(3).setCellValue(s.getSubmittedCount());
                row.createCell(4).setCellValue(s.getMinReviewers());
                row.createCell(5).setCellValue(s.getAvgScore() != null ? s.getAvgScore().doubleValue() : 0);
                row.createCell(6).setCellValue(s.getStdDev() != null ? s.getStdDev().doubleValue() : 0);
                row.createCell(7).setCellValue(s.getMaxScore() != null ? s.getMaxScore().doubleValue() : 0);
                row.createCell(8).setCellValue(s.getMinScore() != null ? s.getMinScore().doubleValue() : 0);
                row.createCell(9).setCellValue(StringUtil.nullToEmpty(s.getRecommendation()));
            }
            Sheet opinionSheet = wb.createSheet("评审意见");
            Row oh = opinionSheet.createRow(0);
            oh.createCell(0).setCellValue("项目");
            oh.createCell(1).setCellValue(mask ? "评审专家" : "专家姓名");
            oh.createCell(2).setCellValue("得分");
            oh.createCell(3).setCellValue("结论");
            oh.createCell(4).setCellValue("意见");
            int oRow = 1;
            for (ProjectSummaryVO s : summaries) {
                if (s.getOpinions() == null) continue;
                for (OpinionVO o : s.getOpinions()) {
                    Row row = opinionSheet.createRow(oRow++);
                    row.createCell(0).setCellValue(s.getProjectName());
                    row.createCell(1).setCellValue(o.getExpertName());
                    row.createCell(2).setCellValue(o.getTotalScore() != null ? o.getTotalScore().doubleValue() : 0);
                    row.createCell(3).setCellValue(StringUtil.nullToEmpty(o.getConclusion()));
                    row.createCell(4).setCellValue(StringUtil.nullToEmpty(o.getOpinion()));
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    public byte[] exportWord(Long taskId, Boolean maskOverride) {
        ReviewTask task = requireTask(taskId);
        boolean mask = resolveMaskFlag(maskOverride);
        auditService.log("EXPORT_SUMMARY", "TASK", taskId,
                "导出 Word 汇总" + (mask ? "（脱敏）" : ""));
        List<ProjectSummaryVO> summaries = summarizeByTask(taskId, mask);
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setText("评审结果汇总报告");

            XWPFParagraph sub = doc.createParagraph();
            sub.createRun().setText("任务名称：" + task.getName());
            sub.createRun().addBreak();
            sub.createRun().setText("评审类型：" + StringUtil.nullToEmpty(task.getReviewType()));
            sub.createRun().addBreak();
            sub.createRun().setText("批次：" + StringUtil.nullToEmpty(task.getBatchName()));
            if (mask) {
                sub.createRun().addBreak();
                sub.createRun().setText("（领导视图：专家个人信息已脱敏）");
            }

            for (ProjectSummaryVO s : summaries) {
                doc.createParagraph().createRun().addBreak();
                XWPFParagraph h = doc.createParagraph();
                XWPFRun hr = h.createRun();
                hr.setBold(true);
                hr.setFontSize(14);
                hr.setText("项目：" + s.getProjectName());

                XWPFParagraph info = doc.createParagraph();
                info.createRun().setText(String.format("申请人：%s  单位：%s  提交/%d  平均分：%s  结论：%s",
                        StringUtil.nullToEmpty(s.getApplicant()), StringUtil.nullToEmpty(s.getOrganization()), s.getMinReviewers(),
                        s.getAvgScore() != null ? s.getAvgScore().toPlainString() : "-",
                        StringUtil.nullToEmpty(s.getRecommendation())));

                if (s.getOpinions() != null && !s.getOpinions().isEmpty()) {
                    XWPFTable table = doc.createTable(s.getOpinions().size() + 1, 4);
                    setCell(table, 0, 0, mask ? "评审专家" : "专家");
                    setCell(table, 0, 1, "得分");
                    setCell(table, 0, 2, "结论");
                    setCell(table, 0, 3, "意见");
                    for (int i = 0; i < s.getOpinions().size(); i++) {
                        OpinionVO o = s.getOpinions().get(i);
                        setCell(table, i + 1, 0, o.getExpertName());
                        setCell(table, i + 1, 1, o.getTotalScore() != null ? o.getTotalScore().toPlainString() : "");
                        setCell(table, i + 1, 2, StringUtil.nullToEmpty(o.getConclusion()));
                        setCell(table, i + 1, 3, StringUtil.nullToEmpty(o.getOpinion()));
                    }
                }
            }
            doc.write(out);
            auditService.log("EXPORT_SUMMARY", "TASK", taskId, "导出Word" + (mask ? "(脱敏)" : ""));
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Word导出失败: " + e.getMessage());
        }
    }

    public byte[] exportPdf(Long taskId, Boolean maskOverride) {
        ReviewTask task = requireTask(taskId);
        boolean mask = resolveMaskFlag(maskOverride);
        List<ProjectSummaryVO> summaries = summarizeByTask(taskId, mask);
        try {
            byte[] pdf = PdfSummaryRenderer.render(task, summaries, mask);
            auditService.log("EXPORT_SUMMARY", "TASK", taskId, "导出正式PDF" + (mask ? "(脱敏)" : ""));
            return pdf;
        } catch (Exception e) {
            throw new BusinessException("PDF导出失败: " + e.getMessage());
        }
    }

    private ProjectRoundComparisonVO buildRoundComparison(ReviewProject project, boolean mask) {
        int currentRound = project.getReviewRound() == null ? 1 : project.getReviewRound();
        List<ReviewSubmissionArchive> archives = archiveMapper.selectList(
                new LambdaQueryWrapper<ReviewSubmissionArchive>()
                        .eq(ReviewSubmissionArchive::getProjectId, project.getId())
                        .orderByAsc(ReviewSubmissionArchive::getReviewRound));
        Map<Integer, List<ReviewSubmissionArchive>> archivedByRound = archives.stream()
                .collect(Collectors.groupingBy(ReviewSubmissionArchive::getReviewRound));

        Set<Integer> roundNumbers = new TreeSet<>(archivedByRound.keySet());
        roundNumbers.add(currentRound);

        List<RoundSnapshotVO> rounds = new ArrayList<>();
        int expertSeq = 1;
        for (Integer round : roundNumbers) {
            if (round.equals(currentRound)) {
                List<ReviewSubmission> live = submissionMapper.selectList(new LambdaQueryWrapper<ReviewSubmission>()
                        .eq(ReviewSubmission::getProjectId, project.getId())
                        .eq(ReviewSubmission::getStatus, "SUBMITTED"));
                if (!live.isEmpty()) {
                    RoundSnapshotVO snap = buildRoundFromSubmissions(round, live, mask, expertSeq);
                    expertSeq += snap.getOpinions() != null ? snap.getOpinions().size() : 0;
                    rounds.add(snap);
                }
            } else {
                List<ReviewSubmissionArchive> items = archivedByRound.getOrDefault(round, List.of());
                if (!items.isEmpty()) {
                    RoundSnapshotVO snap = buildRoundFromArchives(round, items, mask, expertSeq);
                    expertSeq += snap.getOpinions() != null ? snap.getOpinions().size() : 0;
                    rounds.add(snap);
                }
            }
        }

        return ProjectRoundComparisonVO.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .currentRound(currentRound)
                .rounds(rounds)
                .build();
    }

    private RoundSnapshotVO buildRoundFromArchives(int round, List<ReviewSubmissionArchive> items,
                                                     boolean mask, int expertSeqStart) {
        List<BigDecimal> scores = items.stream()
                .map(ReviewSubmissionArchive::getTotalScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        int seq = expertSeqStart;
        List<OpinionVO> opinions = new ArrayList<>();
        for (ReviewSubmissionArchive a : items) {
            String label = mask ? "专家" + (seq++) : StringUtil.nullToEmpty(a.getExpertName());
            if (!mask && label.isEmpty()) {
                label = "专家";
            }
            opinions.add(OpinionVO.builder()
                    .expertName(label)
                    .totalScore(a.getTotalScore())
                    .conclusion(a.getConclusion())
                    .opinion(a.getOpinion())
                    .build());
        }
        return RoundSnapshotVO.builder()
                .round(round)
                .submittedCount(items.size())
                .avgScore(average(scores))
                .maxScore(max(scores))
                .minScore(min(scores))
                .opinions(opinions)
                .build();
    }

    private RoundSnapshotVO buildRoundFromSubmissions(int round, List<ReviewSubmission> submissions,
                                                      boolean mask, int expertSeqStart) {
        List<BigDecimal> scores = submissions.stream()
                .map(ReviewSubmission::getTotalScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        int seq = expertSeqStart;
        List<OpinionVO> opinions = new ArrayList<>();
        for (ReviewSubmission s : submissions) {
            String label;
            if (mask) {
                label = "专家" + (seq++);
            } else {
                Expert expert = expertMapper.selectById(s.getExpertId());
                label = expert != null ? expert.getName() : "专家";
            }
            opinions.add(OpinionVO.builder()
                    .submissionId(s.getId())
                    .expertName(label)
                    .totalScore(s.getTotalScore())
                    .conclusion(s.getConclusion())
                    .opinion(s.getOpinion())
                    .build());
        }
        return RoundSnapshotVO.builder()
                .round(round)
                .submittedCount(submissions.size())
                .avgScore(average(scores))
                .maxScore(max(scores))
                .minScore(min(scores))
                .opinions(opinions)
                .build();
    }

    private BigDecimal average(List<BigDecimal> scores) {
        if (scores.isEmpty()) {
            return null;
        }
        return scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal max(List<BigDecimal> scores) {
        return scores.stream().max(BigDecimal::compareTo).orElse(null);
    }

    private BigDecimal min(List<BigDecimal> scores) {
        return scores.stream().min(BigDecimal::compareTo).orElse(null);
    }

    private ReviewTask requireTask(Long taskId) {
        ReviewTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        dataScopeService.assertTaskAccess(task);
        return task;
    }

    private boolean resolveMaskFlag(Boolean maskOverride) {
        if (maskOverride != null) {
            return maskOverride;
        }
        return RoleUtils.shouldMaskExpertInfo();
    }

    private ProjectSummaryVO summarizeProject(ReviewTask task, ReviewProject project,
                                              boolean mask, int expertSeqStart) {
        List<ReviewSubmission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<ReviewSubmission>()
                .eq(ReviewSubmission::getProjectId, project.getId())
                .eq(ReviewSubmission::getStatus, "SUBMITTED"));
        List<BigDecimal> scores = submissions.stream()
                .map(ReviewSubmission::getTotalScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        BigDecimal avg = null, max = null, min = null, stdDev = null;
        if (!scores.isEmpty()) {
            avg = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
            max = scores.stream().max(BigDecimal::compareTo).orElse(null);
            min = scores.stream().min(BigDecimal::compareTo).orElse(null);
            if (scores.size() >= 2 && avg != null) {
                final BigDecimal avgFinal = avg;
                double variance = scores.stream()
                        .mapToDouble(s -> Math.pow(s.subtract(avgFinal).doubleValue(), 2))
                        .average().orElse(0);
                stdDev = BigDecimal.valueOf(Math.sqrt(variance)).setScale(2, RoundingMode.HALF_UP);
            } else {
                stdDev = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        }

        long assignmentCount = assignmentMapper.selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getProjectId, project.getId()));

        String recommendation = resolveRecommendation(task, avg, submissions.size());

        int seq = expertSeqStart;
        List<OpinionVO> opinions = new ArrayList<>();
        for (ReviewSubmission s : submissions) {
            String expertLabel;
            if (mask) {
                expertLabel = "专家" + (seq++);
            } else {
                Expert expert = expertMapper.selectById(s.getExpertId());
                expertLabel = expert != null ? expert.getName() : "专家";
            }
            opinions.add(OpinionVO.builder()
                    .submissionId(s.getId())
                    .expertName(expertLabel)
                    .totalScore(s.getTotalScore())
                    .conclusion(s.getConclusion())
                    .opinion(s.getOpinion())
                    .build());
        }

        return ProjectSummaryVO.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .applicant(project.getApplicant())
                .organization(project.getOrganization())
                .submittedCount(submissions.size())
                .assignedCount((int) assignmentCount)
                .minReviewers(task.getMinReviewers())
                .avgScore(avg)
                .maxScore(max)
                .minScore(min)
                .stdDev(stdDev)
                .recommendation(recommendation)
                .meetsMinimum(submissions.size() >= task.getMinReviewers())
                .masked(mask)
                .opinions(opinions)
                .build();
    }

    private void setCell(XWPFTable table, int row, int col, String text) {
        XWPFTableCell cell = table.getRow(row).getCell(col);
        cell.setText(text != null ? text : "");
    }

    private String resolveRecommendation(ReviewTask task, BigDecimal avg, int submitted) {
        int minReviewers = task.getMinReviewers() != null ? task.getMinReviewers() : 3;
        if (submitted < minReviewers) {
            return "评审人数不足";
        }
        if (avg == null) {
            return "暂无结论";
        }
        ReviewTypeRegistry.RecommendationRule rule = reviewTypeRegistry.recommendationRule(task.getReviewType());
        double v = avg.doubleValue();
        if (v >= rule.passScore()) {
            return rule.passLabel();
        }
        if (v >= rule.reviseScore()) {
            return rule.reviseLabel();
        }
        return rule.failLabel();
    }

    @Data
    @Builder
    public static class ProjectSummaryVO {
        private Long projectId;
        private String projectName;
        private String applicant;
        private String organization;
        private int submittedCount;
        private int assignedCount;
        private int minReviewers;
        private BigDecimal avgScore;
        private BigDecimal maxScore;
        private BigDecimal minScore;
        private BigDecimal stdDev;
        private Integer rank;
        private String recommendation;
        private boolean meetsMinimum;
        private boolean masked;
        private List<OpinionVO> opinions;
    }

    @Data
    @Builder
    public static class RoundSnapshotVO {
        private int round;
        private int submittedCount;
        private BigDecimal avgScore;
        private BigDecimal maxScore;
        private BigDecimal minScore;
        private List<OpinionVO> opinions;
    }

    @Data
    @Builder
    public static class ProjectRoundComparisonVO {
        private Long projectId;
        private String projectName;
        private int currentRound;
        private List<RoundSnapshotVO> rounds;
    }

    @Data
    @Builder
    public static class OpinionVO {
        private Long submissionId;
        private String expertName;
        private BigDecimal totalScore;
        private String conclusion;
        private String opinion;
    }

    @Data
    @Builder
    public static class DimensionSummaryVO {
        private String key;
        private String name;
        private BigDecimal avgScore;
        private BigDecimal maxScore;
        private BigDecimal minScore;
        private int sampleCount;
    }
}
