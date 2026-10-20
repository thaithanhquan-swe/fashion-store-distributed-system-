# Fashion Store – Frontend

Giao diện quản lý chuỗi cửa hàng thời trang (Quận 1, Quận 7, Thủ Đức và Central).
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

Trước khi commit hãy chạy `npm run lint` và `npm run build`, cả hai phải sạch lỗi.

> Thêm file mới mà trang không ăn kiểu dáng: tắt `npm run dev` (Ctrl+C), chạy lại rồi tải lại trình duyệt bằng Ctrl+Shift+R.

## Tài khoản demo

Mật khẩu chung: `123456`

| Tên đăng nhập | Vai trò   | Chi nhánh  |
| ------------- | --------- | ---------- |
| `staff.q1`    | Nhân viên | Quận 1     |
| `manager.q1`  | Quản lý   | Quận 1     |
| `admin`       | Quản trị  | Toàn chuỗi |

Trang đăng nhập có sẵn ba thẻ demo, bấm là tự điền.

## Thư viện

React 19, Vite, React Router, Tailwind CSS v4, lucide-react.
Không thêm thư viện khác nếu chưa báo cả nhóm.

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
| `ink`                 | Chữ chính, sidebar                |
| `accent`              | Màu nhấn, nút chính               |
| `ok` / `warn` / `bad` | Ổn định / Sắp hết / Hết hàng, lỗi |

Ví dụ: `bg-card`, `text-ink`, `text-accent`, `bg-ok/15`. Font tiêu đề: `font-display`. Font nội dung mặc định.

## Cấu trúc thư mục

```
src/
├── main.jsx, App.jsx        Khai báo route, chỗ cắm trang
├── index.css                Tailwind, màu, font
├── config/                  nav.js (menu + đường dẫn theo role), branches.js, env.js
├── services/                Gọi backend, mỗi nhóm một file
├── context/                 Đăng nhập
├── hooks/                   Hook dùng chung (useAuth)
├── utils/                   format.js (formatMoney, formatDate, formatDateTime)
├── routes/                  RoleGuard: chặn truy cập theo role
├── components/
│   ├── layout/              Sidebar, Topbar, AppLayout (có sẵn, không cần làm lại)
│   └── ui/                  Component dùng chung
└── pages/
    ├── auth/                Đăng nhập, hồ sơ, 403, 404
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

| Người           | Phần việc                                                                                                                                              |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Trần Vĩnh Khang | Khung chung. Trang: Login, Profile, 403, 404, Nhân viên, Tài khoản, Chi nhánh, Chi tiết chi nhánh, Trạng thái Site                                     |
| Nguyễn Bảo      | Bán hàng (POS), Sản phẩm (xem + quản lý), Khách hàng (danh sách + chi tiết), Hóa đơn (danh sách + chi tiết), Dashboard Manager                         |
| Nguyễn Quốc Đạt | Tồn kho, Sắp hết hàng, Tồn kho toàn chuỗi, Tồn kho theo chi nhánh, Dashboard Admin, Thống kê, So sánh doanh thu, Sản phẩm bán chạy, Doanh số nhân viên |

Ưu tiên làm các trang **Admin** trước vì giao diện chủ yếu để quản trị tương tác.

## Cách thêm một trang

1. Tạo file trong đúng thư mục `pages/...`, ví dụ `src/pages/employees/EmployeeListPage.jsx`.
2. Mở `src/App.jsx`, import trang và thêm một dòng vào object `PAGES`:

```jsx
import EmployeeListPage from "./pages/employees/EmployeeListPage";

