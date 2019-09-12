[![saythanks](https://img.shields.io/badge/say-thanks-ff69b4.svg)](https://saythanks.io/to/irmen)
[![Build Status](https://travis-ci.org/irmen/ksim65.svg?branch=master)](https://travis-ci.org/irmen/ksim65)

KSim65 - Kotlin 6502/65C02 microprocessor simulator
===================================================

*Written by Irmen de Jong (irmen@razorvine.net)*

*Software license: MIT, see file LICENSE*


![6502](https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/KL_MOS_6502.jpg/320px-KL_MOS_6502.jpg)

This is a Kotlin library to simulate the 8-bit 6502 and 65C02 microprocessors of the early 80's. 

The simulation is cycle precise, includes BCD mode, and all opcodes are implemented (including the 'illegal' opcodes of the 6502).
 
On my machine the library can simulate a 6502 running at up to ~100Mhz.
