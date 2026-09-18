import { ClipboardList } from 'lucide-react'
import { Link } from 'react-router-dom'

export function OrderHeaderLink() {
  return (
    <Link className="header-orders" to="/orders">
      <ClipboardList size={16} aria-hidden="true" />
      <span>주문 내역</span>
    </Link>
  )
}
