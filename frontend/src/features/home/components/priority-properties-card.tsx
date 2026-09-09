import { Link } from "react-router-dom"
import { useQuery } from "@tanstack/react-query"

import { fetchProperties } from "@/features/properties/api/properties-api"

function PriorityPropertiesCard() {
  const { data: properties = [], isLoading } = useQuery({
    queryKey: ["priority-properties"],
    queryFn: fetchProperties,
  })
  const priorityProperties = properties
    .filter((property) => property.priorityRank !== null)
    .sort((left, right) => (left.priorityRank ?? Number.POSITIVE_INFINITY) - (right.priorityRank ?? Number.POSITIVE_INFINITY))
  const priorityChangeHref = priorityProperties.length >= 2
    ? `/properties/compare?ids=${priorityProperties.map((property) => property.id).join(",")}&priority=true`
    : "/properties?tab=compare"

  return (
    <section className="rounded-[20px] bg-white p-5" aria-labelledby="priority-properties-title">
      <div className="flex items-start justify-between">
        <div>
          <h2 id="priority-properties-title" className="text-base font-bold">
            내 우선순위 매물
          </h2>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">비교 후 선택한 1·2순위예요</p>
        </div>
        <Link to={priorityChangeHref} className="text-xs font-semibold text-[var(--brand)]">
          순위 변경 ›
        </Link>
      </div>

      <ol className="mt-4 border-t border-[#edf0f2]">
        {isLoading && <li className="py-3 text-sm text-[var(--text-secondary)]">우선순위 매물을 불러오는 중이에요.</li>}
        {!isLoading && priorityProperties.length === 0 && <li className="py-3 text-sm text-[var(--text-secondary)]">저장한 우선순위 매물이 없어요.</li>}
        {priorityProperties.map((property) => (
          <li key={property.id} className="flex items-center gap-4 border-b border-[#edf0f2] py-3 last:border-0 last:pb-0">
            <span className="w-4 text-sm font-bold text-[#6e7781]">{property.priorityRank}</span>
            <Link to={`/properties/${property.id}`} className="min-w-0 flex-1 truncate text-sm font-bold">{property.name}</Link>
          </li>
        ))}
      </ol>
    </section>
  )
}

export { PriorityPropertiesCard }
