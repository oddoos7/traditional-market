import { useQuery } from '@tanstack/react-query'
import { ShoppingCart } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getCart } from '../api/cartApi'

export function CartHeaderLink({ userId }: { userId: number }) {
  const cartQuery = useQuery({
    queryKey: ['cart', userId],
    queryFn: getCart,
    retry: false,
    staleTime: 30_000,
  })
  const itemCount = cartQuery.data?.totalItemCount ?? 0

  return (
    <Link className="header-cart" to="/cart" aria-label={`장바구니, 상품 ${itemCount}개`}>
      <ShoppingCart size={16} aria-hidden="true" />
      <span>장바구니</span>
      {itemCount > 0 && (
        <strong aria-hidden="true">{itemCount > 99 ? '99+' : itemCount}</strong>
      )}
    </Link>
  )
}
