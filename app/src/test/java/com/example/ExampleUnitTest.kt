package com.example

import com.example.util.LatexRenderer
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testLatexRendererFractions() {
    val input = "Berapakah hasil dari \$\\frac{3}{8} + \\dfrac{1}{8}\$?"
    val rendered = LatexRenderer.render(input)
    assertEquals("Berapakah hasil dari 3/8 + 1/8?", rendered)
  }

  @Test
  fun testLatexRendererSymbolsAndSqrt() {
    val input = "5 \\times 4 \\div 2 \\le 10 \\approx \\sqrt{100}"
    val rendered = LatexRenderer.render(input)
    assertEquals("5 × 4 ÷ 2 ≤ 10 ≈ √100", rendered)
  }

  @Test
  fun testLatexRendererBracesAndNewlines() {
    val input = "Soal 1:\\\\Hasil dari {4} + {5} adalah..."
    val rendered = LatexRenderer.render(input)
    assertEquals("Soal 1:\nHasil dari 4 + 5 adalah...", rendered)
  }
}
