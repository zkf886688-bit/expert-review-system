-- 演示专家（仅开发/答辩用）。若后端 seed 未生效，可手动执行：
-- mysql -u root -p expert_review < sql/demo_experts.sql

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT * FROM (SELECT '王建国','男','东南理工大学','教授','13800001001','wangjg@demo.local','江苏','高校',
  JSON_ARRAY('人工智能'), JSON_ARRAY('资深专家','演示数据'), 'ACTIVE', 0, 100.00) AS t
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001001');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '李明华','男','材料科学研究院','研究员','13800001002','limh@demo.local','北京','科研院所',
  JSON_ARRAY('材料科学'), JSON_ARRAY('演示数据'), 'ACTIVE', 0, 100.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001002');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '张秀芳','女','省立医院','主任医师','13800001003','zhangxf@demo.local','广东','事业单位',
  JSON_ARRAY('生物技术'), JSON_ARRAY('演示数据'), 'ACTIVE', 0, 100.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001003');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '陈志远','男','华信电子股份有限公司','高级工程师','13800001004','chenzy@demo.local','深圳','企业',
  JSON_ARRAY('电子信息'), JSON_ARRAY('演示数据'), 'ACTIVE', 0, 100.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001004');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '刘文静','女','环境科学与工程系','副教授','13800001005','liuwen@demo.local','浙江','高校',
  JSON_ARRAY('环境工程'), JSON_ARRAY('演示数据'), 'ACTIVE', 0, 100.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001005');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '赵永强','男','重工装备集团','教授级高工','13800001006','zhaoyq@demo.local','辽宁','企业',
  JSON_ARRAY('机械工程'), JSON_ARRAY('演示数据'), 'ACTIVE', 0, 100.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001006');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '孙晓梅','女','化学化工学院','教授','13800001007','sunxm@demo.local','上海','高校',
  JSON_ARRAY('化学工程'), JSON_ARRAY('演示数据'), 'ACTIVE', 0, 100.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001007');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '周德华','男','建筑设计研究院','总工程师','13800001008','zhoudh@demo.local','四川','科研院所',
  JSON_ARRAY('土木建筑'), JSON_ARRAY('演示数据'), 'ACTIVE', 0, 100.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001008');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '吴志强','男','能源动力研究所','研究员','13800001009','wuzq@demo.local','内蒙古','科研院所',
  JSON_ARRAY('能源动力'), JSON_ARRAY('演示数据'), 'ACTIVE', 0, 100.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001009');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '郑海峰','女','管理学院','副教授','13800001010','zhenghf@demo.local','湖北','高校',
  JSON_ARRAY('管理科学'), JSON_ARRAY('演示数据'), 'ACTIVE', 0, 100.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001010');

-- 已有演示专家时，将专业领域同步为单领域
UPDATE expert SET fields = JSON_ARRAY('人工智能') WHERE phone = '13800001001';
UPDATE expert SET fields = JSON_ARRAY('材料科学') WHERE phone = '13800001002';
UPDATE expert SET fields = JSON_ARRAY('生物技术') WHERE phone = '13800001003';
UPDATE expert SET fields = JSON_ARRAY('电子信息') WHERE phone = '13800001004';
UPDATE expert SET fields = JSON_ARRAY('环境工程') WHERE phone = '13800001005';
UPDATE expert SET fields = JSON_ARRAY('机械工程') WHERE phone = '13800001006';
UPDATE expert SET fields = JSON_ARRAY('化学工程') WHERE phone = '13800001007';
UPDATE expert SET fields = JSON_ARRAY('土木建筑') WHERE phone = '13800001008';
UPDATE expert SET fields = JSON_ARRAY('能源动力') WHERE phone = '13800001009';
UPDATE expert SET fields = JSON_ARRAY('管理科学') WHERE phone = '13800001010';

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '黄晓琳','女','量子信息实验室','副研究员','13800001011','huangxl@demo.local','北京','科研院所',
  JSON_ARRAY('人工智能','计算机科学'), JSON_ARRAY('演示数据'), 'ACTIVE', 14, 96.50
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001011');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '马俊杰','男','水利水电勘测设计院','高级工程师','13800001012','majj@demo.local','四川','事业单位',
  JSON_ARRAY('水利工程'), JSON_ARRAY('演示数据'), 'ACTIVE', 9, 94.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001012');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '林雪梅','女','省农业科学院','研究员','13800001013','linxm@demo.local','山东','科研院所',
  JSON_ARRAY('农业科技'), JSON_ARRAY('演示数据'), 'ACTIVE', 11, 98.20
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001013');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '徐文博','男','医科大学附属第一医院','副主任医师','13800001014','xuwb@demo.local','广东','事业单位',
  JSON_ARRAY('临床医学'), JSON_ARRAY('演示数据'), 'ACTIVE', 7, 92.50
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001014');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '胡静怡','女','金融科技股份公司','首席科学家','13800001015','hujy@demo.local','上海','企业',
  JSON_ARRAY('金融科技','大数据'), JSON_ARRAY('演示数据'), 'ACTIVE', 18, 99.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001015');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '高天宇','男','航空航天研究所','研究员','13800001016','gaoty@demo.local','陕西','科研院所',
  JSON_ARRAY('航空航天'), JSON_ARRAY('资深专家','演示数据'), 'ACTIVE', 15, 95.80
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001016');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '何俊杰','男','城市规划与设计院','注册规划师','13800001017','hejj@demo.local','重庆','事业单位',
  JSON_ARRAY('城市规划'), JSON_ARRAY('演示数据'), 'ACTIVE', 5, 100.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001017');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '罗美玲','女','师范大学教育学院','教授','13800001018','luoml@demo.local','湖南','高校',
  JSON_ARRAY('教育学'), JSON_ARRAY('演示数据'), 'ACTIVE', 8, 93.60
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001018');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '邓凯文','男','国家网络安全中心','高级工程师','13800001019','dengkw@demo.local','北京','科研院所',
  JSON_ARRAY('网络安全','信息安全'), JSON_ARRAY('演示数据'), 'ACTIVE', 12, 96.00
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001019');

INSERT INTO expert (name, gender, organization, title, phone, email, region, org_type, fields, tags, status, review_count, on_time_rate)
SELECT '谢佳音','女','新能源科技有限公司','技术总监','13800001020','xiejy@demo.local','江苏','企业',
  JSON_ARRAY('新能源','储能技术'), JSON_ARRAY('演示数据'), 'ACTIVE', 10, 97.50
WHERE NOT EXISTS (SELECT 1 FROM expert WHERE phone = '13800001020');

UPDATE expert SET fields = JSON_ARRAY('人工智能','计算机科学') WHERE phone = '13800001011';
UPDATE expert SET fields = JSON_ARRAY('水利工程') WHERE phone = '13800001012';
UPDATE expert SET fields = JSON_ARRAY('农业科技') WHERE phone = '13800001013';
UPDATE expert SET fields = JSON_ARRAY('临床医学') WHERE phone = '13800001014';
UPDATE expert SET fields = JSON_ARRAY('金融科技','大数据') WHERE phone = '13800001015';
UPDATE expert SET fields = JSON_ARRAY('航空航天') WHERE phone = '13800001016';
UPDATE expert SET fields = JSON_ARRAY('城市规划') WHERE phone = '13800001017';
UPDATE expert SET fields = JSON_ARRAY('教育学') WHERE phone = '13800001018';
UPDATE expert SET fields = JSON_ARRAY('网络安全','信息安全') WHERE phone = '13800001019';
UPDATE expert SET fields = JSON_ARRAY('新能源','储能技术') WHERE phone = '13800001020';
