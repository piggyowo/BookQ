package com.boocool.bookq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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

    @Test
    fun parsesAnswerHeadingOnSameLine() {
        val text = """
            1.第一题
            A、选项一 B、选项二 C、选项三 D、选项四
            2.第二题
            A、选项一 B、选项二 C、选项三 D、选项四
            参考答案及解析：1.A 2.B
        """.trimIndent()
        val p = parseText(text)
        assertEquals(2, p.questions.size)
        assertEquals("A", p.questions[0].answer)
        assertEquals("B", p.questions[1].answer)
    }

    @Test
    fun keepsAnswerGroupsSeparatedWhenNumbersRestart() {
        val text = """
            一、选择题
            1.选择一
            A、选项一 B、选项二 C、选项三 D、选项四
            2.选择二
            A、选项一 B、选项二 C、选项三 D、选项四
            二、填空题
            1.填空一（　）
            2.填空二（　）
            参考答案
            一、选择题
            1-2 AB
            二、填空题
            1、填空答案一
            2、填空答案二
        """.trimIndent()
        val p = parseText(text)
        assertEquals(4, p.questions.size)
        assertEquals("A", p.questions[0].answer)
        assertEquals("B", p.questions[1].answer)
        assertEquals("填空答案一", p.questions[2].answer)
        assertEquals("填空答案二", p.questions[3].answer)
    }

    @Test
    fun parsesSeparatedRangeAnswerKey() {
        val text = """
            1.第一题
            A、选项一 B、选项二 C、选项三 D、选项四
            2.第二题
            A、选项一 B、选项二 C、选项三 D、选项四
            3.第三题
            A、选项一 B、选项二 C、选项三 D、选项四
            4.第四题
            A、选项一 B、选项二 C、选项三 D、选项四
            5.第五题
            A、选项一 B、选项二 C、选项三 D、选项四
            参考答案
            1-5：A、B、C、D、A
        """.trimIndent()
        val p = parseText(text)
        assertEquals(5, p.questions.size)
        assertEquals(listOf("A", "B", "C", "D", "A"), p.questions.map { it.answer })
    }

    @Test
    fun parsesGroupHeadingAndAnswersOnSameLine() {
        val text = """
            1.第一题
            A、选项一 B、选项二 C、选项三 D、选项四
            2.第二题
            A、选项一 B、选项二 C、选项三 D、选项四
            3.第三题
            A、选项一 B、选项二 C、选项三 D、选项四
            参考答案 一、选择题 1-3 A、B、C
        """.trimIndent()
        val p = parseText(text)
        assertEquals(3, p.questions.size)
        assertEquals(listOf("A", "B", "C"), p.questions.map { it.answer })
    }

    @Test
    fun parsesAnswerKeyInParentheses() {
        val text = """
            1.第一题
            A、选项一 B、选项二 C、选项三 D、选项四
            2.第二题
            A、选项一 B、选项二 C、选项三 D、选项四
            答案：1（A）2（B）
        """.trimIndent()
        val p = parseText(text)
        assertEquals(listOf("A", "B"), p.questions.map { it.answer })
    }

    @Test
    fun parsesPartHeadingsCompleteAnswerKeyAndMixedKinds() {
        val text = """
            人事行政综合管理考核试卷
            第一部分 单项选择题（共4题，每题1分，共4分）
            1.第一题（　）
            A.选项一 B.选项二 C.选项三 D.选项四
            2.第二题（　）
            A.选项一 B.选项二 C.选项三 D.选项四
            3.第三题（　）
            A.选项一 B.选项二 C.选项三 D.选项四
            4.第四题（　）
            A.选项一 B.选项二 C.选项三 D.选项四
            第二部分 多项选择题（共2题，每题1分）
            5.第五题（　）
            A.选项一 B.选项二 C.选项三 D.选项四
            6.第六题（　）
            A.选项一 B.选项二 C.选项三 D.选项四
            第三部分 判断题（共2题，每题0.5分）
            7.第七题（　）
            8.第八题（　）
            第四部分 简答题（共1题，每题3分）
            9.第九题，请简述。
            完整参考答案
            一、单项选择题（1-4）
            1.B 2.B 3.A 4.B
            二、多项选择题（5-6）
            5.ABC 6.AB
            三、判断题（7-8）
            7.× 8.√
            四、简答题（9）
            9.答：这是简答题答案。
        """.trimIndent()
        val p = parseText(text)
        assertEquals(9, p.questions.size)
        assertEquals(listOf("choice", "choice", "choice", "choice", "multi", "multi", "judgment", "judgment", "short"),
            p.questions.map { it.kind })
        assertEquals(listOf("B", "B", "A", "B", "ABC", "AB", "B", "A"), p.questions.take(8).map { it.answer })
        assertEquals("答：这是简答题答案。", p.questions.last().answer)
        assertTrue(p.warnings.isEmpty())
    }

    @Test
    fun parsesPartDigitHeadingsAndAnswerTitleVariants() {
        val text = """
            第1部分 单选题
            1.第一题
            A、选项一 B、选项二 C、选项三 D、选项四
            2.第二题
            A、选项一 B、选项二 C、选项三 D、选项四
            第2部分 多选题
            3.第三题
            A、选项一 B、选项二 C、选项三 D、选项四
            完整版参考答案（完整版）
            一、单选题（1-2）
            1.A 2.B
            二、多选题（3）
            3.ACD
        """.trimIndent()
        val p = parseText(text)
        assertEquals(3, p.questions.size)
        assertEquals(listOf("choice", "choice", "multi"), p.questions.map { it.kind })
        assertEquals(listOf("A", "B", "ACD"), p.questions.map { it.answer })
    }

    @Test
    fun parsesCompactInlineOptionsWithoutSpaceAfterLabel() {
        val text = """
            1.A类客户验厂，业务招待费参考金额为（　）
            A.120元/人 B.200元/人 C.60元/人 D.50元/人
        """.trimIndent()
        val p = parseText(text)
        assertEquals(1, p.questions.size)
        assertEquals(4, p.questions.single().options.size)
        assertEquals("120元/人", p.questions.single().options.first()[1])
        assertTrue(p.warnings.isEmpty())
    }

    @Test
    fun keepsBareOptionLabelWithoutPunctuation() {
        val text = """
            1.第一题（C）
            B关机走人 C、选项三 D、选项四
        """.trimIndent()
        val p = parseText(text)
        assertEquals(1, p.questions.size)
        assertEquals("C", p.questions.single().answer)
        assertTrue(p.questions.single().options.size >= 2)
    }

    @Test
    fun answerTitleSupportsFullAndScoringVariants() {
        val text = """
            1.第一题
            A、选项一 B、选项二 C、选项三 D、选项四
            2.第二题
            A、选项一 B、选项二 C、选项三 D、选项四
            参考答案与评分标准：1.A 2.B
        """.trimIndent()
        val p = parseText(text)
        assertEquals(listOf("A", "B"), p.questions.map { it.answer })
    }


    @Test
    fun parsesMultiAnswersWithPunctuationOrSpaces() {
        val text = """
            一、多项选择题
            1.第一题
            A、选项一 B、选项二 C、选项三 D、选项四
            2.第二题
            A、选项一 B、选项二 C、选项三 D、选项四
            参考答案
            一、多项选择题（1-2）
            1.A、B、C 2.A B D
        """.trimIndent()
        val p = parseText(text)
        assertEquals(2, p.questions.size)
        assertEquals(listOf("ABC", "ABD"), p.questions.map { it.answer })
    }


    @Test
    fun keepsShortAnswerParagraphsAndModelSemantics() {
        val text = """
            一、简答题
            99.第一道简答题。
            100.第二道简答题。
            完整参考答案
            四、简答题（99、100）
            99.答：第一行
            第二行
            100.答：另一段答案
        """.trimIndent()
        val p = parseText(text)
        assertEquals(2, p.questions.size)
        assertTrue(p.questions.first().answer!!.contains("第二行"))
        assertEquals("答：另一段答案", p.questions.last().answer)
        assertFalse(p.questions.first().kind == "choice")

        val multi = Question(
            id = "q", bankId = "b", chapter = "c", num = "81", stem = "多选",
            options = listOf(listOf("A", "甲"), listOf("B", "乙"), listOf("C", "丙"), listOf("D", "丁")),
            answer = "ABC", kind = "multi"
        )
        assertTrue(multi.hasAnswer)
        assertTrue(multi.isCorrect("ABC"))
        assertTrue(multi.isCorrect("CAB"))
        assertFalse(multi.isCorrect("AB"))
        assertEquals("多选题 · 4 个选项", multi.metaLine.removePrefix("题库第 81 题 · "))

        val short = Question(
            id = "s", bankId = "b", chapter = "c", num = "99", stem = "简答",
            options = emptyList(), answer = "答：第一行\n第二行", kind = "short"
        )
        assertFalse(short.isGraded)
        assertTrue(short.hasAnswer)
        assertEquals("答：第一行\n第二行", short.answerText)
    }

}
