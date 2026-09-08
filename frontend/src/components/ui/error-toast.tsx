import { useEffect } from "react"

type ErrorToastProps = {
  message: string
  onDismiss: () => void
  duration?: number
}

function ErrorToast({ message, onDismiss, duration = 3000 }: ErrorToastProps) {
  useEffect(() => {
    const timeoutId = globalThis.setTimeout(onDismiss, duration)
    return () => globalThis.clearTimeout(timeoutId)
  }, [duration, onDismiss])

  return (
    <div
      role="alert"
      aria-live="assertive"
      className="absolute right-4 bottom-[calc(100%+12px)] left-4 flex min-h-16 items-center gap-3 rounded-xl bg-[#15171c] px-4 py-3 text-white shadow-lg"
    >
      <span aria-hidden="true" className="grid size-8 shrink-0 place-items-center rounded-full bg-[#ff705d] text-lg font-bold">
        !
      </span>
      <p className="text-sm font-semibold">{message}</p>
    </div>
  )
}

export { ErrorToast }
