import { useEffect } from 'react'
import { X } from 'lucide-react'

// Hộp thoại. Bấm nền tối hoặc phím Esc sẽ gọi onClose.
// <Modal title="Thêm khách hàng" onClose={close} footer={<Button>Lưu</Button>}>...</Modal>
export default function Modal({ title, onClose, footer, wide = false, children }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-ink/50 p-4"
      onMouseDown={(e) => e.target === e.currentTarget && onClose()}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`flex max-h-[90vh] w-full flex-col rounded-2xl bg-card shadow-2xl ${wide ? 'max-w-2xl' : 'max-w-md'}`}
      >
        <header className="flex items-center justify-between gap-3 px-5 pb-2 pt-5">
          <h3 className="font-display text-xl font-bold text-ink">{title}</h3>
          <button type="button" onClick={onClose} aria-label="Đóng" className="grid size-8 cursor-pointer place-items-center rounded-lg text-ink/60 hover:bg-ink/5">
            <X size={18} />
          </button>
        </header>
        <div className="overflow-y-auto px-5 py-3">{children}</div>
        {footer && <footer className="flex justify-end gap-2 border-t border-ink/10 px-5 py-4">{footer}</footer>}
      </div>
    </div>
  )
}