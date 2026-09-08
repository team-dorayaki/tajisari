import { Link } from "react-router-dom"

const properties = ["신주쿠 원룸 A", "나카노 원룸 B"]

function PriorityPropertiesCard() {
  return (
    <section className="rounded-[20px] bg-white p-5" aria-labelledby="priority-properties-title">
      <div className="flex items-start justify-between">
        <div>
          <h2 id="priority-properties-title" className="text-base font-bold">
            내 우선순위 매물
          </h2>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">비교 후 선택한 1·2순위예요</p>
        </div>
        <Link to="/properties/compare" className="text-xs font-semibold text-[var(--brand)]">
          순위 변경 ›
        </Link>
      </div>

      <ol className="mt-4 border-t border-[#edf0f2]">
        {properties.map((property, index) => (
          <li key={property} className="flex items-center gap-4 border-b border-[#edf0f2] py-3 last:border-0 last:pb-0">
            <span className="w-4 text-sm font-bold text-[#6e7781]">{index + 1}</span>
            <strong className="text-sm">{property}</strong>
          </li>
        ))}
      </ol>
    </section>
  )
}

export { PriorityPropertiesCard }
