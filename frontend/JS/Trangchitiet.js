/**
 * Trangchitiet.js - Product Detail Page
 * Fetch real data from backend and populate the page dynamically
 */

const API_BASE_URL = 'http://localhost:8080';

/**
 * Get product ID from URL parameter
 * URL format: Trangchitiet.html?id=123
 */
function getProductIdFromURL() {
    const urlParams = new URLSearchParams(window.location.search);
    const id = urlParams.get('id');

    if (!id) {
        console.error('No product_id in URL');
        showError('Không tìm thấy ID sản phẩm trong URL');
        return null;
    }

    console.log('Product ID from URL:', id);
    return parseInt(id);
}

/**
 * Show error message on page
 */
function showError(message) {
    const container = document.querySelector('.container') || document.body;
    container.innerHTML = `
        <div style="text-align:center; padding:100px 20px; max-width:600px; margin:0 auto;">
            <div style="font-size:5rem; margin-bottom:30px;">😞</div>
            <h1 style="font-size:2.5rem; margin-bottom:20px; color:#CC0843; font-weight:700;">Sản phẩm không tồn tại</h1>
            <p style="font-size:1.2rem; margin-bottom:40px; color:#6C757D; line-height:1.6;">${message}</p>
            <div style="display:flex; gap:15px; justify-content:center; flex-wrap:wrap;">
                <button onclick="window.history.back()" 
                        style="padding:14px 40px; background:#EC4899; color:white; 
                               border:none; border-radius:8px; cursor:pointer; font-size:1.1rem; 
                               font-weight:600; box-shadow:0 4px 12px rgba(236,72,153,0.3);
                               transition:all 0.3s;" 
                        onmouseover="this.style.background='#D14488'"
                        onmouseout="this.style.background='#EC4899'">
                    ← Quay lại
                </button>
                <button onclick="window.location.href='Trangchu.html'" 
                        style="padding:14px 40px; background:#6C757D; color:white; 
                               border:none; border-radius:8px; cursor:pointer; font-size:1.1rem; 
                               font-weight:600; box-shadow:0 4px 12px rgba(108,117,125,0.3);
                               transition:all 0.3s;"
                        onmouseover="this.style.background='#5A6268'"
                        onmouseout="this.style.background='#6C757D'">
                    🏠 Trang chủ
                </button>
            </div>
        </div>
    `;
}

/**
 * Fetch product detail from backend
 */
async function fetchProductDetail(productId) {
    try {
        console.log(`🔍 Fetching product detail for ID: ${productId}`);

        const response = await fetch(`${API_BASE_URL}/product-detail`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ product_id: productId })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        console.log('Product detail received:', data);

        if (!data.success) {
            throw new Error(data.error || 'Không thể tải thông tin sản phẩm');
        }

        return data;

    } catch (error) {
        console.error('Error fetching product detail:', error);
        throw error;
    }
}

/**
 * Populate product header section
 */
