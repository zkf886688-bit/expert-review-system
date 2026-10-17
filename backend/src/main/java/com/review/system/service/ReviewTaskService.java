package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.review.system.common.BusinessException;
import com.review.system.dto.*;
import com.review.system.entity.*;
import com.review.system.mapper.*;
import com.review.system.config.ReviewTypeRegistry;
import com.review.system.security.DataScopeService;
import com.review.system.security.SecurityUtils;
import com.review.system.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewTaskService {

    private final ReviewTaskMapper taskMapper;
    private final ReviewProjectMapper projectMapper;
    private final ReviewMaterialMapper materialMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final ExpertMapper expertMapper;
    private final SysUserMapper sysUserMapper;
    private final FileService fileService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final NotificationTemplateService notificationTemplateService;
    private final DataScopeService dataScopeService;
    private final ReviewTypeRegistry reviewTypeRegistry;
    private final ExpertAvoidanceService expertAvoidanceService;
    private final ExpertAssignmentScorer expertAssignmentScorer;

    public PageResult<TaskListVO> page(int page, int size, String status, String keyword) {
        LambdaQueryWrapper<ReviewTask> wrapper = buildTaskListQuery(status, keyword);
        Page<ReviewTask> result = taskMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, Long> projectCounts = countProjectsByTask(result.getRecords());
        Map<Long, Long> completedCounts = countCompletedProjectsByTask(result.getRecords());
        List<TaskListVO> records = result.getRecords().stream().map(task -> TaskListVO.builder()
                .id(task.getId())
                .name(task.getName())
                .reviewType(task.getReviewType())
                .batchName(task.getBatchName())
                .dataDomain(task.getDataDomain())
                .deadline(task.getDeadline())
                .status(task.getStatus())
                .minReviewers(task.getMinReviewers())
                .blindReview(task.getBlindReview())
                .projectCount(projectCounts.getOrDefault(task.getId(), 0L))
                .completedProjectCount(completedCounts.getOrDefault(task.getId(), 0L))
                .build()).toList();
        return new PageResult<>(result.getTotal(), records);
    }

    public byte[] exportTaskList(String status, String keyword) {
        LambdaQueryWrapper<ReviewTask> wrapper = buildTaskListQuery(status, keyword);
        wrapper.last("LIMIT 2000");
        List<ReviewTask> tasks = taskMapper.selectList(wrapper);
        Map<Long, Long> projectCounts = countProjectsByTask(tasks);
        Map<Long, Long> completedCounts = countCompletedProjectsByTask(tasks);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("评审任务");
            Row header = sheet.createRow(0);
            String[] headers = {"任务名称", "评审类型", "批次", "数据域", "截止时间", "状态", "项目数", "已完成", "最低人数", "盲审"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (ReviewTask task : tasks) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(StringUtil.nullToEmpty(task.getName()));
                row.createCell(1).setCellValue(StringUtil.nullToEmpty(task.getReviewType()));
                row.createCell(2).setCellValue(StringUtil.nullToEmpty(task.getBatchName()));
                row.createCell(3).setCellValue(StringUtil.nullToEmpty(task.getDataDomain()));
                row.createCell(4).setCellValue(task.getDeadline() != null ? task.getDeadline().toString() : "");
                row.createCell(5).setCellValue(taskStatusLabel(task.getStatus()));
                row.createCell(6).setCellValue(projectCounts.getOrDefault(task.getId(), 0L));
                row.createCell(7).setCellValue(completedCounts.getOrDefault(task.getId(), 0L));
                row.createCell(8).setCellValue(task.getMinReviewers() != null ? task.getMinReviewers() : 3);
                row.createCell(9).setCellValue(Boolean.TRUE.equals(task.getBlindReview()) ? "是" : "否");
            }
            auditService.log("EXPORT_TASK_LIST", "TASK", null, "导出任务列表 " + tasks.size() + " 条");
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Transactional
    public ReviewTask create(TaskCreateRequest req) {
        ReviewTask task = new ReviewTask();
        task.setName(req.getName());
        task.setReviewType(req.getReviewType());
        task.setBatchName(req.getBatchName());
        task.setDataDomain(dataScopeService.resolveTaskDomain(req.getDataDomain()));
        task.setStartTime(req.getStartTime());
        task.setDeadline(req.getDeadline());
        task.setMinReviewers(req.getMinReviewers() != null ? req.getMinReviewers() : 3);
        task.setFormConfig(req.getFormConfig() != null ? req.getFormConfig()
                : reviewTypeRegistry.defaultFormConfig(req.getReviewType()));
        task.setBlindReview(Boolean.TRUE.equals(req.getBlindReview()));
        task.setAvoidSameOrg(req.getAvoidSameOrg() == null || req.getAvoidSameOrg());
        task.setAvoidDuplicateExpert(req.getAvoidDuplicateExpert() == null || req.getAvoidDuplicateExpert());
        task.setStatus("UNASSIGNED");
        task.setCreatedBy(SecurityUtils.currentUserId());
        taskMapper.insert(task);

        if (req.getProjects() != null) {
            int order = 0;
            for (TaskCreateRequest.ProjectItem item : req.getProjects()) {
                ReviewProject project = new ReviewProject();
                project.setTaskId(task.getId());
                project.setProjectName(item.getProjectName());
                project.setApplicant(item.getApplicant());
                project.setOrganization(item.getOrganization());
                project.setApplicantUserId(item.getApplicantUserId());
                project.setSortOrder(order++);
                projectMapper.insert(project);
            }
        }
        auditService.log("CREATE_TASK", "TASK", task.getId(), "创建评审任务: " + task.getName());
        return task;
    }

    public TaskDetailVO detail(Long taskId) {
        ReviewTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        dataScopeService.assertTaskAccess(task);
        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getTaskId, taskId)
                .orderByAsc(ReviewProject::getSortOrder));
        List<ReviewMaterial> materials = materialMapper.selectList(new LambdaQueryWrapper<ReviewMaterial>()
                .eq(ReviewMaterial::getTaskId, taskId));
        List<ReviewAssignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getTaskId, taskId));
        int minReviewers = task.getMinReviewers() != null ? task.getMinReviewers() : 3;
        Map<Long, List<ReviewAssignment>> byProject = assignments.stream()
                .filter(a -> !"REJECTED".equals(a.getStatus()))
                .collect(Collectors.groupingBy(ReviewAssignment::getProjectId));
        List<TaskDetailVO.ProjectStatVO> projectStats = projects.stream().map(p -> {
            List<ReviewAssignment> list = byProject.getOrDefault(p.getId(), List.of());
            int submitted = (int) list.stream().filter(a -> "SUBMITTED".equals(a.getStatus())).count();
            return TaskDetailVO.ProjectStatVO.builder()
                    .projectId(p.getId())
                    .assignedCount(list.size())
                    .submittedCount(submitted)
                    .minReviewers(minReviewers)
                    .completed(submitted >= minReviewers)
                    .build();
        }).toList();
        long completedProjects = projectStats.stream().filter(TaskDetailVO.ProjectStatVO::isCompleted).count();
        TaskDetailVO.AssignSummaryVO assignSummary = TaskDetailVO.AssignSummaryVO.builder()
                .totalProjects(projects.size())
                .completedProjects((int) completedProjects)
                .totalAssignments((int) assignments.stream().filter(a -> !"REJECTED".equals(a.getStatus())).count())
                .submittedAssignments((int) assignments.stream().filter(a -> "SUBMITTED".equals(a.getStatus())).count())
                .build();
        List<TaskDetailVO.AssignmentVO> assignmentVOs = assignments.stream().map(a -> {
            Expert expert = expertMapper.selectById(a.getExpertId());
            ReviewProject project = projectMapper.selectById(a.getProjectId());
            return TaskDetailVO.AssignmentVO.builder()
                    .id(a.getId())
                    .projectId(a.getProjectId())
                    .projectName(project != null ? project.getProjectName() : null)
                    .expertId(a.getExpertId())
                    .expertName(expert != null ? expert.getName() : null)
                    .status(a.getStatus())
                    .rejectReason(a.getRejectReason())
                    .build();
        }).toList();
        return TaskDetailVO.builder()
                .task(task)
                .projects(projects)
                .materials(materials)
                .assignments(assignmentVOs)
                .projectStats(projectStats)
                .assignSummary(assignSummary)
                .build();
    }

    @Transactional
    public ReviewProject addProject(Long taskId, TaskCreateRequest.ProjectItem item) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        ReviewProject project = new ReviewProject();
        project.setTaskId(taskId);
        project.setProjectName(item.getProjectName());
        project.setApplicant(item.getApplicant());
        project.setOrganization(item.getOrganization());
        project.setApplicantUserId(item.getApplicantUserId());
        projectMapper.insert(project);
        return project;
    }

    @Transactional
    public void bindApplicant(Long taskId, Long projectId, Long applicantUserId) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        ReviewProject project = requireProject(taskId, projectId);
        project.setApplicantUserId(applicantUserId);
        projectMapper.updateById(project);
        auditService.log("BIND_APPLICANT", "PROJECT", projectId, "绑定申报人账号: " + applicantUserId);
    }

    @Transactional
    public ReviewMaterial uploadMaterial(Long taskId, Long projectId, MultipartFile file, String materialType) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        String path = fileService.save(file, "task/" + taskId);
        ReviewMaterial material = new ReviewMaterial();
        material.setTaskId(taskId);
        material.setProjectId(projectId);
        material.setFileName(file.getOriginalFilename());
        material.setFilePath(path);
        material.setFileSize(file.getSize());
        material.setMaterialType(materialType);
        material.setUploadedBy(SecurityUtils.currentUserId());
        materialMapper.insert(material);
        auditService.log("UPLOAD_MATERIAL", "TASK", taskId, "上传材料: " + material.getFileName());
        return material;
    }

    @Transactional
    public void assignExperts(Long taskId, AssignRequest req) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        ReviewProject project = requireProject(taskId, req.getProjectId());
        for (Long expertId : req.getExpertIds()) {
            createAssignment(task, project, expertId);
        }
        updateTaskInReview(task);
        auditService.log("ASSIGN_EXPERT", "TASK", taskId,
                "项目" + req.getProjectId() + "手动分配专家" + req.getExpertIds());
    }

    @Transactional
    public List<Long> randomAssign(Long taskId, RandomAssignRequest req) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        ReviewProject project = requireProject(taskId, req.getProjectId());
        List<Long> picked = pickRandomExperts(task, project, req.getCount(), req.getField(), req.isExcludeSameOrg());
        for (Long expertId : picked) {
            createAssignment(task, project, expertId);
        }
        updateTaskInReview(task);
        auditService.log("RANDOM_ASSIGN", "TASK", taskId,
                "项目" + req.getProjectId() + "随机抽取专家" + picked);
        return picked;
    }

    @Transactional
    public BatchRandomAssignResultVO batchRandomAssign(Long taskId, BatchRandomAssignRequest req) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        if ("SUMMARIZED".equals(task.getStatus()) || "ARCHIVED".equals(task.getStatus())) {
            throw new BusinessException("已结束的任务不可分配专家");
        }
        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getTaskId, taskId)
                .orderByAsc(ReviewProject::getSortOrder));
        if (projects.isEmpty()) {
            throw new BusinessException("暂无待评项目");
        }

        List<BatchRandomAssignResultVO.ProjectResult> details = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int skipped = 0;
        int totalAssigned = 0;

        for (ReviewProject project : projects) {
            long existing = countActiveAssignments(taskId, project.getId());
            if (req.isSkipAssigned() && existing >= req.getCount()) {
                skipped++;
                continue;
            }
            int need = req.getCount() - (int) existing;
            if (need <= 0) {
                skipped++;
                continue;
            }
            try {
                List<Long> picked = pickRandomExperts(task, project, need, req.getField(), req.isExcludeSameOrg());
                for (Long expertId : picked) {
                    createAssignment(task, project, expertId);
                }
                totalAssigned += picked.size();
                List<String> expertNames = picked.stream()
                        .map(id -> {
                            Expert expert = expertMapper.selectById(id);
                            return expert != null ? expert.getName() : String.valueOf(id);
                        })
                        .toList();
                details.add(BatchRandomAssignResultVO.ProjectResult.builder()
                        .projectId(project.getId())
                        .projectName(project.getProjectName())
                        .expertIds(picked)
                        .expertNames(expertNames)
                        .build());
            } catch (BusinessException e) {
                errors.add(project.getProjectName() + ": " + e.getMessage());
            }
        }

        if (details.isEmpty() && errors.isEmpty() && skipped == projects.size()) {
            throw new BusinessException("所有项目均已分配足够专家，无需重复操作");
        }
        if (details.isEmpty() && !errors.isEmpty()) {
            throw new BusinessException("批量分配失败：" + String.join("；", errors));
        }

        if (!details.isEmpty()) {
            updateTaskInReview(task);
        }
        auditService.log("BATCH_RANDOM_ASSIGN", "TASK", taskId,
                "批量随机分配 " + details.size() + " 个项目，共 " + totalAssigned + " 人次，跳过 " + skipped + " 个");
        return BatchRandomAssignResultVO.builder()
                .processedProjects(details.size())
                .skippedProjects(skipped)
                .totalAssigned(totalAssigned)
                .details(details)
                .errors(errors)
                .build();
    }

    @Transactional
    public void summarize(Long taskId) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        task.setStatus("SUMMARIZED");
        taskMapper.updateById(task);
        auditService.log("SUMMARIZE_TASK", "TASK", taskId, "任务汇总完成");
    }

    @Transactional
    public void archive(Long taskId) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        task.setStatus("ARCHIVED");
        taskMapper.updateById(task);
        auditService.log("ARCHIVE_TASK", "TASK", taskId, "任务已归档");
    }

    @Transactional
    public void publish(Long taskId) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        if ("SUMMARIZED".equals(task.getStatus()) || "ARCHIVED".equals(task.getStatus())) {
            throw new BusinessException("已结束的任务不可发布");
        }
        long projectCount = projectMapper.selectCount(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getTaskId, taskId));
        if (projectCount == 0) {
            throw new BusinessException("请先添加待评项目");
        }
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("截止时间已过期，请修改后再发布");
        }
        task.setStatus("PUBLISHED");
        taskMapper.updateById(task);
        auditService.log("PUBLISH_TASK", "TASK", taskId,
                "任务已发布，共 " + projectCount + " 个项目，可开始分配专家");
    }

    @Transactional
    public ReviewTask cloneTask(Long taskId, TaskCloneRequest req) {
        ReviewTask source = requireTask(taskId);
        dataScopeService.assertTaskAccess(source);
        ReviewTask task = new ReviewTask();
        task.setName(StringUtils.hasText(req.getName()) ? req.getName().trim() : source.getName() + " (副本)");
        task.setReviewType(source.getReviewType());
        task.setBatchName(source.getBatchName());
        task.setDataDomain(source.getDataDomain());
        task.setStartTime(source.getStartTime());
        task.setDeadline(req.getDeadline());
        task.setMinReviewers(source.getMinReviewers());
        task.setFormConfig(source.getFormConfig());
        task.setBlindReview(source.getBlindReview());
        task.setAvoidSameOrg(source.getAvoidSameOrg());
        task.setAvoidDuplicateExpert(source.getAvoidDuplicateExpert() == null || source.getAvoidDuplicateExpert());
        task.setStatus("UNASSIGNED");
        task.setCreatedBy(SecurityUtils.currentUserId());
        taskMapper.insert(task);

        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getTaskId, taskId)
                .orderByAsc(ReviewProject::getSortOrder));
        int order = 0;
        for (ReviewProject p : projects) {
            ReviewProject copy = new ReviewProject();
            copy.setTaskId(task.getId());
            copy.setProjectName(p.getProjectName());
            copy.setApplicant(p.getApplicant());
            copy.setOrganization(p.getOrganization());
            copy.setApplicantUserId(p.getApplicantUserId());
            copy.setSortOrder(order++);
            projectMapper.insert(copy);
        }
        auditService.log("CLONE_TASK", "TASK", task.getId(),
                "从任务 " + taskId + " 复制，含 " + projects.size() + " 个项目");
        return task;
    }

    @Transactional
    public ReviewTask updateDeadline(Long taskId, TaskDeadlineRequest req) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        if ("ARCHIVED".equals(task.getStatus())) {
            throw new BusinessException("已归档任务不可修改截止时间");
        }
        if (req.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("截止时间不能早于当前时间");
        }
        task.setDeadline(req.getDeadline());
        taskMapper.updateById(task);
        auditService.log("UPDATE_DEADLINE", "TASK", taskId,
                "截止时间调整为 " + req.getDeadline());
        return task;
    }

    @Transactional
    public ReviewTask updateFormConfig(Long taskId, TaskFormConfigUpdateRequest req) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        if ("SUMMARIZED".equals(task.getStatus()) || "ARCHIVED".equals(task.getStatus())) {
            throw new BusinessException("已结束的任务不可修改评分表");
        }
        validateFormConfig(req.getFormConfig());
        task.setFormConfig(req.getFormConfig());
        taskMapper.updateById(task);
        auditService.log("UPDATE_FORM_CONFIG", "TASK", taskId,
                "更新评分表 " + req.getFormConfig().size() + " 项");
        return task;
    }

    @Transactional
    public RemindResultVO remindPending(Long taskId, RemindRequest req) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        if ("SUMMARIZED".equals(task.getStatus()) || "ARCHIVED".equals(task.getStatus())) {
            throw new BusinessException("已结束的任务不可催办");
        }
        LambdaQueryWrapper<ReviewAssignment> wrapper = new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getTaskId, taskId)
                .in(ReviewAssignment::getStatus, "PENDING", "ACCEPTED", "OVERDUE");
        if (req != null && req.getAssignmentIds() != null && !req.getAssignmentIds().isEmpty()) {
            wrapper.in(ReviewAssignment::getId, req.getAssignmentIds());
        }
        List<ReviewAssignment> pending = assignmentMapper.selectList(wrapper);
        if (pending.isEmpty()) {
            throw new BusinessException("暂无待催办的专家");
        }
        long days = task.getDeadline() != null
                ? Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), task.getDeadline()))
                : 0;
        int count = 0;
        for (ReviewAssignment a : pending) {
            ReviewProject project = projectMapper.selectById(a.getProjectId());
            Map<String, String> vars = new HashMap<>();
            vars.put("taskName", task.getName());
            vars.put("projectName", project != null ? project.getProjectName() : "");
            vars.put("days", String.valueOf(days));
            if ("PENDING".equals(a.getStatus())) {
                notificationService.notifyExpert(a.getExpertId(),
                        notificationTemplateService.inviteTitle(vars),
                        notificationTemplateService.inviteContent(vars),
                        "REMIND", a.getId());
            } else {
                notificationService.notifyExpert(a.getExpertId(),
                        notificationTemplateService.deadlineTitle(vars),
                        notificationTemplateService.deadlineContent(vars),
                        "REMIND", a.getId());
            }
            count++;
        }
        auditService.log("REMIND_PENDING", "TASK", taskId, "催办 " + count + " 位专家");
        return RemindResultVO.builder().remindedCount(count).build();
    }

    public byte[] exportAssignments(Long taskId) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        List<ReviewAssignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getTaskId, taskId)
                .orderByAsc(ReviewAssignment::getProjectId));
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("专家分配");
            Row header = sheet.createRow(0);
            String[] headers = {"项目名称", "专家姓名", "专家单位", "状态", "邀请时间", "提交时间", "拒绝原因"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (ReviewAssignment a : assignments) {
                Expert expert = expertMapper.selectById(a.getExpertId());
                ReviewProject project = projectMapper.selectById(a.getProjectId());
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(StringUtil.nullToEmpty(project != null ? project.getProjectName() : null));
                row.createCell(1).setCellValue(StringUtil.nullToEmpty(expert != null ? expert.getName() : null));
                row.createCell(2).setCellValue(StringUtil.nullToEmpty(expert != null ? expert.getOrganization() : null));
                row.createCell(3).setCellValue(assignmentStatusLabel(a.getStatus()));
                row.createCell(4).setCellValue(a.getInvitedAt() != null ? a.getInvitedAt().toString() : "");
                row.createCell(5).setCellValue(a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : "");
                row.createCell(6).setCellValue(StringUtil.nullToEmpty(a.getRejectReason()));
            }
            auditService.log("EXPORT_ASSIGNMENT", "TASK", taskId, "导出专家分配 " + assignments.size() + " 条");
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    public byte[] projectImportTemplate() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("项目导入模板");
            Row row = sheet.createRow(0);
            String[] headers = {"项目名称*", "申请人", "申报单位", "申报人账号(可选)"};
            for (int i = 0; i < headers.length; i++) {
                row.createCell(i).setCellValue(headers[i]);
            }
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("示例项目A");
            sample.createCell(1).setCellValue("张三");
            sample.createCell(2).setCellValue("某某研究院");
            sample.createCell(3).setCellValue("applicant");
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("模板生成失败");
        }
    }

    @Transactional
    public ImportResultVO importProjects(Long taskId, MultipartFile file) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        List<String> errors = new ArrayList<>();
        int success = 0;
        int nextOrder = nextProjectOrder(taskId);
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String projectName = cellStr(row.getCell(0));
                if (!StringUtils.hasText(projectName)) continue;
                try {
                    ReviewProject project = new ReviewProject();
                    project.setTaskId(taskId);
                    project.setProjectName(projectName);
                    project.setApplicant(cellStr(row.getCell(1)));
                    project.setOrganization(cellStr(row.getCell(2)));
                    String applicantUsername = cellStr(row.getCell(3));
                    if (StringUtils.hasText(applicantUsername)) {
                        SysUser applicant = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getUsername, applicantUsername));
                        if (applicant == null) {
                            throw new BusinessException("申报人账号不存在: " + applicantUsername);
                        }
                        project.setApplicantUserId(applicant.getId());
                    }
                    project.setSortOrder(nextOrder++);
                    projectMapper.insert(project);
                    success++;
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
            auditService.log("IMPORT_PROJECT", "TASK", taskId,
                    "批量导入项目 " + success + " 条，错误 " + errors.size() + " 条");
        } catch (Exception e) {
            throw new BusinessException("导入失败: " + e.getMessage());
        }
        return ImportResultVO.builder().successCount(success).errors(errors).build();
    }

    public byte[] exportProjects(Long taskId) {
        ReviewTask task = requireTask(taskId);
        dataScopeService.assertTaskAccess(task);
        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getTaskId, taskId)
                .orderByAsc(ReviewProject::getSortOrder));
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("项目列表");
            Row header = sheet.createRow(0);
            String[] headers = {"项目名称", "申请人", "申报单位", "申报人账号"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (ReviewProject p : projects) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getProjectName());
                row.createCell(1).setCellValue(StringUtil.nullToEmpty(p.getApplicant()));
                row.createCell(2).setCellValue(StringUtil.nullToEmpty(p.getOrganization()));
                String username = "";
                if (p.getApplicantUserId() != null) {
                    SysUser u = sysUserMapper.selectById(p.getApplicantUserId());
                    username = u != null ? u.getUsername() : "";
                }
                row.createCell(3).setCellValue(username);
            }
            wb.write(out);
            auditService.log("EXPORT_PROJECT", "TASK", taskId, "导出项目 " + projects.size() + " 条");
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private int nextProjectOrder(Long taskId) {
        ReviewProject last = projectMapper.selectOne(new LambdaQueryWrapper<ReviewProject>()
                .eq(ReviewProject::getTaskId, taskId)
                .orderByDesc(ReviewProject::getSortOrder)
                .last("LIMIT 1"));
        return last != null && last.getSortOrder() != null ? last.getSortOrder() + 1 : 0;
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        return new DataFormatter().formatCellValue(cell).trim();
    }

    private LambdaQueryWrapper<ReviewTask> buildTaskListQuery(String status, String keyword) {
        LambdaQueryWrapper<ReviewTask> wrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyTaskFilter(wrapper);
        if (StringUtils.hasText(status)) {
            wrapper.eq(ReviewTask::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ReviewTask::getName, keyword)
                    .or().like(ReviewTask::getBatchName, keyword));
        }
        wrapper.orderByDesc(ReviewTask::getCreatedAt);
        return wrapper;
    }

    private Map<Long, Long> countProjectsByTask(List<ReviewTask> tasks) {
        if (tasks.isEmpty()) {
            return Map.of();
        }
        List<Long> taskIds = tasks.stream().map(ReviewTask::getId).toList();
        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                .in(ReviewProject::getTaskId, taskIds));
        return projects.stream().collect(Collectors.groupingBy(ReviewProject::getTaskId, Collectors.counting()));
    }

    private Map<Long, Long> countCompletedProjectsByTask(List<ReviewTask> tasks) {
        if (tasks.isEmpty()) {
            return Map.of();
        }
        List<Long> taskIds = tasks.stream().map(ReviewTask::getId).toList();
        Map<Long, Integer> minByTask = tasks.stream().collect(Collectors.toMap(
                ReviewTask::getId, t -> t.getMinReviewers() != null ? t.getMinReviewers() : 3));
        List<ReviewProject> projects = projectMapper.selectList(new LambdaQueryWrapper<ReviewProject>()
                .in(ReviewProject::getTaskId, taskIds));
        List<ReviewAssignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .in(ReviewAssignment::getTaskId, taskIds)
                .eq(ReviewAssignment::getStatus, "SUBMITTED"));
        Map<Long, Map<Long, Long>> submittedByTaskProject = assignments.stream()
                .collect(Collectors.groupingBy(ReviewAssignment::getTaskId,
                        Collectors.groupingBy(ReviewAssignment::getProjectId, Collectors.counting())));
        Map<Long, Long> result = new HashMap<>();
        for (ReviewTask task : tasks) {
            int min = minByTask.get(task.getId());
            Map<Long, Long> byProject = submittedByTaskProject.getOrDefault(task.getId(), Map.of());
            long completed = projects.stream()
                    .filter(p -> p.getTaskId().equals(task.getId()))
                    .filter(p -> byProject.getOrDefault(p.getId(), 0L) >= min)
                    .count();
            result.put(task.getId(), completed);
        }
        return result;
    }

    private String taskStatusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "UNASSIGNED" -> "待分配";
            case "PUBLISHED" -> "已发布";
            case "PENDING_CONFIRM" -> "待确认";
            case "IN_REVIEW" -> "评审中";
            case "SUMMARIZED" -> "已汇总";
            case "ARCHIVED" -> "已归档";
            default -> status;
        };
    }

    private String assignmentStatusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "PENDING" -> "待确认";
            case "ACCEPTED" -> "已接受";
            case "REJECTED" -> "已拒绝";
            case "SUBMITTED" -> "已提交";
            case "OVERDUE" -> "已逾期";
            default -> status;
        };
    }

    private Set<Long> expertsBusyInTask(ReviewTask task) {
        return assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                        .eq(ReviewAssignment::getTaskId, task.getId())
                        .ne(ReviewAssignment::getStatus, "REJECTED"))
                .stream().map(ReviewAssignment::getExpertId).collect(Collectors.toSet());
    }

    private boolean isDuplicateExpertBlocked(ReviewTask task, Long expertId, Set<Long> busyInTask) {
        if (task.getAvoidDuplicateExpert() != null && !task.getAvoidDuplicateExpert()) {
            return false;
        }
        return busyInTask.contains(expertId);
    }

    private long countActiveAssignments(Long taskId, Long projectId) {
        return assignmentMapper.selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getTaskId, taskId)
                .eq(ReviewAssignment::getProjectId, projectId)
                .ne(ReviewAssignment::getStatus, "REJECTED"));
    }

    private List<Long> pickRandomExperts(ReviewTask task, ReviewProject project, int count,
                                         String field, boolean excludeSameOrg) {
        LambdaQueryWrapper<Expert> wrapper = new LambdaQueryWrapper<Expert>()
                .eq(Expert::getStatus, "ACTIVE");
        if (StringUtils.hasText(field)) {
            wrapper.apply("JSON_CONTAINS(fields, {0})", "\"" + field + "\"");
        }
        List<Expert> candidates = expertMapper.selectList(wrapper);

        Set<Long> assigned = assignmentMapper.selectList(new LambdaQueryWrapper<ReviewAssignment>()
                        .eq(ReviewAssignment::getTaskId, task.getId())
                        .eq(ReviewAssignment::getProjectId, project.getId()))
                .stream().map(ReviewAssignment::getExpertId).collect(Collectors.toSet());

        boolean avoidSameOrg = excludeSameOrg
                && (task.getAvoidSameOrg() == null || task.getAvoidSameOrg());
        Set<Long> busyInTask = expertsBusyInTask(task);

        List<Expert> pool = candidates.stream()
                .filter(e -> !assigned.contains(e.getId()))
                .filter(e -> !avoidSameOrg || !sameOrganization(e.getOrganization(), project.getOrganization()))
                .filter(e -> !isDuplicateExpertBlocked(task, e.getId(), busyInTask))
                .filter(e -> !expertAvoidanceService.shouldAvoid(e, project))
                .collect(Collectors.toList());

        if (pool.size() < count) {
            throw new BusinessException("符合条件的专家不足，当前可选 " + pool.size() + " 人");
        }
        return expertAssignmentScorer.pickWeighted(pool, count);
    }

    private void createAssignment(ReviewTask task, ReviewProject project, Long expertId) {
        assertAssignable(task);
        Long exists = assignmentMapper.selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getTaskId, task.getId())
                .eq(ReviewAssignment::getProjectId, project.getId())
                .eq(ReviewAssignment::getExpertId, expertId));
        if (exists > 0) {
            return;
        }
        Expert expert = expertMapper.selectById(expertId);
        if (expert == null) {
            throw new BusinessException("专家不存在: " + expertId);
        }
        if (!"ACTIVE".equals(expert.getStatus())) {
            throw new BusinessException("专家「" + expert.getName() + "」状态为 " + expert.getStatus() + "，不可分配");
        }
        if (expertAvoidanceService.shouldAvoid(expert, project)) {
            throw new BusinessException("专家「" + expert.getName() + "」存在回避规则，不可分配");
        }
        if (Boolean.TRUE.equals(task.getAvoidSameOrg()) && sameOrganization(expert.getOrganization(), project.getOrganization())) {
            throw new BusinessException("专家「" + expert.getName() + "」与项目单位相同，已触发同单位回避");
        }
        if (isDuplicateExpertBlocked(task, expertId, expertsBusyInTask(task))) {
            throw new BusinessException("专家「" + expert.getName() + "」已在该任务其他项目中分配，已触发专家不重复规则");
        }
        ReviewAssignment assignment = new ReviewAssignment();
        assignment.setTaskId(task.getId());
        assignment.setProjectId(project.getId());
        assignment.setExpertId(expertId);
        assignment.setStatus("PENDING");
        assignment.setInvitedAt(LocalDateTime.now());
        assignmentMapper.insert(assignment);

        Map<String, String> vars = Map.of(
                "taskName", task.getName(),
                "projectName", project.getProjectName());
        notificationService.notifyExpert(expertId,
                notificationTemplateService.inviteTitle(vars),
                notificationTemplateService.inviteContent(vars),
                "INVITE", assignment.getId());
    }

    private void updateTaskInReview(ReviewTask task) {
        if ("UNASSIGNED".equals(task.getStatus())) {
            task.setStatus("PENDING_CONFIRM");
            taskMapper.updateById(task);
        } else if ("PENDING_CONFIRM".equals(task.getStatus())) {
            // keep
        } else if (!"SUMMARIZED".equals(task.getStatus()) && !"ARCHIVED".equals(task.getStatus())) {
            task.setStatus("IN_REVIEW");
            taskMapper.updateById(task);
        }
    }

    private ReviewTask requireTask(Long taskId) {
        ReviewTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return task;
    }

    private ReviewProject requireProject(Long taskId, Long projectId) {
        ReviewProject project = projectMapper.selectById(projectId);
        if (project == null || !project.getTaskId().equals(taskId)) {
            throw new BusinessException("项目不存在");
        }
        return project;
    }

    private boolean sameOrganization(String a, String b) {
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private void assertAssignable(ReviewTask task) {
        if ("UNASSIGNED".equals(task.getStatus())) {
            throw new BusinessException("请先发布任务后再分配专家");
        }
        if ("SUMMARIZED".equals(task.getStatus()) || "ARCHIVED".equals(task.getStatus())) {
            throw new BusinessException("已结束的任务不可分配专家");
        }
    }

    private void validateFormConfig(List<Map<String, Object>> formConfig) {
        if (formConfig == null || formConfig.isEmpty()) {
            throw new BusinessException("评分表至少包含一项");
        }
        double weightSum = 0;
        for (Map<String, Object> item : formConfig) {
            Object key = item.get("key");
            Object name = item.get("name");
            if (key == null || !StringUtils.hasText(String.valueOf(key))) {
                throw new BusinessException("评分项缺少 key");
            }
            if (name == null || !StringUtils.hasText(String.valueOf(name))) {
                throw new BusinessException("评分项缺少名称");
            }
            Object maxScore = item.get("maxScore");
            if (maxScore == null || Double.parseDouble(String.valueOf(maxScore)) <= 0) {
                throw new BusinessException("评分项「" + name + "」满分须大于 0");
            }
            Object weight = item.get("weight");
            if (weight != null) {
                weightSum += Double.parseDouble(String.valueOf(weight));
            }
        }
        if (weightSum > 0 && Math.abs(weightSum - 1.0) > 0.01) {
            throw new BusinessException("权重之和须为 1.0，当前为 " + weightSum);
        }
    }
}
