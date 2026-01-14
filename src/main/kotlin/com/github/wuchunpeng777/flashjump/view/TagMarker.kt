package com.github.wuchunpeng777.flashjump.view

import com.intellij.openapi.editor.Editor
import com.github.wuchunpeng777.flashjump.search.SearchMatch
import java.awt.*

/**
 * 标签标记
 * 代表一个可跳转标签在编辑器中的视觉表示
 */
class TagMarker(
    val match: SearchMatch,
    val label: String
) {
    /**
     * 绘制标签
     * @param typedPrefix 已输入的标签前缀（用于置灰显示）
     * @param isDefault 是否是默认匹配项（使用特殊背景色）
     */
    fun paint(
        g: Graphics2D, 
        editor: Editor, 
        font: TagFont, 
        occupied: MutableList<Rectangle>,
        typedPrefix: String = "",
        isDefault: Boolean = false
    ) {
        val point = editor.offsetToXY(match.startOffset)
        
        // 计算标签尺寸
        val labelWidth = font.getWidth(label, g)
        val labelHeight = font.height
        
        // 标签位置（在匹配文本位置）
        var x = point.x
        var y = point.y
        
        // 创建标签矩形
        var labelRect = Rectangle(x, y, labelWidth + 6, labelHeight)
        
        // 避免重叠
        var attempts = 0
        while (isOverlapping(labelRect, occupied) && attempts < 3) {
            x += labelWidth + 2
            labelRect = Rectangle(x, y, labelWidth + 6, labelHeight)
            attempts++
        }
        
        occupied.add(labelRect)
        
        // 绘制背景（默认匹配项使用橘黄色）
        val bgColor = if (isDefault) font.defaultBackgroundColor else font.backgroundColor
        g.color = bgColor
        g.fillRoundRect(x, y, labelWidth + 6, labelHeight, 4, 4)
        
        // 绘制边框
        g.color = bgColor.darker()
        g.drawRoundRect(x, y, labelWidth + 6, labelHeight, 4, 4)
        
        // 绘制文本
        g.font = font.font
        val textY = y + font.ascent
        var textX = x + 3
        
        // 分段绘制：已输入部分置灰，未输入部分高亮
        val lowerLabel = label.lowercase()
        val lowerPrefix = typedPrefix.lowercase()
        
        if (lowerPrefix.isNotEmpty() && lowerLabel.startsWith(lowerPrefix)) {
            // 已输入部分（置灰）
            val typedPart = label.substring(0, lowerPrefix.length)
            g.color = font.dimmedColor
            g.drawString(typedPart, textX, textY)
            textX += font.getWidth(typedPart, g)
            
            // 未输入部分（高亮）
            val remainingPart = label.substring(lowerPrefix.length)
            g.color = font.foregroundColor
            g.drawString(remainingPart, textX, textY)
        } else {
            // 全部高亮
            g.color = font.foregroundColor
            g.drawString(label, textX, textY)
        }
    }
    
    private fun isOverlapping(rect: Rectangle, occupied: List<Rectangle>): Boolean {
        return occupied.any { it.intersects(rect) }
    }
    
    companion object {
        /**
         * 创建标签标记
         */
        fun create(match: SearchMatch, label: String): TagMarker {
            return TagMarker(match, label)
        }
    }
}

/**
 * 标签字体配置
 */
class TagFont(private val editor: Editor) {
    private val editorFont = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
    
    val font: Font = editorFont.deriveFont(Font.BOLD, editorFont.size * 0.9f)
    val height: Int = editor.lineHeight
    val ascent: Int = (height * 0.75).toInt()
    
    val backgroundColor: Color = com.github.wuchunpeng777.flashjump.config.FlashConfig.getLabelBackground()
    val foregroundColor: Color = com.github.wuchunpeng777.flashjump.config.FlashConfig.getLabelForeground()
    val defaultBackgroundColor: Color = com.github.wuchunpeng777.flashjump.config.FlashConfig.getDefaultMatchBackground()
    
    // 已输入部分的置灰颜色
    val dimmedColor: Color = Color(
        foregroundColor.red,
        foregroundColor.green,
        foregroundColor.blue,
        100  // 透明度降低
    )
    
    /**
     * 获取文本宽度
     */
    fun getWidth(text: String, g: Graphics2D): Int {
        return g.getFontMetrics(font).stringWidth(text)
    }
}
