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
        1: 'fa-mobile',             // Điện thoại
        2: 'fa-laptop',             // Laptop và thiết bị văn phòng
        3: 'fa-mars',              // Thời trang nam
        4: 'fa-venus',              // Thời trang nữ 
        5: 'fa-plug',               // Đồ điện tử gia dụng
        6: 'fa-spray-can-sparkles', // Sản phẩm làm đẹp
        7: 'fa-box',                // Thực phẩm đóng gói
        8: 'fa-blender'             // Thiết bị gia dụng
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
    
    // Calculate total products
    const totalProducts = categories.reduce((sum, cat) => sum + cat.product_count, 0);
    
    // Build HTML with title
    let html = `
        <p style="color: #EC4899; font-family: 'roboto', sans-serif; font-weight: bold; font-size: 32px; margin: 50px 0 20px 0; text-align: center;">
            Danh mục sản phẩm
        </p>
        <p style="color: #4B5563; font-family: 'inter', sans-serif; font-weight: lighter; font-size: 16px; margin: 0 0 20px 0; text-align: center;">
            Khám phá hơn ${totalProducts.toLocaleString('vi-VN')} sản phẩm được theo dõi giá trong ${categories.length} danh mục khác nhau
        </p>
        <p id="frame_danhmuc">
    `;
    
    // Group into rows of 4
    for (let i = 0; i < categories.length; i += 4) {
        const rowCategories = categories.slice(i, i + 4);
        
        html += '<div class="hang">';
        
        rowCategories.forEach(category => {
            const icon = categoryIcons[category.group_id] || 'fa-box';
            const colors = categoryColors[category.group_id] || { color: '#6B7280', bg: '#F3F4F6' };
            
            html += `
                <div class="loai_sp" 
                     data-group-id="${category.group_id}"
                     data-group-name="${category.group_name.replace(/"/g, '&quot;')}"
                     data-product-count="${category.product_count}"
                     onclick='loadCategoryProducts(${category.group_id}, "${category.group_name.replace(/"/g, '&quot;')}", ${category.product_count})'>
                    <i class="fa-solid ${icon}" style="color: ${colors.color}; background-color: ${colors.bg};"></i>
                    <p class="ten_loai">${category.group_name}</p>
                    <p class="so_luong"><span>${category.product_count}</span> sản phẩm</p>
                </div>
            `;
        });
        
        html += '</div>';
    }
    
    html += '</p>'; // Close frame_danhmuc
    
    container.innerHTML = html;
    
    // Add click animation and cursor pointer
    const categoryCards = document.querySelectorAll('.loai_sp');
    categoryCards.forEach(card => {
        card.style.cursor = 'pointer';
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
 * Load products for a specific category
 */
async function loadCategoryProducts(groupId, groupName, productCount) {
    try {
        console.log(`🔍 Loading products for category: ${groupName} (ID: ${groupId})`);
        
        // Show loading in product section
        const productSection = document.getElementById('product-display-section');
        const categoryTitle = document.getElementById('category-title');
        const productCountText = document.getElementById('product-count-text');
        const productsGrid = document.getElementById('products-grid');
        
        productSection.style.display = 'block';
        categoryTitle.textContent = groupName;
        productCountText.textContent = `${productCount} sản phẩm`;
        productsGrid.innerHTML = '<p style="text-align: center; padding: 40px; color: #6B7280;">Đang tải sản phẩm...</p>';
        
        // Scroll to product display section
        setTimeout(() => {
            productSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 100);
        
        const response = await fetch(`http://${SERVER_HOST}:${SERVER_PORT}/search`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                action: 'SEARCH_BY_CATEGORY',
                group_id: groupId
            })
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('✅ Products loaded:', data);
        
        if (data.success && data.products && data.products.length > 0) {
            currentProducts = data.products; // Store for sorting
            displayProducts(data.products);
            // Reset sort select to default
            document.getElementById('sort-select').value = 'default';
        } else {
            productsGrid.innerHTML = `
                <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px;">
                    <i class="fa-solid fa-box-open" style="font-size: 4rem; color: #D1D5DB; margin-bottom: 20px;"></i>
                    <h3 style="color: #4B5563; margin-bottom: 10px;">Không tìm thấy sản phẩm</h3>
                    <p style="color: #9CA3AF;">Danh mục này hiện chưa có sản phẩm nào.</p>
                </div>
            `;
        }
        
    } catch (error) {
        console.error('❌ Error loading products:', error);
        const productsGrid = document.getElementById('products-grid');
        productsGrid.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: #EF4444;">
                <i class="fa-solid fa-exclamation-circle" style="font-size: 3rem; margin-bottom: 15px;"></i>
                <p>Lỗi khi tải sản phẩm. Vui lòng thử lại!</p>
            </div>
        `;
    }
}

/**
 * Display products in grid
 */
function displayProducts(products) {
    const productsGrid = document.getElementById('products-grid');
    
    let html = '';
    products.forEach(product => {
        const price = parseFloat(product.price || 0);
        const originalPrice = parseFloat(product.original_price || 0);
        const discountPercent = product.discount_percent || 0;
        const formattedPrice = price.toLocaleString('vi-VN') + 'đ';
        const formattedOriginalPrice = originalPrice.toLocaleString('vi-VN') + 'đ';
        
        html += `
            <div class="product-card" style="
                background: white;
                border-radius: 12px;
                padding: 16px;
                box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
                transition: all 0.3s ease;
                cursor: pointer;
            " onmouseover="this.style.transform='translateY(-4px)'; this.style.boxShadow='0 4px 16px rgba(0, 0, 0, 0.12)';"
               onmouseout="this.style.transform=''; this.style.boxShadow='0 2px 8px rgba(0, 0, 0, 0.08)';"
               onclick="window.location.href='Trangchitiet.html?id=${product.product_id}'">
                <div style="width: 100%; height: 200px; background: #F3F4F6; border-radius: 8px; margin-bottom: 12px; display: flex; align-items: center; justify-content: center; overflow: hidden;">
                    ${product.image_url ? 
                        `<img src="${product.image_url}" alt="${product.name}" style="max-width: 100%; max-height: 100%; object-fit: contain;">` :
                        `<i class="fa-solid fa-image" style="font-size: 3rem; color: #D1D5DB;"></i>`
                    }
                </div>
                <h3 style="
                    color: #1F2937;
                    font-size: 14px;
                    font-weight: 600;
                    margin-bottom: 8px;
                    display: -webkit-box;
                    -webkit-line-clamp: 2;
                    -webkit-box-orient: vertical;
                    overflow: hidden;
                    line-height: 1.4;
                    min-height: 40px;
                ">${product.name}</h3>
                <div style="margin-bottom: 8px;">
                    <p style="color: #EC4899; font-size: 18px; font-weight: bold; display: inline;">${formattedPrice}</p>
                    ${discountPercent > 0 ? `
                        <span style="background: #FEE2E2; color: #DC2626; font-size: 12px; padding: 2px 6px; border-radius: 4px; margin-left: 8px; font-weight: 600;">-${discountPercent}%</span>
                    ` : ''}
                </div>
                ${originalPrice > price ? `
                    <p style="color: #9CA3AF; font-size: 14px; text-decoration: line-through; margin-bottom: 8px;">${formattedOriginalPrice}</p>
                ` : ''}
                <div style="display: flex; align-items: center; gap: 4px; color: #6B7280; font-size: 12px;">
                    <i class="fa-solid fa-store"></i>
                    <span>Tiki</span>
                </div>
            </div>
        `;
    });
    
    productsGrid.innerHTML = html;
}

// Store current products for sorting
let currentProducts = [];

/**
 * Sort products based on selected option
 */
function sortProducts() {
    const sortSelect = document.getElementById('sort-select');
    const sortValue = sortSelect.value;
    
    console.log('🔄 Sorting products by:', sortValue);
    
    let sortedProducts = [...currentProducts];
    
    switch(sortValue) {
        case 'price-asc':
            sortedProducts.sort((a, b) => (a.price || 0) - (b.price || 0));
            break;
        case 'price-desc':
            sortedProducts.sort((a, b) => (b.price || 0) - (a.price || 0));
            break;
        case 'discount':
            sortedProducts.sort((a, b) => (b.discount_percent || 0) - (a.discount_percent || 0));
            break;
        default:
            // Keep original order
            break;
    }
    
    displayProducts(sortedProducts);
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
