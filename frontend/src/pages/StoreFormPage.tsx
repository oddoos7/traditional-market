import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, LoaderCircle, Save, Store } from 'lucide-react'
import { FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getMarket } from '../api/marketApi'
import { createStore, getStore, StoreApiError, updateStore } from '../api/storeApi'
import { STORE_CATEGORY_LABELS, STORE_STATUS_LABELS } from '../constants/store'
import type { StoreCategory, StoreSaveInput, StoreStatus } from '../types/store'

interface StoreFormState {
  name: string
  description: string
  category: StoreCategory
  location: string
  phone: string
  openingHours: string
  closedDays: string
  imageUrl: string
  status: StoreStatus
}

const EMPTY_FORM: StoreFormState = {
  name: '',
  description: '',
  category: 'AGRICULTURAL',
  location: '',
  phone: '',
  openingHours: '',
  closedDays: '',
  imageUrl: '',
  status: 'OPEN',
}

export function StoreFormPage() {
  const { marketId: marketIdParam, storeId: storeIdParam } = useParams()
  const marketId = Number(marketIdParam)
  const storeId = storeIdParam ? Number(storeIdParam) : null
  const isEditing = storeId !== null
  const isValidMarketId = Number.isInteger(marketId) && marketId > 0
  const isValidStoreId = !isEditing || (Number.isInteger(storeId) && Number(storeId) > 0)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<StoreFormState>(EMPTY_FORM)
  const [formInitialized, setFormInitialized] = useState(!isEditing)
  const [errorMessage, setErrorMessage] = useState('')

  const marketQuery = useQuery({
    queryKey: ['market', marketId],
    queryFn: () => getMarket(marketId),
    enabled: isValidMarketId,
  })

  const storeQuery = useQuery({
    queryKey: ['store', marketId, storeId],
    queryFn: () => getStore(marketId, Number(storeId)),
    enabled: isValidMarketId && isValidStoreId && isEditing,
  })

  useEffect(() => {
    document.title = `${isEditing ? '점포 수정' : '점포 등록'} | 우리동네 전통시장`
  }, [isEditing])

  useEffect(() => {
    if (!storeQuery.data || formInitialized) return

    setForm({
      name: storeQuery.data.name,
      description: storeQuery.data.description || '',
      category: storeQuery.data.category,
      location: storeQuery.data.location || '',
      phone: storeQuery.data.phone || '',
      openingHours: storeQuery.data.openingHours || '',
      closedDays: storeQuery.data.closedDays || '',
      imageUrl: storeQuery.data.imageUrl || '',
      status: storeQuery.data.status,
    })
    setFormInitialized(true)
  }, [formInitialized, storeQuery.data])

  const saveMutation = useMutation({
    mutationFn: (input: StoreSaveInput) => isEditing
      ? updateStore(marketId, Number(storeId), input)
      : createStore(marketId, input),
    onSuccess: async (savedStore) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['stores', marketId] }),
        queryClient.invalidateQueries({ queryKey: ['store', marketId, savedStore.id] }),
      ])
      navigate(`/markets/${marketId}/stores/${savedStore.id}`, {
        replace: true,
        state: {
          message: isEditing
            ? '점포 정보가 수정되었습니다.'
            : '새 점포가 등록되었습니다.',
        },
      })
    },
  })

  const updateField = (field: keyof StoreFormState, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setErrorMessage('')

    if (!form.name.trim()) {
      setErrorMessage('점포명을 입력해주세요.')
      return
    }

    const nullable = (value: string) => value.trim() || null
    const input: StoreSaveInput = {
      name: form.name.trim(),
      description: nullable(form.description),
      category: form.category,
      location: nullable(form.location),
      phone: nullable(form.phone),
      openingHours: nullable(form.openingHours),
      closedDays: nullable(form.closedDays),
      imageUrl: nullable(form.imageUrl),
      status: form.status,
    }

    try {
      await saveMutation.mutateAsync(input)
    } catch (error) {
      setErrorMessage(
        error instanceof StoreApiError
          ? error.message
          : '점포 정보를 저장하지 못했습니다. 잠시 후 다시 시도해주세요.',
      )
    }
  }

  const isLoading = marketQuery.isLoading || (isEditing && storeQuery.isLoading)
  const hasLoadError = !isValidMarketId || !isValidStoreId
    || marketQuery.isError || (isEditing && storeQuery.isError)

  if (isLoading || (isEditing && !formInitialized && !hasLoadError)) {
    return (
      <main className="auth-route-loading" aria-label="점포 정보를 불러오는 중">
        <LoaderCircle className="spin" size={28} aria-hidden="true" />
      </main>
    )
  }

  if (hasLoadError || !marketQuery.data) {
    return (
      <main className="detail-state-wrap">
        <div className="state-card detail-state" role="alert">
          <h1>점포 관리 정보를 불러오지 못했어요</h1>
          <p>시장 또는 점포 주소를 확인한 뒤 다시 시도해주세요.</p>
          <div className="detail-state-actions"><Link to="/">시장 목록으로</Link></div>
        </div>
      </main>
    )
  }

  const cancelPath = isEditing
    ? `/markets/${marketId}/stores/${storeId}`
    : `/markets/${marketId}`

  return (
    <main className="store-form-main">
      <nav className="detail-breadcrumb" aria-label="현재 위치">
        <Link to={cancelPath}>
          <ArrowLeft size={17} aria-hidden="true" />
          돌아가기
        </Link>
        <span aria-hidden="true">/</span>
        <span>{isEditing ? '점포 수정' : '점포 등록'}</span>
      </nav>

      <section className="store-form-heading" aria-labelledby="store-form-title">
        <span className="store-form-icon" aria-hidden="true"><Store size={24} /></span>
        <div>
          <span className="eyebrow">Store management</span>
          <h1 id="store-form-title">{isEditing ? '점포 정보 수정' : '새 점포 등록'}</h1>
          <p><strong>{marketQuery.data.name}</strong>에 표시할 점포 정보를 입력해주세요.</p>
        </div>
      </section>

      <form className="store-form" onSubmit={handleSubmit}>
        <fieldset disabled={saveMutation.isPending}>
          <div className="store-form-section">
            <div className="store-form-section-title">
              <span>01</span>
              <div><h2>기본 정보</h2><p>점포를 구분하는 필수 정보를 입력합니다.</p></div>
            </div>
            <div className="store-form-grid">
              <label className="store-form-field store-form-wide">
                <span>점포명 <em>*</em></span>
                <input value={form.name} onChange={(event) => updateField('name', event.target.value)} maxLength={100} placeholder="예: 행복청과" required />
              </label>
              <label className="store-form-field">
                <span>카테고리 <em>*</em></span>
                <select value={form.category} onChange={(event) => updateField('category', event.target.value)}>
                  {Object.entries(STORE_CATEGORY_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              </label>
              <label className="store-form-field">
                <span>운영 상태 <em>*</em></span>
                <select value={form.status} onChange={(event) => updateField('status', event.target.value)}>
                  {Object.entries(STORE_STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              </label>
              <label className="store-form-field store-form-wide">
                <span>점포 소개</span>
                <textarea value={form.description} onChange={(event) => updateField('description', event.target.value)} maxLength={5000} rows={5} placeholder="대표 상품과 점포의 특징을 소개해주세요." />
                <small>{form.description.length.toLocaleString()} / 5,000자</small>
              </label>
            </div>
          </div>

          <div className="store-form-section">
            <div className="store-form-section-title">
              <span>02</span>
              <div><h2>방문 정보</h2><p>고객이 점포를 찾고 방문할 때 필요한 정보입니다.</p></div>
            </div>
            <div className="store-form-grid">
              <label className="store-form-field"><span>시장 내 위치</span><input value={form.location} onChange={(event) => updateField('location', event.target.value)} maxLength={100} placeholder="예: 가동 12호" /></label>
              <label className="store-form-field"><span>전화번호</span><input type="tel" value={form.phone} onChange={(event) => updateField('phone', event.target.value)} maxLength={20} placeholder="예: 02-1234-5678" /></label>
              <label className="store-form-field"><span>운영시간</span><input value={form.openingHours} onChange={(event) => updateField('openingHours', event.target.value)} maxLength={100} placeholder="예: 09:00 ~ 20:00" /></label>
              <label className="store-form-field"><span>휴무일</span><input value={form.closedDays} onChange={(event) => updateField('closedDays', event.target.value)} maxLength={100} placeholder="예: 매주 월요일" /></label>
              <label className="store-form-field store-form-wide"><span>대표 이미지 URL</span><input type="url" value={form.imageUrl} onChange={(event) => updateField('imageUrl', event.target.value)} maxLength={500} placeholder="https://example.com/store.jpg" /></label>
            </div>
          </div>

          {errorMessage && <div className="auth-notice error-notice store-form-error" role="alert">{errorMessage}</div>}

          <div className="store-form-actions">
            <Link to={cancelPath}>취소</Link>
            <button type="submit">
              {saveMutation.isPending ? <LoaderCircle className="spin" size={18} /> : <Save size={18} />}
              {saveMutation.isPending ? '저장 중...' : isEditing ? '수정 내용 저장' : '점포 등록'}
            </button>
          </div>
        </fieldset>
      </form>
    </main>
  )
}
