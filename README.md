[![saythanks](https://img.shields.io/badge/say-thanks-ff69b4.svg)](https://saythanks.io/to/irmen)
[![Build Status](https://travis-ci.org/irmen/ksim65.svg?branch=master)](https://travis-ci.org/irmen/ksim65)

# KSim65 - Kotlin/JVM 6502/65C02 microprocessor simulator

*Written by Irmen de Jong (irmen@razorvine.net)*

*Software license: MIT, see file LICENSE*


![6502](https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/KL_MOS_6502.jpg/320px-KL_MOS_6502.jpg)

This is a Kotlin/JVM library that simulates the 8-bit 6502 and 65C02 microprocessors,
 which became very popular in the the early 1980's. 

Properties of this simulator:

- Written in Kotlin. It is low-level code, but hopefully still readable :-)
- Designed to simulate various hardware components (bus, cpu, memory, i/o controllers) 
- IRQ and NMI simulation
- Aims to simulate correct instruction cycle timing, but is not 100% cycle exact for simplicity 
- Aims to implements all 6502 and 65c02 instructions, including the 'illegal' 6502 instructions (not yet done)
- correct BCD mode for adc/sbc instructions on both cpu types   
- passes several extensive unit test suites that verify instruction and cpu flags behavior
- provide a few virtual example machines, one of which is a Commodore-64 (character display mode only for now)  


## Documentation

Still to be written. For now, use the source ;-)


## Virtual machine examples

Three virtual example machines are included.
The default one starts with ``gradle run`` or run the ``ksim64vm`` command.
There's another one ``ehBasicMain`` that is configured to run the "enhanced 6502 basic" ROM:

![ehBasic](ehbasic.png)

Finally there is a fairly functional C64 emulator running the actual roms (not included,
but can be easily found elsewhere for example with the Vice emulator):

![C64 emulation](c64.png)
