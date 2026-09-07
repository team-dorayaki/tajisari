import { useState, type ReactNode } from "react"
import { ArrowLeft, ChevronRight, ExternalLink, ImageIcon, Link2, Plus, Sparkles, Upload, X } from "lucide-react"
import { useNavigate } from "react-router-dom"

type RegistrationLayoutProps = {
  children: ReactNode
  progress: number
}

function RegistrationLayout({ children, progress }: RegistrationLayoutProps) {
  const navigate = useNavigate()

  return (
    <main className="flex min-h-dvh flex-col bg-[#f5f6f7]">
      <header className="bg-white pt-[env(safe-area-inset-top)]">
        <div className="grid h-14 grid-cols-[44px_1fr_44px] items-center px-2">
          <button
            type="button"
            onClick={() => void navigate(-1)}
            className="grid size-11 place-items-center rounded-full"
            aria-label="뒤로 가기"
          >
            <ArrowLeft aria-hidden="true" className="size-5" />
          </button>
          <span className="text-center text-sm font-bold">매물 등록</span>
        </div>
        <div className="mx-4 h-1 overflow-hidden rounded-full bg-[#e4e9eb]">
          <div className="h-full rounded-full bg-[var(--brand)] transition-[width]" style={{ width: `${progress}%` }} />
        </div>
      </header>
      {children}
    </main>
  )
}

function Intro({ title, description }: { title: string; description: string }) {
  return (
    <div>
      <h1 className="text-[19px] font-bold tracking-[-0.025em]">{title}</h1>
      <p className="mt-1 text-xs leading-5 text-[var(--text-secondary)]">{description}</p>
    </div>
  )
}

function InfoCard({ children }: { children: ReactNode }) {
  return (
    <div className="flex items-start gap-3 px-2 py-1">
      <span className="inline-flex h-6 shrink-0 items-center rounded-md bg-[var(--brand)] px-2 text-[10px] font-bold text-white">
        AI
      </span>
      <div className="min-w-0 text-xs leading-5">{children}</div>
    </div>
  )
}

function TipNote({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="flex items-start gap-3 px-2 py-1">
      <span className="shrink-0 rounded-md bg-[var(--brand-soft)] px-2 py-1 text-[10px] font-bold text-[var(--brand)]">
        TIP
      </span>
      <div className="min-w-0 text-xs leading-5">
        <strong className="font-bold">{title}</strong>
        <p className="mt-1 text-[var(--text-secondary)]">{children}</p>
      </div>
    </div>
  )
}

function PropertyRegistrationPage() {
  const navigate = useNavigate()

  return (
    <RegistrationLayout progress={50}>
      <section className="flex-1 px-4 pt-8">
        <Intro
          title="매물을 어떻게 등록할까요?"
          description="이미지를 올리거나 매물 페이지 주소를 입력할 수 있어요."
        />

        <div className="mt-6 space-y-4">
          <button
            type="button"
            onClick={() => void navigate("/properties/new/image")}
            className="flex min-h-[132px] w-full items-center gap-4 rounded-xl bg-white p-5 text-left shadow-[0_1px_2px_rgb(15_23_42/0.04)]"
          >
            <span className="grid size-12 shrink-0 place-items-center rounded-full bg-[var(--brand-soft)] text-[var(--brand)]">
              <ImageIcon aria-hidden="true" className="size-6" />
            </span>
            <span className="flex-1">
              <span className="block text-sm font-bold">이미지로 등록</span>
              <span className="mt-1 block text-xs leading-5 text-[var(--text-secondary)]">
                매물 화면을 최대 3장까지<br />업로드해요.
              </span>
            </span>
            <span className="flex items-center text-xs font-bold text-[var(--brand)]">
              이미지 선택하기 <ChevronRight aria-hidden="true" className="size-4" />
            </span>
          </button>

          <button
            type="button"
            onClick={() => void navigate("/properties/new/url")}
            className="flex min-h-[132px] w-full items-center gap-4 rounded-xl bg-white p-5 text-left shadow-[0_1px_2px_rgb(15_23_42/0.04)]"
          >
            <span className="grid size-12 shrink-0 place-items-center rounded-full bg-[var(--brand-soft)] text-[var(--brand)]">
              <Link2 aria-hidden="true" className="size-6" />
            </span>
            <span className="flex-1">
              <span className="block text-sm font-bold">URL로 등록</span>
              <span className="mt-1 block text-xs leading-5 text-[var(--text-secondary)]">
                공개된 매물 페이지 주소를<br />붙여넣어요.
              </span>
            </span>
            <span className="flex items-center text-xs font-bold text-[var(--brand)]">
              URL 입력하기 <ChevronRight aria-hidden="true" className="size-4" />
            </span>
          </button>
        </div>

        <p className="mt-6 text-xs leading-5 text-[var(--text-secondary)]">
          두 방식 모두 AI 분석 후 비용을 직접 확인하고 수정할 수 있어요.
        </p>
      </section>
    </RegistrationLayout>
  )
}

