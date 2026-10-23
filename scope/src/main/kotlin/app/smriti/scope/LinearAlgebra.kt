package app.smriti.scope

import kotlin.math.sqrt

/** Minimal dense linear algebra for small symmetric positive-definite systems (deterministic). */
internal object Linalg {
    fun dot(a: DoubleArray, b: DoubleArray): Double {
        var s = 0.0
        for (i in a.indices) s += a[i] * b[i]
        return s
    }

    /** Lower-triangular Cholesky factor of a symmetric positive-definite matrix, or null if not SPD. */
    fun cholesky(m: Array<DoubleArray>): Array<DoubleArray>? {
        val n = m.size
        val l = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0..i) {
                var s = m[i][j]
                for (k in 0 until j) s -= l[i][k] * l[j][k]
                if (i == j) {
                    if (s <= 0.0 || s.isNaN()) return null
                    l[i][i] = sqrt(s)
                } else {
                    l[i][j] = s / l[j][j]
                }
            }
        }
        return l
    }

    /** Solves (L Lᵀ) x = b. */
    fun cholSolve(l: Array<DoubleArray>, b: DoubleArray): DoubleArray {
        val n = b.size
        val y = DoubleArray(n)
        for (i in 0 until n) {
            var s = b[i]
            for (k in 0 until i) s -= l[i][k] * y[k]
            y[i] = s / l[i][i]
        }
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var s = y[i]
            for (k in i + 1 until n) s -= l[k][i] * x[k]
            x[i] = s / l[i][i]
        }
        return x
    }

    fun inverseFromCholesky(l: Array<DoubleArray>): Array<DoubleArray> {
        val n = l.size
        val inv = Array(n) { DoubleArray(n) }
        for (j in 0 until n) {
            val e = DoubleArray(n).also { it[j] = 1.0 }
            val col = cholSolve(l, e)
            for (i in 0 until n) inv[i][j] = col[i]
        }
        return inv
    }

    fun quadForm(m: Array<DoubleArray>, v: DoubleArray): Double {
        var s = 0.0
        for (i in v.indices) {
            if (v[i] == 0.0) continue
            var r = 0.0
            for (j in v.indices) r += m[i][j] * v[j]
            s += v[i] * r
        }
        return s
    }
}
