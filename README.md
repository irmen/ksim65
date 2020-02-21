[![saythanks](https://img.shields.io/badge/say-thanks-ff69b4.svg)](https://saythanks.io/to/irmen)
[![Build Status](https://travis-ci.org/irmen/ksim65.svg?branch=master)](https://travis-ci.org/irmen/ksim65)
JCenter: [![Download from Jcenter](https://api.bintray.com/packages/irmen/maven/ksim65/images/download.svg)](https://bintray.com/irmen/maven/ksim65/_latestVersion)

# KSim65 - Kotlin/JVM 6502/65C02 microprocessor simulator

*Written by Irmen de Jong (irmen@razorvine.net)*

*Software license: MIT, see file LICENSE*


![6502](https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/KL_MOS_6502.jpg/320px-KL_MOS_6502.jpg)

This is a Kotlin/JVM library that simulates the 8-bit 6502 and 65C02 microprocessors,
 which became very popular in the the early 1980's.

Properties of this simulator:

- written in Kotlin. It is low-level code, but hopefully still readable :-)
- simulates various hardware components (bus, cpu, memory, i/o controllers)
- IRQ and NMI
- instruction cycle times are simulated (however the *internal* cpu behavior is not cycle-exact for simplicity reasons)
- has all 6502 and 65c02 instructions, including many of the 'illegal' 6502 instructions (goal is 100% eventually)
- correct BCD mode for adc/sbc instructions on both cpu types
- passes several extensive unit test suites that verify instruction and cpu flags behavior
- simple debugging machine monitor, which basic disassembler and assembler functions
- provide a few virtual example machines, one of which is a fairly capable Commodore-64


## Documentation

Still to be written. For now, use the source ;-)

## Using it as a library in your own project

Ksim65 is available on the [JCenter maven repository](https://bintray.com/irmen/maven/ksim65/_latestVersion).

You can simply add it as a dependency to your project.
For Maven:

    <dependency>
        <groupId>net.razorvine</groupId>
        <artifactId>ksim65</artifactId>
        <version>1.8</version>
        <type>pom</type>
    </dependency>

For Gradle:

    implementation 'net.razorvine:ksim65:1.8'

Update the version as required.


## Virtual machine examples

Three virtual example machines are included.
The default one starts with ``gradle run`` or run the ``ksim64vm`` command.
There's another one ``ehBasicMain`` that is configured to run the "enhanced 6502 basic" ROM:

![ehBasic](ehbasic.png)

Finally there is a fairly functional C64 emulator running the actual roms (not included,
but can be easily found elsewhere for example with the [Vice emulator](http://vice-emu.sourceforge.net/).
The emulator supports character mode, bitmap mode (hires and multicolor), hardware sprites and
various timers and IRQs.  It's not cycle perfect, and the video display is drawn on a per-frame basis,
so raster splits/rasterbars are impossible. But many other things work fine.

![C64 emulation](c64.png)