const PAGES = {
  "/admin/employees": <EmployeeListPage />,
};
```

Đường dẫn lấy đúng từ `src/config/nav.js`. Không cần tự viết `<Route>`: khung đã tự sinh route, menu và chặn quyền theo role. Trang nào chưa có sẽ hiện trang "đang được xây dựng".

Trang cần thêm đường dẫn mới (ví dụ chi tiết hóa đơn `/staff/invoices/:id`) thì báo Khang để thêm vào khung.

## Component dùng chung (`components/ui`)

Import: `import { Button, DataTable } from '../../components/ui'`

| Component      | Dùng cho                                                                          |
| -------------- | --------------------------------------------------------------------------------- |
| `Button`       | `variant` primary / secondary / ghost / danger, `size` sm / md, `icon`, `loading` |
| `Badge`        | Nhãn trạng thái, `tone` neutral / ok / warn / bad / accent                        |
| `Card`         | Khối nội dung, `title`, `action`                                                  |
| `StatCard`     | Ô số liệu: `icon`, `label`, `value`, `hint`, `tone`                               |
| `Spinner`      | Đang tải                                                                          |
| `EmptyState`   | Danh sách rỗng                                                                    |
| `Field`        | Bọc ô nhập: `label`, `hint`, `error`                                              |
| `Input`        | Ô nhập, `onChange` nhận sự kiện gốc                                               |
| `Select`       | Ô chọn, `options=[{ value, label }]`, `onChange` nhận sự kiện gốc                 |
| `SearchBar`    | Thanh tìm kiếm, `onChange` nhận **chuỗi**, nội dung bên trong là bộ lọc           |
| `Pagination`   | Phân trang (page bắt đầu từ 1). `DataTable` đã gồm sẵn                            |
| `DataTable`    | Bảng: `columns=[{ key, header, align, render }]`, `rows`, `loading`, `onRowClick` |
| `Modal`        | Hộp thoại: `title`, `onClose`, `footer`, `wide`                                   |
| `ConfirmModal` | Hỏi xác nhận: `title`, `message`, `danger`, `loading`, `onConfirm`, `onClose`     |

Cần component mới dùng chung thì báo Khang, đừng tự sửa `components/ui`.

## Gọi dữ liệu

- Trang **không gọi `fetch`**. Chỉ gọi hàm trong `src/services/`.
- Khi backend chưa có API, hàm trong service trả dữ liệu giả (xem `authService.js` làm mẫu), sau này đổi sang gọi API mà trang không phải sửa.
- Tiền dùng `formatMoney(n)`, ngày dùng `formatDate`, `formatDateTime` từ `utils/format.js`.
- Thông tin người đăng nhập: `const { user } = useAuth()` gồm `name`, `role`, `branchId`, `employeeId`.

## Quy ước dữ liệu

- Tồn kho **sắp hết** so với `MucTonToiThieu` của từng sản phẩm, không dùng số cố định. Tồn bằng 0 là "Hết hàng".
- Staff và Manager chỉ thấy dữ liệu chi nhánh của mình (`user.branchId`). Admin xem toàn chuỗi.
- Bán hàng diễn ra tại chi nhánh. Admin không có màn hình bán hàng.

## Quy ước làm việc

- Mỗi trang một file, tên viết hoa chữ đầu và kết thúc bằng `Page` (`InvoiceListPage.jsx`).
- Mỗi người làm trên nhánh riêng (`feature/pos`, `feature/inventory`...), xong mới gộp vào `main`. Pull `main` trước khi bắt đầu và trước khi gộp.
- Mọi bảng và danh sách có đủ trạng thái đang tải, rỗng và lỗi.
- Văn bản trên giao diện bằng tiếng Việt, dữ liệu giả cũng bằng tiếng Việt.
- Không viết mã màu rời, không thêm thư viện khi chưa báo.

## Role và menu

Menu và đường dẫn nằm trong `src/config/nav.js`.

- **Staff**: Bán hàng, Sản phẩm, Tồn kho, Khách hàng, Hóa đơn (chỉ chi nhánh mình).
- **Manager**: Tổng quan, Bán hàng, Sản phẩm, Tồn kho (kèm nhập kho), Sắp hết hàng, Khách hàng, Hóa đơn, Thống kê, Nhân viên, Tài khoản (chỉ chi nhánh mình).
- **Admin**: Tổng quan, Chi nhánh, Sản phẩm, Tồn kho, Khách hàng, Hóa đơn, Nhân viên, Tài khoản, Thống kê chuỗi, So sánh doanh thu, Bán chạy, Doanh số nhân viên, Trạng thái Site.

Vào nhầm trang của role khác sẽ bị chuyển sang `/403`.
