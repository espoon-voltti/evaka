// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { qrCodeSvg } from './QrCode'

describe('qrCodeSvg', () => {
  it('works', () => {
    expect(qrCodeSvg('https://example.com/qr-code')).toEqual({
      size: 27,
      path:
        'M1 1h1v1h-1zM2 1h1v1h-1zM3 1h1v1h-1zM4 1h1v1h-1zM5 1h1v1h-1zM6 1h1v1h-1z' +
        'M7 1h1v1h-1zM9 1h1v1h-1zM12 1h1v1h-1zM13 1h1v1h-1zM15 1h1v1h-1zM19 1h1v1h-1z' +
        'M20 1h1v1h-1zM21 1h1v1h-1zM22 1h1v1h-1zM23 1h1v1h-1zM24 1h1v1h-1zM25 1h1v1h-1z' +
        'M1 2h1v1h-1zM7 2h1v1h-1zM9 2h1v1h-1zM10 2h1v1h-1zM11 2h1v1h-1zM13 2h1v1h-1z' +
        'M14 2h1v1h-1zM15 2h1v1h-1zM17 2h1v1h-1zM19 2h1v1h-1zM25 2h1v1h-1zM1 3h1v1h-1z' +
        'M3 3h1v1h-1zM4 3h1v1h-1zM5 3h1v1h-1zM7 3h1v1h-1zM11 3h1v1h-1zM12 3h1v1h-1z' +
        'M13 3h1v1h-1zM14 3h1v1h-1zM17 3h1v1h-1zM19 3h1v1h-1zM21 3h1v1h-1zM22 3h1v1h-1z' +
        'M23 3h1v1h-1zM25 3h1v1h-1zM1 4h1v1h-1zM3 4h1v1h-1zM4 4h1v1h-1zM5 4h1v1h-1z' +
        'M7 4h1v1h-1zM11 4h1v1h-1zM14 4h1v1h-1zM15 4h1v1h-1zM19 4h1v1h-1zM21 4h1v1h-1z' +
        'M22 4h1v1h-1zM23 4h1v1h-1zM25 4h1v1h-1zM1 5h1v1h-1zM3 5h1v1h-1zM4 5h1v1h-1z' +
        'M5 5h1v1h-1zM7 5h1v1h-1zM9 5h1v1h-1zM10 5h1v1h-1zM11 5h1v1h-1zM12 5h1v1h-1z' +
        'M15 5h1v1h-1zM19 5h1v1h-1zM21 5h1v1h-1zM22 5h1v1h-1zM23 5h1v1h-1zM25 5h1v1h-1z' +
        'M1 6h1v1h-1zM7 6h1v1h-1zM9 6h1v1h-1zM12 6h1v1h-1zM15 6h1v1h-1zM19 6h1v1h-1z' +
        'M25 6h1v1h-1zM1 7h1v1h-1zM2 7h1v1h-1zM3 7h1v1h-1zM4 7h1v1h-1zM5 7h1v1h-1z' +
        'M6 7h1v1h-1zM7 7h1v1h-1zM9 7h1v1h-1zM11 7h1v1h-1zM13 7h1v1h-1zM15 7h1v1h-1z' +
        'M17 7h1v1h-1zM19 7h1v1h-1zM20 7h1v1h-1zM21 7h1v1h-1zM22 7h1v1h-1zM23 7h1v1h-1z' +
        'M24 7h1v1h-1zM25 7h1v1h-1zM9 8h1v1h-1zM11 8h1v1h-1zM12 8h1v1h-1zM13 8h1v1h-1z' +
        'M15 8h1v1h-1zM16 8h1v1h-1zM17 8h1v1h-1zM1 9h1v1h-1zM2 9h1v1h-1zM3 9h1v1h-1z' +
        'M6 9h1v1h-1zM7 9h1v1h-1zM9 9h1v1h-1zM10 9h1v1h-1zM11 9h1v1h-1zM13 9h1v1h-1z' +
        'M15 9h1v1h-1zM17 9h1v1h-1zM18 9h1v1h-1zM19 9h1v1h-1zM20 9h1v1h-1zM21 9h1v1h-1z' +
        'M24 9h1v1h-1zM25 9h1v1h-1zM2 10h1v1h-1zM3 10h1v1h-1zM4 10h1v1h-1zM9 10h1v1h-1z' +
        'M10 10h1v1h-1zM11 10h1v1h-1zM14 10h1v1h-1zM16 10h1v1h-1zM18 10h1v1h-1zM19 10h1v1h-1z' +
        'M20 10h1v1h-1zM22 10h1v1h-1zM24 10h1v1h-1zM25 10h1v1h-1zM1 11h1v1h-1zM2 11h1v1h-1z' +
        'M3 11h1v1h-1zM4 11h1v1h-1zM5 11h1v1h-1zM7 11h1v1h-1zM12 11h1v1h-1zM16 11h1v1h-1z' +
        'M18 11h1v1h-1zM21 11h1v1h-1zM22 11h1v1h-1zM23 11h1v1h-1zM25 11h1v1h-1zM1 12h1v1h-1z' +
        'M3 12h1v1h-1zM4 12h1v1h-1zM5 12h1v1h-1zM6 12h1v1h-1zM8 12h1v1h-1zM9 12h1v1h-1z' +
        'M10 12h1v1h-1zM13 12h1v1h-1zM15 12h1v1h-1zM17 12h1v1h-1zM20 12h1v1h-1zM22 12h1v1h-1z' +
        'M2 13h1v1h-1zM4 13h1v1h-1zM5 13h1v1h-1zM6 13h1v1h-1zM7 13h1v1h-1zM8 13h1v1h-1z' +
        'M9 13h1v1h-1zM10 13h1v1h-1zM12 13h1v1h-1zM13 13h1v1h-1zM16 13h1v1h-1zM17 13h1v1h-1z' +
        'M19 13h1v1h-1zM20 13h1v1h-1zM25 13h1v1h-1zM4 14h1v1h-1zM9 14h1v1h-1zM12 14h1v1h-1z' +
        'M13 14h1v1h-1zM15 14h1v1h-1zM16 14h1v1h-1zM17 14h1v1h-1zM19 14h1v1h-1zM20 14h1v1h-1z' +
        'M24 14h1v1h-1zM25 14h1v1h-1zM1 15h1v1h-1zM2 15h1v1h-1zM4 15h1v1h-1zM5 15h1v1h-1z' +
        'M7 15h1v1h-1zM10 15h1v1h-1zM12 15h1v1h-1zM13 15h1v1h-1zM14 15h1v1h-1zM15 15h1v1h-1z' +
        'M16 15h1v1h-1zM17 15h1v1h-1zM18 15h1v1h-1zM19 15h1v1h-1zM22 15h1v1h-1zM23 15h1v1h-1z' +
        'M25 15h1v1h-1zM3 16h1v1h-1zM4 16h1v1h-1zM6 16h1v1h-1zM10 16h1v1h-1zM11 16h1v1h-1z' +
        'M13 16h1v1h-1zM15 16h1v1h-1zM17 16h1v1h-1zM18 16h1v1h-1zM20 16h1v1h-1zM21 16h1v1h-1z' +
        'M22 16h1v1h-1zM1 17h1v1h-1zM2 17h1v1h-1zM3 17h1v1h-1zM4 17h1v1h-1zM5 17h1v1h-1z' +
        'M6 17h1v1h-1zM7 17h1v1h-1zM9 17h1v1h-1zM11 17h1v1h-1zM12 17h1v1h-1zM13 17h1v1h-1z' +
        'M17 17h1v1h-1zM18 17h1v1h-1zM19 17h1v1h-1zM20 17h1v1h-1zM21 17h1v1h-1zM24 17h1v1h-1z' +
        'M9 18h1v1h-1zM12 18h1v1h-1zM13 18h1v1h-1zM15 18h1v1h-1zM17 18h1v1h-1zM21 18h1v1h-1z' +
        'M25 18h1v1h-1zM1 19h1v1h-1zM2 19h1v1h-1zM3 19h1v1h-1zM4 19h1v1h-1zM5 19h1v1h-1z' +
        'M6 19h1v1h-1zM7 19h1v1h-1zM12 19h1v1h-1zM15 19h1v1h-1zM17 19h1v1h-1zM19 19h1v1h-1z' +
        'M21 19h1v1h-1zM25 19h1v1h-1zM1 20h1v1h-1zM7 20h1v1h-1zM9 20h1v1h-1zM10 20h1v1h-1z' +
        'M12 20h1v1h-1zM13 20h1v1h-1zM16 20h1v1h-1zM17 20h1v1h-1zM21 20h1v1h-1zM24 20h1v1h-1z' +
        'M25 20h1v1h-1zM1 21h1v1h-1zM3 21h1v1h-1zM4 21h1v1h-1zM5 21h1v1h-1zM7 21h1v1h-1z' +
        'M10 21h1v1h-1zM11 21h1v1h-1zM13 21h1v1h-1zM15 21h1v1h-1zM16 21h1v1h-1zM17 21h1v1h-1z' +
        'M18 21h1v1h-1zM19 21h1v1h-1zM20 21h1v1h-1zM21 21h1v1h-1zM24 21h1v1h-1zM1 22h1v1h-1z' +
        'M3 22h1v1h-1zM4 22h1v1h-1zM5 22h1v1h-1zM7 22h1v1h-1zM10 22h1v1h-1zM11 22h1v1h-1z' +
        'M12 22h1v1h-1zM13 22h1v1h-1zM14 22h1v1h-1zM18 22h1v1h-1zM21 22h1v1h-1zM23 22h1v1h-1z' +
        'M24 22h1v1h-1zM1 23h1v1h-1zM3 23h1v1h-1zM4 23h1v1h-1zM5 23h1v1h-1zM7 23h1v1h-1z' +
        'M9 23h1v1h-1zM12 23h1v1h-1zM14 23h1v1h-1zM16 23h1v1h-1zM17 23h1v1h-1zM18 23h1v1h-1z' +
        'M20 23h1v1h-1zM21 23h1v1h-1zM22 23h1v1h-1zM24 23h1v1h-1zM25 23h1v1h-1zM1 24h1v1h-1z' +
        'M7 24h1v1h-1zM9 24h1v1h-1zM12 24h1v1h-1zM15 24h1v1h-1zM16 24h1v1h-1zM17 24h1v1h-1z' +
        'M18 24h1v1h-1zM19 24h1v1h-1zM20 24h1v1h-1zM21 24h1v1h-1zM1 25h1v1h-1zM2 25h1v1h-1z' +
        'M3 25h1v1h-1zM4 25h1v1h-1zM5 25h1v1h-1zM6 25h1v1h-1zM7 25h1v1h-1zM9 25h1v1h-1z' +
        'M10 25h1v1h-1zM11 25h1v1h-1zM12 25h1v1h-1zM13 25h1v1h-1zM17 25h1v1h-1zM19 25h1v1h-1z' +
        'M22 25h1v1h-1zM25 25h1v1h-1z'
    })
  })
})
