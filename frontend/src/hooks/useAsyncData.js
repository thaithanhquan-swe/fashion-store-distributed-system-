import { useCallback, useEffect, useState } from 'react'

// Tải dữ liệu từ service, trả về trạng thái đang tải / lỗi / dữ liệu.
// const { data, loading, error, reload } = useAsyncData(() => getEmployees({ branchId }), [branchId])
// `deps` là các giá trị mà khi đổi thì phải tải lại (giống mảng phụ thuộc của useEffect).
export function useAsyncData(load, deps = []) {
  const [version, setVersion] = useState(0)
  const [state, setState] = useState({ key: null, data: null, error: '' })
  const key = `${JSON.stringify(deps)}|${version}`

  useEffect(() => {
    let cancelled = false
    load().then(
      (data) => !cancelled && setState({ key, data, error: '' }),
      (err) => !cancelled && setState({ key, data: null, error: err.message || 'Không tải được dữ liệu' }),
    )
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])

  const reload = useCallback(() => setVersion((v) => v + 1), [])
  const loading = state.key !== key
  return { data: state.data, loading, error: loading ? '' : state.error, reload }
}