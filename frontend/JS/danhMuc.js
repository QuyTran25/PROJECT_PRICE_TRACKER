/**
 * danhMuc.js - Category page logic
 * Fetches categories from server and displays them with product counts
 */

const SERVER_HOST = 'localhost';
const SERVER_PORT = 8080;

/**
 * Fetch all categories from server
 */
async function loadCategories() {
    try {
        console.log('🔍 Loading categories from server...');
        
        const response = await fetch(`http://${SERVER_HOST}:${SERVER_PORT}/categories`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('✅ Categories loaded:', data);
        
        if (data.success) {
            displayCategories(data.categories);
        } else {
            showError(data.error || 'Không thể tải danh mục');
        }
        
    } catch (error) {
        console.error('❌ Error loading categories:', error);
        showError('Lỗi kết nối server. Vui lòng đảm bảo server đang chạy!');
    }
}

/**
 * Display categories on the page
 */
function displayCategories(categories) {
    const container = document.getElementById('danhmucsp');
    
    if (!container) {
        console.error('Cannot find #danhmucsp container');
        return;
    }
    
    // Category icon mapping
    const categoryIcons = {
        1: 'fa-mobile',           // Điện tử
        2: 'fa-plug',             // Điện gia dụng
        3: 'fa-shirt',            // Thời trang
        4: 'fa-spray-can',        // Làm đẹp
        5: 'fa-book',             // Sách
        6: 'fa-baby',             // Đồ chơi
        7: 'fa-dumbbell',         // Thể thao
        8: 'fa-star'              // Sản phẩm mới
    };
    
    const categoryColors = {
        1: { color: '#3B82F6', bg: '#DBEAFE' },
        2: { color: '#A855F7', bg: '#F3E8FF' },
        3: { color: '#22C55E', bg: '#DCFCE7' },
        4: { color: '#F97316', bg: '#FFEDD5' },
        5: { color: '#EF4444', bg: '#FEE2E2' },
        6: { color: '#EC4899', bg: '#FCE7F3' },
        7: { color: '#14B8A6', bg: '#CCFBF1' },
        8: { color: '#8B5CF6', bg: '#EDE9FE' }
    };
    
    // Sort categories by group_id
    categories.sort((a, b) => a.group_id - b.group_id);
    
    // Group into rows of 4
    let html = '';
    for (let i = 0; i < categories.length; i += 4) {
        const rowCategories = categories.slice(i, i + 4);
        
        html += '<div class="hang">';
        
        rowCategories.forEach(category => {
            const icon = categoryIcons[category.group_id] || 'fa-box';
            const colors = categoryColors[category.group_id] || { color: '#6B7280', bg: '#F3F4F6' };
            
            html += `
                <a href="timKiem.html?group_id=${category.group_id}&category=${encodeURIComponent(category.group_name)}" 
                   class="loai_sp" 
                   data-group-id="${category.group_id}">
                    <i class="fa-solid ${icon}" style="color: ${colors.color}; background-color: ${colors.bg};"></i>
                    <p class="ten_loai">${category.group_name}</p>
                    <p class="so_luong"><span>${category.product_count}</span> sản phẩm</p>
                </a>
            `;
        });
        
        html += '</div>';
    }
    
    container.innerHTML = html;
    
    // Add click animation
    const categoryCards = document.querySelectorAll('.loai_sp');
    categoryCards.forEach(card => {
        card.addEventListener('click', function(e) {
            // Add a subtle click animation
            this.style.transform = 'scale(0.95)';
            setTimeout(() => {
                this.style.transform = '';
            }, 150);
        });
    });
}

/**
 * Show error message
 */
function showError(message) {
    const container = document.getElementById('danhmucsp');
    
    if (container) {
        container.innerHTML = `
            <div style="text-align: center; padding: 50px; width: 100%;">
                <i class="fa-solid fa-exclamation-circle" style="font-size: 4rem; color: #EF4444; margin-bottom: 20px;"></i>
                <h2 style="color: #CC0843; margin-bottom: 10px;">Lỗi tải danh mục</h2>
                <p style="color: #6B7280; font-size: 1.1rem;">${message}</p>
                <button onclick="loadCategories()" style="
                    margin-top: 20px;
                    padding: 12px 30px;
                    background: #EC4899;
                    color: white;
                    border: none;
                    border-radius: 8px;
                    font-size: 1rem;
                    font-weight: 600;
                    cursor: pointer;
                    box-shadow: 0 4px 6px rgba(236, 72, 153, 0.3);
                ">
                    <i class="fa-solid fa-rotate-right"></i> Thử lại
                </button>
            </div>
        `;
    }
}

/**
 * Show loading state
 */
function showLoading() {
    const container = document.getElementById('danhmucsp');
    
    if (container) {
        container.innerHTML = `
            <div style="text-align: center; padding: 50px; width: 100%;">
                <div class="loading-spinner" style="
                    border: 4px solid #f3f3f3;
                    border-top: 4px solid #EC4899;
                    border-radius: 50%;
                    width: 60px;
                    height: 60px;
                    animation: spin 1s linear infinite;
                    margin: 0 auto 20px;
                "></div>
                <p style="color: #6B7280; font-size: 1.1rem;">Đang tải danh mục...</p>
                <style>
                    @keyframes spin {
                        0% { transform: rotate(0deg); }
                        100% { transform: rotate(360deg); }
                    }
                </style>
            </div>
        `;
    }
}

/**
 * Initialize on page load
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('📦 Category page initialized');
    
    // Show loading state
    showLoading();
    
    // Load categories from server
    loadCategories();
});
