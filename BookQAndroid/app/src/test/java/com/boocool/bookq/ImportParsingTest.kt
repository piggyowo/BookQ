package com.boocool.bookq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ImportParsingTest {
    private fun parseText(text: String): ParsedBank {
        val f = File.createTempFile("bookq-import-", ".txt")
        try {
            f.writeText(text)
            return ImportService.parse(f, "测试题库.txt")
        } finally {
            f.delete()
        }
    }

    @Test
    fun parsesIrregularChoiceLayouts() {
        // 三选项 + 缺 A 标签 + B 无顿号 + 下一题被塞进 D + 无标签四行 + 数字答案
        val text = """
            1.第一题（B）
            选项一 B、选项二 C、选项三
            2.第二题（C）
            B关机走人 C、选项三 D、选项四
            3.第三题（A）
            A、选项一 B、选项二 C、选项三 D、4.第四题（B）
            A、选项一 B、选项二 C、选项三 D、选项四
            5.排序题（B）
            第一步
            第二步
            第三步
            第四步
        """.trimIndent()
        val p = parseText(text)
        assertEquals(5, p.questions.size)
        assertEquals(3, p.questions[0].options.size)
        assertEquals("C", p.questions[1].answer)
        assertEquals(3, p.questions[2].options.size)
        assertEquals("4", p.questions[3].num)
        assertEquals(4, p.questions[4].options.size)
        assertTrue(p.warnings.isEmpty())
    }

    @Test
    fun mapsNumericAnswerToOptionKey() {
        val text = """
            1.包装数量（5）
            A、4个 B、6个 C、8个 D、5个
        """.trimIndent()
        val p = parseText(text)
        assertEquals("D", p.questions.single().answer)
    }

    @Test
    fun keepsFillInQuestionsAsRevealOnly() {
        val text = """
            1.选择题（A）
            A、对 B、错
            二、填空题
            1.易拉宝种类有（标准型）（水滴型）。
        """.trimIndent()
        val p = parseText(text)
        assertEquals(2, p.questions.size)
        assertEquals("fill", p.questions[1].kind)
        assertEquals(0, p.questions[1].options.size)
        assertEquals("标准型、水滴型", p.questions[1].answer)
    }

    @Test
    fun parsesJudgmentAndAnswerKeyAtEnd() {
        val text = """
            一、判断题
            1.户外写真需要覆膜。
            2.所有材料都可以户外长期使用。
            二、选择题
            3.第一题
            A、选项一 B、选项二 C、选项三 D、选项四
            4.第二题
            A、选项一 B、选项二 C、选项三 D、选项四
            参考答案
            1.对 2.错 3.A 4.B
        """.trimIndent()
        val p = parseText(text)
        assertEquals(4, p.questions.size)
        assertEquals("judgment", p.questions[0].kind)
        assertEquals("A", p.questions[0].answer)
        assertEquals("B", p.questions[1].answer)
        assertEquals("A", p.questions[2].answer)
        assertEquals("B", p.questions[3].answer)
    }

    @Test
    fun parsesFillAnswerKeyAtEnd() {
        val text = """
            一、选择题
            1.选择题（A）
            A、对 B、错
            二、填空题
            1.请填写______。
            2.再填写______。
            答案
            1.第一空答案
            2.第二空答案
        """.trimIndent()
        val p = parseText(text)
        assertEquals(3, p.questions.size)
        assertEquals("第一空答案", p.questions[1].answer)
        assertEquals("第二空答案", p.questions[2].answer)
    }
}
