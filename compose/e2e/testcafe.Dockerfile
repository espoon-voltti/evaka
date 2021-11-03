# SPDX-FileCopyrightText: 2017-2021 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG NODE_VERSION=16.13

FROM cimg/node:${NODE_VERSION}-browsers

RUN sudo apt-get update \
 && sudo apt-get --yes install --no-install-recommends ffmpeg \
 && curl --silent --show-error --location --fail --retry 3 --output /tmp/google-chrome-stable_current_amd64.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
 && sudo dpkg -i /tmp/google-chrome-stable_current_amd64.deb \
 && sudo apt-get -fy install \
 && rm -rf /tmp/google-chrome-stable_current_amd64.deb \
 && sudo sed -i 's|HERE/chrome"|HERE/chrome" --disable-setuid-sandbox --no-sandbox|g' "/opt/google/chrome/google-chrome" \
 && google-chrome --version \
 && export CHROME_VERSION="$(google-chrome --version)" \
 && export CHROMEDRIVER_RELEASE=${CHROME_VERSION//Google Chrome /} \
 && export CHROMEDRIVER_RELEASE=${CHROMEDRIVER_RELEASE%%.*} \
 && CHROMEDRIVER_VERSION=$(curl --silent --show-error --location --fail --retry 4 --retry-delay 5 "http://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROMEDRIVER_RELEASE}") \
 && curl --silent --show-error --location --fail --retry 4 --retry-delay 5 --output /tmp/chromedriver_linux64.zip "http://chromedriver.storage.googleapis.com/${CHROMEDRIVER_VERSION}/chromedriver_linux64.zip" \
 && cd /tmp \
 && unzip chromedriver_linux64.zip \
 && rm -f chromedriver_linux64.zip \
 && sudo mv chromedriver /usr/local/bin/chromedriver \
 && sudo chmod +x /usr/local/bin/chromedriver \
 && sudo rm -rf $HOME/.cache/pip /var/lib/apt/lists/*

USER root
COPY ./testcafe/bin/run-tests.sh ./entrypoint.sh /bin/

ENTRYPOINT ["/bin/entrypoint.sh"]
CMD ["/bin/run-tests.sh"]
