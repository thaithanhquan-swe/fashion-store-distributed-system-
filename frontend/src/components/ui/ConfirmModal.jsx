import Button from './Button'
import Modal from './Modal'

// Hỏi xác nhận trước khi khóa, mở, hủy, xóa...
// <ConfirmModal title="Khóa tài khoản?" message="..." danger loading={busy} onConfirm={lock} onClose={close} />
export default function ConfirmModal({
  title,
  message,
  confirmLabel = 'Xác nhận',
  danger = false,
  loading = false,
  onConfirm,
  onClose,
}) {
  return (
    <Modal
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button variant={danger ? 'danger' : 'primary'} loading={loading} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <p className="text-sm text-ink/70">{message}</p>
    </Modal>
  )
}