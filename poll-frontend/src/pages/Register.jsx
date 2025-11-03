import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// Import Ant Design icons and notification
import { notification } from 'antd'
import { UserOutlined, MailOutlined, LockOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons'

export default function Register() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const { register } = useAuth()
  const navigate = useNavigate()

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword)
  }

  const toggleConfirmPasswordVisibility = () => {
    setShowConfirmPassword(!showConfirmPassword)
  }

  const validateForm = () => {
    const errors = {}

    if (password !== passwordConfirm) {
      errors.passwordConfirm = "Passwords don't match"
    }
    if (password.length < 6) {
      errors.password = "Password must be at least 6 characters long"
    }

    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    setFieldErrors({})

    // Validate form before submission
    if (!validateForm()) {
      setSubmitting(false)
      return
    }

    try {
      await register({ name, email, password, passwordConfirm })

      // Success notification popup on LEFT side
      notification.success({
        message: 'Registration Successful',
        description: "Thank you! You're successfully registered. Please log in to continue!",
        duration: 4,
        placement: 'topLeft',
        style: {
          marginTop: '50px'
        }
      })

      navigate('/', { replace: true })
    } catch (err) {
      // register() throws either a string or returns a map for field errors
      if (typeof err === 'object' && err !== null) {
        setFieldErrors(err)

        // Show field errors in notification if any
        const firstError = Object.values(err)[0]
        if (firstError) {
          notification.error({
            message: 'Registration Failed',
            description: firstError,
            duration: 4,
            placement: 'topLeft',
            style: {
              marginTop: '50px'
            }
          })
        }
      } else {
        const errorMsg = String(err || 'Registration failed')
        setError(errorMsg)

        // Error notification popup on LEFT side
        notification.error({
          message: 'Registration Failed',
          description: errorMsg,
          duration: 4,
          placement: 'topLeft',
          style: {
            marginTop: '50px'
          }
        })
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-md mx-auto bg-white border rounded-lg p-6 shadow-sm">
      <h1 className="text-xl font-semibold mb-6 text-center">Create Account</h1>
      <form onSubmit={onSubmit} className="space-y-4">
        {/* Name Field with Icon */}
        <div>
          <label className="block text-sm mb-2 font-medium">Full Name</label>
          <div className="relative">
            <UserOutlined className="absolute left-3 top-3 text-gray-400" />
            <input
              className="w-full border rounded px-3 py-2 pl-10 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Enter your full name"
              required
            />
          </div>
          {fieldErrors.name && <p className="text-red-600 text-sm mt-1">{fieldErrors.name}</p>}
        </div>

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
          {fieldErrors.email && <p className="text-red-600 text-sm mt-1">{fieldErrors.email}</p>}
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
          {fieldErrors.password && <p className="text-red-600 text-sm mt-1">{fieldErrors.password}</p>}
        </div>

        {/* Confirm Password Field with Icon and Eye Toggle */}
        <div>
          <label className="block text-sm mb-2 font-medium">Confirm Password</label>
          <div className="relative">
            <LockOutlined className="absolute left-3 top-3 text-gray-400" />
            <input
              type={showConfirmPassword ? "text" : "password"}
              className="w-full border rounded px-3 py-2 pl-10 pr-10 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              placeholder="Confirm your password"
              required
            />
            <button
              type="button"
              className="absolute right-3 top-3 text-gray-400 hover:text-gray-600"
              onClick={toggleConfirmPasswordVisibility}
            >
              {showConfirmPassword ? <EyeInvisibleOutlined /> : <EyeOutlined />}
            </button>
          </div>
          {fieldErrors.passwordConfirm && (
            <p className="text-red-600 text-sm mt-1">{fieldErrors.passwordConfirm}</p>
          )}
        </div>

        {/* Password strength indicator */}
        {password && (
          <div className="text-xs text-gray-500">
            Password strength:
            <span className={`ml-1 ${
              password.length < 6 ? 'text-red-500' :
              password.length < 8 ? 'text-yellow-500' : 'text-green-500'
            }`}>
              {password.length < 6 ? 'Weak' : password.length < 8 ? 'Medium' : 'Strong'}
            </span>
          </div>
        )}

        {/* Match indicator */}
        {passwordConfirm && (
          <div className={`text-xs ${password === passwordConfirm ? 'text-green-500' : 'text-red-500'}`}>
            {password === passwordConfirm ? '✓ Passwords match' : '✗ Passwords do not match'}
          </div>
        )}

        {error && (
          <div className="text-red-600 text-sm bg-red-50 p-3 rounded border border-red-200">
            {error}
          </div>
        )}

        <button
          disabled={submitting}
          className="w-full bg-gray-900 text-white rounded py-2 hover:bg-gray-800 disabled:bg-gray-400 disabled:cursor-not-allowed transition duration-200"
        >
          {submitting ? 'Creating Account…' : 'Create Account'}
        </button>

        <p className="text-sm text-center text-gray-600">
          Already have an account?{' '}
          <Link className="text-blue-600 hover:underline font-medium" to="/login">
            Log in
          </Link>
        </p>
      </form>
    </div>
  )
}
