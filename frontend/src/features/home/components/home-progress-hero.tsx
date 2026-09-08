import { Link } from "react-router-dom"

import objectAssetsUrl from "@/assets/home-object-assets.png"
import type { HomeStageContent } from "@/features/home/home-data"

type HomeProgressHeroProps = {
  content: HomeStageContent
  completed: boolean
}

function HomeProgressHero({ content, completed }: HomeProgressHeroProps) {
  return (
    <section
      className={
        completed
          ? "relative min-h-[230px] overflow-hidden bg-[#879b96] px-5 py-7 text-white"
          : "relative min-h-[230px] overflow-hidden bg-[#177568] px-5 py-7 text-white"
      }
    >
      <div className="relative z-10 max-w-[74%]">
        <p className="text-sm font-bold tracking-wide text-white/90">{content.label}</p>
        <h1 className="mt-2 text-[25px] font-bold leading-[1.25] tracking-[-0.04em]">
          {content.title}
        </h1>
        <p className="mt-2 text-sm text-white/80">{content.description}</p>

        {content.action && content.actionHref && (
          <Link
            to={content.actionHref}
            className="mt-6 inline-flex min-h-11 min-w-36 items-center justify-center rounded-lg bg-[#00b59c] px-5 text-sm font-bold text-white transition active:translate-y-px active:bg-[#00a68f] focus-visible:ring-3 focus-visible:ring-white/40"
          >
            {content.action}
          </Link>
        )}
      </div>

      <span
        aria-hidden="true"
        className={`absolute overflow-hidden ${
          content.illustration === 2
            ? "right-[-4px] bottom-4 h-[156px] w-[220px]"
            : [0, 1, 4].includes(content.illustration)
              ? completed
                ? "right-2 bottom-3 h-[104px] w-[156px] rotate-[-8deg]"
                : "right-2 bottom-7 h-[104px] w-[156px]"
              : content.illustration === 3
                ? "right-[-8px] bottom-7 h-[104px] w-[156px]"
                : "right-[-2px] bottom-7 h-[104px] w-[156px]"
        }`}
      >
        <img
          src={objectAssetsUrl}
          alt=""
          className={`${content.illustration === 2 ? "h-[156px] w-[1170px]" : "h-[104px] w-[780px]"} max-w-none object-contain`}
          style={{ transform: `translateX(-${content.illustration === 2 ? 430 : content.illustration * 156}px)` }}
        />
      </span>

      {content.progress !== undefined && (
        <div className="absolute inset-x-5 bottom-7 h-1.5 overflow-hidden rounded-full bg-white/85">
          <span
            className="block h-full rounded-full bg-[#a7c2bd]"
            style={{ width: `${content.progress}%` }}
          />
        </div>
      )}
    </section>
  )
}

export { HomeProgressHero }
