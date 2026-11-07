package dev.tomasmed.jlight.wear.domain.qr

/**
 * Pure Kotlin ISO/IEC 18004 QR Code Version 1–6 (Byte mode, ECC Level L) matrix generator.
 *
 * Designed specifically for rendering `https://<host>/?claimBot=BOT-XXXX-XXXX` pairing URLs
 * on circular Wear OS AMOLED screens with zero external image/ZXing dependencies.
 */
object QrCodeMatrixGenerator {
    data class QrMatrix(
        val size: Int,
        private val modules: Array<BooleanArray>,
    ) {
        fun isDark(row: Int, col: Int): Boolean =
            row in 0 until size && col in 0 until size && modules[row][col]
    }

    private data class VersionSpec(
        val version: Int,
        val totalDataCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocksGroup1: Int,
        val dataCodewordsGroup1: Int,
        val numBlocksGroup2: Int = 0,
        val dataCodewordsGroup2: Int = 0,
        val alignmentCenters: IntArray = intArrayOf(),
    )

    // ECC Level L specifications for Versions 1 through 9 (up to 230 UTF-8 bytes)
    private val VERSION_SPECS = listOf(
        VersionSpec(version = 1, totalDataCodewords = 19, ecCodewordsPerBlock = 7, numBlocksGroup1 = 1, dataCodewordsGroup1 = 19),
        VersionSpec(version = 2, totalDataCodewords = 34, ecCodewordsPerBlock = 10, numBlocksGroup1 = 1, dataCodewordsGroup1 = 34, alignmentCenters = intArrayOf(6, 18)),
        VersionSpec(version = 3, totalDataCodewords = 55, ecCodewordsPerBlock = 15, numBlocksGroup1 = 1, dataCodewordsGroup1 = 55, alignmentCenters = intArrayOf(6, 22)),
        VersionSpec(version = 4, totalDataCodewords = 80, ecCodewordsPerBlock = 20, numBlocksGroup1 = 1, dataCodewordsGroup1 = 80, alignmentCenters = intArrayOf(6, 26)),
        VersionSpec(version = 5, totalDataCodewords = 108, ecCodewordsPerBlock = 26, numBlocksGroup1 = 1, dataCodewordsGroup1 = 108, alignmentCenters = intArrayOf(6, 30)),
        VersionSpec(version = 6, totalDataCodewords = 136, ecCodewordsPerBlock = 18, numBlocksGroup1 = 2, dataCodewordsGroup1 = 68, alignmentCenters = intArrayOf(6, 34)),
        VersionSpec(version = 7, totalDataCodewords = 156, ecCodewordsPerBlock = 20, numBlocksGroup1 = 2, dataCodewordsGroup1 = 78, alignmentCenters = intArrayOf(6, 22, 38)),
        VersionSpec(version = 8, totalDataCodewords = 194, ecCodewordsPerBlock = 24, numBlocksGroup1 = 2, dataCodewordsGroup1 = 97, alignmentCenters = intArrayOf(6, 24, 42)),
        VersionSpec(version = 9, totalDataCodewords = 232, ecCodewordsPerBlock = 30, numBlocksGroup1 = 2, dataCodewordsGroup1 = 116, alignmentCenters = intArrayOf(6, 26, 46)),
    )

    fun encode(payload: String): QrMatrix {
        val bytes = payload.encodeToByteArray()
        require(bytes.isNotEmpty()) { "QR payload must not be empty." }

        val spec = requireNotNull(VERSION_SPECS.firstOrNull { bytes.size + 2 <= it.totalDataCodewords }) {
            "QR payload length (${bytes.size} bytes) exceeds maximum supported capacity (230 bytes)."
        }

        val dataCodewords = buildDataCodewords(bytes, spec.totalDataCodewords)
        val finalBitStream = buildInterleavedBitStream(dataCodewords, spec)
        val size = 17 + 4 * spec.version
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        drawFunctionPatterns(modules, isFunction, spec)
        drawCodewords(modules, isFunction, finalBitStream)
        applyMask0(modules, isFunction)
        drawFormatBits(modules, isFunction, mask = 0)

        return QrMatrix(size = size, modules = modules)
    }

