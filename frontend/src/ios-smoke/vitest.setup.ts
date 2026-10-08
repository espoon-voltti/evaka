// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// Vitest copies Vite's import.meta.env into process.env, where the e2e test
// config would read Vite's BASE_URL ("/") as the eVaka URL
if (process.env.BASE_URL === '/') delete process.env.BASE_URL
