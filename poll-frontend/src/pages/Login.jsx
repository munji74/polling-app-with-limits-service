import { useState } from 'react'
import { useLocation, useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// Import Ant Design icons and notification
import { notification } from 'antd'
import { MailOutlined, LockOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from?.pathname || '/'

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword)
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await login(email, password)

      // Success notification popup on LEFT side
      notification.success({
        message: 'Login Successful',
        description: 'Welcome back! You have successfully logged in.',
        duration: 4,
        placement: 'topLeft',
        style: {
          marginTop: '50px'
        }
      })

      navigate(from, { replace: true })
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        'Login failed'
      setError(msg)

      // Error notification popup on LEFT side
      notification.error({
        message: 'Login Failed',
        description: msg,
        duration: 4,
        placement: 'topLeft',
        style: {
          marginTop: '50px'
        }
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-md mx-auto bg-white border rounded-lg p-6 shadow-sm">
      <h1 className="text-xl font-semibold mb-6 text-center">Login</h1>
      <form onSubmit={onSubmit} className="space-y-4">
        {/* Email Field with Icon */}
        <div>
          <label className="block text-sm mb-2 font-medium">Email</label>
          <div className="relative">
            <MailOutlined className="absolute left-3 top-3 text-gray-400" />
            <input
              type="email"
              className="w-full border rounded px-3 py-2 pl-10 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Enter your email"
              required
            />
          </div>
        </div>

        {/* Password Field with Icon and Eye Toggle */}
        <div>
          <label className="block text-sm mb-2 font-medium">Password</label>
          <div className="relative">
            <LockOutlined className="absolute left-3 top-3 text-gray-400" />
            <input
              type={showPassword ? "text" : "password"}
              className="w-full border rounded px-3 py-2 pl-10 pr-10 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              required
            />
            <button
              type="button"
              className="absolute right-3 top-3 text-gray-400 hover:text-gray-600"
              onClick={togglePasswordVisibility}
            >
              {showPassword ? <EyeInvisibleOutlined /> : <EyeOutlined />}
            </button>
          </div>
        </div>

        {error && (
          <div className="text-red-600 text-sm bg-red-50 p-3 rounded border border-red-200">
            {error}
          </div>
        )}

        <button
          disabled={submitting}
          className="w-full bg-gray-900 text-white rounded py-2 hover:bg-gray-800 disabled:bg-gray-400 disabled:cursor-not-allowed transition duration-200"
        >
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>

        <p className="text-sm text-center text-gray-600">
          No account?{' '}
          <Link className="text-blue-600 hover:underline font-medium" to="/register">
            Create one
          </Link>
        </p>


      </form>
    </div>
  )
}
