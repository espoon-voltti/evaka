// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return ref
}
