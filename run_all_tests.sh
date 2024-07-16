#!/usr/bin/env bash
sbt clean compile scalafmtAll coverage test it/test coverageOff dependencyUpdates coverageReport
