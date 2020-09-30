<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div
    :class="{
      content: true,
      secondary: secondary
    }"
  >
    <slot />
  </div>
</template>

<script>
  export default {
    props: {
      secondary: {
        type: Boolean,
        default: false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .content {
    padding: 2em;

    &.secondary {
      border-top: 1px dashed #bbb;
      background-color: #f5f5f5;
    }
  }
</style>
