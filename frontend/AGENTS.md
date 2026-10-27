# AGENTS.md – Fashion Store Frontend

Instructions for AI coding agents (and new contributors) working in `frontend/`.
See `README.md` for setup and task assignment. This file lists the **mandatory rules** and the easy-to-miss pitfalls.

## 1. Project

Management UI for a distributed fashion store chain: three branches (CN01 District 1, CN02 District 7, CN03 Thu Duc) plus Central.
The frontend **only calls the backend API**; it never connects to SQL Server directly. The backend is built by other team members. Where an API does not exist yet, services return mock data.

Stack: React 19, Vite, React Router 7, Tailwind CSS v4, lucide-react.
**Do not add dependencies** without telling the whole team (no UI kits, chart libraries, state managers or form libraries).

## 2. Commands

Run inside `frontend/`:

| Command           | Purpose                                   |
| ----------------- | ----------------------------------------- |
| `npm install`     | Install dependencies                      |
| `npm run dev`     | Development server, http://localhost:5173 |
| `npm run lint`    | Check code for errors                     |
| `npm run build`   | Production build                          |
| `npm run preview` | Preview the built app                     |

Before reporting work as done, `npm run lint` and `npm run build` **must both pass cleanly**.
If a newly added file is not styled, stop `npm run dev`, restart it, then hard-reload the browser (Ctrl+Shift+R).
There is no automated test suite yet. Verify by running `npm run dev` and trying the page with the demo accounts (section 10).

## 3. Directory layout

```
src/
├── main.jsx, App.jsx          Routing: PAGES (menu pages) and EXTRA_PAGES (detail pages with params)
├── index.css                  Tailwind, colors and fonts (@theme block)
├── config/
│   ├── nav.js                 Menu and paths per role (the SOURCE of generated routes)
│   ├── branches.js            The three fixed branches: BRANCHES, getBranch
│   └── env.js                 API_URL (from VITE_API_URL)
├── context/                   Authentication (AuthContext)
├── hooks/                     useAuth, useAsyncData
├── routes/RoleGuard.jsx       Role-based access control
├── utils/format.js            formatMoney, formatDate, formatDateTime, STOCK_STATUS, STOCK_STATUS_LABEL
├── services/                  All data access, one file per domain (+ mockData.js, mockStore.js)
├── components/
│   ├── layout/                Sidebar, Topbar, AppLayout
│   └── ui/                    Shared components (section 6)
└── pages/
    ├── auth/                  Login, profile, change password, 403, 404
    ├── pos/                   Point of sale
    ├── products/              Products
    ├── inventory/             Inventory, stock intake, low stock
    ├── customers/             Customers
    ├── invoices/              Invoices
    ├── employees/             Employees
    ├── accounts/              Accounts and roles
    ├── dashboard/             Manager and Admin dashboards
    ├── statistics/            Statistics, revenue comparison
    └── admin/                 Branches, branch detail, site status
```

## 4. UI rules

- **All user-facing text is in Vietnamese** (with diacritics), including mock data. Code identifiers are in English. Code comments are short Vietnamese notes explaining *why*, matching the existing codebase.
- **Colors: use only the named tokens** defined in `src/index.css`. Never write raw color values (hex, rgb, hsl).

  | Token                 | Used for                                  |
  | --------------------- | ----------------------------------------- |
  | `bg`                  | Page background                           |
  | `card`                | Cards, tables, modals                     |
  | `ink`                 | Primary text, sidebar                     |
  | `accent`              | Accent color, primary buttons             |
  | `ok` / `warn` / `bad` | Healthy / Low stock / Out of stock, error |

  Examples: `bg-card`, `text-ink`, `text-ink/60`, `text-accent`, `bg-ok/15`. Heading font: `font-display`.
