export default function OptionBar({ label, value, total }) {
  const pct = total > 0 ? Math.round((value / total) * 100) : 0
  return (
    <div className="mb-3">
      <div className="flex justify-between text-sm mb-1">
        <span>{label}</span>
        <span>{pct}%</span>
      </div>
      <div className="h-2 bg-gray-200 rounded">
        <div className="h-2 rounded bg-gray-900" style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}
