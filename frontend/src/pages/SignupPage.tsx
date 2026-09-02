import { Eye, EyeOff, LoaderCircle, LockKeyhole, Mail, UserRound } from 'lucide-react'
import { FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthApiError } from '../api/authApi'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'

export function SignupPage() {
  const navigate = useNavigate()
  const { signupUser, isSigningUp } = useAuth()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    document.title = '회원가입 | 우리동네 전통시장'
  }, [])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setErrorMessage('')

    if (password.length < 8) {
      setErrorMessage('비밀번호는 8자 이상 입력해주세요.')
      return
    }

    if (password !== passwordConfirm) {
      setErrorMessage('비밀번호 확인이 일치하지 않습니다.')
      return
    }

    try {
      await signupUser({ name: name.trim(), email: email.trim(), password })
      navigate('/login', {
        replace: true,
        state: {
          email: email.trim(),
          message: '회원가입이 완료되었습니다. 이제 로그인해주세요.',
        },
      })
    } catch (error) {
      setErrorMessage(
        error instanceof AuthApiError
          ? error.message
          : '회원가입 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.',
      )
    }
  }

  return (
    <AuthLayout
      eyebrow="Join the neighborhood"
      title="우리 동네 시장과 가까워지는 첫걸음"
      description="간단한 정보만 입력하면 바로 시작할 수 있어요."
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="auth-field">
          <span>이름</span>
          <div className="auth-input-wrap">
            <UserRound size={18} aria-hidden="true" />
            <input
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="이름을 입력해주세요"
              autoComplete="name"
              maxLength={50}
              required
            />
          </div>
        </label>

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
              placeholder="8자 이상 입력해주세요"
              autoComplete="new-password"
              minLength={8}
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
          <small>영문·숫자 등을 조합해 8자 이상 입력해주세요.</small>
        </label>

        <label className="auth-field">
          <span>비밀번호 확인</span>
          <div className="auth-input-wrap">
            <LockKeyhole size={18} aria-hidden="true" />
            <input
              type={showPassword ? 'text' : 'password'}
              value={passwordConfirm}
              onChange={(event) => setPasswordConfirm(event.target.value)}
              placeholder="비밀번호를 한 번 더 입력해주세요"
              autoComplete="new-password"
              minLength={8}
              maxLength={72}
              required
            />
          </div>
        </label>

        {errorMessage && (
          <div className="auth-notice error-notice" role="alert">{errorMessage}</div>
        )}

        <button className="auth-submit" type="submit" disabled={isSigningUp}>
          {isSigningUp && <LoaderCircle className="spin" size={18} aria-hidden="true" />}
          {isSigningUp ? '가입하는 중...' : '회원가입'}
        </button>
      </form>

      <p className="auth-switch">
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </AuthLayout>
  )
}
