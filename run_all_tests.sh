#!/usr/bin/env bash
sbt scalastyle compile coverage test it:test coverageOff coverageReport