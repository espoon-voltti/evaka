<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="view-toolbar">
    <slot />
  </div>
</template>

<style lang="scss">
  .view-toolbar {
    display: flex;
    flex-direction: row;
    justify-content: center;

    button {
      min-width: 18%;

      &:not(:first-child) {
        margin-left: 2em;
      }
    }

    @include onMobile() {
      flex-direction: column-reverse;
      justify-content: flex-start;

      button {
        &:not(:first-child) {
          margin-left: 0;
        }

        margin-bottom: 1em;
      }
    }
  }
</style>
