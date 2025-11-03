import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import api from '../api/client';
import OptionBar from '../components/OptionBar';
import { useAuth } from '../context/AuthContext';
import {
  BarChartOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  LockOutlined,
  CheckOutlined
} from '@ant-design/icons';

export default function PollDetail() {
  const { id } = useParams();
  const [poll, setPoll] = useState(null);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { user } = useAuth();

  const totalVotes = useMemo(
    () => (poll?.totalVotes ?? (poll?.options?.reduce((s, o) => s + (o.votes || 0), 0) || 0)),
    [poll]
  );

  const expired = useMemo(
    () => (poll?.expiresAt ? new Date(poll.expiresAt) < new Date() : false),
    [poll]
  );

  useEffect(() => {
    async function load() {
      try {
        const { data } = await api.get(`/api/polls/${id}`);
        setPoll(data);
        if (data?.hasVoted && data?.userOptionId) {
          setSelected(data.userOptionId);
        }
      } catch {
        setError('Failed to load poll');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  const submitVote = async () => {
    if (!selected) return;
    try {
      await api.post(`/api/polls/${id}/votes`, { optionId: selected });
      const { data } = await api.get(`/api/polls/${id}`);
      setPoll(data);
    } catch (err) {
      if (err?.response?.status === 409) {
        setPoll(p => p ? { ...p, hasVoted: true, userOptionId: p.userOptionId ?? selected } : p);
      } else if (err?.response?.status === 409 || err?.response?.status === 403) {
      } else {
        setError('Failed to submit vote');
      }
    }
  };

  if (loading) return (
    <div className="flex items-center justify-center py-12">
      <div className="text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 mx-auto mb-2"></div>
        <p className="text-gray-600">Loading poll...</p>
      </div>
    </div>
  );

  if (error) return (
    <div className="flex items-center justify-center py-12">
      <div className="text-center text-red-600">
        <div className="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-2">
          <ExclamationCircleOutlined className="text-red-600 text-sm" />
        </div>
        <p>{error}</p>
      </div>
    </div>
  );

  if (!poll) return null;

  const alreadyVoted = !!poll.hasVoted;

  return (
    <div className="max-w-2xl mx-auto bg-white rounded-xl border shadow-sm p-6">
      {/* Poll Header */}
      <div className="flex items-start justify-between mb-6">
        <div className="flex-1">
          <h1 className="text-2xl font-bold text-gray-900 mb-3">{poll.question}</h1>

          {/* Poll Metadata */}
          <div className="flex items-center gap-4 text-sm text-gray-600">
            <div className="flex items-center gap-1">
              <BarChartOutlined className="text-gray-400 text-base" />
              <span>{totalVotes} {totalVotes === 1 ? 'vote' : 'votes'}</span>
            </div>

            {poll.expiresAt && (
              <div className="flex items-center gap-1">
                <ClockCircleOutlined className="text-gray-400 text-base" />
                <span>
                  {expired ? 'Expired' : 'Expires'} {new Date(poll.expiresAt).toLocaleDateString()}
                </span>
              </div>
            )}
          </div>
        </div>

        {expired && (
          <span className="flex items-center gap-1 px-3 py-1 bg-red-100 text-red-700 rounded-full text-sm font-medium">
            <ExclamationCircleOutlined className="text-xs" />
            Expired
          </span>
        )}

        {alreadyVoted && !expired && (
          <span className="flex items-center gap-1 px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">
            <CheckCircleOutlined className="text-xs" />
            Voted
          </span>
        )}
      </div>

      {/* Voting Options */}
      <div className="space-y-3 mb-6">
        {poll.options?.map((opt) => {
          const isSelected = selected === opt.id;
          const percentage = totalVotes > 0 ? ((opt.votes || 0) / totalVotes) * 100 : 0;

          return (
            <label
              key={opt.id}
              className={`block p-4 rounded-lg border-2 cursor-pointer transition-all ${
                isSelected
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
              } ${
                (expired || !user || alreadyVoted) ? 'opacity-80 cursor-not-allowed' : ''
              }`}
            >
              <div className="flex items-center gap-3">
                <div className={`flex items-center justify-center w-6 h-6 rounded-full border ${
                  isSelected ? 'border-blue-500 bg-blue-500' : 'border-gray-400'
                }`}>
                  {isSelected && <div className="w-2 h-2 rounded-full bg-white"></div>}
                </div>
                <span className="font-medium flex-1">{opt.text}</span>
                {(alreadyVoted || expired) && (
                  <span className="text-sm text-gray-600 font-medium">
                    {opt.votes || 0} votes ({percentage.toFixed(1)}%)
                  </span>
                )}
              </div>

              {(alreadyVoted || expired) && (
                <div className="mt-3">
                  <OptionBar
                    value={opt.votes || 0}
                    total={totalVotes}
                    showPercentage={true}
                  />
                </div>
              )}

              <input
                type="radio"
                name="option"
                value={opt.id}
                checked={isSelected}
                onChange={() => setSelected(opt.id)}
                disabled={expired || !user || alreadyVoted}
                className="hidden"
              />
            </label>
          );
        })}
      </div>

      {/* Action Section */}
      <div className="flex items-center justify-between pt-4 border-t">
        <div className="flex items-center gap-3">
          <button
            disabled={!user || expired || !selected || alreadyVoted}
            onClick={submitVote}
            className="flex items-center gap-2 px-6 py-3 rounded-lg bg-gray-900 text-white hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {alreadyVoted ? 'Already Voted' : 'Cast Vote'}
          </button>

          {!user && (
            <div className="flex items-center gap-2 text-orange-600 text-sm">
              <LockOutlined />
              <span>Login to vote</span>
            </div>
          )}

          {selected && !alreadyVoted && user && !expired && (
            <span className="text-sm text-gray-600">
              Selected: <strong>{poll.options?.find(opt => opt.id === selected)?.text}</strong>
            </span>
          )}
        </div>

        {alreadyVoted && (
          <div className="text-sm text-green-600 flex items-center gap-1">
            <CheckOutlined />
            <span>Your vote has been recorded</span>
          </div>
        )}
      </div>
    </div>
  );
}
