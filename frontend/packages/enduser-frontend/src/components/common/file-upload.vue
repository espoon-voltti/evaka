<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="file-upload-container">
    <input
      type="file"
      accept="image/jpeg, image/png, application/pdf, application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document, application/vnd.oasis.opendocument.text"
      @change="onChange"
    />
    <div class="spacer" />
    <div class="files">
      <div class="file" v-for="file in files" v-bind:key="file.key">
        <div class="file-icon-container">
          <font-awesome-icon
            :icon="['fal', fileIcon(file)]"
            size="lg"
          ></font-awesome-icon>
        </div>
        <div class="file-details">
          <div class="file-header">
            <span>{{ file.file.name }}</span>
            <div>
              <button
                class="file-header-icon-button"
                @click="deleteFile(file)"
                :disabled="!file.done"
              >
                <font-awesome-icon
                  :icon="['fal', 'times']"
                  size="2x"
                ></font-awesome-icon>
              </button>
            </div>
          </div>
          <transition name="progress-bar">
            <div class="file-progress-bar" v-show="!file.done">
              <div class="file-progress-bar-background">
                <div
                  class="file-progress-bar-progress"
                  :style="progressBarStyles(file)"
                />
              </div>
              <div class="file-progress-bar-details">
                <span>{{
                  file.done
                    ? $t('file-upload.loaded')
                    : $t('file-upload.loading')
                }}</span>
                <span>{{ file.progress }} %</span>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      files: Array,
      onUpload: Function,
      onDelete: Function
    },
    methods: {
      onChange(event) {
        this.onUpload(event.target.files[0])
      },
      deleteFile(file) {
        this.onDelete(file)
      },
      fileIcon(file) {
        switch (file.file.type) {
          case 'image/jpeg':
          case 'image/png':
            return 'file-image'
          case 'application/pdf':
            return 'file-pdf'
          case 'application/msword':
          case 'application/vnd.openxmlformats-officedocument.wordprocessingml.document':
          case 'application/vnd.oasis.opendocument.text':
            return 'file-word'
          default:
            return 'file'
        }
      },
      progressBarStyles(file) {
        return `width: ${file.progress}%;`
      }
    }
  }
</script>

<style lang="scss">
  $progress-bar-height: 3px;
  $progress-bar-border-radius: 1px;

  .file-upload-container {
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;

    input {
      width: 300px;
    }

    .spacer {
      padding: 20px;
    }

    .files {
      display: flex;
      flex-direction: column;

      > *:not(:last-child) {
        margin-bottom: 20px;
      }

      .file {
        display: flex;
        flex-direction: row;
        flex-wrap: nowrap;
        color: $grey-dark;

        .file-icon-container {
          margin-right: 16px;
          flex: 0;
        }

        .file-details {
          flex: 1;
          display: flex;
          flex-direction: column;
          flex-wrap: nowrap;

          .file-header {
            display: flex;
            flex-direction: row;
            flex-wrap: nowrap;
            justify-content: space-between;
            font-size: 15px;

            .file-header-icon-button {
              border: none;
              background: none;
              padding: 4px;
              margin-left: 12px;
              color: $grey;
              cursor: pointer;

              &:hover {
                color: $blue;
              }

              &:disabled {
                color: $grey-light;
                cursor: not-allowed;
              }
            }

            span {
              width: min(400px, 50vw);
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
          }

          .file-progress-bar {
            margin-top: 16px;

            .file-progress-bar-background {
              position: relative;
              background: $grey;
              height: $progress-bar-height;
              border-radius: $progress-bar-border-radius;

              .file-progress-bar-progress {
                position: absolute;
                top: 0;
                left: 0;
                background: $blue;
                height: $progress-bar-height;
                border-radius: $progress-bar-border-radius;
                transition: width 0.5s ease-out;
              }
            }

            .file-progress-bar-details {
              display: flex;
              flex-direction: row;
              justify-content: space-between;
              margin-top: 4px;
              font-size: 14px;
              color: $grey-dark;
            }
          }

          .progress-bar-leave-active {
            transition: opacity 0s ease-in 2s;
          }

          .progress-bar-leave-to {
            opacity: 0;
          }
        }
      }
    }
  }
</style>
