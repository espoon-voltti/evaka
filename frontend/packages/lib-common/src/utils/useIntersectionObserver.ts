import { useEffect, useRef, useState } from 'react'

export default function useIntersectionObserver<E extends Element>(
  onEnter: () => void
) {
  const ref = useRef<E>(null)

  const [observer] = useState(
    new IntersectionObserver((entries) => {
      if (entries[0] && entries[0].isIntersecting) {
        onEnter()
      }
    })
  )

  useEffect(() => {
    if (ref.current) {
      observer.observe(ref.current)
    }
    return () => observer.disconnect()
  }, [])

  return ref
}
