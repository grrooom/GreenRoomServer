CREATE TABLE `users` (
                         `user_id` bigint PRIMARY KEY AUTO_INCREMENT,
                         `grade_id` varchar(255),
                         `name` varchar(255),
                         `email` varchar(255),
                         `password` varchar(255),
                         `weekly_seed` int,
                         `total_seed` int,
                         `profile_url` varchar(255),
                         `role` varchar(255),
                         `provider` varchar(255),
                         `user_status` varchar(255),
                         `delete_date` timestamp,
                         `create_date` timestamp  DEFAULT CURRENT_TIMESTAMP,
                         `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `suggestion` (
                              `suggestion_id` bigint PRIMARY KEY AUTO_INCREMENT,
                              `user_id` bigint,
                              `content` varchar(255),
                              `is_registered` bit,
                              `create_date` timestamp  DEFAULT CURRENT_TIMESTAMP,
                              `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `grade` (
                         `grade_id` bigint PRIMARY KEY AUTO_INCREMENT,
                         `level` int,
                         `description` varchar(255),
                         `grade_image_url` varchar(255),
                         `required_seed` bigint,
                         `create_date` timestamp DEFAULT CURRENT_TIMESTAMP,
                         `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `adornment` (
                             `adornment_id` bigint PRIMARY KEY,
                             `item_id` bigint,
                             `greenroom_id` bigint,
                             `create_date` timestamp DEFAULT CURRENT_TIMESTAMP,
                             `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `greenroom` (
                             `greenroom_id` bigint PRIMARY KEY AUTO_INCREMENT,
                             `user_id` bigint,
                             `plant_id` bigint,
                             `name` varchar(255),
                             `picture_url` varchar(255),
                             `memo` varchar(255),
                             `greenroom_status` varchar(255),
                             `create_date` timestamp DEFAULT CURRENT_TIMESTAMP,
                             `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `item` (
                        `item_id` bigint PRIMARY KEY AUTO_INCREMENT,
                        `item_type` varchar(255),
                        `grade_id` bigint,
                        `item_name` varchar(255),
                        `image_url` varchar(255),
                        `create_date` timestamp DEFAULT CURRENT_TIMESTAMP,
                        `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `todo` (
                        `todo_id` bigint PRIMARY KEY AUTO_INCREMENT,
                        `activity_id` bigint,
                        `greenroom_id` bigint,
                        `first_start_date` timestamp,
                        `last_update_date` timestamp,
                        `next_todo_date` timestamp,
                        `duration` date,
                        `use_yn` bit,
                        `create_date` timestamp DEFAULT CURRENT_TIMESTAMP,
                        `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `todo_log` (
                            `todo_log_id` bigint PRIMARY KEY AUTO_INCREMENT,
                            `todo_id` bigint,
                            `create_date` timestamp DEFAULT CURRENT_TIMESTAMP,
                            `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `activity` (
                            `activity_id` bigint PRIMARY KEY AUTO_INCREMENT,
                            `name` varchar(255),
                            `create_date` timestamp DEFAULT CURRENT_TIMESTAMP,
                            `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `plant` (
                         `plant_id` bigint PRIMARY KEY AUTO_INCREMENT,
                         `plant_category` varchar(255),
                         `plant_alias` varchar(255),
                         `distribution_name` varchar(255),
                         `plant_picture_url` varchar(255),
                         `plant_count` int,
                         `water_cycle` varchar(200),
                         `light_demand` varchar(255),
                         `growth_temperature` varchar(255),
                         `humidity` varchar(255),
                         `fertilizer` varchar(255),
                         `manage_level` varchar(255),
                         `other_information` varchar(255),
                         `create_date` timestamp  DEFAULT CURRENT_TIMESTAMP,
                         `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `notification` (
                         `notification_id` bigint PRIMARY KEY AUTO_INCREMENT,
                         `user_id` bigint,
                         `notification_enabled` bit,
                         `fcm_token` varchar(255),
                         `create_date` timestamp  DEFAULT CURRENT_TIMESTAMP,
                         `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `diary` (
                         `diary_id` bigint PRIMARY KEY AUTO_INCREMENT,
                         `greenroom_id` bigint,
                         `diary_picture_url` varchar(255),
                         `title` varchar(255),
                         `content` varchar(1000),
                         `create_date` timestamp DEFAULT CURRENT_TIMESTAMP,
                         `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `email_verification_logs` (
                                           `email_log_id` bigint PRIMARY KEY AUTO_INCREMENT,
                                           `email` varchar(255),
                                           `number_of_trial` bigint,
                                           `verification_token` varchar(255),
                                           `verification_status` varchar(255),
                                           `expires_at` timestamp,
                                           `create_date` timestamp  DEFAULT CURRENT_TIMESTAMP,
                                           `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `refresh_token` (
                                 `refresh_token_id` bigint PRIMARY KEY AUTO_INCREMENT,
                                 `user_id` bigint,
                                 `refresh_token` varchar(255),
                                 `create_date` timestamp  DEFAULT CURRENT_TIMESTAMP,
                                 `update_date` timestamp ON UPDATE CURRENT_TIMESTAMP
);


CREATE TABLE `user_exit_reason` (
                                `user_exit_reason_id` bigint PRIMARY KEY AUTO_INCREMENT,
                                `reason` varchar(255),
                                `reason_type` varchar(255),
                                `count` bigint
);



ALTER TABLE `greenroom` ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `greenroom` ADD FOREIGN KEY (`plant_id`) REFERENCES `plant` (`plant_id`);

ALTER TABLE `diary` ADD FOREIGN KEY (`greenroom_id`) REFERENCES `greenroom` (`greenroom_id`);

ALTER TABLE `todo` ADD FOREIGN KEY (`greenroom_id`) REFERENCES `greenroom` (`greenroom_id`);

ALTER TABLE `todo` ADD FOREIGN KEY (`activity_id`) REFERENCES `activity` (`activity_id`);

ALTER TABLE `users` ADD FOREIGN KEY (`grade_id`) REFERENCES `grade` (`grade_id`);

ALTER TABLE `adornment` ADD FOREIGN KEY (`item_id`) REFERENCES `item` (`item_id`);

ALTER TABLE `adornment` ADD FOREIGN KEY (`greenroom_id`) REFERENCES `greenroom` (`greenroom_id`);

ALTER TABLE `suggestion` ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `todo_log` ADD FOREIGN KEY (`todo_id`) REFERENCES `todo` (`todo_id`);

ALTER TABLE `item` ADD FOREIGN KEY (`grade_id`) REFERENCES `grade` (`grade_id`);

ALTER TABLE `notification` ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `refresh_token` ADD FOREIGN KEY(`user_id`) REFERENCES `users` (`user_id`);