function populateProductHeader(product, price, reviews) {
    // Product image
    const productImage = document.querySelector('.product-image');
    if (productImage) {
        productImage.src = product.image_url || 'https://via.placeholder.com/400x400?text=No+Image';
        productImage.alt = product.name;
        productImage.onerror = function () {
            this.src = 'https://via.placeholder.com/400x400?text=No+Image';
        };
    }

    // Badges
    const badgeContainer = document.querySelector('.product-image-section');
    if (badgeContainer && price.discount_percent > 0) {
        // Remove existing badges
        const existingBadges = badgeContainer.querySelectorAll('.badge');
        existingBadges.forEach(b => b.remove());

        // Discount badge
        const discountBadge = document.createElement('div');
        discountBadge.className = 'badge discount';
        discountBadge.innerHTML = `<span>${price.discount_percent}%</span>`;
        badgeContainer.appendChild(discountBadge);

        // Deal type badge
        if (price.deal_type && price.deal_type !== 'Normal') {
            const dealBadge = document.createElement('div');
            dealBadge.className = 'badge hot-deal';
            let dealText = '🔥 Hot Deal';
            if (price.deal_type === 'FLASH_SALE') dealText = '⚡ Flash Sale';
            else if (price.deal_type === 'TRENDING') dealText = '📈 Trending';
            dealBadge.textContent = dealText;
            badgeContainer.appendChild(dealBadge);
        }
    }

    // Product name
    const productName = document.querySelector('.product-name');
    if (productName) {
        productName.textContent = product.name;
    }

    // Brand
    const brandElement = document.querySelector('.brand');
    if (brandElement && product.brand) {
        brandElement.textContent = `Thương hiệu: ${product.brand}`;
    }

    // Prices
    const currentPriceEl = document.querySelector('.current-price');
    const originalPriceEl = document.querySelector('.original-price');
    const savingsEl = document.querySelector('.savings');
    const discountInfo = document.querySelector('.discount-info');

    if (currentPriceEl) {
        currentPriceEl.textContent = formatCurrency(price.current_price, price.currency);
    }

    if (originalPriceEl && price.original_price > price.current_price) {
        originalPriceEl.textContent = formatCurrency(price.original_price, price.currency);
        originalPriceEl.style.display = 'inline';
    } else if (originalPriceEl) {
        originalPriceEl.style.display = 'none';
    }

    // Show savings amount if there's a discount
    if (savingsEl && discountInfo && price.original_price > price.current_price) {
        const savings = price.original_price - price.current_price;
        savingsEl.textContent = formatCurrency(savings, price.currency);
        discountInfo.style.display = 'flex';
    } else if (discountInfo) {
        discountInfo.style.display = 'none';
    }

    // Buy button - link to Tiki
    const buyButton = document.querySelector('.btn-primary');
    if (buyButton && product.url) {
        buyButton.onclick = function () {
            window.open(product.url, '_blank');
        };
    }
}

/**
 * Generate smart price analysis based on price history
 */
function generatePriceAnalysis(priceHistory, currentPrice, currency) {
    if (!priceHistory || priceHistory.length === 0) {
        return {
            text: 'Chưa có đủ dữ liệu lịch sử giá để phân tích.',
            recommendation: 'Hãy theo dõi thêm để nhận được phân tích chính xác hơn.'
        };
    }

    // Sort by date (oldest first)
    const sorted = [...priceHistory].sort((a, b) =>
        new Date(a.captured_at) - new Date(b.captured_at)
    );

    const prices = sorted.map(p => p.price);
    const latestPrice = currentPrice;
    const oldestPrice = prices[0];
    const lowestPrice = Math.min(...prices);
    const highestPrice = Math.max(...prices);
    const avgPrice = prices.reduce((a, b) => a + b, 0) / prices.length;

    // Calculate time range
    const oldestDate = new Date(sorted[0].captured_at);
    const latestDate = new Date(sorted[sorted.length - 1].captured_at);
    const daysDiff = Math.ceil((latestDate - oldestDate) / (1000 * 60 * 60 * 24));

    // Price trend
    const priceChange = latestPrice - oldestPrice;
    const priceChangePercent = ((priceChange / oldestPrice) * 100).toFixed(1);

    // Is current price near lowest?
    const lowestPriceDiff = ((latestPrice - lowestPrice) / lowestPrice * 100).toFixed(1);

    // Build analysis text
    let analysisText = '';
    let recommendation = '';

    if (priceChange < 0) {
        analysisText = `Giá sản phẩm đã giảm ${Math.abs(priceChangePercent)}% trong ${daysDiff} ngày qua. `;
    } else if (priceChange > 0) {
        analysisText = `Giá sản phẩm đã tăng ${priceChangePercent}% trong ${daysDiff} ngày qua. `;
    } else {
        analysisText = `Giá sản phẩm ổn định trong ${daysDiff} ngày qua. `;
    }

    // Check if current price is lowest
    if (latestPrice === lowestPrice) {
        analysisText += `Đây là mức giá thấp nhất được ghi nhận.`;
        recommendation = 'Giá hiện tại đang ở mức thấp nhất trong lịch sử theo dõi.';
    } else if (lowestPriceDiff < 5) {
        analysisText += `Giá hiện tại chỉ cao hơn mức thấp nhất ${lowestPriceDiff}%.`;
        recommendation = 'Giá đang ở mức khá tốt so với lịch sử biến động.';
    } else if (lowestPriceDiff < 15) {
        analysisText += `Giá hiện tại cao hơn mức thấp nhất ${lowestPriceDiff}%.`;
        recommendation = 'Giá đang ở mức trung bình, có thể tiếp tục theo dõi thêm.';
    } else {
        analysisText += `Giá hiện tại cao hơn mức thấp nhất ${lowestPriceDiff}%.`;
        recommendation = 'Giá chưa ở mức tối ưu, nên theo dõi thêm để có quyết định tốt hơn.';
    }

    return {
        text: analysisText,
        recommendation: recommendation,
        stats: {
            lowest: formatCurrency(lowestPrice, currency),
            highest: formatCurrency(highestPrice, currency),
            average: formatCurrency(avgPrice, currency),
            current: formatCurrency(latestPrice, currency)
        }
    };
}

