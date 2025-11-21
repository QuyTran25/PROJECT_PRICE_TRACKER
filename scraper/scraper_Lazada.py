#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Lazada Price Scraper - Tang 1 Giai doan 2
Cao gia san pham tu Lazada va luu vao database
Ho tro: Logging, Retry, Email notification
Chay: python scraper_lazada.py hoac run_scraper_lazada.bat
"""

import requests
import mysql.connector
from datetime import datetime
import time
import re
import sys
import logging
import configparser
from pathlib import Path
from bs4 import BeautifulSoup

# Fix encoding for Windows console
if sys.platform == 'win32':
    import codecs
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.buffer, 'strict')
    sys.stderr = codecs.getwriter('utf-8')(sys.stderr.buffer, 'strict')

# ===== DOC CAU HINH =====
def load_config():
    config = configparser.ConfigParser()
    config_file = Path(__file__).parent / 'config.ini'
    
    if not config_file.exists():
        print("ERROR: Khong tim thay config.ini")
        sys.exit(1)
    
    config.read(config_file, encoding='utf-8')
    return config

CONFIG = load_config()

# ===== DATABASE CONFIG =====
DB_CONFIG = {
    'host': CONFIG.get('DATABASE', 'host'),
    'port': CONFIG.getint('DATABASE', 'port'),
    'user': CONFIG.get('DATABASE', 'user'),
    'password': CONFIG.get('DATABASE', 'password'),
    'database': CONFIG.get('DATABASE', 'database'),
    'charset': CONFIG.get('DATABASE', 'charset')
}

# ===== SCRAPER CONFIG =====
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7',
    'Referer': 'https://www.lazada.vn/',
}

DELAY = CONFIG.getint('SCRAPER', 'delay_between_requests', fallback=3)
TIMEOUT = CONFIG.getint('SCRAPER', 'request_timeout', fallback=15)
MAX_RETRIES = CONFIG.getint('SCRAPER', 'max_retries', fallback=3)
RETRY_DELAY = CONFIG.getint('SCRAPER', 'retry_delay', fallback=60)

# ===== LOGGING =====
LOG_DIR = Path(__file__).parent / 'logs'
LOG_DIR.mkdir(exist_ok=True)
log_file = LOG_DIR / f"scraper_lazada_{datetime.now().strftime('%Y%m%d')}.log"

logging.basicConfig(
    level='INFO',
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(log_file, encoding='utf-8'),
        logging.StreamHandler(sys.stdout)
    ]
)

logger = logging.getLogger(__name__)


class LazadaScraper:
    def __init__(self):
        self.db_connection = None
        self.cursor = None
        self.stats = {'total': 0, 'success': 0, 'failed': 0, 'skipped': 0}
        logger.info("="*60)
        logger.info("LAZADA PRICE SCRAPER - Layer 1 Phase 2")
        logger.info("="*60)
    
    def remove_accents(self, text):
        """Remove Vietnamese accents from text"""
        vietnamese_map = {
            'à': 'a', 'á': 'a', 'ả': 'a', 'ã': 'a', 'ạ': 'a',
            'ă': 'a', 'ằ': 'a', 'ắ': 'a', 'ẳ': 'a', 'ẵ': 'a', 'ặ': 'a',
            'â': 'a', 'ầ': 'a', 'ấ': 'a', 'ẩ': 'a', 'ẫ': 'a', 'ậ': 'a',
            'è': 'e', 'é': 'e', 'ẻ': 'e', 'ẽ': 'e', 'ẹ': 'e',
            'ê': 'e', 'ề': 'e', 'ế': 'e', 'ể': 'e', 'ễ': 'e', 'ệ': 'e',
            'ì': 'i', 'í': 'i', 'ỉ': 'i', 'ĩ': 'i', 'ị': 'i',
            'ò': 'o', 'ó': 'o', 'ỏ': 'o', 'õ': 'o', 'ọ': 'o',
            'ô': 'o', 'ồ': 'o', 'ố': 'o', 'ổ': 'o', 'ỗ': 'o', 'ộ': 'o',
            'ơ': 'o', 'ờ': 'o', 'ớ': 'o', 'ở': 'o', 'ỡ': 'o', 'ợ': 'o',
            'ù': 'u', 'ú': 'u', 'ủ': 'u', 'ũ': 'u', 'ụ': 'u',
            'ư': 'u', 'ừ': 'u', 'ứ': 'u', 'ử': 'u', 'ữ': 'u', 'ự': 'u',
            'ỳ': 'y', 'ý': 'y', 'ỷ': 'y', 'ỹ': 'y', 'ỵ': 'y',
            'đ': 'd',
            'À': 'A', 'Á': 'A', 'Ả': 'A', 'Ã': 'A', 'Ạ': 'A',
            'Ă': 'A', 'Ằ': 'A', 'Ắ': 'A', 'Ẳ': 'A', 'Ẵ': 'A', 'Ặ': 'A',
            'Â': 'A', 'Ầ': 'A', 'Ấ': 'A', 'Ẩ': 'A', 'Ẫ': 'A', 'Ậ': 'A',
            'È': 'E', 'É': 'E', 'Ẻ': 'E', 'Ẽ': 'E', 'Ẹ': 'E',
            'Ê': 'E', 'Ề': 'E', 'Ế': 'E', 'Ể': 'E', 'Ễ': 'E', 'Ệ': 'E',
            'Ì': 'I', 'Í': 'I', 'Ỉ': 'I', 'Ĩ': 'I', 'Ị': 'I',
            'Ò': 'O', 'Ó': 'O', 'Ỏ': 'O', 'Õ': 'O', 'Ọ': 'O',
            'Ô': 'O', 'Ồ': 'O', 'Ố': 'O', 'Ổ': 'O', 'Ỗ': 'O', 'Ộ': 'O',
            'Ơ': 'O', 'Ờ': 'O', 'Ớ': 'O', 'Ở': 'O', 'Ỡ': 'O', 'Ợ': 'O',
            'Ù': 'U', 'Ú': 'U', 'Ủ': 'U', 'Ũ': 'U', 'Ụ': 'U',
            'Ư': 'U', 'Ừ': 'U', 'Ứ': 'U', 'Ử': 'U', 'Ữ': 'U', 'Ự': 'U',
            'Ỳ': 'Y', 'Ý': 'Y', 'Ỷ': 'Y', 'Ỹ': 'Y', 'Ỵ': 'Y',
            'Đ': 'D'
        }
        result = ''
        for char in text:
            result += vietnamese_map.get(char, char)
        return result
    
    def connect_db(self):
        for attempt in range(MAX_RETRIES):
            try:
                logger.info(f"Connecting to database (attempt {attempt + 1}/{MAX_RETRIES})...")
                self.db_connection = mysql.connector.connect(**DB_CONFIG)
                self.cursor = self.db_connection.cursor(dictionary=True)
                logger.info("OK - Connected successfully!")
                return True
            except mysql.connector.Error as err:
                logger.error(f"ERROR - Error: {err}")
                if attempt < MAX_RETRIES - 1:
                    time.sleep(RETRY_DELAY)
                else:
                    return False
        return False
    
    def close_db(self):
        if self.cursor:
            self.cursor.close()
        if self.db_connection:
            self.db_connection.close()
        logger.info("Close database")
    
    def get_lazada_products(self):
        try:
            query = "SELECT product_id, name, url FROM product WHERE source = 'Lazada'"
            self.cursor.execute(query)
            products = self.cursor.fetchall()
            logger.info(f"Found {len(products)} Lazada products")
            return products
        except mysql.connector.Error as err:
            logger.error(f"Query error: {err}")
            return []
    
    def extract_price_from_html(self, html):
        """
        Extract gia tu HTML bang regex
        Return: (price, original_price, deal_type)
        """
        # Method 1: Tim dong symbol
        price_patterns = [
            r'₫\s*([0-9,\.]+)',
            r'([0-9,\.]+)\s*₫',
        ]
        
        all_prices = []
        for pattern in price_patterns:
            matches = re.findall(pattern, html)
            for match in matches:
                try:
                    # Convert "123,456" -> 123456
                    num_str = match.replace(',', '').replace('.', '')
                    num = float(num_str)
                    # Filter reasonable prices (1k - 1B VND)
                    if 1000 < num < 1000000000:
                        all_prices.append(int(num))
                except:
                    pass
        
        if not all_prices:
            return None
        
        # Remove duplicates and sort
        unique_prices = sorted(set(all_prices))
        
        # Logic: Gia thap nhat la gia hien tai, cao nhat la gia goc
        current_price = unique_prices[0]
        original_price = unique_prices[-1] if len(unique_prices) > 1 else current_price
        
        # Determine deal_type
        deal_type = 'NORMAL'
        if original_price > current_price:
            discount = ((original_price - current_price) / original_price) * 100
            if discount >= 50:
                deal_type = 'FLASH_SALE'
            elif discount >= 30:
                deal_type = 'HOT_DEAL'
            elif discount >= 10:
                deal_type = 'TRENDING'
        
        return {
            'price': current_price,
            'original_price': original_price,
            'currency': 'VND',
            'deal_type': deal_type
        }
    
    def scrape_lazada_product(self, product_url, update_image=False, product_id=None):
        """
        Scrape data from Lazada
        update_image: If True, will update image_url to DB
        """
        for attempt in range(MAX_RETRIES):
            try:
                response = requests.get(product_url, headers=HEADERS, timeout=TIMEOUT)
                
                if response.status_code == 200:
                    price_data = self.extract_price_from_html(response.text)
                    
                    # Update image if needed
                    if update_image and product_id and price_data:
                        self.update_product_image(product_id, response.text)
                    
                    return price_data
                    
                elif response.status_code == 404:
                    logger.warning("  ! Product not found (404)")
                    return None
                else:
                    logger.warning(f"  ! Status: {response.status_code}")
                    if attempt < MAX_RETRIES - 1:
                        time.sleep(RETRY_DELAY)
                        continue
                    return None
                    
            except requests.exceptions.Timeout:
                logger.warning(f"  ! Timeout ({attempt + 1}/{MAX_RETRIES})")
                if attempt < MAX_RETRIES - 1:
                    time.sleep(RETRY_DELAY)
                else:
                    return None
                    
            except Exception as e:
                logger.error(f"  ! Error: {e}")
                return None
        
        return None
    
    def update_product_image(self, product_id, html):
        """Update image_url for product from HTML"""
        try:
            soup = BeautifulSoup(html, 'html.parser')
            image_url = None
            
            # Method 1: og:image meta tag
            og_image = soup.find('meta', {'property': 'og:image'})
            if og_image and og_image.get('content'):
                image_url = og_image.get('content')
            
            # Method 2: Find img tag with pdp class
            if not image_url:
                img_tag = soup.find('img', class_=re.compile(r'pdp-mod-common-image'))
                if img_tag and img_tag.get('src'):
                    image_url = img_tag['src']
            
            # Method 3: Find any img with lazcdn.com
            if not image_url:
                all_imgs = soup.find_all('img')
                for img in all_imgs:
                    src = img.get('src', '')
                    if 'lazcdn.com' in src and not src.endswith('.gif'):
                        image_url = src
                        break
            
            if image_url:
                # Ensure full URL
                if image_url.startswith('//'):
                    image_url = 'https:' + image_url
                elif image_url.startswith('/'):
                    image_url = 'https://www.lazada.vn' + image_url
                
                query = "UPDATE product SET image_url = %s WHERE product_id = %s"
                self.cursor.execute(query, (image_url, product_id))
                self.db_connection.commit()
                logger.info(f"  OK - Updated image_url")
                return True
        except Exception as e:
            logger.warning(f"  ! Cannot update image: {e}")
        return False
    
    def save_price_history(self, product_id, price_data):
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
            logger.error(f"  Error saving to DB: {err}")
            return False
    
    def log_scrape_session(self):
        """Log to scrape_log table"""
        try:
            query = """
                INSERT INTO scrape_log 
                (scrape_date, source, total_products, status, notes)
                VALUES (%s, %s, %s, %s, %s)
            """
            status = 'SUCCESS' if self.stats['failed'] == 0 else 'PARTIAL_SUCCESS' if self.stats['success'] > 0 else 'FAILED'
            notes = f"Success: {self.stats['success']}, Failed: {self.stats['failed']}, Skipped: {self.stats['skipped']}"
            
            values = (
                datetime.now(),
                'lazada',
                self.stats['total'],
                status,
                notes
            )
            
            self.cursor.execute(query, values)
            self.db_connection.commit()
            logger.info("Logged to database")
            
        except mysql.connector.Error as err:
            logger.error(f"Log error: {err}")
    
    def run(self):
        start_time = datetime.now()
        
        if not self.connect_db():
            logger.error("Cannot connect to database")
            return False
        
        products = self.get_lazada_products()
        if not products:
            logger.warning("No Lazada products!")
            logger.info("Add Lazada products via web interface")
            self.close_db()
            return True
        
        self.stats['total'] = len(products)
        
        logger.info("-"*60)
        logger.info(f"Start scraping {len(products)} Lazada products...")
        logger.info("-"*60)
        
        for idx, product in enumerate(products, 1):
            product_id = product['product_id']
            product_name = product['name']
            product_url = product['url']
            
            # Convert Vietnamese to non-accented (keep all letters)
            safe_name = self.remove_accents(product_name[:50])
            if not safe_name.strip():
                safe_name = f"Product ID {product_id}"
            logger.info(f"[{idx}/{len(products)}] {safe_name}...")
            
            # Scrape price and update image
            price_data = self.scrape_lazada_product(product_url, update_image=True, product_id=product_id)
            
            if price_data:
                if self.save_price_history(product_id, price_data):
                    price_str = f"{price_data['price']:,}d"
                    if price_data['original_price'] != price_data['price']:
                        price_str += f" (original: {price_data['original_price']:,}d)"
                    logger.info(f"  Price: {price_str} | Deal: {price_data['deal_type']}")
                    self.stats['success'] += 1
                else:
                    self.stats['failed'] += 1
            else:
                logger.warning(f"  Cannot scrape price")
                self.stats['failed'] += 1
            
            if idx < len(products):
                time.sleep(DELAY)
        
        # Write log
        self.log_scrape_session()
        
        # Statistics
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        logger.info("="*60)
        logger.info("LAZADA SCRAPING RESULTS:")
        logger.info("="*60)
        logger.info(f"  Total products:        {self.stats['total']}")
        logger.info(f"  Success:               {self.stats['success']} ({self.stats['success']/max(self.stats['total'],1)*100:.1f}%)")
        logger.info(f"  Failed:                {self.stats['failed']}")
        logger.info(f"  Skipped:               {self.stats['skipped']}")
        logger.info(f"  Duration:              {duration:.1f}s ({duration/60:.1f} minutes)")
        logger.info("="*60)
        
        self.close_db()
        
        return self.stats['success'] > 0 or self.stats['total'] == 0


def main():
    scraper = LazadaScraper()
    success = scraper.run()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()