- Money: `formatMoney(n)` (outputs `1.250.000 ₫`). Dates: `formatDate`, `formatDateTime`. Never format by hand.
- Amounts, IDs and status labels in narrow cells must not wrap: add `whitespace-nowrap`. Right-aligned numbers also get `tabular-nums`.
- **Every table and list handles all three states:**
  - first load: `<Spinner />` (check `loading && !data`);
  - error: `<EmptyState>` with a "Thử lại" button that calls `reload`;
  - empty: `emptyText` on `DataTable`, or `<EmptyState>`.
- Icon-only buttons need `aria-label` (and `title`).
- Use icons from `lucide-react`; no emoji as icons.
- Layouts must work on wide and narrow screens (use `sm:`, `lg:` breakpoints) and must not break tables.

## 5. Data and services

- **Pages never call `fetch`.** They call functions in `src/services/` only. The backend URL comes from `API_URL` in `config/env.js`.
- While the backend has no API, service functions return mock data (see `authService.js`, `employeeService.js`). Later, replace only the function body with `fetch(`${API_URL}/...`)` and **keep the returned data shape identical** so pages need no changes. Each service documents which database tables/columns it mirrors.
- Service functions are `async`. Business errors are thrown as `throw new Error('Vietnamese message')` and pages display `err.message`.
- **Mock data must survive a page reload** via `services/mockStore.js`:

  ```js
  let db = loadMock('group-name', SEED_DATA)
  const commit = (next) => {
    db = next
    saveMock('group-name', db)
  }
  // every change goes through commit(db.map(...)) or commit([...db, created])
  ```

  **Never assign `db = ...` directly outside `commit`**, or data is lost on reload. localStorage keys are prefixed `fashion-store-mock-v1-`; delete them to reset mock data.
- Shared seed data lives in `services/mockData.js` (products, inventory, employees, sample invoices...). IDs in new data must match it (for example customer `KH001`, product `SP001`).
- Load data in pages with `useAsyncData(load, deps)`:

  ```jsx
  const { data, loading, error, reload } = useAsyncData(() => listCustomers({ branchId: user.branchId }), [user.branchId])
  ```

  - `deps` must list every value `load` uses and must be **JSON-serializable** (strings, numbers, booleans; no functions or large objects). The hook compares `deps` with `JSON.stringify`.
  - Call `reload()` after create/update/delete to refresh the list.
  - The `eslint-disable-next-line react-hooks/exhaustive-deps` inside the hook is intentional. **Do not** disable that rule anywhere else.
- Current user: `const { user } = useAuth()` with `name`, `role`, `branchId`, `employeeId` (Admin has `branchId = null`).

## 6. Shared components (`components/ui`)

Import: `import { Button, DataTable } from '../../components/ui'`

| Component      | Main props                                                                                     |
| -------------- | ---------------------------------------------------------------------------------------------- |
| `Button`       | `variant` primary / secondary / ghost / danger, `size` sm / md, `icon`, `loading`, `disabled`  |
| `Badge`        | `tone` neutral / ok / warn / bad / accent                                                      |
| `Card`         | `title`, `action`, `padded`                                                                    |
| `StatCard`     | `icon`, `label`, `value` (may be JSX), `hint`, `tone` accent / ok / warn / bad                 |
| `Spinner`      | Loading indicator                                                                              |
| `EmptyState`   | `icon`, `title`, `description`, `action`                                                       |
| `Field`        | Wraps an input: `label`, `hint`, `error`                                                       |
| `Input`        | `onChange` receives the **native event**, `error` (boolean)                                    |
| `Select`       | `options=[{ value, label }]`, `onChange` receives the **native event**                         |
| `SearchBar`    | `value`, `onChange` receives a **string**; children are the filters                            |
| `Pagination`   | Pages start at 1; already included in `DataTable`                                              |
| `DataTable`    | `columns=[{ key, header, align, width, render }]`, `rows`, `rowKey`, `pageSize` (default 8), `loading`, `emptyText`, `onRowClick` |
| `Modal`        | `title`, `onClose`, `footer`, `wide`                                                           |
| `ConfirmModal` | `title`, `message`, `confirmLabel`, `danger`, `loading`, `onConfirm`, `onClose`                |

