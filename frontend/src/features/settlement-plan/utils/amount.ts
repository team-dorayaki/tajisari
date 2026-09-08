function parseAmount(value: string) {
  return Number(value.replace(/\D/g, "")) || 0
}

export { parseAmount }
