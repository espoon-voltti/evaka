// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useState } from 'react'

export function usePendingUserInput<T>(
  transform: (input: string) => T | null,
  initialValue?: string
): [string, React.Dispatch<React.SetStateAction<string>>, T | undefined] {
  const [input, setInput] = useState(initialValue ?? '')
  const [validated, setValidated] = useState<T>()

  useEffect(() => {
    const transformed = transform(input)

    if (transformed) {
      setValidated(transformed)
    } else {
      setValidated(undefined)
    }
  }, [input, transform])

  return [input, setInput, validated]
}