function ImageRegistrationPage() {
  const navigate = useNavigate()
  const [images, setImages] = useState<string[]>([])

  const addImages = (files: FileList | null) => {
    if (!files) return

    Array.from(files)
      .slice(0, 3 - images.length)
      .forEach((file) => {
        const reader = new FileReader()
        reader.onload = () => {
          const result = reader.result

          if (typeof result === "string") {
            setImages((current) => [...current, result].slice(0, 3))
          }
        }
        reader.readAsDataURL(file)
      })
  }

  return (
    <RegistrationLayout progress={images.length > 0 ? 75 : 50}>
      <section className="flex-1 px-4 pt-8 pb-[calc(92px+env(safe-area-inset-bottom))]">
        <Intro
          title={images.length > 0 ? "등록한 이미지를 확인해주세요" : "매물 이미지를 등록해주세요"}
          description={
            images.length > 0
              ? "비용 정보가 잘 보이는지 확인한 뒤 분석을 시작해주세요."
              : "일본 부동산 앱의 매물 화면을 2~3장 올리면 AI가 비용 항목을 찾아드려요."
          }
        />

        {images.length === 0 ? (
          <label className="mt-6 flex h-48 cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-[var(--brand)] bg-white text-center">
            <Upload aria-hidden="true" className="size-7 text-[var(--brand)]" />
            <span className="mt-3 text-sm font-bold text-[var(--brand)]">이미지 업로드하기</span>
            <span className="mt-2 text-xs text-[var(--text-secondary)]">JPG, PNG · 최대 3장</span>
            <input
              type="file"
              accept="image/jpeg,image/png"
              multiple
              onChange={(event) => addImages(event.target.files)}
              className="sr-only"
            />
          </label>
        ) : (
          <div className="mt-6 rounded-xl bg-white p-4 shadow-[0_1px_2px_rgb(15_23_42/0.04)]">
            <div className="flex items-center justify-between">
              <h2 className="text-xs font-bold">등록 이미지</h2>
              <span className="text-xs font-bold text-[var(--brand)]">{images.length}/3</span>
            </div>
            <div className="mt-4 grid grid-cols-3 gap-2">
              {images.map((image, index) => (
                <div key={`${image}-${index}`} className="relative aspect-[3/4] overflow-hidden rounded-lg bg-[#eef1f2]">
                  <img src={image} alt={`등록 이미지 ${index + 1}`} className="size-full object-cover" />
                  <button
                    type="button"
                    onClick={() => setImages((current) => current.filter((_, itemIndex) => itemIndex !== index))}
                    className="absolute top-1 right-1 grid size-6 place-items-center rounded-full bg-[#30343a] text-white"
                    aria-label={`등록 이미지 ${index + 1} 삭제`}
                  >
                    <X aria-hidden="true" className="size-4" />
                  </button>
                </div>
              ))}
            </div>
            {images.length < 3 && (
              <label className="mt-3 flex min-h-11 cursor-pointer items-center justify-center gap-1 rounded-lg border border-[#dce1e4] text-xs font-semibold">
                <Plus aria-hidden="true" className="size-4" /> 이미지 다시 선택
                <input
                  type="file"
                  accept="image/jpeg,image/png"
                  multiple
                  onChange={(event) => addImages(event.target.files)}
                  className="sr-only"
                />
              </label>
            )}
          </div>
        )}

        <div className="mt-5">
          {images.length > 0 ? (
            <InfoCard>
              <strong className="font-bold">등록한 {images.length}장의 이미지를 분석해요</strong>
              <p className="mt-1 text-[var(--text-secondary)]">
                월세, 관리비, 보증금, 사례금 등 비용 항목을 추출한 뒤 직접 확인·수정할 수 있어요.
              </p>
            </InfoCard>
          ) : (
            <TipNote title="비용 정보가 보이게 올려주세요">
              월세, 관리비, 보증금, 사례금 등 비용 항목을 추출한 뒤 직접 확인·수정할 수 있어요.
            </TipNote>
          )}
        </div>
      </section>

      <div className="fixed inset-x-0 bottom-0 z-20 mx-auto w-full max-w-[430px] bg-gradient-to-t from-[#f5f6f7] from-70% to-transparent px-4 pt-6 pb-[calc(14px+env(safe-area-inset-bottom))]">
        <button
          type="button"
          disabled={images.length === 0}
          onClick={() => void navigate("/properties/analyzing")}
          className="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-[var(--brand)] text-sm font-bold text-white disabled:bg-[#dce1e7]"
        >
          {images.length > 0 && <Sparkles aria-hidden="true" className="size-4" />}
          {images.length > 0 ? "AI로 비용 분석하기" : "이미지를 먼저 등록해주세요"}
        </button>
      </div>
    </RegistrationLayout>
  )
}

