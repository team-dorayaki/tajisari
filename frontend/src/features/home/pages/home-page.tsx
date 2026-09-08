function HomePage() {
  return (
    <main className="flex min-h-dvh flex-col bg-[#f6f7f8]">
      <header className="bg-white px-5 pb-5 pt-[max(24px,env(safe-area-inset-top))]">
        <span className="text-xl font-extrabold tracking-[-0.04em]">타지살이</span>
      </header>
      <section className="flex flex-1 flex-col items-center justify-center px-6 text-center">
        <div
          className="grid size-16 place-items-center rounded-full bg-[var(--brand-soft)] text-3xl"
          aria-hidden="true"
        >
          🏠
        </div>
        <h1 className="mt-5 text-2xl font-bold tracking-[-0.04em]">
          홈 화면을 준비하고 있어요
        </h1>
        <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
          시작 화면의 이동을 확인하기 위한 임시 화면입니다.
        </p>
      </section>
    </main>
  )
}

export { HomePage }