    private fun buildDataCodewords(bytes: ByteArray, capacity: Int): IntArray {
        val bits = ArrayList<Int>(capacity * 8)
        fun appendBits(value: Int, length: Int) {
            for (i in length - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        // Byte mode indicator (0100) + 8-bit character count (Versions 1..9)
        appendBits(0b0100, 4)
        appendBits(bytes.size, 8)
        for (b in bytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }

        // Terminator (up to 4 zero bits)
        val maxBits = capacity * 8
        val terminatorLen = minOf(4, maxBits - bits.size)
        appendBits(0, terminatorLen)

        // Pad to byte boundary
        while (bits.size % 8 != 0) {
            bits.add(0)
        }

        // Pad with alternating 0xEC and 0x11
        var padToggle = true
        while (bits.size < maxBits) {
            appendBits(if (padToggle) 0xEC else 0x11, 8)
            padToggle = !padToggle
        }

        return IntArray(capacity) { byteIdx ->
            var acc = 0
            for (bitOffset in 0 until 8) {
                acc = (acc shl 1) or bits[byteIdx * 8 + bitOffset]
            }
            acc
        }
    }

    private fun buildInterleavedBitStream(dataCodewords: IntArray, spec: VersionSpec): BooleanArray {
        val dataBlocks = mutableListOf<IntArray>()
        val ecBlocks = mutableListOf<IntArray>()
        var offset = 0

        repeat(spec.numBlocksGroup1) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.dataCodewordsGroup1)
            offset += spec.dataCodewordsGroup1
            dataBlocks.add(block)
            ecBlocks.add(computeReedSolomonRemainder(block, spec.ecCodewordsPerBlock))
        }
        repeat(spec.numBlocksGroup2) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.dataCodewordsGroup2)
            offset += spec.dataCodewordsGroup2
            dataBlocks.add(block)
            ecBlocks.add(computeReedSolomonRemainder(block, spec.ecCodewordsPerBlock))
        }

        val interleaved = ArrayList<Int>()
        val maxDataLen = maxOf(spec.dataCodewordsGroup1, spec.dataCodewordsGroup2)
        for (i in 0 until maxDataLen) {
            for (block in dataBlocks) {
                if (i < block.size) interleaved.add(block[i])
            }
        }
        for (i in 0 until spec.ecCodewordsPerBlock) {
            for (ec in ecBlocks) {
                interleaved.add(ec[i])
            }
        }

        val bits = BooleanArray(interleaved.size * 8)
        for (i in interleaved.indices) {
            val cw = interleaved[i]
            for (bit in 0 until 8) {
                bits[i * 8 + bit] = ((cw ushr (7 - bit)) and 1) != 0
            }
        }
        return bits
    }

    private fun drawFunctionPatterns(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        spec: VersionSpec,
    ) {
        val size = modules.size

        // Timing patterns
        for (i in 0 until size) {
            setFunctionModule(modules, isFunction, 6, i, i % 2 == 0)
            setFunctionModule(modules, isFunction, i, 6, i % 2 == 0)
        }

        // 3 Finder patterns
        drawFinderPattern(modules, isFunction, 3, 3)
        drawFinderPattern(modules, isFunction, 3, size - 4)
        drawFinderPattern(modules, isFunction, size - 4, 3)

        // Alignment patterns
        val centers = spec.alignmentCenters
        for (r in centers) {
            for (c in centers) {
                val overlapsFinder = (r == 6 && c == 6) ||
                    (r == 6 && c == size - 7) ||
                    (r == size - 7 && c == 6)
                if (!overlapsFinder) {
                    drawAlignmentPattern(modules, isFunction, r, c)
                }
            }
        }

        // Reserve format information areas + dark module
        for (i in 0..8) {
            if (i != 6) {
                setFunctionModule(modules, isFunction, 8, i, false)
                setFunctionModule(modules, isFunction, i, 8, false)
            }
        }
        for (i in 0..7) {
            setFunctionModule(modules, isFunction, 8, size - 1 - i, false)
            setFunctionModule(modules, isFunction, size - 1 - i, 8, false)
        }
        setFunctionModule(modules, isFunction, size - 8, 8, true)

        // Draw 18-bit version information blocks for Versions 7..9 (ISO/IEC 18004 Annex D)
        if (spec.version >= 7) {
            var rem = spec.version
            repeat(12) {
                rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25)
            }
            val versionBits = (spec.version shl 12) or rem
            for (i in 0 until 18) {
                val dark = ((versionBits ushr i) and 1) != 0
                val a = size - 11 + (i % 3)
                val b = i / 3
                setFunctionModule(modules, isFunction, a, b, dark)
                setFunctionModule(modules, isFunction, b, a, dark)
            }
        }
    }

    private fun drawFinderPattern(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        centerRow: Int,
        centerCol: Int,
    ) {
        val size = modules.size
        for (dy in -4..4) {
            for (dx in -4..4) {
                val r = centerRow + dy
                val c = centerCol + dx
                if (r in 0 until size && c in 0 until size) {
                    val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                    val dark = dist != 2 && dist != 4
                    setFunctionModule(modules, isFunction, r, c, dark)
                }
            }
        }
    }

    private fun drawAlignmentPattern(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        centerRow: Int,
        centerCol: Int,
    ) {
        for (dy in -2..2) {
            for (dx in -2..2) {
                val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                setFunctionModule(modules, isFunction, centerRow + dy, centerCol + dx, dist != 1)
            }
        }
    }

    private fun drawCodewords(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        bits: BooleanArray,
    ) {
        val size = modules.size
        var bitIdx = 0
        var rightCol = size - 1
        var upward = true

        while (rightCol >= 1) {
            if (rightCol == 6) rightCol = 5
            val rowIndices = if (upward) (size - 1 downTo 0) else (0 until size)
            for (row in rowIndices) {
                for (colOffset in 0..1) {
                    val col = rightCol - colOffset
                    if (!isFunction[row][col]) {
                        modules[row][col] = if (bitIdx < bits.size) bits[bitIdx] else false
                        bitIdx++
                    }
                }
            }
            upward = !upward
            rightCol -= 2
        }
    }

    private fun applyMask0(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
    ) {
        val size = modules.size
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!isFunction[r][c] && (r + c) % 2 == 0) {
                    modules[r][c] = !modules[r][c]
                }
            }
        }
    }

    private fun drawFormatBits(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        mask: Int,
    ) {
        val size = modules.size
        // ECC Level L = 01, Mask = 000 -> data = 0b01000
        val data = (0b01 shl 3) or mask
        var rem = data
        repeat(10) {
            rem = (rem shl 1) xor ((rem ushr 9) * 0x537)
        }
        val bits = ((data shl 10) or rem) xor 0x5412

        fun bit(index: Int): Boolean = ((bits ushr index) and 1) != 0

        for (i in 0..5) setFunctionModule(modules, isFunction, 8, i, bit(14 - i))
        setFunctionModule(modules, isFunction, 8, 7, bit(8))
        setFunctionModule(modules, isFunction, 8, 8, bit(7))
        setFunctionModule(modules, isFunction, 7, 8, bit(6))
        for (i in 9..14) setFunctionModule(modules, isFunction, 14 - i, 8, bit(14 - i))

        for (i in 0..7) setFunctionModule(modules, isFunction, 8, size - 1 - i, bit(i))
        for (i in 8..14) setFunctionModule(modules, isFunction, size - 15 + i, 8, bit(i))
        setFunctionModule(modules, isFunction, size - 8, 8, true)
    }

    private fun setFunctionModule(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        row: Int,
        col: Int,
        dark: Boolean,
    ) {
        modules[row][col] = dark
        isFunction[row][col] = true
    }

    private fun computeReedSolomonRemainder(
        data: IntArray,
        degree: Int,
    ): IntArray {
        val generator = IntArray(degree)
        generator[degree - 1] = 1
        var root = 1
        for (i in 0 until degree) {
            for (j in 0 until degree) {
                generator[j] = gfMultiply(generator[j], root)
                if (j + 1 < degree) {
                    generator[j] = generator[j] xor generator[j + 1]
                }
            }
            root = gfMultiply(root, 0x02)
        }

        val result = IntArray(degree)
        for (b in data) {
            val factor = b xor result[0]
            for (i in 0 until degree - 1) {
                result[i] = result[i + 1] xor gfMultiply(generator[i], factor)
            }
            result[degree - 1] = gfMultiply(generator[degree - 1], factor)
        }
        return result
    }

    private fun gfMultiply(x: Int, y: Int): Int {
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor (((y ushr i) and 1) * x)
        }
        return z
    }
}
