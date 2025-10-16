CREATE DATABASE CSDL;
USE CSDL;

CREATE TABLE product_group (
  group_id INT AUTO_INCREMENT PRIMARY KEY,
  group_name VARCHAR(100),
  description TEXT
);

CREATE TABLE product (
  product_id INT AUTO_INCREMENT PRIMARY KEY,
  group_id INT,
  name VARCHAR(255),
  brand VARCHAR(100),
  url VARCHAR(500),
  image_url VARCHAR(500),
  description TEXT,
  source VARCHAR(100),
  is_featured BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (group_id) REFERENCES product_group(group_id)
);

CREATE TABLE price_history (
  price_id INT AUTO_INCREMENT PRIMARY KEY,
  product_id INT,
  price DECIMAL(15,2),
  original_price DECIMAL(15,2),
  currency VARCHAR(10),
  deal_type ENUM('NORMAL','FLASH_SALE','HOT_DEAL','TRENDING') DEFAULT 'NORMAL',
  recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES product(product_id)
);

CREATE TABLE review (
  review_id INT AUTO_INCREMENT PRIMARY KEY,
  product_id INT,
  reviewer_name VARCHAR(100),
  rating DECIMAL(2,1),
  comment TEXT,
  review_date DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES product(product_id)
);

CREATE TABLE scrape_log (
  log_id INT AUTO_INCREMENT PRIMARY KEY,
  scrape_date DATETIME DEFAULT CURRENT_TIMESTAMP,
  source VARCHAR(100),
  total_products INT,
  total_reviews INT,
  status ENUM('SUCCESS','FAILED') DEFAULT 'SUCCESS',
  notes TEXT
);

CREATE TABLE error_log (
  error_id INT AUTO_INCREMENT PRIMARY KEY,
  occurred_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  component VARCHAR(100),
  message TEXT,
  stacktrace TEXT
);