**Do not edit `components/ui` on your own.** If you need a new shared component or a change to an existing one, tell the frontend lead (Khang). If something is missing, compose it from existing components inside your page file.

## 7. Adding a page

1. Create `src/pages/<group>/NamePage.jsx`: one page per file, PascalCase, name ends with `Page`.
2. In `src/App.jsx`, import the page and add it to the `PAGES` object using the path **exactly as defined in `config/nav.js`**:

   ```jsx
   '/admin/employees': <EmployeeListPage />,
   ```
3. Detail pages with URL params (`/staff/invoices/:invoiceId`) are not in the menu. Add them to the `EXTRA_PAGES` array, one entry per role that may open them:

   ```jsx
   { path: '/staff/invoices/:invoiceId', roles: ['staff'], element: <InvoiceDetailPage /> },
   ```

   Read params with `useParams()`. Navigate with `` `/${user.role}/invoices/${id}` ``.
4. **Do not write `<Route>` by hand.** The shell generates routes, menu and role checks. Pages not yet built show an "under construction" placeholder.
5. New menu entries or paths go in `config/nav.js` (tell the frontend lead).

## 8. Roles and access

- Roles: `staff`, `manager`, `admin`. `RoleGuard` redirects users who open another role's page to `/403`.
- **Staff and Manager see only their own branch's data** (`user.branchId`). Admin sees the whole chain (pass `branchId = null`).
- Point of sale (POS) happens at a branch; **Admin has no POS screen**.
- Detail pages must **check access themselves**: if Staff/Manager opens data from another branch, show a "no permission" error (see `InvoiceDetailPage`).
- Existing rules: a Manager cannot change roles; nobody can lock their own account or an Admin account.
- Inventory **"low stock"** is measured against each product's own minimum level (no fixed number); quantity 0 is "out of stock". Use `STOCK_STATUS` from `utils/format.js`.

## 9. Lint and code conventions

- ESLint runs `react-hooks` and `react-refresh`: **page files export components only.** Put shared functions and constants in a separate file (service or utils).
- No unused variables or imports. `npm run lint` must report no errors and no new warnings.
- Call hooks at the top of the component, never conditionally or after an early `return`.
- Wrap values derived from `data` in `useMemo` when they appear in another hook's dependency array (for example `data ?? []`).
- Keep comments short and explain *why*, not what the code does.

## 10. Demo accounts (password `123456`)

| Username     | Role    | Branch      | Landing page after login |
| ------------ | ------- | ----------- | ------------------------ |
| `staff.q1`   | Staff   | CN01        | `/pos`                   |
| `manager.q1` | Manager | CN01        | `/manager/dashboard`     |
| `admin`      | Admin   | Whole chain | `/admin/dashboard`       |

When building a page, test it with the role that uses it **and** with a role that must not access it (it should redirect to `/403`).

## 11. Git

- Work on your own branch and merge into `develop` through a Pull Request. **Pull `develop` before starting and before merging.**
- Conflicts usually occur in `App.jsx`: keep **both sides** (imports, `PAGES`, `EXTRA_PAGES`), remove the `<<<<<<<`, `=======`, `>>>>>>>` markers, then re-run lint and build.
- If you change a shared file (`components/ui`, another member's service), tell the team before merging to avoid file clashes.
- Never commit `.env.local`, `node_modules` or `dist`. Never commit passwords or secrets.
- Keep commit messages short and clear, using `feat:`, `fix:`, `refactor:` or `docs:` prefixes (for example `feat: add invoice list page`).
- Do not rewrite pushed history (no `push --force` on shared branches).

## 12. Definition of done

1. `npm run lint` and `npm run build` pass cleanly.
2. The page handles loading, error and empty states; create/update/delete refreshes the list and survives a reload.
3. Tested with the right role; a disallowed role is redirected to `/403`.
4. No raw colors, new dependencies, `fetch` in pages, or unannounced edits to `components/ui`.
5. If you added a page or changed a convention, update `README.md` and this file.
