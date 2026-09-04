import { Link } from "react-router-dom"

const checklistItems = ["주소등록", "건강보험", "통신 개통", "공과금", "입주 확인"]

type SettlementChecklistCardProps = {
  completed?: boolean
}

function SettlementChecklistCard({ completed = false }: SettlementChecklistCardProps) {
  return (
    <section className="rounded-[20px] bg-white p-5" aria-labelledby="settlement-checklist-title">
      <div className="flex items-center justify-between">
        <h2 id="settlement-checklist-title" className="text-base font-bold">
          정착 체크리스트
        </h2>
        <Link to="/checklist" className="text-xs font-semibold text-[var(--brand)]">
          전체보기 ›
        </Link>
      </div>

      <ol className="mt-4 grid grid-cols-5 gap-1">
        {checklistItems.map((item, index) => {
          const isCompleted = completed && index === 0

          return (
            <li key={item} className="flex min-w-0 flex-col items-center gap-2 text-center">
              <span
                aria-hidden="true"
                className={
                  isCompleted
                    ? "h-6 w-10 rounded-[50%] bg-[#cc7b13] shadow-[inset_0_-5px_0_#9b5210]"
                    : "h-6 w-10 rounded-[50%] bg-[#dfe3e6] shadow-[inset_0_-5px_0_#cfd5d9]"
                }
              />
              <span className={isCompleted ? "text-[10px] font-semibold text-[var(--brand)]" : "text-[10px] text-[#9da5ad]"}>
                {item}
              </span>
            </li>
          )
        })}
      </ol>
    </section>
  )
}

export { SettlementChecklistCard }
