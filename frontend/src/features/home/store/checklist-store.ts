import { create } from "zustand"

type ChecklistItem = {
  id: string
  label: string
  completed: boolean
}

type ChecklistState = {
  items: ChecklistItem[]
  addItem: () => void
  updateItem: (id: string, label: string) => void
  removeItem: (id: string) => void
  toggleItem: (id: string) => void
}

const storageKey = "tajisari.settlementChecklist"
const defaultItems: ChecklistItem[] = ["주소등록", "건강보험", "통신 개통", "공과금", "입주 확인"].map((label, index) => ({
  id: `default-${index}`,
  label,
  completed: false,
}))

function loadItems() {
  if (typeof window === "undefined") return defaultItems

  try {
    const savedItems = JSON.parse(window.localStorage.getItem(storageKey) ?? "null")
    return Array.isArray(savedItems) ? savedItems as ChecklistItem[] : defaultItems
  } catch {
    return defaultItems
  }
}

function saveItems(items: ChecklistItem[]) {
  window.localStorage.setItem(storageKey, JSON.stringify(items))
}

const useChecklistStore = create<ChecklistState>((set) => ({
  items: loadItems(),
  addItem: () => set((state) => {
    const items = [...state.items, { id: crypto.randomUUID(), label: "새 체크 항목", completed: false }]
    saveItems(items)
    return { items }
  }),
  updateItem: (id, label) => set((state) => {
    const items = state.items.map((item) => item.id === id ? { ...item, label } : item)
    saveItems(items)
    return { items }
  }),
  removeItem: (id) => set((state) => {
    const items = state.items.filter((item) => item.id !== id)
    saveItems(items)
    return { items }
  }),
  toggleItem: (id) => set((state) => {
    const items = state.items.map((item) => item.id === id ? { ...item, completed: !item.completed } : item)
    saveItems(items)
    return { items }
  }),
}))

export { useChecklistStore }
export type { ChecklistItem }
