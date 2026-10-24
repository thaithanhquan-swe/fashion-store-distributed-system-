import { useState } from 'react'
import { Button, Field, Input, Modal, Select } from '../../components/ui'
import { CATEGORIES, saveProduct } from '../../services/productService'

// Thêm hoặc sửa sản phẩm (Admin). product: null = thêm mới.
export default function ProductFormModal({ product, onClose, onSaved }) {
  const [form, setForm] = useState({
    productName: product?.productName ?? '',
    category: product?.category ?? CATEGORIES[0],
    brand: product?.brand ?? '',
    color: product?.color ?? '',
    size: product?.size ?? '',
    price: product?.price ?? '',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [submitError, setSubmitError] = useState('')

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const validate = () => {
    const e = {}
    if (!form.productName.trim()) e.productName = 'Nhập tên sản phẩm'
    if (!form.brand.trim()) e.brand = 'Nhập thương hiệu'
    if (!form.color.trim()) e.color = 'Nhập màu sắc'
    if (!form.size.trim()) e.size = 'Nhập kích thước'
    if (!(Number(form.price) > 0)) e.price = 'Giá bán phải lớn hơn 0'
    return e
  }

  const submit = async (ev) => {
    ev?.preventDefault()
    if (loading) return
    const e = validate()
    setErrors(e)
    if (Object.keys(e).length) return

    setLoading(true)
    setSubmitError('')
    try {
      await saveProduct({ ...(product ?? {}), ...form })
      onSaved()
    } catch (err) {
      setSubmitError(err.message)
      setLoading(false)
    }
  }

  return (
    <Modal
      wide
      title={product ? `Sửa sản phẩm ${product.productId}` : 'Thêm sản phẩm'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button loading={loading} onClick={submit}>
            {product ? 'Lưu thay đổi' : 'Thêm sản phẩm'}
          </Button>
        </>
      }
    >
      <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <Field label="Tên sản phẩm" error={errors.productName}>
            <Input value={form.productName} onChange={set('productName')} error={!!errors.productName} autoFocus />
          </Field>
        </div>
        <Field label="Loại sản phẩm">
          <Select value={form.category} onChange={set('category')} options={CATEGORIES.map((c) => ({ value: c, label: c }))} />
        </Field>
        <Field label="Thương hiệu" error={errors.brand}>
          <Input value={form.brand} onChange={set('brand')} error={!!errors.brand} />
        </Field>
        <Field label="Màu sắc" error={errors.color}>
          <Input value={form.color} onChange={set('color')} error={!!errors.color} />
        </Field>
        <Field label="Kích thước" hint="Ví dụ S, M, L hoặc 39" error={errors.size}>
          <Input value={form.size} onChange={set('size')} error={!!errors.size} />
        </Field>
        <div className="sm:col-span-2">
          <Field label="Giá bán (₫)" error={errors.price}>
            <Input type="number" min="0" step="1000" value={form.price} onChange={set('price')} error={!!errors.price} />
          </Field>
        </div>
        {submitError && <p className="text-sm font-medium text-bad sm:col-span-2">{submitError}</p>}
        <button type="submit" className="hidden" />
      </form>
    </Modal>
  )
}