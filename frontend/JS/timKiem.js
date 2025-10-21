// JavaScript cho chức năng tìm kiếm

// Ẩn phần kết quả khi trang vừa load
document.addEventListener('DOMContentLoaded', function() {
    const ketQuaSection = document.getElementById('ket_qua');
    const framesp = document.getElementById('frame_sp');
    
    // Ẩn phần kết quả ban đầu
    if (ketQuaSection) {
        ketQuaSection.style.display = 'none';
    }
    if (framesp) {
        framesp.style.display = 'none';
    }
    
    // Xử lý sự kiện click nút tìm kiếm
    const searchButton = document.getElementById('search_button');
    const searchInput = document.getElementById('search_input');
    
    if (searchButton) {
        searchButton.addEventListener('click', function() {
            handleSearch();
        });
    }
    
    // Xử lý sự kiện nhấn Enter trong ô input
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                handleSearch();
            }
        });
    }
    
    // Xử lý click vào các tag gợi ý
    const tagElements = document.querySelectorAll('#loai_sp p');
    tagElements.forEach(function(tag) {
        tag.addEventListener('click', function() {
            const tagText = this.textContent;
            searchInput.value = tagText;
            handleSearch();
        });
    });
});

// Hàm xử lý tìm kiếm
function handleSearch() {
    const searchInput = document.getElementById('search_input');
    const searchValue = searchInput.value.trim();
    
    if (searchValue === '') {
        alert('Vui lòng nhập từ khóa tìm kiếm!');
        return;
    }
    
    // Hiển thị phần kết quả
    const ketQuaSection = document.getElementById('ket_qua');
    const framesp = document.getElementById('frame_sp');
    
    if (ketQuaSection) {
        ketQuaSection.style.display = 'block';
    }
    if (framesp) {
        framesp.style.display = 'flex';
    }
    
    // Scroll xuống phần kết quả
    if (ketQuaSection) {
        ketQuaSection.scrollIntoView({ 
            behavior: 'smooth',
            block: 'start'
        });
    }
    
    // Có thể thêm logic tìm kiếm thực tế ở đây
    console.log('Đang tìm kiếm:', searchValue);
    
    // TODO: Gọi API hoặc lọc dữ liệu theo searchValue
}
