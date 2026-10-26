# AGENTS.md – Fashion Store Frontend

## 1. Dự án

Giao diện quản lý chuỗi cửa hàng thời trang phân tán: 3 chi nhánh (CN01 Quận 1, CN02 Quận 7, CN03 Thủ Đức) và Central.
Frontend **chỉ gọi API backend**, không kết nối trực tiếp SQL Server. Backend do thành viên khác làm; khi API chưa có, service trả dữ liệu giả.

Stack: React 19, Vite, React Router 7, Tailwind CSS v4, lucide-react.
**Không thêm thư viện** khi chưa báo cả nhóm (không thêm UI kit, thư viện biểu đồ, state manager, form library...).

## 2. Lệnh

Chạy trong thư mục `frontend/`:

| Lệnh              | Tác dụng                               |
| ----------------- | -------------------------------------- |
| `npm install`     | Cài thư viện                           |
| `npm run dev`     | Chạy phát triển, http://localhost:5173 |
| `npm run lint`    | Kiểm tra lỗi code                      |
| `npm run build`   | Build production                       |
| `npm run preview` | Xem thử bản đã build                   |

Trước khi báo xong việc, `npm run lint` và `npm run build` **phải sạch lỗi**.
Thêm file mới mà kiểu dáng không ăn: tắt `npm run dev`, chạy lại, rồi Ctrl+Shift+R trên trình duyệt.
Chưa có bộ test tự động; kiểm tra bằng cách chạy `npm run dev` và thử trang bằng tài khoản demo (mục 10).

## 3. Cấu trúc thư mục

```
src/
├── main.jsx, App.jsx          Khai báo route: PAGES (trang trong menu) và EXTRA_PAGES (trang chi tiết có tham số)
├── index.css                  Tailwind, màu và font (khối @theme)
├── config/
│   ├── nav.js                 Menu và đường dẫn theo vai trò (NGUỒN sinh route)
│   ├── branches.js            Ba chi nhánh cố định: BRANCHES, getBranch
│   └── env.js                 API_URL (từ VITE_API_URL)
├── context/                   Đăng nhập (AuthContext)
├── hooks/                     useAuth, useAsyncData
├── routes/RoleGuard.jsx       Chặn truy cập theo vai trò
├── utils/format.js            formatMoney, formatDate, formatDateTime, STOCK_STATUS, STOCK_STATUS_LABEL
├── services/                  Mọi lệnh gọi dữ liệu, mỗi nhóm một file (+ mockData.js, mockStore.js)
├── components/
│   ├── layout/                Sidebar, Topbar, AppLayout
│   └── ui/                    Component dùng chung (mục 6)
└── pages/
    ├── auth/                  Đăng nhập, hồ sơ, đổi mật khẩu, 403, 404
    ├── pos/                   Bán hàng
    ├── products/              Sản phẩm
    ├── inventory/             Tồn kho, nhập kho, sắp hết hàng
    ├── customers/             Khách hàng
    ├── invoices/              Hóa đơn
    ├── employees/             Nhân viên
    ├── accounts/              Tài khoản, phân quyền
    ├── dashboard/             Tổng quan Manager và Admin
    ├── statistics/            Thống kê, so sánh doanh thu
    └── admin/                 Chi nhánh, chi tiết chi nhánh, trạng thái Site
```

## 4. Giao diện

- Văn bản trên giao diện và dữ liệu giả đều bằng **tiếng Việt** (có dấu).
- **Màu chỉ dùng tên khai báo** trong `src/index.css`, không viết mã màu rời (hex, rgb, hsl):

  | Tên                   | Dùng cho                          |
  | --------------------- | --------------------------------- |
  | `bg`                  | Nền trang                         |
  | `card`                | Thẻ, bảng, modal                  |
  | `ink`                 | Chữ chính, sidebar                |
  | `accent`              | Màu nhấn, nút chính               |
  | `ok` / `warn` / `bad` | Ổn định / Sắp hết / Hết hàng, lỗi |

  Ví dụ: `bg-card`, `text-ink`, `text-ink/60`, `text-accent`, `bg-ok/15`. Font tiêu đề: `font-display`.

- Tiền: `formatMoney(n)` (ra `1.250.000 ₫`). Ngày: `formatDate`, `formatDateTime`. Không tự định dạng.
- Số tiền, mã, nhãn trạng thái trong ô hẹp phải không bị xuống dòng: thêm `whitespace-nowrap`. Số căn phải dùng thêm `tabular-nums`.
- **Mọi bảng và danh sách có đủ 3 trạng thái**:
  - đang tải lần đầu: `<Spinner />` (kiểm tra `loading && !data`);
  - lỗi: `<EmptyState>` có nút "Thử lại" gọi `reload`;
  - rỗng: `emptyText` của `DataTable` hoặc `<EmptyState>`.
