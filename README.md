# KSim65 - Kotlin/JVM 6502/65C02 microprocessor simulator

*Written by Irmen de Jong (irmen@razorvine.net)*

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

**TODO move to another repository for published packages.**

You can simply add it as a dependency to your project.
For Maven:

    <dependency>
        <groupId>net.razorvine</groupId>
        <artifactId>ksim65</artifactId>
        <version>1.10</version>
        <type>pom</type>
    </dependency>

For Gradle:

    implementation 'net.razorvine:ksim65:1.10'

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


### License information

Ksim65 itself is licensed under the MIT software license, see file LICENSE.

It includes the 'Spleen' bitmap font (https://github.com/fcambus/spleen),
which has the following license (BSD):

Copyright (c) 2018-2020, Frederic Cambus
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
