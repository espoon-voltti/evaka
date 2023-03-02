#!/usr/bin/python

# SPDX-FileCopyrightText: 2017-2023 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# Usage ./timings.py <chunk> <max-chunks>
#
# Read stadin "<duration> <tests>" lines
# and print evenly distributed chunks by duration

import sys

ZERO_TIME = 5   

class Stack:
    total = 0
    paths = None

    def __init__(self) -> None:
        self.paths = []

    def add(self, path, duration):
        self.paths.append(path)
        self.total += duration

class Stacks:
    stacks = []

    def __init__(self, count) -> None:
        self.stacks = [Stack() for _ in range(count)]

    def get_min(self):
        return sorted([(item.total, item) for item in self.stacks], key=lambda x: x[0])[0][1]

    def add_to_min(self, path, duration):
        self.get_min().add(path, duration)

    def print(self, chunk):
        for s in self.stacks[chunk].paths:
            print(s)
        #print(self.stacks[chunk].total)


if __name__ == '__main__':
    data = {}

    for line in sys.stdin:
        value, key = line.strip().split(" ")
        data[key] = float(value)

    chunk = int(sys.argv[1]) - 1
    stacks = Stacks(int(sys.argv[2]))

    for path, duration in sorted(data.items(), key=lambda x: x[0], reverse=True):
        stacks.add_to_min(path, duration or ZERO_TIME)

    stacks.print(chunk)
