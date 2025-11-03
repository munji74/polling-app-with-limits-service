import { useEffect, useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client'
import {
  PlusOutlined,
  EyeOutlined,
  DeleteOutlined,
  ClockCircleOutlined,
  BarChartOutlined,
  CalendarOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  FileTextOutlined,
  RocketOutlined
} from '@ant-design/icons'

export default function MyPolls() {
  const [polls, setPolls] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Helper: try multiple endpoints until one works
  async function fetchMyPolls() {
    const endpoints = [
      '/api/polls/mine',
      '/api/users/me/polls',
      '/api/polls?createdBy=me',
    ]
    let lastErr
    for (const ep of endpoints) {
      try {
        const { data } = await api.get(ep)
        return Array.isArray(data) ? data : (data?.items ?? data ?? [])
      } catch (e) {
        lastErr = e
      }
    }
    throw lastErr
  }

  useEffect(() => {
    (async () => {
      try {
        const res = await fetchMyPolls()
        setPolls(res)
      } catch (_) {
        setError('Failed to load your polls')
      } finally {
        setLoading(false)
      }
    })()
  }, [])

  const active = useMemo(
    () => polls.filter(p => !p.expiresAt || new Date(p.expiresAt) >= new Date()),
    [polls]
  )
  const expired = useMemo(
    () => polls.filter(p => p.expiresAt && new Date(p.expiresAt) < new Date()),
    [polls]
  )

  if (loading) return (
    <div className="flex items-center justify-center py-12">
      <div className="text-center">
        <ClockCircleOutlined className="text-4xl text-gray-400 mb-4" />
        <p className="text-gray-600">Loading your polls...</p>
      </div>
    </div>
  )

  if (error) return (
    <div className="flex items-center justify-center py-12">
      <div className="text-center text-red-600">
        <ExclamationCircleOutlined className="text-4xl mb-4" />
        <p>{error}</p>
      </div>
    </div>
  )

  return (
    <div className="max-w-4xl mx-auto space-y-8 p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-3">
            <BarChartOutlined className="text-blue-600" />
            My Polls
          </h1>
          <p className="text-gray-500 mt-1">Manage and view your created polls</p>
        </div>
        <Link
          to="/create"
          className="flex items-center gap-2 px-4 py-3 rounded-lg bg-gray-900 text-white hover:bg-gray-700 transition-colors"
        >
          <PlusOutlined />
          <span>New Poll</span>
        </Link>
      </div>

      {/* Active Polls Section */}
      <section className="bg-white rounded-xl border p-6">
        <div className="flex items-center gap-3 mb-6">
          <CheckCircleOutlined className="text-green-600 text-xl" />
          <h2 className="text-lg font-semibold text-gray-900">Active Polls</h2>
          <span className="px-2 py-1 bg-green-100 text-green-700 text-xs rounded-full font-medium">
            {active.length} polls
          </span>
        </div>
        <div className="grid gap-4">
          {active.length ? (
            active.map(poll => (
              <MyPollRow key={poll.id} poll={poll} isActive={true} />
            ))
          ) : (
            <div className="text-center py-8 text-gray-500">
              <RocketOutlined className="text-4xl mb-3 text-gray-300" />
              <p>No active polls yet</p>
              <Link to="/create" className="text-blue-600 hover:underline text-sm flex items-center gap-1 justify-center">
                <PlusOutlined />
                Create your first poll
              </Link>
            </div>
          )}
        </div>
      </section>

      {/* Expired Polls Section */}
      <section className="bg-white rounded-xl border p-6">
        <div className="flex items-center gap-3 mb-6">
          <ClockCircleOutlined className="text-gray-400 text-xl" />
          <h2 className="text-lg font-semibold text-gray-900">Expired Polls</h2>
          <span className="px-2 py-1 bg-gray-100 text-gray-700 text-xs rounded-full font-medium">
            {expired.length} polls
          </span>
        </div>
        <div className="grid gap-4">
          {expired.length ? (
            expired.map(poll => (
              <MyPollRow key={poll.id} poll={poll} isActive={false} />
            ))
          ) : (
            <div className="text-center py-8 text-gray-500">
              <FileTextOutlined className="text-4xl mb-3 text-gray-300" />
              <p>No expired polls</p>
            </div>
          )}
        </div>
      </section>
    </div>
  )
}

function MyPollRow({ poll, isActive }) {
  const totalVotes = (poll.options || []).reduce((s, o) => s + (o.votes || 0), 0)

  return (
    <div className={`p-5 rounded-lg border-l-4 ${
      isActive
        ? 'border-l-green-500 bg-green-50 hover:bg-green-100'
        : 'border-l-gray-400 bg-gray-50 hover:bg-gray-100'
    } transition-colors`}>
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <h3 className="text-base font-semibold text-gray-900 mb-2">{poll.question}</h3>
          <div className="flex items-center gap-4 text-sm text-gray-600">
            <div className="flex items-center gap-1">
              <BarChartOutlined className="text-gray-400" />
              <span>{totalVotes} votes</span>
            </div>
            {poll.expiresAt && (
              <div className="flex items-center gap-1">
                <CalendarOutlined className="text-gray-400" />
                <span>Expires: {new Date(poll.expiresAt).toLocaleDateString()}</span>
              </div>
            )}
            {!isActive && (
              <span className="flex items-center gap-1 px-2 py-1 bg-red-100 text-red-700 text-xs rounded-full">
                <ExclamationCircleOutlined className="text-xs" />
                Expired
              </span>
            )}
          </div>
        </div>
        <div className="flex items-center gap-3">
          <Link
            to={`/polls/${poll.id}`}
            className="flex items-center gap-2 px-3 py-2 rounded-lg border border-gray-300 hover:border-blue-500 hover:text-blue-600 transition-colors text-sm"
          >
            <EyeOutlined />
            <span>View</span>
          </Link>
          {/* Optional: uncomment if your backend supports deletion
          <button
            onClick={() => handleDelete(poll.id)}
            className="flex items-center gap-2 px-3 py-2 rounded-lg border border-gray-300 hover:border-red-500 hover:text-red-600 transition-colors text-sm"
          >
            <DeleteOutlined />
            <span>Delete</span>
          </button>
          */}
        </div>
      </div>
    </div>
  )
}

/* Example delete handler if your API supports it:
async function handleDelete(id) {
  if (!confirm('Delete this poll?')) return
  await api.delete(`/api/polls/${id}`)
  // refetch or optimistically update state
}
*/
