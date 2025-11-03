import { useState } from 'react'
import api from '../api/client'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext' // Import useAuth

// Import Ant Design icons
import { PlusOutlined, CloseOutlined, CalendarOutlined, UserOutlined } from '@ant-design/icons'

export default function CreatePoll() {
  const [question, setQuestion] = useState('')
  const [options, setOptions] = useState(['', ''])
  const [expiresAt, setExpiresAt] = useState('')
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const navigate = useNavigate()
  const { user } = useAuth() // Get user from auth context

  const updateOption = (i, val) => {
    setOptions((prev) => prev.map((o, idx) => (idx === i ? val : o)))
  }

  const addOption = () => setOptions((prev) => [...prev, ''])
  const removeOption = (i) =>
    setOptions((prev) => prev.filter((_, idx) => idx !== i))

  const onSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body = { title: question, options: options.filter(Boolean) }
      if (expiresAt) body.expiresAt = new Date(expiresAt).toISOString()

      const { data, headers } = await api.post('/api/polls', body)

      // Prefer Location header if present, else fallback to response body id
      const location = headers['location'] || `/api/polls/${data.id}`
      const id = String(location).split('/').pop()
      navigate(`/polls/${id}`)
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to create poll')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-xl mx-auto bg-white border rounded-lg p-6 shadow-sm">
      {/* User Welcome Card */}
      {user && (
        <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg flex items-center gap-3">
          <div className="p-2 bg-blue-100 rounded-full">
            <UserOutlined className="text-blue-600 text-lg" />
          </div>
          <div>
            <p className="text-sm text-blue-600 font-medium">Welcome back!</p>
            <p className="text-lg font-semibold text-gray-800">
              {user.name || user.email}
            </p>
          </div>
        </div>
      )}

      <h1 className="text-2xl font-bold mb-6 text-center text-gray-800">Create a New Poll</h1>

      <form onSubmit={onSubmit} className="space-y-6">
        {/* Question Field */}
        <div>
          <label className="block text-sm font-medium mb-2 text-gray-700">Poll Question</label>
          <input
            className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition duration-200"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="What should we build next?"
            required
          />
        </div>

        {/* Options Section */}
        <div>
          <label className="block text-sm font-medium mb-3 text-gray-700">Poll Options</label>
          <div className="space-y-3">
            {options.map((opt, i) => (
              <div key={i} className="flex gap-3 items-center">
                <div className="flex-1 relative">
                  <input
                    className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition duration-200"
                    value={opt}
                    onChange={(e) => updateOption(i, e.target.value)}
                    placeholder={`Option ${i + 1}`}
                    required
                  />
                </div>
                {options.length > 2 && (
                  <button
                    type="button"
                    className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition duration-200"
                    onClick={() => removeOption(i)}
                    title="Remove option"
                  >
                    <CloseOutlined className="text-lg" />
                  </button>
                )}
              </div>
            ))}
            <button
              type="button"
              className="flex items-center gap-2 px-4 py-2.5 border-2 border-dashed border-gray-300 rounded-lg text-gray-600 hover:border-blue-500 hover:text-blue-500 transition duration-200 w-full justify-center"
              onClick={addOption}
            >
              <PlusOutlined />
              <span>Add another option</span>
            </button>
          </div>
          <p className="text-xs text-gray-500 mt-2">Minimum 2 options required</p>
        </div>

        {/* Expiration Date */}
        <div>
          <label className="block text-sm font-medium mb-2 text-gray-700">
            <CalendarOutlined className="mr-2" />
            Expiration Date
          </label>
          <input
            type="datetime-local"
            className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition duration-200"
            value={expiresAt}
            onChange={(e) => setExpiresAt(e.target.value)}
          />
          <p className="text-xs text-gray-500 mt-1">Leave empty if the poll should never expire</p>
        </div>

        {/* Error Display */}
        {error && (
          <div className="text-red-600 text-sm bg-red-50 p-3 rounded-lg border border-red-200">
            {error}
          </div>
        )}

        {/* Submit Button */}
        <button
          disabled={saving}
          className="w-full bg-black text-white rounded-lg py-3 hover:bg-gray-600 disabled:bg-gray-400 disabled:cursor-not-allowed transition duration-200 font-medium text-lg"
        >
          {saving ? 'Creating Poll…' : 'Create Poll'}
        </button>
      </form>
    </div>
  )
}