/**
 * Populate price history section with Chart.js
 */
function populatePriceHistory(priceHistory, currency = 'VND') {
    if (!priceHistory || priceHistory.length === 0) {
        console.log('No price history available');
        return;
    }

    // Sort by date (oldest first)
    const sorted = [...priceHistory].sort((a, b) =>
        new Date(a.captured_at) - new Date(b.captured_at)
    );

    // Prepare data for chart
    const labels = sorted.map(p => {
        const date = new Date(p.captured_at);
        return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: '2-digit' });
    });

    const data = sorted.map(p => p.price);

    // Find lowest price
    const lowestPrice = Math.min(...data);
    const minIndex = data.lastIndexOf(lowestPrice);

    // Get current price
    const currentPrice = sorted[sorted.length - 1].price;

    // Generate smart analysis
    const analysis = generatePriceAnalysis(priceHistory, currentPrice, currency);

    // Calculate stats
    const highestPrice = Math.max(...data);
    const avgPrice = data.reduce((a, b) => a + b, 0) / data.length;

    // Calculate discount percentage
    const discountPercent = highestPrice > currentPrice
        ? Math.round(((highestPrice - currentPrice) / highestPrice) * 100)
        : 0;

    // Update 4 stat cards
    updatePriceStats(highestPrice, lowestPrice, avgPrice, discountPercent, currency);

    // Update last update time
    const latestDate = new Date(sorted[sorted.length - 1].captured_at);
    updateLastUpdateTime(latestDate);

    // Update analysis text with new structure
    const priceNote = document.querySelector('.price-note');
    if (priceNote) {
        priceNote.innerHTML = `
            <span class="analysis-label">📊 Phân tích biến động giá</span>
            <p>${analysis.text}</p>
            <div class="recommendation">
                💡 <strong>Nhận xét:</strong> ${analysis.recommendation}
            </div>
        `;
    }

    // Create chart
    const canvas = document.getElementById('priceChart');
    if (!canvas) return;

    // Destroy existing chart if any
    if (canvas.chartInstance) {
        canvas.chartInstance.destroy();
    }

    const ctx = canvas.getContext('2d');
    const chart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: `Giá (${currency})`,
                data: data,
                borderColor: '#ff4d6d',
                backgroundColor: 'rgba(255, 77, 109, 0.15)',
                borderWidth: 2,
                fill: true,
                tension: 0,
                pointRadius: 4,
                pointHoverRadius: 7,
                pointBackgroundColor: (ctx) => {
                    const index = ctx.dataIndex;
                    return index === minIndex ? '#16A34A' : '#ff4d6d';
                },
                pointBorderColor: '#fff',
                pointBorderWidth: 1
            }]

        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'nearest',
                intersect: false
            },
            plugins: {
                legend: {
                    display: false
                },
                title: {
                    display: true,
                    text: 'Biểu đồ biến động giá',
                    color: '#333',
                    font: { size: 16 }
                },
                tooltip: {
                    backgroundColor: '#fff',
                    titleColor: '#000',
                    bodyColor: '#ff4d6d',
                    borderColor: '#ff4d6d',
                    borderWidth: 1,
                    padding: 10,
                    displayColors: false,
                    callbacks: {
                        title: (items) => `Thời gian: ${items[0].label}`,
                        label: (item) => 'Giá: ' + formatCurrency(item.parsed.y, currency)
                    }
                },
            },
            scales: {
                x: {
                    ticks: {
                        maxTicksLimit: 8,
                        color: '#666',
                        font: { size: 11 }
                    }
                },
                y: {
                    ticks: {
                        color: '#333',
                        callback: (value) => formatCurrency(value, currency)
                    }
                }
            }
        }
    });

    canvas.chartInstance = chart;
}

