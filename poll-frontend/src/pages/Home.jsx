import { useEffect, useState } from 'react'
import api from '../api/client'
import PollCard from '../components/PollCard'
import { useAuth } from '../context/AuthContext'

export default function Home() {
  const [polls, setPolls] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const { user } = useAuth()

  useEffect(() => {
    async function load() {
      try {
        const { data } = await api.get('/api/polls')
        setPolls(Array.isArray(data) ? data : [])
      } catch (e) {
        setError('Failed to load polls')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  // Calculate polls that user hasn't voted on yet
  const unvotedPolls = user ? polls.filter(poll =>
    !poll.expiresAt || new Date(poll.expiresAt) >= new Date()
  ).filter(poll => !poll.hasVoted) : []

  const hasUnvotedPolls = unvotedPolls.length > 0

  if (loading) return (
    <div className="flex items-center justify-center py-12">
      <div className="text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 mx-auto mb-3"></div>
        <p className="text-gray-600">Loading polls...</p>
      </div>
    </div>
  )

  if (error) return (
    <div className="flex items-center justify-center py-12">
      <div className="text-center text-red-600">
        <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-3">
          <span className="text-red-600 text-xl font-bold">!</span>
        </div>
        <p className="text-lg font-medium mb-2">Failed to load polls</p>
        <p className="text-sm text-gray-600">Please try refreshing the page</p>
      </div>
    </div>
  )

  const totalVotesAll = polls.reduce((total, poll) => total + (poll.totalVotes || 0), 0)

  return (
    <div className="max-w-4xl mx-auto">
      {/* Header */}
      <div className="mb-8 text-center">

        <h1 className="text-3xl font-bold text-gray-900 mb-3">
          Community Polls
        </h1>
        <p className="text-gray-600">
          Discover and participate in polls created by the community
        </p>
      </div>

      {/* Unvoted Polls Notification */}
      {user && hasUnvotedPolls && (
        <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <div className="flex items-center gap-3">
            <div className="flex-shrink-0 w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
              <svg className="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <div className="flex-1">
              <p className="text-blue-800 font-medium">
                You have {unvotedPolls.length} active poll{unvotedPolls.length > 1 ? 's' : ''} to vote on!
              </p>
              <p className="text-blue-600 text-sm">
                Don't miss your chance to participate in these polls before they expire.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Polls Grid */}
      {polls.length > 0 ? (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {polls.map((p) => (
            <PollCard key={p.id} poll={p} />
          ))}
        </div>
      ) : (
        <div className="text-center py-12 bg-white rounded-xl border border-dashed">
          <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">No polls yet</h3>
          <p className="text-gray-600 mb-4">Be the first to create a poll!</p>
          <button
            onClick={() => window.location.href = '/create'}
            className="flex items-center gap-2 px-6 py-2 bg-gray-900 text-white rounded-lg hover:bg-gray-800 transition-colors mx-auto"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
            Create Poll
          </button>
        </div>
      )}

      {/* Stats Footer */}
      {polls.length > 0 && (
        <div className="mt-8 text-center">
          <div className="inline-flex items-center gap-6 px-6 py-3 bg-gray-50 rounded-lg">
            <div className="text-center">
              <div className="flex items-center justify-center gap-2 mb-1">
                <svg className="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
                <div className="text-2xl font-bold text-gray-900">{polls.length}</div>
              </div>
              <div className="text-sm text-gray-600">Total Polls</div>
            </div>
            <div className="h-8 w-px bg-gray-300"></div>
            <div className="text-center">
              <div className="flex items-center justify-center gap-2 mb-1">
                <svg className="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <div className="text-2xl font-bold text-gray-900">
                  {totalVotesAll}
                </div>
              </div>
              <div className="text-sm text-gray-600">Total Votes</div>
            </div>
            {user && (
              <>
                <div className="h-8 w-px bg-gray-300"></div>
                <div className="text-center">
                  <div className="flex items-center justify-center gap-2 mb-1">
                    <svg className="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <div className="text-2xl font-bold text-gray-900">{unvotedPolls.length}</div>
                  </div>
                  <div className="text-sm text-gray-600">Your Unvoted</div>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
