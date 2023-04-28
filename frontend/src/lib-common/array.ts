export function swapElements<T>(arr: T[], index1: number, index2: number): T[] {
  const arrayCopy = [...arr]
  ;[arrayCopy[index1], arrayCopy[index2]] = [
    arrayCopy[index2],
    arrayCopy[index1]
  ]
  return arrayCopy
}