/**
 * Update 4 price stat cards with real data
 */
function updatePriceStats(highestPrice, lowestPrice, avgPrice, discountPercent, currency) {
    const statCards = document.querySelectorAll('.stat-card');

    if (statCards.length >= 4) {
        // Giá cao nhất
        const highestValue = statCards[0].querySelector('.stat-value');
        if (highestValue) {
            highestValue.textContent = formatCurrency(highestPrice, currency);
        }

        // Giá thấp nhất
        const lowestValue = statCards[1].querySelector('.stat-value');
        if (lowestValue) {
            lowestValue.textContent = formatCurrency(lowestPrice, currency);
        }

        // Giá trung bình
        const avgValue = statCards[2].querySelector('.stat-value');
        if (avgValue) {
            avgValue.textContent = formatCurrency(avgPrice, currency);
        }

        // Mức giảm
        const discountValue = statCards[3].querySelector('.stat-value');
        if (discountValue) {
            discountValue.textContent = discountPercent + '%';
        }
    }
}

/**
 * Update last update time with latest price record date
 */
function updateLastUpdateTime(latestDate) {
    const updateTimeElement = document.querySelector('.update-time');
    if (!updateTimeElement) return;

    const now = new Date();
    const diffMs = now - latestDate;
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    let timeText = '';

    if (diffMins < 1) {
        timeText = 'Vừa xong';
    } else if (diffMins < 60) {
        timeText = `${diffMins} phút trước`;
    } else if (diffHours < 24) {
        const hours = latestDate.getHours().toString().padStart(2, '0');
        const mins = latestDate.getMinutes().toString().padStart(2, '0');
        timeText = `Hôm nay, ${hours}:${mins}`;
    } else if (diffDays === 1) {
        const hours = latestDate.getHours().toString().padStart(2, '0');
        const mins = latestDate.getMinutes().toString().padStart(2, '0');
        timeText = `Hôm qua, ${hours}:${mins}`;
    } else {
        timeText = latestDate.toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    updateTimeElement.textContent = `Cập nhật: ${timeText}`;
}
/**
 * Tạo HTML cho 1 card sản phẩm tương tự
 */
function createProductHTML(product) {
    const discount = product.discount_percent || 0;
    const price = formatCurrency(product.price);
    const originalPrice = product.original_price > product.price ? formatCurrency(product.original_price) : '';
    const savings = product.original_price > product.price ? formatCurrency(product.original_price - product.price) : '';
    
    // Xác định loại badge - CHỈ hiển thị nếu có deal thật (không hiển thị cho NORMAL)
    let badgeHTML = '';
    
    if (product.deal_type === 'FLASH_SALE') {
        badgeHTML = `
            <div class="nhan_deal Sale">
                <i class="fa-solid fa-bolt-lightning"></i>
                <span>Flash Sale</span>
            </div>`;
    } else if (product.deal_type === 'HOT_DEAL') {
        badgeHTML = `
            <div class="nhan_deal deal">
                <i class="fa-solid fa-fire"></i>
                <span>Hot Deal</span>
            </div>`;
    } else if (product.deal_type === 'TRENDING') {
        badgeHTML = `
            <div class="nhan_deal Trending">
                <i class="fa-solid fa-arrow-trend-up"></i>
                <span>Trending</span>
            </div>`;
    }
    // NORMAL: không hiển thị badge
    
    return `
        <div class="mathang" data-url="${product.url || ''}" data-product-id="${product.product_id}">
            <div class="hinh">
                <img src="${product.image_url || 'https://via.placeholder.com/300x300'}" 
                     alt="${product.name}"
                     style="width: 100%; height: 300px; object-fit: cover; display: block;"
                     onerror="this.src='https://via.placeholder.com/300x300?text=No+Image'">
                <div class="tren_hinh">
                    ${badgeHTML}
                    ${discount > 0 ? `<div class="phan_tram">-${discount}%</div>` : ''}
                </div>
            </div>
            <div class="thong_tin">
                <div class="nhom_sp">${product.group_name || 'Sản phẩm'}</div>
                <div class="ten_sp">${truncate(product.name, 50)}</div>
                <div class="gia_sp"><span>${price}</span> đ</div>
                ${originalPrice && savings ? `
                <div class="tiet_kiem">
                    <span class="gia_goc">${originalPrice} đ</span>
                    <p class="khau_tru">Tiết kiệm <span>${savings}</span> đ</p>
                </div>
                ` : ''}
                <button class="chi_tiet">Xem chi tiết</button>
            </div>
        </div>
    `;
}

/**
 * Hiển thị sản phẩm tương tự (similar products)
 */
function populateSimilarProducts(similarProducts) {
    const container = document.querySelector('.sanpham');
    if (!container) return;

    container.innerHTML = '';

    if (!similarProducts || similarProducts.length === 0) {
        container.innerHTML = `
            <div style="text-align:center; padding:60px 20px; color:#666; grid-column: 1/-1;">
                <div style="font-size:3rem; margin-bottom:15px;">🔍</div>
                <h3 style="color:#CC0843; font-size:1.3rem; margin-bottom:10px;">Không có sản phẩm tương tự</h3>
                <p style="font-size:1rem; color:#888;">Hiện tại chưa có sản phẩm nào trong cùng danh mục.</p>
            </div>
        `;
        return;
    }

    // Hiển thị tối đa 8 sản phẩm (2 hàng x 4 cột)
    const productsToShow = similarProducts.slice(0, 8);
    
    // Tạo các hàng (4 sản phẩm/hàng giống giamGia.js)
    for (let i = 0; i < productsToShow.length; i += 4) {
        const row = document.createElement('div');
        row.className = 'hang';
        
        for (let j = 0; j < 4; j++) {
            const productIndex = i + j;
            if (productIndex < productsToShow.length) {
                row.innerHTML += createProductHTML(productsToShow[productIndex]);
            } else {
                // Thêm div rỗng để giữ layout
                row.innerHTML += '<div class="mathang" style="visibility:hidden;"></div>';
            }
        }
        
        container.appendChild(row);
    }
    
    // Gắn sự kiện click
    attachClickEvents(container);
}

/**
 * Gắn sự kiện click cho các card sản phẩm
 */
function attachClickEvents(container) {
    container.querySelectorAll('.chi_tiet').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            const mathang = this.closest('.mathang');
            const productId = mathang.getAttribute('data-product-id');
            
            if (productId) {
                window.location.href = `Trangchitiet.html?id=${productId}`;
            }
        });
    });
}

