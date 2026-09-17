import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Image, LoaderCircle, PackagePlus, Save } from 'lucide-react'
import { FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  createProduct,
  getProduct,
  ProductApiError,
  updateProduct,
} from '../api/productApi'
import { getStore } from '../api/storeApi'
import { PRODUCT_STATUS_LABELS } from '../constants/product'
import type { ProductSaveInput, ProductStatus } from '../types/product'

interface ProductFormState {
  name: string
  description: string
  price: string
  stockQuantity: string
  imageUrl: string
  status: ProductStatus
}

const EMPTY_FORM: ProductFormState = {
  name: '',
  description: '',
  price: '',
  stockQuantity: '0',
  imageUrl: '',
  status: 'ON_SALE',
}

export function ProductFormPage() {
  const {
    marketId: marketIdParam,
    storeId: storeIdParam,
    productId: productIdParam,
  } = useParams()
  const marketId = Number(marketIdParam)
  const storeId = Number(storeIdParam)
  const productId = productIdParam ? Number(productIdParam) : null
  const isEditing = productId !== null
  const isValidMarketId = Number.isInteger(marketId) && marketId > 0
  const isValidStoreId = Number.isInteger(storeId) && storeId > 0
  const isValidProductId = !isEditing
    || (Number.isInteger(productId) && Number(productId) > 0)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<ProductFormState>(EMPTY_FORM)
  const [formInitialized, setFormInitialized] = useState(!isEditing)
  const [errorMessage, setErrorMessage] = useState('')

  const storeQuery = useQuery({
    queryKey: ['store', marketId, storeId],
    queryFn: () => getStore(marketId, storeId),
    enabled: isValidMarketId && isValidStoreId,
  })

  const productQuery = useQuery({
    queryKey: ['product', storeId, productId],
    queryFn: () => getProduct(storeId, Number(productId)),
    enabled: isValidStoreId && isValidProductId && isEditing,
  })

  useEffect(() => {
    document.title = `${isEditing ? '상품 수정' : '상품 등록'} | 우리동네 전통시장`
  }, [isEditing])

  useEffect(() => {
    if (!productQuery.data || formInitialized) return

    setForm({
      name: productQuery.data.name,
      description: productQuery.data.description || '',
      price: String(productQuery.data.price),
      stockQuantity: String(productQuery.data.stockQuantity),
      imageUrl: productQuery.data.imageUrl || '',
      status: productQuery.data.status,
    })
    setFormInitialized(true)
  }, [formInitialized, productQuery.data])

  const saveMutation = useMutation({
    mutationFn: (input: ProductSaveInput) => isEditing
      ? updateProduct(storeId, Number(productId), input)
      : createProduct(storeId, input),
    onSuccess: async (savedProduct) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['products', storeId] }),
        queryClient.invalidateQueries({ queryKey: ['product', storeId, savedProduct.id] }),
      ])
      const savedStorePath = `/markets/${savedProduct.marketId}/stores/${storeId}`
      const destination = savedProduct.status === 'HIDDEN'
        ? savedStorePath
        : `${savedStorePath}/products/${savedProduct.id}`
      const message = savedProduct.status === 'HIDDEN'
        ? '상품이 숨김 처리되어 고객 화면에서 제외되었습니다.'
        : isEditing
          ? '상품 정보가 수정되었습니다.'
          : '새 상품이 등록되었습니다.'

      navigate(destination, {
        replace: true,
        state: { message },
      })
    },
  })

  const updateField = (field: keyof ProductFormState, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setErrorMessage('')

    const pricePattern = /^\d{1,10}(\.\d{1,2})?$/
    const price = Number(form.price)
    const stockQuantity = Number(form.stockQuantity)

    if (!form.name.trim()) {
      setErrorMessage('상품명을 입력해주세요.')
      return
    }
    if (!pricePattern.test(form.price) || price <= 0 || price > 9_999_999_999.99) {
      setErrorMessage('가격은 0원보다 크고 소수점 둘째 자리까지 입력할 수 있습니다.')
      return
    }
    if (!Number.isInteger(stockQuantity) || stockQuantity < 0 || stockQuantity > 1_000_000) {
      setErrorMessage('재고는 0개 이상 1,000,000개 이하의 정수로 입력해주세요.')
      return
    }

    const nullable = (value: string) => value.trim() || null
    const input: ProductSaveInput = {
      name: form.name.trim(),
      description: nullable(form.description),
      price,
      stockQuantity,
      imageUrl: nullable(form.imageUrl),
      status: form.status,
    }

    try {
      await saveMutation.mutateAsync(input)
    } catch (error) {
      setErrorMessage(
        error instanceof ProductApiError
          ? error.message
          : '상품 정보를 저장하지 못했습니다. 잠시 후 다시 시도해주세요.',
      )
    }
  }

  const isLoading = storeQuery.isLoading || (isEditing && productQuery.isLoading)
  const hasLoadError = !isValidMarketId || !isValidStoreId || !isValidProductId
    || storeQuery.isError || (isEditing && productQuery.isError)
    || (storeQuery.data !== undefined && storeQuery.data.marketId !== marketId)
    || (productQuery.data !== undefined && productQuery.data.marketId !== marketId)

  if (isLoading || (isEditing && !formInitialized && !hasLoadError)) {
    return (
      <main className="auth-route-loading" aria-label="상품 정보를 불러오는 중">
        <LoaderCircle className="spin" size={28} aria-hidden="true" />
      </main>
    )
  }

  if (hasLoadError || !storeQuery.data) {
    return (
      <main className="detail-state-wrap">
        <div className="state-card detail-state" role="alert">
          <h1>상품 관리 정보를 불러오지 못했어요</h1>
          <p>시장, 점포 또는 상품 주소를 확인한 뒤 다시 시도해주세요.</p>
          <div className="detail-state-actions"><Link to="/">시장 목록으로</Link></div>
        </div>
      </main>
    )
  }

  const storePath = `/markets/${marketId}/stores/${storeId}`
  const cancelPath = isEditing
    ? `${storePath}/products/${productId}`
    : `${storePath}#store-products`

  return (
    <main className="product-form-main">
      <nav className="detail-breadcrumb" aria-label="현재 위치">
        <Link to={cancelPath}>
          <ArrowLeft size={17} aria-hidden="true" />
          돌아가기
        </Link>
        <span aria-hidden="true">/</span>
        <span>{isEditing ? '상품 수정' : '상품 등록'}</span>
      </nav>

      <section className="product-form-heading" aria-labelledby="product-form-title">
        <span className="product-form-icon" aria-hidden="true"><PackagePlus size={25} /></span>
        <div>
          <span className="eyebrow">Product management</span>
          <h1 id="product-form-title">{isEditing ? '상품 정보 수정' : '새 상품 등록'}</h1>
          <p><strong>{storeQuery.data.name}</strong>에서 판매할 상품 정보를 입력해주세요.</p>
        </div>
      </section>

      <form className="product-form" onSubmit={handleSubmit}>
        <fieldset disabled={saveMutation.isPending}>
          <div className="product-form-section">
            <div className="product-form-section-title">
              <span>01</span>
              <div><h2>상품 기본 정보</h2><p>고객에게 표시할 상품명과 설명을 입력합니다.</p></div>
            </div>
            <div className="product-form-grid">
              <label className="store-form-field product-form-wide">
                <span>상품명 <em>*</em></span>
                <input
                  value={form.name}
                  onChange={(event) => updateField('name', event.target.value)}
                  maxLength={100}
                  placeholder="예: 새콤달콤 사과 1kg"
                  required
                />
              </label>
              <label className="store-form-field product-form-wide">
                <span>상품 설명</span>
                <textarea
                  value={form.description}
                  onChange={(event) => updateField('description', event.target.value)}
                  maxLength={5000}
                  rows={6}
                  placeholder="상품의 특징, 원산지, 구성 등을 소개해주세요."
                />
                <small>{form.description.length.toLocaleString()} / 5,000자</small>
              </label>
            </div>
          </div>

          <div className="product-form-section">
            <div className="product-form-section-title">
              <span>02</span>
              <div><h2>판매 정보</h2><p>가격, 재고와 고객에게 보일 판매 상태를 설정합니다.</p></div>
            </div>
            <div className="product-form-grid">
              <label className="store-form-field">
                <span>판매 가격 <em>*</em></span>
                <div className="product-number-field">
                  <input
                    type="number"
                    min="0.01"
                    max="9999999999.99"
                    step="0.01"
                    value={form.price}
                    onChange={(event) => updateField('price', event.target.value)}
                    placeholder="8500"
                    required
                  />
                  <span>원</span>
                </div>
              </label>
              <label className="store-form-field">
                <span>재고 수량 <em>*</em></span>
                <div className="product-number-field">
                  <input
                    type="number"
                    min="0"
                    max="1000000"
                    step="1"
                    value={form.stockQuantity}
                    onChange={(event) => updateField('stockQuantity', event.target.value)}
                    required
                  />
                  <span>개</span>
                </div>
              </label>
              <label className="store-form-field product-form-wide">
                <span>판매 상태 <em>*</em></span>
                <select
                  value={form.status}
                  onChange={(event) => updateField('status', event.target.value)}
                >
                  {Object.entries(PRODUCT_STATUS_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {value === 'HIDDEN' ? `${label} (목록에서 제거)` : label}
                    </option>
                  ))}
                </select>
                <small>‘숨김’ 상품은 고객 화면에서 제외되며 현재 관리 화면에서도 다시 조회할 수 없습니다.</small>
              </label>
            </div>
          </div>

          <div className="product-form-section">
            <div className="product-form-section-title">
              <span>03</span>
              <div><h2>상품 이미지</h2><p>상품을 잘 보여주는 이미지 URL을 등록합니다.</p></div>
            </div>
            <div className="product-form-image-layout">
              <label className="store-form-field product-form-wide">
                <span>이미지 URL</span>
                <input
                  type="url"
                  value={form.imageUrl}
                  onChange={(event) => updateField('imageUrl', event.target.value)}
                  maxLength={500}
                  placeholder="https://example.com/product.jpg"
                />
              </label>
              <div className="product-image-preview" aria-label="상품 이미지 미리보기">
                {form.imageUrl ? (
                  <img src={form.imageUrl} alt="등록할 상품 미리보기" />
                ) : (
                  <span><Image size={27} aria-hidden="true" />이미지 URL을 입력해주세요.</span>
                )}
              </div>
            </div>
          </div>

          {errorMessage && (
            <div className="auth-notice error-notice store-form-error" role="alert">
              {errorMessage}
            </div>
          )}

          <div className="store-form-actions">
            <Link to={cancelPath}>취소</Link>
            <button type="submit">
              {saveMutation.isPending
                ? <LoaderCircle className="spin" size={18} />
                : <Save size={18} />}
              {saveMutation.isPending ? '저장 중...' : isEditing ? '수정 내용 저장' : '상품 등록'}
            </button>
          </div>
        </fieldset>
      </form>
    </main>
  )
}
