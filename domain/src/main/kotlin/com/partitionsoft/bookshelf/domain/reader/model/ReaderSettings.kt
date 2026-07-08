package com.partitionsoft.bookshelf.domain.reader.model

enum class ReaderFontFamily { SYSTEM, SERIF, SANS_SERIF, MONOSPACE }

enum class ReaderFontWeight { NORMAL, MEDIUM, BOLD }

enum class ReaderSpacing { COMPACT, NORMAL, RELAXED }

enum class ReaderPageMargin { NARROW, NORMAL, WIDE }

enum class ReaderTheme { SYSTEM, LIGHT, DARK, SEPIA, AMOLED }

enum class ReaderMode { VERTICAL_SCROLL, PAGE_TURN }

data class ReaderSettings(
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SYSTEM,
    val fontSizePercent: Int = DEFAULT_FONT_SIZE_PERCENT,
    val fontWeight: ReaderFontWeight = ReaderFontWeight.NORMAL,
    val lineSpacing: ReaderSpacing = ReaderSpacing.NORMAL,
    val paragraphSpacing: ReaderSpacing = ReaderSpacing.NORMAL,
    val pageMargin: ReaderPageMargin = ReaderPageMargin.NORMAL,
    val theme: ReaderTheme = ReaderTheme.SYSTEM,
    val mode: ReaderMode = ReaderMode.VERTICAL_SCROLL
) {
    fun normalized(): ReaderSettings = copy(
        fontSizePercent = fontSizePercent.coerceIn(MIN_FONT_SIZE_PERCENT, MAX_FONT_SIZE_PERCENT)
    )

    companion object {
        const val MIN_FONT_SIZE_PERCENT = 80
        const val MAX_FONT_SIZE_PERCENT = 200
        const val DEFAULT_FONT_SIZE_PERCENT = 110

        val Default = ReaderSettings()
    }
}
