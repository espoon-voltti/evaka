// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue, { CreateElement } from 'vue'
import { Component } from 'vue-property-decorator'
import styled from 'vue-styled-components'

import { Loader } from './loader'
import { Loader as LoaderType } from '../../store/modules/loader'
import { mapGetters } from 'vuex'

@Component({
  computed: {
    ...mapGetters({ visible: 'loader/activeLoader' })
  }
})
export class ViewLoader extends Vue {
  public visible: false | LoaderType

  public render(h: CreateElement) {
    const loader = this.visible
    const { $t } = this

    return this.visible ? (
      <LoaderOverlay>
        <div class="loader-wrapper">
          <Loader />
          {loader ? $t(loader.message) : null}
        </div>
      </LoaderOverlay>
    ) : null
  }
}

const LoaderOverlay = styled.div`
  background-color: #0050bb60;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999999;

  .loader-wrapper {
    color: white;
    text-align: center;
  }
`
