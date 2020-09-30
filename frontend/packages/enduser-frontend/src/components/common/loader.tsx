// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue, { CreateElement } from 'vue'
import { Component } from 'vue-property-decorator'
import styled from 'vue-styled-components'

@Component
export class Loader extends Vue {
  public render(h: CreateElement) {
    return <LoaderElement />
  }
}

const loaderSize = '10em'
const loaderColor = 'rgba(255, 255, 255, 0.2)'
const loaderBarColor = 'rgba(255, 255, 255, 1)'
const loaderThickness = '1.1em'

const LoaderElement = styled.div`
  z-index: 1800;
  border-radius: 50%;
  width: ${loaderSize};
  height: ${loaderSize};

  &:after {
    border-radius: 50%;
    width: ${loaderSize};
    height: ${loaderSize};
  }

  margin: 2em;
  font-size: 10px;
  position: relative;
  text-indent: -9999em;
  border-top: ${loaderThickness} solid ${loaderColor};
  border-right: ${loaderThickness} solid ${loaderColor};
  border-bottom: ${loaderThickness} solid ${loaderColor};
  border-left: ${loaderThickness} solid ${loaderBarColor};
  -webkit-transform: translateZ(0);
  -ms-transform: translateZ(0);
  transform: translateZ(0);
  -webkit-animation: load8 1.1s infinite linear;
  animation: load8 1.1s infinite linear;

  @-webkit-keyframes load8 {
    0% {
      -webkit-transform: rotate(0deg);
      transform: rotate(0deg);
    }
    100% {
      -webkit-transform: rotate(360deg);
      transform: rotate(360deg);
    }
  }
  @keyframes load8 {
    0% {
      -webkit-transform: rotate(0deg);
      transform: rotate(0deg);
    }
    100% {
      -webkit-transform: rotate(360deg);
      transform: rotate(360deg);
    }
  }
`
