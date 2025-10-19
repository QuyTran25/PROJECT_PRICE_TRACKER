// File loadFrame.js - Tải header và footer cho tất cả các trang

function loadFrame() {
    // Load Header
    fetch('Header.html')
        .then(response => {
            if (!response.ok) {
                throw new Error('Cannot load header');
            }
            return response.text();
        })
        .then(data => {
            document.getElementById('header').innerHTML = data;
        })
        .catch(error => console.error('Error loading header:', error));

    // Load Footer
    fetch('footer.html')
        .then(response => {
            if (!response.ok) {
                throw new Error('Cannot load footer');
            }
            return response.text();
        })
        .then(data => {
            document.getElementById('footer').innerHTML = data;
        })
        .catch(error => console.error('Error loading footer:', error));
}

// Tự động load khi trang được tải
document.addEventListener('DOMContentLoaded', loadFrame);
