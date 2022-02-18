// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useRef } from 'react'

function hasParentElement(target: EventTarget | null): target is HTMLElement {
  return !!target && 'parentElement' in target
}

export default function useCloseOnOutsideClick<T extends HTMLElement>(
  close: () => void
) {
  const containerRef = useRef<T | null>(null)

  useEffect(() => {
    function onClick({ target }: { target: EventTarget | null }) {
      if (target === containerRef.current) {
        return
      }

      if (hasParentElement(target)) {
        let el = target
        while (el.parentElement) {
          el = el.parentElement
          if (el === containerRef.current) {
            return
          }
        }
        close()
      }
    }

    document.addEventListener('click', onClick, false)
    return () => document.removeEventListener('click', onClick, false)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return containerRef
}
