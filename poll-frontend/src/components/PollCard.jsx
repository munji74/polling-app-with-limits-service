import { Link } from 'react-router-dom'

export default function PollCard({ poll }) {
  const totalVotes = poll.options?.reduce((sum, o) => sum + (o.votes || 0), 0) || 0
  const expired = poll.expiresAt ? new Date(poll.expiresAt) < new Date() : false

  return (
    <div className="p-4 bg-white rounded-lg border">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">{poll.question}</h3>
        {expired && <span className="text-xs px-2 py-1 bg-red-100 text-red-700 rounded">Expired</span>}
      </div>
      <p className="text-sm text-gray-500 mt-1">Total votes: {totalVotes}</p>
      <Link to={`/polls/${poll.id}`} className="inline-block mt-3 text-blue-600 hover:underline">View</Link>
    </div>
  )
}
