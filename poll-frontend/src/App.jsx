import { Routes, Route, Navigate } from 'react-router-dom'
import Navbar from './components/Navbar'
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import PollDetail from './pages/PollDetail'
import CreatePoll from './pages/CreatePoll'
import ProtectedRoute from './components/ProtectedRoute'
import MyPolls from './pages/MyPolls'

export default function App() {
  return (
    <div className="min-h-screen bg-gray-50 text-gray-900">
      <Navbar />
      <main className="max-w-4xl mx-auto p-4">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/polls/:id" element={<PollDetail />} />

          <Route
            path="/create"
            element={
              <ProtectedRoute>
                <CreatePoll />
              </ProtectedRoute>
            }
          />

          <Route
            path="/my-polls"
            element={
              <ProtectedRoute>
                <MyPolls />
              </ProtectedRoute>
              }
          />


          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}
