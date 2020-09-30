<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <base-tooltip :placement="placement">
    <div class="basic-tooltip padded-tooltip" v-html="message"></div>
    <span slot="trigger">
      <slot></slot>
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
      message: String,
      placement: String
    }
  }
</script>

<style lang="scss" scoped>
  .basic-tooltip {
    max-width: 350px;

    @include onMobile() {
      max-width: 500px;
    }
  }
</style>
