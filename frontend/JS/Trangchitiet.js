document.addEventListener("DOMContentLoaded", () => {
  const tabButtons = document.querySelectorAll(".details-tab-btn");
  const tabContents = document.querySelectorAll(".details-tab-content");

  tabButtons.forEach((btn) => {
    btn.addEventListener("click", () => {
      tabButtons.forEach((b) => b.classList.remove("active"));
      tabContents.forEach((tab) => tab.classList.remove("active"));
      btn.classList.add("active");

      const targetTab = btn.getAttribute("data-tab");
      document.getElementById(targetTab).classList.add("active");

      if (targetTab === "tab-chart") initPriceChart();
    });
  });

  initPriceChart();
});

function initPriceChart() {
  const canvas = document.getElementById("priceChart");
  if (!canvas) return;

  // Xóa chart cũ nếu có
  if (canvas.chartInstance) canvas.chartInstance.destroy();

  // ===== TẠO DỮ LIỆU 30 NGÀY - MỖI 8 GIỜ =====
  const now = new Date();
  const labels = [];
  const data = [];
  let basePrice = 7990000;

  for (let i = 90; i >= 0; i--) {
    const time = new Date(now - i * 8 * 60 * 60 * 1000);
    const label =
      time.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" }) +
      " " +
      time.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
    labels.push(label);

    const fluctuation = 1 + (Math.random() - 0.5) * 0.06;
    basePrice = Math.max(7000000, Math.min(9000000, basePrice * fluctuation));
    data.push(Math.round(basePrice / 1000) * 1000);
  }

  // ===== TÌM GIÁ THẤP NHẤT =====
  const minPrice = Math.min(...data);
  const minIndex = data.indexOf(minPrice);

  // ===== TẠO BIỂU ĐỒ =====
  const ctx = canvas.getContext("2d");
  const chart = new Chart(ctx, {
    type: "line",
    data: {
      labels,
      datasets: [
        {
          label: "Giá (VNĐ)",
          data,
          borderColor: "#ff4d6d",
          backgroundColor: "rgba(255, 77, 109, 0.15)",
          borderWidth: 2,
          fill: true,
          tension: 0.3,
          pointRadius: 3,
          pointHoverRadius: 6,
          pointBackgroundColor: "#ff4d6d",
          pointBorderColor: "#fff",
          pointBorderWidth: 1,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: "nearest",
        intersect: false,
      },
      plugins: {
        legend: { display: false },
        title: {
          display: true,
          text: "Biểu đồ biến động giá 30 ngày gần đây",
          color: "#333",
          font: { size: 16 },
        },
        tooltip: {
          backgroundColor: "#fff",
          titleColor: "#000",
          bodyColor: "#ff4d6d",
          borderColor: "#ff4d6d",
          borderWidth: 1,
          padding: 10,
          displayColors: false,
          callbacks: {
            title: (items) => `Thời gian: ${items[0].label}`,
            label: (item) =>
              "Giá: " + item.formattedValue.replace(",", ".") + " đ",
          },
        },
        annotation: {
          annotations: {
            // Chỉ 1 nút tại điểm thấp nhất
            minPoint: {
              type: "point",
              xValue: labels[minIndex],
              yValue: minPrice,
              backgroundColor: "#16A34A",
              radius: 7,
              borderWidth: 3,
              borderColor: "#fff",
            },
          },
        },
      },
      scales: {
        x: {
          ticks: {
            maxTicksLimit: 8,
            color: "#666",
            font: { size: 11 },
          },
        },
        y: {
          ticks: {
            color: "#333",
            callback: (v) => v.toLocaleString("vi-VN") + "đ",
          },
        },
      },
    },
  });

  canvas.chartInstance = chart;
}
