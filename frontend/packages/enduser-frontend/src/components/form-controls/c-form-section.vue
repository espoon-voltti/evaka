<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div
    class="card collapse-item"
    :class="{ 'is-active': isOpen, 'has-errors': hasErrors }"
  >
    <header
      class="card-header touchable"
      role="tab"
      :aria-expanded="isOpen"
      @click="toggle"
      @keyup.enter.self="toggle"
      tabindex="0"
      ref="header"
    >
      <h4 class="card-header-title">{{ title }}</h4>
      <warnings-tooltip
        class="card-header-icon warning-tooltip"
        v-if="hasErrors"
        :warnings="errors"
        :isError="true"
      >
      </warnings-tooltip>

      <span class="card-header-icon card-header-arrow">
        <font-awesome-icon
          :icon="['fal', `angle-${isOpen ? 'up' : 'down'}`]"
        ></font-awesome-icon>
      </span>
    </header>
    <div class="card-content" v-show="isOpen">
      <div class="card-content-box">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script>
  import { scrollToElement } from '@/utils/scroll-to-element'
  import WarningsTooltip from '@/components/tooltip/warnings-tooltip'

  export default {
    components: {
      WarningsTooltip
    },

    props: {
      title: {
        type: String,
        required: true
      },
      isOpen: {
        type: Boolean,
        default: true
      },
      errors: {
        type: Array,
        default: () => []
      }
    },
    computed: {
      hasErrors() {
        return this.errors.length
      }
    },
    methods: {
      toggle() {
        this.$emit('toggle', !this.isOpen)
      }
    },

    watch: {
      isOpen() {
        if (this.isOpen) {
          window.requestAnimationFrame(() => {
            const element = this.$refs.header
            if (element instanceof Element && document.contains(element)) {
              scrollToElement(element)
            }
          })
        }
      }
    }
  }
</script>

<style lang="scss" scoped>
  .collapse-item {
    border-radius: 8px;

    &.has-errors {
      border-color: $red;
    }

    .card-header {
      cursor: pointer;
    }

    .card-content {
      padding-top: 0;
      padding-bottom: 0;
      overflow: visible;
    }

    .card-content-box {
      padding-top: 20px;
      padding-bottom: 20px;
    }
  }
</style>
