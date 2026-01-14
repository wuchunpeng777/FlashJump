package com.github.wuchunpeng777.flashjump.search

/**
 * 搜索查询封装
 * 支持字面量搜索和正则表达式搜索
 */
sealed class SearchQuery {
    abstract val rawText: String
    
    /**
     * 转换为正则表达式
     */
    abstract fun toRegex(): Regex?
    
    /**
     * 获取高亮长度
     */
    abstract fun getHighlightLength(text: String, offset: Int): Int
    
    /**
     * 字面量搜索
     */
    data class Literal(override val rawText: String) : SearchQuery() {
        override fun toRegex(): Regex? {
            if (rawText.isEmpty()) return null
            return try {
                // 转义特殊字符并创建忽略大小写的正则
                Regex(Regex.escape(rawText), RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                null
            }
        }
        
        override fun getHighlightLength(text: String, offset: Int): Int = rawText.length
    }
    
    /**
     * 正则表达式搜索
     */
    data class RegularExpression(override val rawText: String) : SearchQuery() {
        override fun toRegex(): Regex? {
            if (rawText.isEmpty()) return null
            return try {
                Regex(rawText, RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                null
            }
        }
        
        override fun getHighlightLength(text: String, offset: Int): Int {
            val regex = toRegex() ?: return 0
            val match = regex.find(text, offset)
            return match?.value?.length ?: rawText.length
        }
    }
    
    /**
     * 单词边界搜索（匹配单词开头）
     */
    data class WordStart(override val rawText: String) : SearchQuery() {
        override fun toRegex(): Regex? {
            if (rawText.isEmpty()) return null
            return try {
                Regex("\\b${Regex.escape(rawText)}", RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                null
            }
        }
        
        override fun getHighlightLength(text: String, offset: Int): Int = rawText.length
    }
    
    /**
     * 行首/行尾搜索
     */
    sealed class LinePattern : SearchQuery() {
        data object LineStart : LinePattern() {
            override val rawText: String = "^"
            override fun toRegex(): Regex = Regex("^", RegexOption.MULTILINE)
            override fun getHighlightLength(text: String, offset: Int): Int = 0
        }
        
        data object LineEnd : LinePattern() {
            override val rawText: String = "$"
            override fun toRegex(): Regex = Regex("$", RegexOption.MULTILINE)
            override fun getHighlightLength(text: String, offset: Int): Int = 0
        }
        
        data object LineIndent : LinePattern() {
            override val rawText: String = "^\\s*\\S"
            override fun toRegex(): Regex = Regex("^\\s*\\S", RegexOption.MULTILINE)
            override fun getHighlightLength(text: String, offset: Int): Int = 1
        }
    }
    
    /**
     * 所有单词匹配
     */
    data object AllWords : SearchQuery() {
        override val rawText: String = "\\b\\w"
        override fun toRegex(): Regex = Regex("\\b\\w")
        override fun getHighlightLength(text: String, offset: Int): Int = 1
    }
}
