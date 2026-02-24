package org.bogsnebes.engines.countstrings.logic

internal object LineDifferenceCounter {
    fun countChangedLines(before: String, after: String): Int {
        if (before == after) {
            return 0
        }

        val beforeLines = toLines(before)
        val afterLines = toLines(after)
        return computeEditDistance(beforeLines, afterLines)
    }

    private fun toLines(text: String): List<String> {
        if (text.isEmpty()) {
            return emptyList()
        }
        return text.split('\n')
    }

    private fun computeEditDistance(before: List<String>, after: List<String>): Int {
        val n = before.size
        val m = after.size
        val max = n + m
        val offset = max
        val diagonalEnd = 2 * max + 1
        val v = IntArray(diagonalEnd)

        for (d in 0..max) {
            var k = -d
            while (k <= d) {
                val index = offset + k
                val goDown = (k == -d) || (k != d && v[index - 1] < v[index + 1])
                var x = if (goDown) v[index + 1] else v[index - 1] + 1
                var y = x - k

                while (x < n && y < m && before[x] == after[y]) {
                    x++
                    y++
                }

                v[index] = x
                if (x >= n && y >= m) {
                    return d
                }
                k += 2
            }
        }

        return max
    }
}
