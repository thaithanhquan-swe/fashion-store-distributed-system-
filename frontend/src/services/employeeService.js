import { EMPLOYEES, wait } from './mockData'
import { loadMock, saveMock } from './mockStore'

// Dữ liệu giả cho đến khi backend có API nhân viên.
// Khi có API: thay thân các hàm bằng fetch(`${API_URL}/...`), giữ nguyên dữ liệu trả về.
//
// Trường bám theo bảng NhanVien: employeeId (MaNV), employeeName (HoTen), branchId (MaChiNhanh),
// position (ChucVu), gender (GioiTinh), birthDate (NgaySinh), phone, status.
// status: 'WORKING' = đang làm, 'RESIGNED' = đã nghỉ.

export const EMPLOYEE_STATUS = { WORKING: 'WORKING', RESIGNED: 'RESIGNED' }
export const EMPLOYEE_STATUS_LABEL = { WORKING: 'Đang làm', RESIGNED: 'Đã nghỉ' }

export const POSITIONS = ['Nhân viên bán hàng', 'Quản lý chi nhánh']

// Thông tin bổ sung cho danh sách nhân viên dùng chung trong mockData
const EXTRA = {
  NV001: ['Nam', '1988-04-12', '0903111001'],
  NV002: ['Nữ', '1995-09-03', '0903111002'],
  NV003: ['Nam', '1998-01-21', '0903111003'],
  NV004: ['Nữ', '1990-06-30', '0903111004'],
  NV005: ['Nam', '1996-11-08', '0903111005'],
  NV006: ['Nữ', '1999-02-17', '0903111006'],
  NV007: ['Nam', '1987-12-25', '0903111007'],
  NV008: ['Nữ', '1997-07-14', '0903111008'],
  NV009: ['Nam', '2000-03-05', '0903111009'],
}

const SEED = [
  ...EMPLOYEES.map((e) => {
    const [gender, birthDate, phone] = EXTRA[e.employeeId]
    return { ...e, gender, birthDate, phone, status: EMPLOYEE_STATUS.WORKING }
  }),
  {
    employeeId: 'NV010',
    branchId: null,
    employeeName: 'Ngô Thanh Tùng',
    position: 'Quản trị hệ thống',
    gender: 'Nam',
    birthDate: '1985-08-19',
    phone: '0903111010',
    status: EMPLOYEE_STATUS.WORKING,
  },
]

let db = loadMock('employees', SEED)
const commit = (next) => {
  db = next
  saveMock('employees', db)
}

// Lấy danh sách. Truyền branchId để chỉ lấy nhân viên của một chi nhánh (dùng cho Manager).
export async function getEmployees({ branchId } = {}) {
  await wait(300)
  return db.filter((e) => !branchId || e.branchId === branchId).map((e) => ({ ...e }))
}

// Thêm mới (không có employeeId) hoặc cập nhật (có employeeId). Trả về nhân viên đã lưu.
export async function saveEmployee(data) {
  await wait(400)
  if (data.employeeId) {
    commit(db.map((e) => (e.employeeId === data.employeeId ? { ...e, ...data } : e)))
    return db.find((e) => e.employeeId === data.employeeId)
  }
  const next = Math.max(...db.map((e) => Number(e.employeeId.slice(2)))) + 1
  const created = { ...data, employeeId: `NV${String(next).padStart(3, '0')}`, status: EMPLOYEE_STATUS.WORKING }
  commit([...db, created])
  return created
}

// Cho nghỉ việc hoặc khôi phục: status là 'RESIGNED' hoặc 'WORKING'.
export async function setEmployeeStatus(employeeId, status) {
  await wait(300)
  commit(db.map((e) => (e.employeeId === employeeId ? { ...e, status } : e)))
}