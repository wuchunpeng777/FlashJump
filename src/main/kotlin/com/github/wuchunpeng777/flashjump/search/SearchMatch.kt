package com.github.wuchunpeng777.flashjump.search

import com.intellij.openapi.editor.Editor

/**
 * 表示一个搜索匹配结果
 */
data class SearchMatch(
    /**
     * 匹配所在的编辑器
     */
    val editor: Editor,
    
    /**
     * 匹配开始的偏移量
     */
    val startOffset: Int,
    
    /**
     * 匹配结束的偏移量
     */
    val endOffset: Int,
    
    /**
     * 分配的跳转标签（可为空，表示未分配标签）
     */
    var label: String? = null
) {
    /**
     * 匹配的长度
     */
    val length: Int get() = endOffset - startOffset
    
    /**
     * 获取匹配的行号（0-based）
     */
    fun getLine(): Int = editor.document.getLineNumber(startOffset)
    
    /**
     * 获取匹配在行内的列号（0-based）
     */
    fun getColumn(): Int {
        val lineStartOffset = editor.document.getLineStartOffset(getLine())
        return startOffset - lineStartOffset
    }
    
    /**
     * 检查此匹配是否在可见区域内
     */
    fun isVisible(): Boolean {
        val scrollingModel = editor.scrollingModel
        val visibleArea = scrollingModel.visibleArea
        val point = editor.offsetToXY(startOffset)
        return visibleArea.contains(point)
    }
    
    /**
     * 计算到光标的距离
     */
    fun distanceToCaret(): Int {
        val caretOffset = editor.caretModel.offset
        return kotlin.math.abs(startOffset - caretOffset)
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchMatch) return false
        return editor == other.editor && startOffset == other.startOffset
    }
    
    override fun hashCode(): Int {
        return 31 * editor.hashCode() + startOffset
    }
}
