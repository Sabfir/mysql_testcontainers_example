CREATE DATABASE IF NOT EXISTS employeedb;

ALTER DATABASE employeedb
  DEFAULT CHARACTER SET utf8
  DEFAULT COLLATE utf8_general_ci;

-- GRANT ALL PRIVILEGES ON employeedb.* TO pc@localhost IDENTIFIED BY 'pc';

USE employeedb;

CREATE TABLE IF NOT EXISTS employee (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(30),
  last_name VARCHAR(30),
  INDEX(last_name)
) engine=InnoDB;
