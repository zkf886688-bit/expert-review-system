# 示例数据

专家导入示例由 `GenerateExpertImportXlsxTest` 生成，包含 10 条合成记录，用于测试 Excel 导入。姓名、单位和联系方式均为示例值。

在项目根目录执行：

```bash
cd backend
mvn -s ../maven-settings.xml -Dtest=GenerateExpertImportXlsxTest test
```

生成文件位于 `demo/expert_import_10.xlsx`。使用管理员账号进入“专家库”，通过“导入 Excel”上传。生成文件不纳入版本管理。

`app.demo.seed-tasks=true` 时，后端会维护带有“【演示】”前缀的评审任务，用于检查任务分配、评审提交和结果汇总。
