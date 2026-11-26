-- 移除任务功能：清理 TASK 类型站内消息、通知偏好中的 TASK 项，并删除 iot_task 表
DELETE FROM `iot_message` WHERE `type` = 'TASK';

UPDATE `iot_notify_pref`
SET `receive_kinds` = JSON_REMOVE(`receive_kinds`, JSON_UNQUOTE(JSON_SEARCH(`receive_kinds`, 'one', 'TASK')))
WHERE JSON_CONTAINS(`receive_kinds`, JSON_QUOTE('TASK'));

DROP TABLE IF EXISTS `iot_task`;
