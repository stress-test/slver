package com.example.bbsolver

data class Move(val pi: Int, val r: Int, val c: Int, val cells: List<IntArray>, val lines: Int)

object Solver {
    private fun perms(a: List<Int>): List<List<Int>> =
        if (a.size <= 1) listOf(a) else a.flatMapIndexed { i, x -> perms(a.filterIndexed { j, _ -> j != i }).map { listOf(x) + it } }

    private fun eval(rows: IntArray, lines: Int): Double {
        fun g(r: Int, c: Int) = if (r < 0 || c < 0 || r >= 8 || c >= 8) 1 else (rows[r] shr c) and 1
        var empty = 0; var holes = 0; var tr = 0
        for (r in 0 until 8) for (c in 0 until 8) {
            if (g(r, c) == 0) { empty++; if (g(r - 1, c) + g(r + 1, c) + g(r, c - 1) + g(r, c + 1) == 4) holes++ }
            if (g(r, c) != g(r, c + 1)) tr++
            if (g(r, c) != g(r + 1, c)) tr++
        }
        return lines * lines * 12.0 + empty * 1.5 - holes * 7 - tr * 0.8 + (if (empty == 64) 60 else 0)
    }

    /** rows: 8 bitmask ints (bit c = column c). pieces: list of cell lists [r,c] (empty = already used). */
    fun solve(board: IntArray, pieces: List<List<IntArray>>): List<Move>? {
        val used = pieces.indices.filter { pieces[it].isNotEmpty() }
        if (used.isEmpty()) return null
        var best: List<Move>? = null
        var bestS = -1e18
        fun dfs(order: List<Int>, k: Int, rows: IntArray, moves: ArrayList<Move>, lines: Int) {
            if (k == order.size) {
                val s = eval(rows, lines)
                if (s > bestS) { bestS = s; best = ArrayList(moves) }
                return
            }
            val pi = order[k]; val cs = pieces[pi]
            val h = cs.maxOf { it[0] } + 1; val w = cs.maxOf { it[1] } + 1
            for (r in 0..8 - h) for (c in 0..8 - w) {
                if (cs.any { ((rows[r + it[0]] shr (c + it[1])) and 1) == 1 }) continue
                val pre = rows.copyOf()
                for (p in cs) pre[r + p[0]] = pre[r + p[0]] or (1 shl (c + p[1]))
                var colsFull = 255
                for (x in pre) colsFull = colsFull and x
                var nl = Integer.bitCount(colsFull)
                val after = IntArray(8) { i -> if (pre[i] == 255) { nl++; 0 } else pre[i] and colsFull.inv() and 255 }
                moves.add(Move(pi, r, c, cs, nl))
                dfs(order, k + 1, after, moves, lines + nl)
                moves.removeAt(moves.size - 1)
            }
        }
        perms(used).forEach { dfs(it, 0, board, ArrayList(), 0) }
        return best
    }
}
