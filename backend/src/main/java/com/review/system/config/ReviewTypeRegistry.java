package com.review.system.config;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReviewTypeRegistry {

    private static final List<String> ALL_TYPES = List.of(
            "揭榜挂帅", "项目立项", "中期检查", "结题验收", "成果评价", "奖项评审", "人才评审"
    );

    public List<String> allTypes() {
        return ALL_TYPES;
    }

    private static Map<String, Object> item(String key, String name, int maxScore, double weight) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("name", name);
        m.put("maxScore", maxScore);
        m.put("weight", weight);
        return m;
    }

    public List<Map<String, Object>> defaultFormConfig(String reviewType) {
        return switch (reviewType != null ? reviewType : "") {
            case "中期检查" -> form(
                    item("progress", "进展完成度", 30, 0.3),
                    item("stage_result", "阶段成果", 30, 0.3),
                    item("budget_use", "经费使用", 20, 0.2),
                    item("risk", "问题与风险", 20, 0.2));
            case "结题验收" -> form(
                    item("goal", "目标完成", 25, 0.25),
                    item("quality", "成果质量", 30, 0.3),
                    item("promotion", "应用推广", 25, 0.25),
                    item("final_budget", "经费决算", 20, 0.2));
            case "成果评价", "奖项评审" -> form(
                    item("innovation", "创新性", 30, 0.3),
                    item("advanced", "先进性", 25, 0.25),
                    item("value", "应用价值", 25, 0.25),
                    item("evidence", "佐证材料", 20, 0.2));
            case "人才评审" -> form(
                    item("academic", "学术水平", 30, 0.3),
                    item("innovation", "创新能力", 25, 0.25),
                    item("team", "团队贡献", 25, 0.25),
                    item("potential", "发展潜力", 20, 0.2));
            case "揭榜挂帅" -> form(
                    item("match", "榜单匹配度", 25, 0.25),
                    item("plan", "技术方案", 30, 0.3),
                    item("team", "团队能力", 25, 0.25),
                    item("condition", "实施条件", 20, 0.2));
            default -> form(
                    item("innovation", "创新性", 25, 0.25),
                    item("feasibility", "可行性", 25, 0.25),
                    item("team", "团队基础", 20, 0.2),
                    item("value", "应用价值", 20, 0.2),
                    item("budget", "经费合理性", 10, 0.1));
        };
    }

    public List<String> conclusionOptions(String reviewType) {
        return switch (reviewType != null ? reviewType : "") {
            case "中期检查" -> List.of("进展正常", "需加强管理", "建议整改", "不建议继续");
            case "结题验收" -> List.of("验收通过", "整改后验收", "验收不通过");
            case "成果评价", "奖项评审" -> List.of("推荐", "备选", "不推荐");
            case "人才评审" -> List.of("推荐入选", "待定", "不推荐");
            case "揭榜挂帅" -> List.of("建议揭榜", "建议修改后揭榜", "不建议揭榜");
            default -> List.of("推荐立项", "建议修改后通过", "不推荐立项");
        };
    }

    public RecommendationRule recommendationRule(String reviewType) {
        return switch (reviewType != null ? reviewType : "") {
            case "中期检查" -> new RecommendationRule(75, 60, "进展正常", "需加强管理", "建议整改");
            case "结题验收" -> new RecommendationRule(80, 65, "验收通过", "整改后验收", "验收不通过");
            case "成果评价", "奖项评审" -> new RecommendationRule(85, 70, "推荐", "备选", "不推荐");
            case "人才评审" -> new RecommendationRule(80, 65, "推荐入选", "待定", "不推荐");
            case "揭榜挂帅" -> new RecommendationRule(80, 60, "建议揭榜", "建议修改后揭榜", "不建议揭榜");
            default -> new RecommendationRule(80, 60, "推荐立项", "建议修改后通过", "不推荐立项");
        };
    }

    public Map<String, Object> configForType(String reviewType) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reviewType", reviewType);
        map.put("formConfig", defaultFormConfig(reviewType));
        map.put("conclusions", conclusionOptions(reviewType));
        map.put("recommendationRule", recommendationRule(reviewType));
        return map;
    }

    @SafeVarargs
    private static List<Map<String, Object>> form(Map<String, Object>... items) {
        return List.of(items);
    }

    public record RecommendationRule(double passScore, double reviseScore,
                                     String passLabel, String reviseLabel, String failLabel) {
    }
}