- Nút chỉ có biểu tượng phải có `aria-label` (và `title`).
- Dùng icon từ `lucide-react`, không dùng emoji làm icon.
- Giao diện phải dùng được ở màn hình rộng và thu nhỏ (grid/flex dùng `sm:`, `lg:`), không vỡ bảng.

## 5. Dữ liệu và service

- **Trang không gọi `fetch`**. Chỉ gọi hàm trong `src/services/`. Địa chỉ backend lấy từ `API_URL` trong `config/env.js`.
- Khi backend chưa có API, hàm trong service trả dữ liệu giả (mẫu: `authService.js`, `employeeService.js`). Sau này chỉ thay thân hàm bằng `fetch(`${API_URL}/...`)`, **giữ nguyên hình dạng dữ liệu trả về** để trang không phải sửa. Mỗi service ghi chú tên bảng/cột CSDL mà nó bám theo.
- Hàm service là `async`, lỗi nghiệp vụ ném `throw new Error('Thông báo tiếng Việt')` để trang hiển thị `err.message`.
- **Dữ liệu giả phải lưu qua F5** bằng `services/mockStore.js`:

  ```js
  let db = loadMock("ten-nhom", DU_LIEU_MAU);
  const commit = (next) => {
    db = next;
    saveMock("ten-nhom", db);
  };
  // mọi thay đổi: commit(db.map(...)) hoặc commit([...db, moi])
  ```

  **Không gán `db = ...` trực tiếp** ngoài `commit` (dữ liệu sẽ mất sau F5). Khóa localStorage có tiền tố `fashion-store-mock-v1-`; muốn đưa dữ liệu giả về ban đầu thì xóa các khóa này.

- Dữ liệu mẫu dùng chung nằm ở `services/mockData.js` (sản phẩm, tồn kho, nhân viên, hóa đơn mẫu...). Mã trong dữ liệu mới phải khớp mã ở đó (ví dụ khách `KH001`, sản phẩm `SP001`).
- Tải dữ liệu trong trang bằng `useAsyncData(load, deps)`:

  ```jsx
  const { data, loading, error, reload } = useAsyncData(
    () => listCustomers({ branchId: user.branchId }),
    [user.branchId],
  );
  ```

  - `deps` phải liệt kê mọi giá trị mà `load` dùng, và phải **tuần tự hóa được bằng JSON** (chuỗi, số, boolean; không truyền hàm hay đối tượng lớn). Hook so sánh `deps` bằng `JSON.stringify`.
  - Gọi `reload()` sau khi thêm/sửa/xóa để làm mới danh sách.
  - Dòng `eslint-disable-next-line react-hooks/exhaustive-deps` trong hook là có chủ đích; **không** tắt cảnh báo này ở nơi khác.

- Người đăng nhập: `const { user } = useAuth()` gồm `name`, `role`, `branchId`, `employeeId` (Admin có `branchId = null`).

## 6. Component dùng chung (`components/ui`)

Import: `import { Button, DataTable } from '../../components/ui'`

| Component      | Props chính                                                                                                                        |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `Button`       | `variant` primary / secondary / ghost / danger, `size` sm / md, `icon`, `loading`, `disabled`                                      |
| `Badge`        | `tone` neutral / ok / warn / bad / accent                                                                                          |
| `Card`         | `title`, `action`, `padded`                                                                                                        |
| `StatCard`     | `icon`, `label`, `value` (có thể là JSX), `hint`, `tone` accent / ok / warn / bad                                                  |
| `Spinner`      | Đang tải                                                                                                                           |
| `EmptyState`   | `icon`, `title`, `description`, `action`                                                                                           |
| `Field`        | Bọc ô nhập: `label`, `hint`, `error`                                                                                               |
| `Input`        | Ô nhập, `onChange` nhận **sự kiện gốc**, `error` (boolean)                                                                         |
| `Select`       | `options=[{ value, label }]`, `onChange` nhận **sự kiện gốc**                                                                      |
| `SearchBar`    | `value`, `onChange` nhận **chuỗi**, nội dung bên trong là bộ lọc                                                                   |
| `Pagination`   | Phân trang (page bắt đầu từ 1); `DataTable` đã gồm sẵn                                                                             |
| `DataTable`    | `columns=[{ key, header, align, width, render }]`, `rows`, `rowKey`, `pageSize` (mặc định 8), `loading`, `emptyText`, `onRowClick` |
| `Modal`        | `title`, `onClose`, `footer`, `wide`                                                                                               |
| `ConfirmModal` | `title`, `message`, `confirmLabel`, `danger`, `loading`, `onConfirm`, `onClose`                                                    |

**Không tự sửa `components/ui`**; cần component mới dùng chung hoặc cần đổi component hiện có thì báo trưởng nhóm frontend (Khang). Thiếu thì ghép từ component có sẵn ngay trong file trang.

## 7. Thêm một trang

