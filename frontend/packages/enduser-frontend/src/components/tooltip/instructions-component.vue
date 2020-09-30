<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <base-tooltip>
    <span slot="trigger" class="instructions">
      <font-awesome-icon :icon="['fas', 'info-circle']"></font-awesome-icon>
    </span>

    <div class="info-tooltip padded-tooltip">
      <header v-if="header" class="info-tooltip-header">
        <span>{{ header }}</span>
      </header>
      <div class="info-tooltip-inner" v-html="instruction"></div>
    </div>
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
      instruction: String
    }
  }
</script>

<style lang="scss" scoped>
  .instructions {
    padding: 0.25rem;
    color: $soft-blue;
  }

  i {
    vertical-align: baseline;
    font-size: 1rem;
    cursor: help;

    svg path {
      fill: $soft-blue;
    }
  }

  .info-tooltip {
    max-width: 350px;
    border: 1px solid $grey-light;
    text-align: left;
    line-height: 1.5em;
    padding: 16px;

    @include onMobile() {
      max-width: 500px;
    }
  }
</style>