function UrlRegistrationPage() {
  const navigate = useNavigate()
  const [url, setUrl] = useState("")
  const [confirmedUrl, setConfirmedUrl] = useState("")
  const confirmed = confirmedUrl !== ""

  return (
    <RegistrationLayout progress={confirmed ? 75 : 50}>
      <section className="flex-1 px-4 pt-8 pb-[calc(92px+env(safe-area-inset-bottom))]">
        <Intro
          title={confirmed ? "등록한 URL을 확인해주세요" : "매물 URL을 입력해주세요"}
          description={
            confirmed
              ? "주소가 맞는지 확인한 뒤 AI 분석을 시작해주세요."
              : "일본 부동산 사이트의 매물 주소를 붙여넣어주세요."
          }
        />

        {confirmed ? (
          <div className="mt-6 rounded-xl bg-white p-4 shadow-[0_1px_2px_rgb(15_23_42/0.04)]">
            <span className="inline-flex rounded-full bg-[var(--brand-soft)] px-3 py-1 text-[10px] font-bold text-[var(--brand)]">PROPERTY URL</span>
            <a
              href={confirmedUrl}
              target="_blank"
              rel="noreferrer"
              className="mt-4 flex items-start justify-between gap-3 rounded-md outline-none focus-visible:ring-2 focus-visible:ring-[var(--brand)]/30"
              aria-label="등록한 매물 페이지 새 탭에서 열기"
            >
              <div className="min-w-0">
                <p className="text-sm font-bold">등록한 매물 페이지</p>
                <p className="mt-2 truncate text-xs text-[var(--text-secondary)]">{confirmedUrl}</p>
              </div>
              <ExternalLink aria-hidden="true" className="size-5 shrink-0 text-[var(--brand)]" />
            </a>
            <button
              type="button"
              onClick={() => setConfirmedUrl("")}
              className="mt-4 text-xs font-semibold text-[var(--brand)]"
            >
              URL 수정하기
            </button>
          </div>
        ) : (
          <label className="mt-7 block">
            <span className="text-xs font-bold">매물 URL</span>
            <span className="mt-3 flex min-h-12 items-center gap-3 rounded-lg bg-white px-4 shadow-[0_1px_2px_rgb(15_23_42/0.04)] focus-within:ring-2 focus-within:ring-[var(--brand)]/20">
              <input
                type="url"
                value={url}
                onChange={(event) => setUrl(event.target.value)}
                placeholder="https://suumo.jp/..."
                className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-[#bbc2c9]"
              />
              <Link2 aria-hidden="true" className="size-5 text-[var(--brand)]" />
            </span>
          </label>
        )}

        <div className="mt-5">
          {confirmed ? (
            <InfoCard>
              <strong className="font-bold">URL의 매물 정보를 분석해요</strong>
              <p className="mt-1 text-[var(--text-secondary)]">
                월세, 관리비, 보증금과 초기비용을 찾아 확인 가능한 항목으로 정리해드려요.
              </p>
            </InfoCard>
          ) : (
            <TipNote title="페이지가 열리지 않는다면">
              로그인이 필요하거나 접근이 제한된 URL은 이미지로 등록해주세요.
            </TipNote>
          )}
        </div>
      </section>

      <div className="fixed inset-x-0 bottom-0 z-20 mx-auto w-full max-w-[430px] bg-gradient-to-t from-[#f5f6f7] from-70% to-transparent px-4 pt-6 pb-[calc(14px+env(safe-area-inset-bottom))]">
        <button
          type="button"
          disabled={!confirmed && url.trim() === ""}
          onClick={() => {
            if (confirmed) {
              void navigate("/properties/analyzing")
            } else {
              setConfirmedUrl(url.trim())
            }
          }}
          className="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-[var(--brand)] text-sm font-bold text-white disabled:bg-[#dce1e7]"
        >
          {confirmed && <Sparkles aria-hidden="true" className="size-4" />}
          {confirmed ? "AI로 비용 분석하기" : "URL 확인하기"}
        </button>
      </div>
    </RegistrationLayout>
  )
}

export { ImageRegistrationPage, PropertyRegistrationPage, UrlRegistrationPage }
