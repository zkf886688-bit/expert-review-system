package com.review.system.tools;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 生成答辩用专家导入 Excel：mvn -s ../maven-settings.xml -Dtest=GenerateExpertImportXlsxTest test
 */
class GenerateExpertImportXlsxTest {

    private static final String[][] ROWS = {
            {"姓名*", "性别", "单位", "职称", "学历", "手机", "邮箱", "地区", "单位性质", "专业领域(逗号分隔)", "标签(逗号分隔)", "状态"},
            {"王建国", "男", "东南理工大学", "教授", "博士", "13800001001", "wangjg@demo.local", "江苏", "高校", "人工智能", "资深专家,演示数据", "ACTIVE"},
            {"李明华", "男", "材料科学研究院", "研究员", "博士", "13800001002", "limh@demo.local", "北京", "科研院所", "材料科学", "演示数据", "ACTIVE"},
            {"张秀芳", "女", "省立医院", "主任医师", "博士", "13800001003", "zhangxf@demo.local", "广东", "事业单位", "生物技术", "演示数据", "ACTIVE"},
            {"陈志远", "男", "华信电子股份有限公司", "高级工程师", "硕士", "13800001004", "chenzy@demo.local", "深圳", "企业", "电子信息", "演示数据", "ACTIVE"},
            {"刘文静", "女", "环境科学与工程系", "副教授", "博士", "13800001005", "liuwen@demo.local", "浙江", "高校", "环境工程", "演示数据", "ACTIVE"},
            {"赵永强", "男", "重工装备集团", "教授级高工", "本科", "13800001006", "zhaoyq@demo.local", "辽宁", "企业", "机械工程", "演示数据", "ACTIVE"},
            {"孙晓梅", "女", "化学化工学院", "教授", "博士", "13800001007", "sunxm@demo.local", "上海", "高校", "化学工程", "演示数据", "ACTIVE"},
            {"周德华", "男", "建筑设计研究院", "总工程师", "硕士", "13800001008", "zhoudh@demo.local", "四川", "科研院所", "土木建筑", "演示数据", "ACTIVE"},
            {"吴志强", "男", "能源动力研究所", "研究员", "博士", "13800001009", "wuzq@demo.local", "内蒙古", "科研院所", "能源动力", "演示数据", "ACTIVE"},
            {"郑海峰", "女", "管理学院", "副教授", "博士", "13800001010", "zhenghf@demo.local", "湖北", "高校", "管理科学", "演示数据", "ACTIVE"},
    };

    @Test
    void generate() throws IOException {
        Path out = Paths.get("..", "demo", "expert_import_10.xlsx").normalize().toAbsolutePath();
        Files.createDirectories(out.getParent());
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("专家导入模板");
            for (int i = 0; i < ROWS.length; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < ROWS[i].length; j++) {
                    row.createCell(j).setCellValue(ROWS[i][j]);
                }
            }
            try (var outStream = Files.newOutputStream(out)) {
                wb.write(outStream);
            }
        }
        System.out.println("已生成: " + out);
    }
}
