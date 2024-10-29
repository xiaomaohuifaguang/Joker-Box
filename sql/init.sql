INSERT INTO `cat_user` (`id`, `type`, `deleted`, `username`, `password`, `nickname`, `create_time`, `update_time`)
VALUES (0000000001, '0', '0', 'admin', '57b7741a90a361466730bee54e4c7fa99a50516d5d1975e0ceb7b9e986bf1746', '超级管理员', '2024-10-07 00:35:42', '2024-10-07 00:35:45');
INSERT INTO `cat_role` (`id`, `name`, `deleted`, `create_time`, `update_time`) VALUES (1, '超级管理员', '0', '2024-10-07 00:36:18', '2024-10-07 00:36:20');
INSERT INTO `cat_role` (`id`, `name`, `deleted`, `create_time`, `update_time`) VALUES (2, '超级普通人', '0', '2024-10-07 01:48:02', '2024-10-23 00:56:20');
INSERT INTO `cat_user_and_role` (`user_id`, `role_id`, `create_time`) VALUES (1, 1, '2024-10-07 01:23:37');
INSERT INTO `cat_user_and_role` (`user_id`, `role_id`, `create_time`) VALUES (1, 2, '2024-10-14 00:21:35');