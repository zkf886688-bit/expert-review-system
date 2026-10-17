package com.review.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.entity.Expert;
import com.review.system.entity.ReviewProject;
import com.review.system.entity.SysUser;
import com.review.system.mapper.ExpertMapper;
import com.review.system.mapper.ReviewProjectMapper;
import com.review.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.context.annotation.DependsOn("schemaAutoFix")
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final ExpertMapper expertMapper;
    private final ReviewProjectMapper projectMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final DemoTaskSeeder demoTaskSeeder;
    private final ExpertLoginAccountSyncer expertLoginAccountSyncer;

    @Value("${app.demo.reset-passwords:true}")
    private boolean resetDemoPasswords;

    @Value("${app.demo.seed-experts:true}")
    private boolean seedExperts;

    @Override
    public void run(String... args) {
        ensureAdmin();
        ensureLeader();
        ensureApplicant();
        if (!seedExperts) {
            prepareLiveExpertDemo();
        } else {
            ensureDemoExperts();
        }
        expertLoginAccountSyncer.sync();
        demoTaskSeeder.seedIfNeeded();
    }

    private void ensureAdmin() {
        SysUser admin = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "admin"));
        if (admin != null) {
            if (resetDemoPasswords) {
                admin.setPassword(passwordEncoder.encode("admin123"));
            }
            if (admin.getDataDomain() == null) {
                admin.setDataDomain("DEFAULT");
            }
            sysUserMapper.updateById(admin);
        } else {
            jdbcTemplate.update("""
                    INSERT INTO sys_user (username, password, real_name, status, data_domain)
                    VALUES ('admin', ?, '系统管理员', 1, 'DEFAULT')
                    """, passwordEncoder.encode("admin123"));
        }
        ensureUserRoles("admin", "ADMIN", "REVIEW_ADMIN");
    }

    private void ensureLeader() {
        SysUser leader = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "leader"));
        if (leader != null) {
            if (resetDemoPasswords) {
                leader.setPassword(passwordEncoder.encode("leader123"));
                sysUserMapper.updateById(leader);
            }
        } else {
            jdbcTemplate.update("""
                    INSERT INTO sys_user (username, password, real_name, status, data_domain)
                    VALUES ('leader', ?, '研究院领导', 1, 'DEFAULT')
                    """, passwordEncoder.encode("leader123"));
        }
        ensureUserRoles("leader", "LEADER");
    }

    private void ensureApplicant() {
        SysUser applicant = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "applicant"));
        if (applicant != null) {
            if (resetDemoPasswords) {
                applicant.setPassword(passwordEncoder.encode("applicant123"));
                sysUserMapper.updateById(applicant);
            }
        } else {
            jdbcTemplate.update("""
                    INSERT INTO sys_user (username, password, real_name, status, data_domain)
                    VALUES ('applicant', ?, '示例申报人', 1, 'DEFAULT')
                    """, passwordEncoder.encode("applicant123"));
            applicant = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, "applicant"));
        }
        ensureUserRoles("applicant", "APPLICANT");
        if (applicant != null) {
            bindDemoProject(applicant.getId());
        }
    }

    private void ensureUserRoles(String username, String... roleCodes) {
        for (String roleCode : roleCodes) {
            jdbcTemplate.update("""
                    INSERT INTO sys_user_role (user_id, role_id)
                    SELECT u.id, r.id FROM sys_user u, sys_role r
                    WHERE u.username = ? AND r.code = ?
                    AND NOT EXISTS (
                        SELECT 1 FROM sys_user_role ur
                        WHERE ur.user_id = u.id AND ur.role_id = r.id
                    )
                    """, username, roleCode);
        }
    }

    private void bindDemoProject(Long applicantUserId) {
        ReviewProject project = projectMapper.selectOne(new LambdaQueryWrapper<ReviewProject>()
                .isNull(ReviewProject::getApplicantUserId)
                .orderByAsc(ReviewProject::getId)
                .last("LIMIT 1"));
        if (project != null) {
            project.setApplicantUserId(applicantUserId);
            projectMapper.updateById(project);
        }
    }

    private void ensureDemoExperts() {
        if (!seedExperts) {
            log.debug("演示专家种子数据已关闭 (app.demo.seed-experts=false)");
            return;
        }
        List<DemoExpert> demos = demoExpertList();
        try {
            int inserted = 0;
            for (DemoExpert demo : demos) {
                if (upsertDemoExpert(demo)) {
                    inserted++;
                }
            }
            if (inserted > 0) {
                log.info("已新增 {} 条演示专家（库内演示专家共 {} 人），登录账号 expert1 / expert123",
                        inserted, demos.size());
            }
        } catch (Exception e) {
            log.error("演示专家数据初始化失败，请检查 expert 表结构或重启后端: {}", e.getMessage(), e);
        }
    }

    /** 答辩演示：保留 Excel 导入的专家，仅重置进行中演示任务的分配。 */
    private void prepareLiveExpertDemo() {
        demoTaskSeeder.clearLiveDemoAssignments();
        log.debug("演示模式：专家请使用 demo/expert_import_10.xlsx 导入，expert1 将自动绑定库内首位专家");
    }

    /** @return true 表示新插入 */
    private boolean upsertDemoExpert(DemoExpert demo) {
        Expert expert = expertMapper.selectOne(new LambdaQueryWrapper<Expert>()
                .eq(Expert::getPhone, demo.phone()));
        if (expert == null) {
            expert = new Expert();
            applyDemoExpert(expert, demo);
            expertMapper.insert(expert);
            return true;
        }
        expert.setFields(demo.fields());
        expert.setTags(demo.tags());
        expert.setOrganization(demo.organization());
        expert.setTitle(demo.title());
        expert.setRegion(demo.region());
        expert.setOrgType(demo.orgType());
        expertMapper.updateById(expert);
        return false;
    }

    private static void applyDemoExpert(Expert expert, DemoExpert demo) {
        expert.setName(demo.name());
        expert.setGender(demo.gender());
        expert.setOrganization(demo.organization());
        expert.setTitle(demo.title());
        expert.setPhone(demo.phone());
        expert.setEmail(demo.email());
        expert.setRegion(demo.region());
        expert.setOrgType(demo.orgType());
        expert.setFields(demo.fields());
        expert.setTags(demo.tags());
        expert.setStatus("ACTIVE");
        expert.setReviewCount(demo.reviewCount());
        expert.setOnTimeRate(demo.onTimeRate());
    }

    private static List<DemoExpert> demoExpertList() {
        return List.of(
                new DemoExpert("王建国", "男", "东南理工大学", "教授", "13800001001", "wangjg@demo.local", "江苏", "高校",
                        List.of("人工智能"), List.of("资深专家", "演示数据"), 12, new BigDecimal("98.00")),
                new DemoExpert("李明华", "男", "材料科学研究院", "研究员", "13800001002", "limh@demo.local", "北京", "科研院所",
                        List.of("材料科学"), List.of("演示数据"), 10, new BigDecimal("96.00")),
                new DemoExpert("张秀芳", "女", "省立医院", "主任医师", "13800001003", "zhangxf@demo.local", "广东", "事业单位",
                        List.of("生物技术"), List.of("演示数据"), 8, new BigDecimal("99.50")),
                new DemoExpert("陈志远", "男", "华信电子股份有限公司", "高级工程师", "13800001004", "chenzy@demo.local", "深圳", "企业",
                        List.of("电子信息"), List.of("演示数据"), 15, new BigDecimal("94.50")),
                new DemoExpert("刘文静", "女", "环境科学与工程系", "副教授", "13800001005", "liuwen@demo.local", "浙江", "高校",
                        List.of("环境工程"), List.of("演示数据"), 6, new BigDecimal("100.00")),
                new DemoExpert("赵永强", "男", "重工装备集团", "教授级高工", "13800001006", "zhaoyq@demo.local", "辽宁", "企业",
                        List.of("机械工程"), List.of("演示数据"), 11, new BigDecimal("91.00")),
                new DemoExpert("孙晓梅", "女", "化学化工学院", "教授", "13800001007", "sunxm@demo.local", "上海", "高校",
                        List.of("化学工程"), List.of("演示数据"), 9, new BigDecimal("97.00")),
                new DemoExpert("周德华", "男", "建筑设计研究院", "总工程师", "13800001008", "zhoudh@demo.local", "四川", "科研院所",
                        List.of("土木建筑"), List.of("演示数据"), 13, new BigDecimal("95.00")),
                new DemoExpert("吴志强", "男", "能源动力研究所", "研究员", "13800001009", "wuzq@demo.local", "内蒙古", "科研院所",
                        List.of("能源动力"), List.of("演示数据"), 7, new BigDecimal("98.50")),
                new DemoExpert("郑海峰", "女", "管理学院", "副教授", "13800001010", "zhenghf@demo.local", "湖北", "高校",
                        List.of("管理科学"), List.of("演示数据"), 6, new BigDecimal("97.00")),
                new DemoExpert("黄晓琳", "女", "量子信息实验室", "副研究员", "13800001011", "huangxl@demo.local", "北京", "科研院所",
                        List.of("人工智能", "计算机科学"), List.of("演示数据"), 14, new BigDecimal("96.50")),
                new DemoExpert("马俊杰", "男", "水利水电勘测设计院", "高级工程师", "13800001012", "majj@demo.local", "四川", "事业单位",
                        List.of("水利工程"), List.of("演示数据"), 9, new BigDecimal("94.00")),
                new DemoExpert("林雪梅", "女", "省农业科学院", "研究员", "13800001013", "linxm@demo.local", "山东", "科研院所",
                        List.of("农业科技"), List.of("演示数据"), 11, new BigDecimal("98.20")),
                new DemoExpert("徐文博", "男", "医科大学附属第一医院", "副主任医师", "13800001014", "xuwb@demo.local", "广东", "事业单位",
                        List.of("临床医学"), List.of("演示数据"), 7, new BigDecimal("92.50")),
                new DemoExpert("胡静怡", "女", "金融科技股份公司", "首席科学家", "13800001015", "hujy@demo.local", "上海", "企业",
                        List.of("金融科技", "大数据"), List.of("演示数据"), 18, new BigDecimal("99.00")),
                new DemoExpert("高天宇", "男", "航空航天研究所", "研究员", "13800001016", "gaoty@demo.local", "陕西", "科研院所",
                        List.of("航空航天"), List.of("资深专家", "演示数据"), 15, new BigDecimal("95.80")),
                new DemoExpert("何俊杰", "男", "城市规划与设计院", "注册规划师", "13800001017", "hejj@demo.local", "重庆", "事业单位",
                        List.of("城市规划"), List.of("演示数据"), 5, new BigDecimal("100.00")),
                new DemoExpert("罗美玲", "女", "师范大学教育学院", "教授", "13800001018", "luoml@demo.local", "湖南", "高校",
                        List.of("教育学"), List.of("演示数据"), 8, new BigDecimal("93.60")),
                new DemoExpert("邓凯文", "男", "国家网络安全中心", "高级工程师", "13800001019", "dengkw@demo.local", "北京", "科研院所",
                        List.of("网络安全", "信息安全"), List.of("演示数据"), 12, new BigDecimal("96.00")),
                new DemoExpert("谢佳音", "女", "新能源科技有限公司", "技术总监", "13800001020", "xiejy@demo.local", "江苏", "企业",
                        List.of("新能源", "储能技术"), List.of("演示数据"), 10, new BigDecimal("97.50"))
        );
    }

    private record DemoExpert(
            String name, String gender, String organization, String title,
            String phone, String email, String region, String orgType,
            List<String> fields, List<String> tags,
            int reviewCount, BigDecimal onTimeRate) {
    }
}
