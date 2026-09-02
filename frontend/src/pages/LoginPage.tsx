import { Eye, EyeOff, LoaderCircle, LockKeyhole, Mail } from 'lucide-react'
import { FormEvent, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthApiError } from '../api/authApi'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'

interface LoginLocationState {
  email?: string
  message?: string
  from?: string
}

export function LoginPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { loginUser, isLoggingIn } = useAuth()
  const locationState = location.state as LoginLocationState | null
  const [email, setEmail] = useState(locationState?.email ?? '')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    document.title = '로그인 | 우리동네 전통시장'
  }, [])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setErrorMessage('')

    try {
      await loginUser({ email: email.trim(), password })
      navigate(locationState?.from || '/', { replace: true })
    } catch (error) {
      setErrorMessage(
        error instanceof AuthApiError
          ? error.message
          : '로그인 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.',
      )
    }
  }

  return (
    <AuthLayout
      eyebrow="Welcome back"
      title="다시 만나 반가워요"
      description="가입한 이메일과 비밀번호로 로그인해주세요."
    >
      {locationState?.message && (
        <div className="auth-notice success-notice" role="status">
          {locationState.message}
        </div>
      )}

      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="auth-field">
          <span>이메일</span>
          <div className="auth-input-wrap">
            <Mail size={18} aria-hidden="true" />
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="market@example.com"
              autoComplete="email"
              maxLength={255}
              required
            />
          </div>
        </label>

        <label className="auth-field">
          <span>비밀번호</span>
          <div className="auth-input-wrap">
            <LockKeyhole size={18} aria-hidden="true" />
            <input
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="비밀번호를 입력해주세요"
              autoComplete="current-password"
              maxLength={72}
              required
            />
            <button
              className="password-toggle"
              type="button"
              onClick={() => setShowPassword((current) => !current)}
              aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
            >
              {showPassword
                ? <EyeOff size={18} aria-hidden="true" />
                : <Eye size={18} aria-hidden="true" />}
            </button>
          </div>
        </label>

        {errorMessage && (
          <div className="auth-notice error-notice" role="alert">{errorMessage}</div>
        )}

        <button className="auth-submit" type="submit" disabled={isLoggingIn}>
          {isLoggingIn && <LoaderCircle className="spin" size={18} aria-hidden="true" />}
          {isLoggingIn ? '로그인 중...' : '로그인'}
        </button>
      </form>

      <p className="auth-switch">
        아직 계정이 없으신가요? <Link to="/signup">회원가입</Link>
      </p>
    </AuthLayout>
  )
}
