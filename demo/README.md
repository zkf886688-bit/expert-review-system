# 答辩演示说明

## 2 条【演示】任务

| 任务 | 用途 |
|------|------|
| **2026年度项目立项评审** | 进行中，现场导入专家后分配、提交 |
| **结题验收汇总样例** | 已汇总，演示汇总 / 导出 PDF |

## 现场导入专家

admin → **专家库** → **导入 Excel** → `demo/expert_import_10.xlsx`

重新生成 Excel：

```bash
cd backend
mvn -s ../maven-settings.xml -Dtest=GenerateExpertImportXlsxTest test
```