/**
 * Format currency
 */
function formatCurrency(amount, currency = 'VND') {
    if (!amount && amount !== 0) return '';

    const formatted = Math.round(amount).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');

    if (currency === 'VND') {
        return formatted + 'đ';
    }
    return formatted + ' ' + currency;
}

/**
 * Cắt ngắn text
 */
function truncate(text, maxLen) {
    if (!text) return '';
    return text.length > maxLen ? text.substring(0, maxLen) + '...' : text;
}

/**
 * Initialize product detail page
 */
async function initializeProductDetail() {
    try {
        // Get product ID from URL
        const productId = getProductIdFromURL();
        if (!productId) return;

        // Show loading state
        console.log('🔄 Loading product detail...');

        // Fetch data from backend
        const data = await fetchProductDetail(productId);

        // Populate all sections
        populateProductHeader(data.product, data.price, data.reviews);
        populatePriceHistory(data.price_history, data.price.currency);
        populateSimilarProducts(data.similar_products);

        console.log('✅ Product detail page loaded successfully');

    } catch (error) {
        console.error('❌ Failed to load product detail:', error);
        showError(error.message || 'Không thể tải thông tin sản phẩm. Vui lòng thử lại sau.');
    }
}

// Initialize product detail page on DOM ready
document.addEventListener("DOMContentLoaded", () => {
    initializeProductDetail();
});

console.log('✅ Trangchitiet.js loaded');
