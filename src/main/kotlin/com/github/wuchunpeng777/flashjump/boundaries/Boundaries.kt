package com.github.wuchunpeng777.flashjump.boundaries

import com.intellij.openapi.editor.Editor
import com.github.wuchunpeng777.flashjump.config.FlashConfig

/**
 * 定义搜索边界的接口
 */
interface Boundaries {
    /**
     * 获取搜索的偏移量范围
     */
    fun getOffsetRange(editor: Editor): IntRange
    
    /**
     * 检查偏移量是否在边界内
     */
    fun isOffsetInside(editor: Editor, offset: Int): Boolean
    
    /**
     * 与另一个边界取交集
     */
    fun intersection(other: Boundaries): Boundaries = IntersectionBoundaries(this, other)
}

/**
 * 标准边界实现
 */
sealed class StandardBoundaries : Boundaries {
    
    /**
     * 整个文件
     */
    data object WHOLE_FILE : StandardBoundaries() {
        override fun getOffsetRange(editor: Editor): IntRange {
            return 0..editor.document.textLength
        }
        
        override fun isOffsetInside(editor: Editor, offset: Int): Boolean {
            return offset in 0..editor.document.textLength
        }
    }
    
    /**
     * 屏幕可见区域
     */
    data object VISIBLE_ON_SCREEN : StandardBoundaries() {
        override fun getOffsetRange(editor: Editor): IntRange {
            val visibleArea = editor.scrollingModel.visibleArea
            val startOffset = editor.xyToLogicalPosition(visibleArea.location).let {
                editor.logicalPositionToOffset(it)
            }
            val endPoint = java.awt.Point(
                visibleArea.x + visibleArea.width,
                visibleArea.y + visibleArea.height
            )
            val endOffset = editor.xyToLogicalPosition(endPoint).let {
                editor.logicalPositionToOffset(it)
            }
            return startOffset..endOffset.coerceAtMost(editor.document.textLength)
        }
        
        override fun isOffsetInside(editor: Editor, offset: Int): Boolean {
            if (offset < 0 || offset > editor.document.textLength) return false
            val point = editor.offsetToXY(offset)
            return editor.scrollingModel.visibleArea.contains(point)
        }
    }
    
    /**
     * 光标之前
     */
    data object BEFORE_CARET : StandardBoundaries() {
        override fun getOffsetRange(editor: Editor): IntRange {
            val caretOffset = editor.caretModel.offset
            val startBoundary = if (FlashConfig.getInstance().searchWholeFile) {
                0
            } else {
                VISIBLE_ON_SCREEN.getOffsetRange(editor).first
            }
            return startBoundary..caretOffset
        }
        
        override fun isOffsetInside(editor: Editor, offset: Int): Boolean {
            val caretOffset = editor.caretModel.offset
            return offset in 0 until caretOffset
        }
    }
    
    /**
     * 光标之后
     */
    data object AFTER_CARET : StandardBoundaries() {
        override fun getOffsetRange(editor: Editor): IntRange {
            val caretOffset = editor.caretModel.offset
            val endBoundary = if (FlashConfig.getInstance().searchWholeFile) {
                editor.document.textLength
            } else {
                VISIBLE_ON_SCREEN.getOffsetRange(editor).last
            }
            return caretOffset..endBoundary
        }
        
        override fun isOffsetInside(editor: Editor, offset: Int): Boolean {
            val caretOffset = editor.caretModel.offset
            return offset > caretOffset && offset <= editor.document.textLength
        }
    }
    
    companion object {
        /**
         * 根据配置获取默认边界
         */
        fun getDefault(): Boundaries {
            return if (FlashConfig.getInstance().searchWholeFile) {
                WHOLE_FILE
            } else {
                VISIBLE_ON_SCREEN
            }
        }
    }
}

/**
 * 边界交集实现
 */
class IntersectionBoundaries(
    private val a: Boundaries,
    private val b: Boundaries
) : Boundaries {
    override fun getOffsetRange(editor: Editor): IntRange {
        val rangeA = a.getOffsetRange(editor)
        val rangeB = b.getOffsetRange(editor)
        val start = maxOf(rangeA.first, rangeB.first)
        val end = minOf(rangeA.last, rangeB.last)
        return if (start <= end) start..end else start..start
    }
    
    override fun isOffsetInside(editor: Editor, offset: Int): Boolean {
        return a.isOffsetInside(editor, offset) && b.isOffsetInside(editor, offset)
    }
}
