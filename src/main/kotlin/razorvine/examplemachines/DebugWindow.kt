package razorvine.examplemachines

import razorvine.ksim65.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.*
import javax.swing.text.DefaultCaret


class DebugWindow(private val vm: IVirtualMachine) : JFrame("Debugger - ksim65 v${Version.version}"),
        ActionListener {

    private class UnaliasedTextBox(rows: Int, columns: Int) : JTextArea(rows, columns) {
        override fun paintComponent(g: Graphics) {
            g as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            super.paintComponent(g)
        }
    }

    private val cyclesTf = JTextField("00000000000000")
    private val speedKhzTf = JTextField("0000000")
    private val regAtf = JTextField("000")
    private val regXtf = JTextField("000")
    private val regYtf = JTextField("000")
    private val regPCtf = JTextField("00000")
    private val regSPtf = JTextField("000")
    private val regPtf = JTextArea("NV-BDIZC\n000000000")
    private val disassemTf = JTextField("00 00 00   lda   (fffff),x")
    private val pauseBt = JButton("Pause").also { it.actionCommand = "pause" }
    private val zeropageTf = UnaliasedTextBox(8, 102).also {
        it.border = BorderFactory.createEtchedBorder()
        it.isEnabled = false
        it.disabledTextColor = Color.DARK_GRAY
        it.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }
    private val stackpageTf = UnaliasedTextBox(8, 102).also {
        it.border = BorderFactory.createEtchedBorder()
        it.isEnabled = false
        it.disabledTextColor = Color.DARK_GRAY
        it.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }

    init {
        contentPane.layout = GridBagLayout()
        defaultCloseOperation = EXIT_ON_CLOSE
        val cpuPanel = JPanel(GridBagLayout())
        cpuPanel.border = BorderFactory.createTitledBorder("CPU: ${vm.cpu.name}")
        val gc = GridBagConstraints()
        gc.insets = Insets(2, 2, 2, 2)
        gc.anchor = GridBagConstraints.EAST
        val cyclesLb = JLabel("cycles")
        val speedKhzLb = JLabel("speed (kHz)")
        val regAlb = JLabel("A")
        val regXlb = JLabel("X")
        val regYlb = JLabel("Y")
        val regSPlb = JLabel("SP")
        val regPClb = JLabel("PC")
        val regPlb = JLabel("Status")
        val disassemLb = JLabel("Instruction")
        cpuPanel.add(cyclesLb, gc.update(gridx=0, gridy=0))
        cpuPanel.add(speedKhzLb, gc.update(gridx=5, gridy=0))
        cpuPanel.add(regAlb, gc.update(gridx=0, gridy=1))
        cpuPanel.add(regXlb, gc.update(gridx=2, gridy=1))
        cpuPanel.add(regYlb, gc.update(gridx=4, gridy=1))
        cpuPanel.add(regPClb, gc.update(gridx=0, gridy=2))
        cpuPanel.add(regSPlb, gc.update(gridx=2, gridy=2))
        cpuPanel.add(regPlb, gc.update(gridx=0, gridy=3))
        cpuPanel.add(disassemLb, gc.update(gridx=0, gridy=4))
        gc.anchor = GridBagConstraints.WEST
        cpuPanel.add(cyclesTf, gc.update(gridx=1, gridy=0, gridwidth = 3))
        cpuPanel.add(speedKhzTf, gc.update(gridx=6, gridy=0))
        cpuPanel.add(regAtf, gc.update(gridx=1, gridy=1))
        cpuPanel.add(regXtf, gc.update(gridx=3, gridy=1))
        cpuPanel.add(regYtf, gc.update(gridx=5, gridy=1))
        cpuPanel.add(regPCtf, gc.update(gridx=1, gridy=2))
        cpuPanel.add(regSPtf, gc.update(gridx=3, gridy=2))
        cpuPanel.add(regPtf, gc.update(gridx=1, gridy=3, gridwidth=2))
        cpuPanel.add(disassemTf, gc.update(gridx=1, gridy=4, gridwidth=5))
        listOf(cyclesTf, speedKhzTf, regAtf, regXtf, regYtf, regSPtf, regPCtf, disassemTf, regPtf).forEach {
            it.font = Font(Font.MONOSPACED, Font.PLAIN, 14)
            it.disabledTextColor = Color.DARK_GRAY
            it.isEnabled = false
            if (it is JTextField) {
                it.columns = it.text.length
            } else if (it is JTextArea) {
                it.border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                                                               BorderFactory.createEmptyBorder(2, 2, 2, 2))
            }
        }

        val buttonPanel = JPanel(FlowLayout())
        buttonPanel.border = BorderFactory.createTitledBorder("Control")

        val loadBt = JButton("Inject program").also { it.actionCommand = "inject" }
        val resetBt = JButton("Reset").also { it.actionCommand = "reset" }
        val stepBt = JButton("Step").also { it.actionCommand = "step" }
        val irqBt = JButton("IRQ").also { it.actionCommand = "irq" }
        val nmiBt = JButton("NMI").also { it.actionCommand = "nmi" }
        val quitBt = JButton("Quit").also { it.actionCommand = "quit" }
        listOf(loadBt, resetBt, irqBt, nmiBt, pauseBt, stepBt, quitBt).forEach {
            it.addActionListener(this)
            buttonPanel.add(it)
        }

        val zeropagePanel = JPanel()
        zeropagePanel.layout = BoxLayout(zeropagePanel, BoxLayout.Y_AXIS)
        zeropagePanel.border = BorderFactory.createTitledBorder("Zeropage and Stack")
        val showZp = JCheckBox("show Zero page and Stack dumps", true)
        showZp.addActionListener {
            val visible = (it.source as JCheckBox).isSelected
            zeropageTf.isVisible = visible
            stackpageTf.isVisible = visible
        }
        zeropagePanel.add(showZp)
        zeropagePanel.add(zeropageTf)
        zeropagePanel.add(stackpageTf)

        val monitorPanel = JPanel()
        monitorPanel.layout = BoxLayout(monitorPanel, BoxLayout.Y_AXIS)
        monitorPanel.border = BorderFactory.createTitledBorder("Built-in Monitor")
        val output = JTextArea(10, 80)
        output.font = Font(Font.MONOSPACED, Font.PLAIN, 14)
        output.isEditable = false
        val outputScroll = JScrollPane(output)
        monitorPanel.add(outputScroll)
        outputScroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        (output.caret as DefaultCaret).updatePolicy = DefaultCaret.ALWAYS_UPDATE
        val input = JTextField(50)
        input.border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
        input.font = Font(Font.MONOSPACED, Font.PLAIN, 14)
        input.addActionListener {
            output.append("\n")
            val command = input.text.trim()
            val result = vm.executeMonitorCommand(command)
            if (result.echo) output.append("> $command\n")
            output.append(result.output)
            input.text = result.prompt
        }
        monitorPanel.add(input)
        output.append(vm.executeMonitorCommand("h").output)

        gc.gridx = 0
        gc.gridy = 0
        gc.fill = GridBagConstraints.BOTH
        contentPane.add(cpuPanel, gc)
        gc.gridy++
        contentPane.add(zeropagePanel, gc)
        gc.gridy++
        contentPane.add(monitorPanel, gc)
        gc.gridy++
        contentPane.add(buttonPanel, gc)
        pack()
    }

    override fun actionPerformed(e: ActionEvent) {
        when (e.actionCommand) {
            "inject" -> {
                val chooser = JFileChooser()
                chooser.dialogTitle = "Choose binary program or .prg to load"
                chooser.currentDirectory = File(".")
                chooser.isMultiSelectionEnabled = false
                val result = chooser.showOpenDialog(this)
                if (result == JFileChooser.APPROVE_OPTION) {
                    if (chooser.selectedFile.extension == "prg") {
                        vm.loadFileInRam(chooser.selectedFile, null)
                    } else {
                        val addressStr = JOptionPane.showInputDialog(this,
                                                                     "The selected file isn't a .prg.\nSpecify memory load address (hexadecimal) manually.",
                                                                     "Load address", JOptionPane.QUESTION_MESSAGE, null, null,
                                                                     "$") as String

                        val loadAddress = Integer.parseInt(addressStr.removePrefix("$"), 16)
                        vm.loadFileInRam(chooser.selectedFile, loadAddress)
                    }
                }

            }
            "reset" -> {
                vm.bus.reset()
                updateCpu(vm.cpu, vm.bus)
            }
            "step" -> {
                vm.step()
                updateCpu(vm.cpu, vm.bus)
            }
            "pause" -> {
                vm.pause(true)
                pauseBt.actionCommand = "continue"
                pauseBt.text = "Continue"
            }
            "continue" -> {
                vm.pause(false)
                pauseBt.actionCommand = "pause"
                pauseBt.text = "Pause"
            }
            "irq" -> vm.cpu.irqAsserted = true
            "nmi" -> vm.cpu.nmiAsserted = true
            "quit" -> {
                dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
            }
        }
    }

    fun updateCpu(cpu: Cpu6502, bus: Bus) {
        val state = cpu.snapshot()
        cyclesTf.text = state.cycles.toString()
        regAtf.text = hexB(state.A)
        regXtf.text = hexB(state.X)
        regYtf.text = hexB(state.Y)
        regPtf.text = "NV-BDIZC\n"+state.P.asInt().toString(2).padStart(8, '0')
        regPCtf.text = hexW(state.PC)
        regSPtf.text = hexB(state.SP)

        val memory = listOf(bus[state.PC], bus[state.PC+1], bus[state.PC+2]).toTypedArray()
        val disassem = cpu.disassembleOneInstruction(memory, 0, state.PC).first.substringAfter(' ').trim()
        disassemTf.text = disassem

        if (zeropageTf.isVisible || stackpageTf.isVisible) {
            val pages = vm.getZeroAndStackPages()
            if (pages.isNotEmpty()) {
                val zpLines = (0..0xff step 32).map { location ->
                    " ${'$'}${location.toString(16).padStart(2, '0')}  "+((0..31).joinToString(" ") { lineoffset ->
                        pages[location+lineoffset].toString(16).padStart(2, '0')
                    })
                }
                val stackLines = (0x100..0x1ff step 32).map { location ->
                    "${'$'}${location.toString(16).padStart(2, '0')}  "+((0..31).joinToString(" ") { lineoffset ->
                        pages[location+lineoffset].toString(16).padStart(2, '0')
                    })
                }
                zeropageTf.text = zpLines.joinToString("\n")
                stackpageTf.text = stackLines.joinToString("\n")
            }
        }

        speedKhzTf.text = "%.1f".format(cpu.averageSpeedKhzSinceReset)
    }
}


private fun GridBagConstraints.update(gridx: Int, gridy: Int, gridwidth: Int?=null): GridBagConstraints {
    val gc = this.clone() as GridBagConstraints
    gc.gridx = gridx
    gc.gridy = gridy
    if(gridwidth!=null)
        gc.gridwidth=gridwidth
    return gc
}
