<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <base-tooltip>
    <div class="warnings-tooltip">
      <section class="warnings-tooltip-inner">
        <ul>
          <li
            v-for="(warning, index) in warnings"
            :key="index"
            class="warning-item"
          >
            {{ warning }}
          </li>
        </ul>
      </section>
    </div>
    <span slot="trigger" class="warnings-tooltip-trigger">
      <span class="warnings-count">{{ warningCount }}</span>
      <font-awesome-icon
        :class="{ 'color-error': isError }"
        :icon="['fal', 'exclamation-triangle']"
      ></font-awesome-icon>
    </span>
  </base-tooltip>
</template>

<script>
  import BaseTooltip from '@/components/tooltip/base-tooltip.vue'

  export default {
    components: {
      BaseTooltip
    },
    props: {
      header: String,
      instruction: String,
      warnings: {
        type: Array,
        default: () => []
      },
      isError: {
        type: Boolean,
        default: false
      }
    },
    computed: {
      warningCount() {
        return this.warnings.length
      },
      warningIconClasses() {
        return {
          'fa fa-exclamation-triangle color-warning': !this.isError,
          'fa fa-exclamation-circle color-error': this.isError
        }
      }
    }
  }
</script>

<style lang="scss" scoped>
  .svg-inline--fa {
    cursor: help;

    &.color-warning {
      color: $color-warning-icon;
    }

    &.color-error {
      color: $color-error-icon;
    }
  }
  .warnings-tooltip {
    max-width: 350px;

    @include onMobile() {
      max-width: 500px;
    }

    .warnings-tooltip-inner {
      padding: 0.125rem 0.625rem;
      font-family: $font-body;
      background-color: white;
      font-size: 0.875rem;

      list-style: circle;

      .warning-item {
        padding: 0.125rem;
        text-align: left;
      }
    }
  }

  .warnings-tooltip-trigger {
    .warnings-count {
      margin-right: 0.25rem;
      font-weight: 600;
      color: $color-warning-text;
    }
  }
</style>
