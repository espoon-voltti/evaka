// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

declare module 'supertest-session' {
  import { SuperTest, Test } from 'supertest'
  import type { Application } from 'express'

  function superTestSession(app: Application): SuperTest<Test>
  export = superTestSession
}
