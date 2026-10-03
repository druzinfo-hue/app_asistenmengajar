package com.example.util

object LatexRenderer {
    /**
     * Convert LaTeX sederhana ke plain text / Unicode.
     */
    fun render(text: String): String {
        if (text.isBlank()) return text
        var result = text

        // \frac{a}{b} → a/b
        result = result.replace(
            Regex("""\\frac\{([^}]+)\}\{([^}]+)\}""")
        ) { match ->
            "${match.groupValues[1]}/${match.groupValues[2]}"
        }

        // \dfrac{a}{b} → a/b
        result = result.replace(
            Regex("""\\dfrac\{([^}]+)\}\{([^}]+)\}""")
        ) { match ->
            "${match.groupValues[1]}/${match.groupValues[2]}"
        }

        // \sqrt{x} → √x
        result = result.replace(
            Regex("""\\sqrt\{([^}]+)\}""")
        ) { match ->
            "√${match.groupValues[1]}"
        }

        // Simbol matematika
        val symbolMap = mapOf(
            "\\times" to "×",
            "\\div" to "÷",
            "\\pm" to "±",
            "\\le" to "≤",
            "\\leq" to "≤",
            "\\ge" to "≥",
            "\\geq" to "≥",
            "\\ne" to "≠",
            "\\neq" to "≠",
            "\\approx" to "≈",
            "\\infty" to "∞",
            "\\pi" to "π",
            "\\alpha" to "α",
            "\\beta" to "β",
            "\\theta" to "θ",
            "\\degree" to "°",
            "\\cdot" to "·",
            "\\ldots" to "...",
            "\\dots" to "..."
        )

        symbolMap.forEach { (latex, unicode) ->
            result = result.replace(latex, unicode)
        }

        // Hapus $ pembuka/penutup
        result = result.replace(Regex("""\$([^$]+)\$""")) { match ->
            match.groupValues[1]
        }

        // \\ → newline
        result = result.replace("\\\\", "\n")

        // Hapus { } yang tidak perlu
        result = result.replace(Regex("""\{([^{}]+)\}""")) { match ->
            match.groupValues[1]
        }

        return result
    }
}
