<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <span
    class="base-tooltip"
    tabindex="0"
    @keyup.enter.self="toggleTooltip"
    ref="container"
  >
    <span :id="dataId" style="display: none" v-tippy-html>
      <slot></slot>
    </span>

    <span
      v-tippy="{
        html: '#' + dataId,
        interactive: true,
        onShown: focusContainer,
        animateFill: false,
        theme: 'light bordered',
        interactiveBorder: 16,
        placement: placement
      }"
      ref="tooltip"
    >
      <slot name="trigger"></slot>
    </span>
  </span>
</template>

<script>
  import { getUUID } from '@/utils/uuid'

  export default {
    props: {
      placement: {
        type: String,
        default: 'bottom'
      }
    },
    methods: {
      focusContainer() {
        this.$refs.container.focus()
      },
      toggleTooltip() {
        const el = this.$refs.tooltip._tippy
        const isVisible = el.state.visible
        if (isVisible) {
          el.hide()
        } else {
          el.show()
        }
      }
    },
    computed: {
      dataId() {
        return 'tooltip_' + getUUID()
      }
    }
  }
</script>

<style lang="scss">
  .tippy-tooltip {
    background: white;
    padding: 0 !important;

    .tippy-tooltip-content {
      text-align: left;
      font-family: 'Avenir', Helvetica, Arial, sans-serif;
      font-size: 0.875rem;
      padding: 0.125rem 0.325rem;

      p:last-child {
        margin: 0;
      }
    }
  }

  .padded-tooltip {
    padding: 0.5rem 0.625rem;
  }
</style>
