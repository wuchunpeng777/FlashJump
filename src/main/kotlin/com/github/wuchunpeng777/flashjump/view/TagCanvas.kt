package com.github.wuchunpeng777.flashjump.view

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.util.Computable
import com.github.wuchunpeng777.flashjump.config.FlashConfig
import com.github.wuchunpeng777.flashjump.search.SearchMatch
import java.awt.*
import javax.swing.JComponent

/**
 * 在读操作中执行代码
 */
private fun <T> read(action: () -> T): T =
    ApplicationManager.getApplication().runReadAction(Computable { action() })

/**
 * 标签画布
 * 负责在编辑器上绘制跳转标签和视觉效果
 */
class TagCanvas(private val editor: Editor) : JComponent(), CaretListener {
    
    private val LOG = Logger.getInstance(TagCanvas::class.java)
    
    private var markers: List<TagMarker> = emptyList()
    private var showBackdrop: Boolean = false
    private var highlightMatches: List<SearchMatch> = emptyList()
    private var typedLabelPrefix: String = ""
    private var defaultMatch: SearchMatch? = null
    
    init {
        val contentComponent = editor.contentComponent
        contentComponent.add(this)
        setBounds(0, 0, contentComponent.width, contentComponent.height)
        editor.caretModel.addCaretListener(this)
        isOpaque = false
        isVisible = true
        LOG.info("FlashJump: TagCanvas initialized")
    }
    
    /**
     * 解绑画布
     */
    fun unbind() {
        markers = emptyList()
        highlightMatches = emptyList()
        showBackdrop = false
        
        val contentComponent = editor.contentComponent
        contentComponent.remove(this)
        editor.caretModel.removeCaretListener(this)
        
        // 强制重绘编辑器以清除残留的标签
        contentComponent.repaint()
        
        LOG.info("FlashJump: TagCanvas unbound")
    }
    
    /**
     * 设置标签标记
     */
    fun setMarkers(markers: List<TagMarker>) {
        this.markers = markers
        LOG.info("FlashJump: setMarkers called with ${markers.size} markers")
        repaint()
    }
    
    /**
     * 设置已输入的标签前缀（用于部分标签置灰显示）
     */
    fun setTypedLabelPrefix(prefix: String) {
        this.typedLabelPrefix = prefix
        repaint()
    }
    
    /**
     * 设置默认匹配项（橘黄色高亮）
     */
    fun setDefaultMatch(match: SearchMatch?) {
        this.defaultMatch = match
        repaint()
    }
    
    /**
     * 设置高亮的匹配项
     */
    fun setHighlightMatches(matches: List<SearchMatch>) {
        this.highlightMatches = matches
        LOG.info("FlashJump: setHighlightMatches called with ${matches.size} matches")
        repaint()
    }
    
    /**
     * 设置是否显示背景遮罩
     */
    fun setShowBackdrop(show: Boolean) {
        this.showBackdrop = show
        repaint()
    }
    
    /**
     * 清除所有标记
     */
    fun removeMarkers() {
        markers = emptyList()
        highlightMatches = emptyList()
        showBackdrop = false
        repaint()
    }
    
    override fun caretPositionChanged(event: CaretEvent) {
        repaint()
    }
    
    override fun paint(g: Graphics) {
        // 更新大小
        val contentComponent = editor.contentComponent
        setBounds(0, 0, contentComponent.width, contentComponent.height)
        
        // 使用 read 包裹整个绘制过程
        read {
            if (showBackdrop || markers.isNotEmpty() || highlightMatches.isNotEmpty()) {
                super.paint(g)
            }
        }
    }
    
    override fun paintChildren(g: Graphics) {
        super.paintChildren(g)
        
        if (!showBackdrop && markers.isEmpty() && highlightMatches.isEmpty()) {
            return
        }
        
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        val config = FlashConfig.getInstance()
        
        // 绘制背景遮罩
        if (showBackdrop && config.showBackdrop) {
            paintBackdrop(g2d, config)
        }
        
        // 绘制匹配高亮
        if (config.highlightMatches && highlightMatches.isNotEmpty()) {
            paintMatchHighlights(g2d, config)
        }
        
        // 绘制标签
        if (markers.isNotEmpty()) {
            paintMarkers(g2d)
        }
    }
    
    /**
     * 绘制背景遮罩
     */
    private fun paintBackdrop(g: Graphics2D, config: FlashConfig) {
        g.color = Color(0, 0, 0, config.backdropAlpha)
        g.fillRect(0, 0, width, height)
    }
    
    /**
     * 绘制匹配高亮
     */
    private fun paintMatchHighlights(g: Graphics2D, config: FlashConfig) {
        val highlightColor = Color(config.matchHighlightColor, true)
        
        for (match in highlightMatches) {
            if (match.editor != editor) continue
            
            try {
                val startPoint = editor.offsetToXY(match.startOffset)
                val endPoint = editor.offsetToXY(match.endOffset)
                
                val width = if (endPoint.x > startPoint.x) {
                    endPoint.x - startPoint.x
                } else {
                    // 匹配跨行时，高亮到行尾
                    this.width - startPoint.x
                }
                
                g.color = highlightColor
                g.fillRect(startPoint.x, startPoint.y, width.coerceAtLeast(8), editor.lineHeight)
            } catch (e: Exception) {
                LOG.warn("FlashJump: Error painting highlight", e)
            }
        }
    }
    
    /**
     * 绘制标签标记
     */
    private fun paintMarkers(g: Graphics2D) {
        val font = TagFont(editor)
        val occupied = mutableListOf<Rectangle>()
        val foldingModel = editor.foldingModel
        
        LOG.info("FlashJump: painting ${markers.size} markers, prefix='$typedLabelPrefix'")
        
        // 过滤出与当前前缀匹配的标签
        val visibleMarkers = if (typedLabelPrefix.isEmpty()) {
            markers
        } else {
            markers.filter { it.label.lowercase().startsWith(typedLabelPrefix.lowercase()) }
        }
        
        // 先绘制默认匹配项（橘黄色）
        val defaultMarker = visibleMarkers.find { it.match == defaultMatch }
        defaultMarker?.let {
            try {
                if (!foldingModel.isOffsetCollapsed(it.match.startOffset)) {
                    it.paint(g, editor, font, occupied, typedLabelPrefix, isDefault = true)
                }
            } catch (e: Exception) {
                LOG.warn("FlashJump: Error painting default marker", e)
            }
        }
        
        // 绘制其他标签
        for (marker in visibleMarkers) {
            if (marker !== defaultMarker) {
                try {
                    // 跳过折叠区域的标记
                    if (!foldingModel.isOffsetCollapsed(marker.match.startOffset)) {
                        marker.paint(g, editor, font, occupied, typedLabelPrefix, isDefault = false)
                    }
                } catch (e: Exception) {
                    LOG.warn("FlashJump: Error painting marker", e)
                }
            }
        }
    }
}
