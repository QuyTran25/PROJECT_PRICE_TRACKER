


#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Tiki Price Scraper - Tầng 1 Giai đoạn 1
Cào giá sản phẩm từ Tiki và lưu vào database
Chạy thủ công: python scraper.py
"""

import requests
import mysql.connector
from datetime import datetime
import time
import json
import re

# ===== CẤU HÌNH DATABASE =====
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '',  # XAMPP mặc định không có password
    'database': 'price_insight',
    'charset': 'utf8mb4'
}

# ===== CẤU HÌNH SCRAPER =====
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'application/json, text/plain, */*',
    'Accept-Language': 'vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7',
    'Referer': 'https://tiki.vn/',
}

DELAY_BETWEEN_REQUESTS = 2  # Giây delay giữa các request (tránh bị block)


class TikiScraper:
    def __init__(self):
        self.db_connection = None
        self.cursor = None
        self.stats = {
            'total': 0,
            'success': 0,
            'failed': 0,
            'skipped': 0
        }
    
    def connect_db(self):
        """Kết nối đến MySQL database"""
        try:
            self.db_connection = mysql.connector.connect(**DB_CONFIG)
            self.cursor = self.db_connection.cursor(dictionary=True)
            print("✓ Đã kết nối database thành công!")
            return True
        except mysql.connector.Error as err:
            print(f"✗ Lỗi kết nối database: {err}")
            return False
    
    def close_db(self):
        """Đóng kết nối database"""
        if self.cursor:
            self.cursor.close()
        if self.db_connection:
            self.db_connection.close()
        print("✓ Đã đóng kết nối database")
    
    def get_all_products(self):
        """Lấy tất cả sản phẩm từ database"""
        try:
            query = "SELECT product_id, name, url, source FROM product"
            self.cursor.execute(query)
            products = self.cursor.fetchall()
            print(f"✓ Tìm thấy {len(products)} sản phẩm trong database")
            return products
        except mysql.connector.Error as err:
            print(f"✗ Lỗi truy vấn database: {err}")
            return []
    
    def extract_product_id_from_url(self, url):
        """Trích xuất product_id từ URL Tiki"""
        # URL format: https://tiki.vn/...-p12345678.html
        match = re.search(r'-p(\d+)\.html', url)
        if match:
            return match.group(1)
        
        # Hoặc format khác: spid=12345678
        match = re.search(r'spid=(\d+)', url)
        if match:
            return match.group(1)
        
        return None
    
    def scrape_tiki_product(self, tiki_product_id):
        """
        Cào thông tin sản phẩm từ Tiki API
        Returns: dict với price, original_price, deal_type hoặc None nếu lỗi
        """
        try:
            # Thử API endpoint chính thức
            api_url = f"https://tiki.vn/api/v2/products/{tiki_product_id}"
            
            response = requests.get(api_url, headers=HEADERS, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                # Lấy thông tin giá
                price = data.get('price')
                original_price = data.get('original_price', price)
                
                # Xác định deal_type
                deal_type = 'NORMAL'
                badges = data.get('badges_new', [])
                for badge in badges:
                    badge_code = badge.get('code', '').upper()
                    if 'FLASH' in badge_code:
                        deal_type = 'FLASH_SALE'
                        break
                    elif 'HOT' in badge_code or 'DEAL' in badge_code:
                        deal_type = 'HOT_DEAL'
                        break
                    elif 'TREND' in badge_code:
                        deal_type = 'TRENDING'
                        break
                
                # Kiểm tra discount
                if original_price and price and price < original_price:
                    discount_percent = ((original_price - price) / original_price) * 100
                    if discount_percent >= 30 and deal_type == 'NORMAL':
                        deal_type = 'HOT_DEAL'
                
                return {
                    'price': price,
                    'original_price': original_price,
                    'currency': 'VND',
                    'deal_type': deal_type
                }
            
            elif response.status_code == 404:
                print(f"  ! Sản phẩm không tồn tại (404)")
                return None
            
            else:
                print(f"  ! API trả về status code: {response.status_code}")
                return None
                
        except requests.exceptions.Timeout:
            print(f"  ! Timeout khi gọi API")
            return None
        except requests.exceptions.RequestException as e:
            print(f"  ! Lỗi request: {e}")
            return None
        except Exception as e:
            print(f"  ! Lỗi không xác định: {e}")
            return None
    
    def save_price_history(self, product_id, price_data):
        """Lưu thông tin giá vào bảng price_history"""
        try:
            query = """
                INSERT INTO price_history 
                (product_id, price, original_price, currency, deal_type, recorded_at)
                VALUES (%s, %s, %s, %s, %s, %s)
            """
            values = (
                product_id,
                price_data['price'],
                price_data['original_price'],
                price_data['currency'],
                price_data['deal_type'],
                datetime.now()
            )
            
            self.cursor.execute(query, values)
            self.db_connection.commit()
            return True
            
        except mysql.connector.Error as err:
            print(f"  ✗ Lỗi lưu database: {err}")
            return False
    
    def log_scrape_session(self):
        """Ghi log vào bảng scrape_log"""
        try:
            query = """
                INSERT INTO scrape_log 
                (scrape_date, source, total_products, status, notes)
                VALUES (%s, %s, %s, %s, %s)
            """
            status = 'SUCCESS' if self.stats['failed'] == 0 else 'FAILED'
            notes = f"Success: {self.stats['success']}, Failed: {self.stats['failed']}, Skipped: {self.stats['skipped']}"
            
            values = (
                datetime.now(),
                'tiki',
                self.stats['total'],
                status,
                notes
            )
            
            self.cursor.execute(query, values)
            self.db_connection.commit()
            
        except mysql.connector.Error as err:
            print(f"✗ Lỗi ghi log: {err}")
    
    def run(self):
        """Chạy scraper chính"""
        print("\n" + "="*60)
        print("    TIKI PRICE SCRAPER - Tầng 1 Giai đoạn 1")
        print("="*60 + "\n")
        
        # Kết nối database
        if not self.connect_db():
            return
        
        # Lấy danh sách sản phẩm
        products = self.get_all_products()
        if not products:
            print("✗ Không có sản phẩm nào để cào!")
            self.close_db()
            return
        
        self.stats['total'] = len(products)
        
        print(f"\n{'─'*60}")
        print(f"Bắt đầu cào {len(products)} sản phẩm...")
        print(f"{'─'*60}\n")
        
        # Cào từng sản phẩm
        for idx, product in enumerate(products, 1):
            product_id = product['product_id']
            product_name = product['name']
            product_url = product['url']
            
            print(f"[{idx}/{len(products)}] {product_name[:50]}...")
            
            # Trích xuất Tiki product ID từ URL
            tiki_id = self.extract_product_id_from_url(product_url)
            
            if not tiki_id:
                print(f"  ! Không thể trích xuất Tiki ID từ URL")
                self.stats['skipped'] += 1
                continue
            
            # Cào dữ liệu từ Tiki
            price_data = self.scrape_tiki_product(tiki_id)
            
            if price_data:
                # Lưu vào database
                if self.save_price_history(product_id, price_data):
                    price_str = f"{price_data['price']:,.0f}đ"
                    if price_data['original_price'] != price_data['price']:
                        price_str += f" (gốc: {price_data['original_price']:,.0f}đ)"
                    print(f"  ✓ Giá: {price_str} | Deal: {price_data['deal_type']}")
                    self.stats['success'] += 1
                else:
                    self.stats['failed'] += 1
            else:
                print(f"  ✗ Không cào được dữ liệu")
                self.stats['failed'] += 1
            
            # Delay để tránh bị block
            if idx < len(products):
                time.sleep(DELAY_BETWEEN_REQUESTS)
        
        # Ghi log
        self.log_scrape_session()
        
        # Thống kê
        print(f"\n{'='*60}")
        print("KẾT QUẢ SCRAPING:")
        print(f"{'='*60}")
        print(f"  Tổng số sản phẩm:      {self.stats['total']}")
        print(f"  ✓ Thành công:          {self.stats['success']} ({self.stats['success']/self.stats['total']*100:.1f}%)")
        print(f"  ✗ Thất bại:            {self.stats['failed']}")
        print(f"  ! Bỏ qua:              {self.stats['skipped']}")
        print(f"{'='*60}\n")
        
        # Đóng kết nối
        self.close_db()


def main():
    """Main function"""
    scraper = TikiScraper()
    scraper.run()


if __name__ == "__main__":
    main()

