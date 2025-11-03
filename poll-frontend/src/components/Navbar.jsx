import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useState } from 'react'
import {
  HomeOutlined,
  PlusCircleOutlined,
  UserOutlined,
  LogoutOutlined,
  LoginOutlined,
  UserAddOutlined,
  BarsOutlined
} from '@ant-design/icons'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false)

  const onLogout = () => {
    logout()
    setShowLogoutConfirm(false)
    navigate('/')
  }

  const openLogoutConfirm = () => {
    setShowLogoutConfirm(true)
  }

  const closeLogoutConfirm = () => {
    setShowLogoutConfirm(false)
  }

  return (
    <>
      <header className="bg-white border-b">
        <div className="max-w-4xl mx-auto p-4 flex items-center justify-between">
          <Link to="/" className="font-semibold text-lg flex items-center gap-2">
            <BarsOutlined />
            <span>Polls</span>
          </Link>
          <nav className="flex items-center gap-4">
            <Link to="/" className="flex items-center gap-1 hover:text-blue-600 transition-colors">
              <HomeOutlined />
              <span>Home</span>
            </Link>
            {user ? (
              <>
                <Link to="/create" className="flex items-center gap-1 hover:text-blue-600 transition-colors">
                  <PlusCircleOutlined />
                  <span>Create Poll</span>
                </Link>
                <Link to="/my-polls" className="flex items-center gap-1 hover:text-blue-600 transition-colors">
                  <UserOutlined />
                  <span>My Polls</span>
                </Link>
                <button
                  onClick={openLogoutConfirm}
                  className="flex items-center gap-1 px-3 py-1 rounded border hover:bg-gray-50 transition-colors"
                  title="Logout"
                >
                  <LogoutOutlined />
                  <span>Logout</span>
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="flex items-center gap-1 hover:text-blue-600 transition-colors">
                  <LoginOutlined />
                  <span>Login</span>
                </Link>
                <Link to="/register" className="flex items-center gap-1 px-3 py-1 rounded bg-gray-900 text-white hover:bg-gray-700 transition-colors">
                  <UserAddOutlined />
                  <span>Sign up</span>
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>

      {/* Logout Confirmation Modal */}
      {showLogoutConfirm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg p-6 max-w-sm w-full">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2 bg-red-100 rounded-full">
                <LogoutOutlined className="text-red-600 text-lg" />
              </div>
              <h3 className="text-lg font-semibold">Confirm Logout</h3>
            </div>
            <p className="text-gray-600 mb-6">Are you sure you want to log out?</p>
            <div className="flex gap-3 justify-end">
              <button
                onClick={closeLogoutConfirm}
                className="px-4 py-2 rounded border border-gray-300 hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={onLogout}
                className="px-4 py-2 rounded bg-red-600 text-white hover:bg-red-700 transition-colors flex items-center gap-2"
              >
                <LogoutOutlined />
                <span>Logout</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