1. Tạo `src/pages/<nhóm>/TenPage.jsx`: mỗi file một trang, tên viết hoa chữ đầu và kết thúc bằng `Page`.
2. Trong `src/App.jsx`: import trang và thêm vào object `PAGES` với đường dẫn **đúng như trong `config/nav.js`**:

   ```jsx
   '/admin/employees': <EmployeeListPage />,
   ```

3. Trang chi tiết có tham số (`/staff/invoices/:invoiceId`) không nằm trong menu: thêm vào mảng `EXTRA_PAGES`, mỗi vai trò cần vào là một dòng:

   ```jsx
   { path: '/staff/invoices/:invoiceId', roles: ['staff'], element: <InvoiceDetailPage /> },
   ```

   Trong trang lấy tham số bằng `useParams()`. Điều hướng dùng `` `/${user.role}/invoices/${id}` ``.

4. **Không tự viết `<Route>`**: khung đã tự sinh route, menu và chặn quyền. Trang chưa có hiện "đang được xây dựng".
5. Mục menu mới hoặc đường dẫn mới sửa ở `config/nav.js` (báo trưởng nhóm frontend).

## 8. Phân quyền

- Vai trò: `staff`, `manager`, `admin`. `RoleGuard` chặn vào nhầm trang và chuyển sang `/403`.
- **Staff và Manager chỉ thấy dữ liệu chi nhánh của mình** (`user.branchId`); Admin xem toàn chuỗi (truyền `branchId = null`).
- Bán hàng (POS) chỉ ở chi nhánh; **Admin không có màn hình bán hàng**.
- Trang chi tiết phải **tự kiểm tra**: Staff/Manager mở dữ liệu thuộc chi nhánh khác thì hiện lỗi "không có quyền xem" (xem `InvoiceDetailPage`).
- Quy tắc riêng đã có: Manager không đổi vai trò; không ai tự khóa tài khoản của mình hoặc của Admin.
- Tồn kho **"sắp hết"** tính theo mức tối thiểu của từng sản phẩm (không dùng số cố định); tồn bằng 0 là "Hết hàng". Dùng `STOCK_STATUS` trong `utils/format.js`.

## 9. Lint và quy ước code

- ESLint với `react-hooks` và `react-refresh`: **file trang chỉ export component**. Hàm, hằng cần chia sẻ để ở file riêng (service hoặc utils).
- Không để biến hoặc import thừa; `npm run lint` phải trả về không lỗi, không cảnh báo mới.
- Component dùng hook phải đặt hook ở đầu hàm, không đặt trong điều kiện hay sau `return` sớm.
- Dùng `useMemo` cho giá trị dẫn xuất từ `data` khi nó nằm trong mảng phụ thuộc của hook khác (ví dụ `data ?? []`).
- Giữ comment tiếng Việt ngắn, nói **vì sao**, không nhắc lại code làm gì.

## 10. Tài khoản demo (mật khẩu `123456`)

| Tên đăng nhập | Vai trò   | Chi nhánh  | Vào đâu sau đăng nhập |
| ------------- | --------- | ---------- | --------------------- |
| `staff.q1`    | Nhân viên | CN01       | `/pos`                |
| `manager.q1`  | Quản lý   | CN01       | `/manager/dashboard`  |
| `admin`       | Quản trị  | Toàn chuỗi | `/admin/dashboard`    |

Khi làm một trang, thử bằng **đúng vai trò dùng nó** và bằng một vai trò **không được phép** (phải ra 403).

## 11. Git

- Làm trên nhánh riêng, gộp vào `develop` qua Pull Request. **Pull `develop` trước khi bắt đầu và trước khi gộp.**
- Conflict thường ở `App.jsx`: giữ **cả hai bên** (import, `PAGES`, `EXTRA_PAGES`), xóa các dòng `<<<<<<<`, `=======`, `>>>>>>>`, rồi chạy lại lint và build.
- Sửa file dùng chung (`components/ui`, service của người khác) thì báo cả nhóm trước khi gộp để tránh đụng file.
- Không commit `.env.local`, `node_modules`, `dist`. Không commit mật khẩu hay khóa bí mật.
- Tin nhắn commit ngắn, rõ, theo dạng `feat:`, `fix:`, `refactor:`, `docs:` (ví dụ `feat: add invoice list page`).
- Không sửa lịch sử đã đẩy (không `push --force` lên nhánh chung).

## 12. Trước khi báo xong

1. `npm run lint` và `npm run build` sạch lỗi.
2. Trang đủ 3 trạng thái (đang tải, lỗi, rỗng); thao tác thêm/sửa/xóa làm mới danh sách và còn nguyên sau F5.
3. Thử đúng vai trò; vai trò không được phép ra `/403`.
4. Không có mã màu rời, thư viện mới, `fetch` trong trang, hay sửa `components/ui` chưa báo.
5. Nếu thêm trang hoặc thay quy ước: cập nhật `README.md` và file này.
