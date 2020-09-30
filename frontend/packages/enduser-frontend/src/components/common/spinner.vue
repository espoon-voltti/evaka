<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div v-if="show" class="spinner">
    <font-awesome-icon
      :icon="['far', 'spinner']"
      pulse
      size="3x"
    ></font-awesome-icon>
  </div>
</template>

<script>
  export default {
    props: {
      show: {
        type: Boolean,
        default: true
      }
    }
  }
</script>

<style lang="scss">
  .spinner {
    text-align: center;
    padding: 1rem 0;

    i {
      font-size: 2.25rem;
      color: #0050bb;
    }
  }
</style>
