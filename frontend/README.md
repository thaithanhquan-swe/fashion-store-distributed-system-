# Fashion Store – Frontend

Giao diện quản lý chuỗi cửa hàng thời trang (Q1, Q7, Thủ Đức và Central).
Frontend chỉ gọi API của backend, không kết nối trực tiếp SQL Server.

## Yêu cầu

- Node.js **20.19+** (hoặc 22.12+)
- npm 10+
- Git

## Cài đặt và chạy

```bash
cd frontend
npm install
cp .env.example .env.local      # Windows PowerShell: Copy-Item .env.example .env.local
npm run dev                     # http://localhost:5173
```

| Lệnh              | Tác dụng                                   |
| ----------------- | ------------------------------------------ |
| `npm run dev`     | Chạy chế độ phát triển                     |
| `npm run build`   | Build bản production, dùng để kiểm tra lỗi |
| `npm run lint`    | Kiểm tra lỗi code                          |
| `npm run preview` | Xem thử bản đã build                       |

## Thư viện

React 19, Vite, React Router, Tailwind CSS v4, lucide-react.
Không thêm thư viện UI khác nếu chưa báo cả nhóm.

## Cấu hình

Sửa `.env.local` (file này không đưa lên git):

```
VITE_API_URL=http://localhost:8080
```

Chi nhánh gọi backend của chi nhánh đó, Central gọi backend Central.

## Màu và font

Khai báo trong `src/index.css` (khối `@theme`). Chỉ dùng tên màu, không viết mã màu rời:

| Tên                   | Dùng cho                          |
| --------------------- | --------------------------------- |
| `bg`                  | Nền trang                         |
| `card`                | Thẻ, bảng, modal                  |
| `ink`                 | Sidebar                           |
| `accent`              | Màu nhấn, nút chính               |
| `ok` / `warn` / `bad` | Ổn định / Sắp hết / Hết hàng, lỗi |

Font tiêu đề: `font-display` (Bricolage Grotesque). Font nội dung: Figtree (mặc định).

## Cấu trúc thư mục

```
src/
├── main.jsx, App.jsx        Khai báo route
├── index.css                Tailwind, màu, font
├── config/                  Menu theo role, biến môi trường
├── services/                Gọi backend, mỗi nhóm một file
├── context/                 Đăng nhập, thông báo
├── hooks/                   Hook dùng chung
├── utils/                   Hàm tiện ích (định dạng tiền, ngày)
├── routes/                  Chặn truy cập theo role
├── components/
│   ├── layout/              Sidebar, Topbar, AppLayout
│   └── ui/                  Button, Badge, Card, Modal, DataTable...
└── pages/
    ├── auth/                Đăng nhập, hồ sơ, đổi mật khẩu, 403, 404
    ├── pos/                 Bán hàng
    ├── products/            Sản phẩm
    ├── inventory/           Tồn kho, nhập kho, sắp hết hàng
    ├── customers/           Khách hàng
    ├── invoices/            Hóa đơn
    ├── employees/           Nhân viên
    ├── accounts/            Tài khoản, phân quyền
    ├── dashboard/           Tổng quan Manager và Admin
    ├── statistics/          Thống kê, so sánh doanh thu
    └── admin/               Chi nhánh, trạng thái Site
```

## Phân công

| Phần                                | Thư mục                                                                                   | Người phụ trách |
| ----------------------------------- | ----------------------------------------------------------------------------------------- | --------------- |
| Khung chung, đăng nhập              | `components/`, `routes/`, `context/`, `pages/auth`                                        |                 |
| Bán hàng, hóa đơn                   | `pages/pos`, `pages/invoices`                                                             |                 |
| Sản phẩm, tồn kho, khách hàng       | `pages/products`, `pages/inventory`, `pages/customers`                                    |                 |
| Nhân sự, tài khoản, thống kê, admin | `pages/employees`, `pages/accounts`, `pages/dashboard`, `pages/statistics`, `pages/admin` |                 |

## Quy ước làm việc

- Mỗi trang một file, tên file viết hoa chữ đầu (`InvoiceListPage.jsx`).
- Không gọi `fetch` trong trang. Chỉ gọi hàm trong `services/`. Khi backend chưa có API, trả dữ liệu giả ở đó.
- Mỗi người thêm route của mình vào `App.jsx`. Pull trước khi sửa để tránh xung đột.
- Không sửa `components/ui` nếu chưa báo người phụ trách khung chung.
- Mỗi người làm trên nhánh riêng (`feature/pos`, `feature/inventory`...), xong mới gộp vào `main`.
- Mọi bảng và danh sách có đủ trạng thái đang tải, rỗng và lỗi.

## Role và menu

- **Staff**: Bán hàng, Sản phẩm, Tồn kho, Khách hàng, Hóa đơn (chỉ chi nhánh mình).
- **Manager**: thêm Tổng quan, Nhập kho, Sắp hết hàng, Nhân viên, Tài khoản, Thống kê (chỉ chi nhánh mình).
- **Admin**: toàn chuỗi, thêm Chi nhánh, So sánh doanh thu, Trạng thái Site.
