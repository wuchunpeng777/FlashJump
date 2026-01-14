package com.github.wuchunpeng777.flashjump.session

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.colors.EditorColors
import com.github.wuchunpeng777.flashjump.boundaries.Boundaries
import com.github.wuchunpeng777.flashjump.boundaries.StandardBoundaries
import com.github.wuchunpeng777.flashjump.config.FlashConfig
import com.github.wuchunpeng777.flashjump.input.EditorKeyListener
import com.github.wuchunpeng777.flashjump.input.JumpMode
import com.github.wuchunpeng777.flashjump.input.SpecialKeyHandler
import com.github.wuchunpeng777.flashjump.input.JumpModeTracker
import com.github.wuchunpeng777.flashjump.labeler.Labeler
import com.github.wuchunpeng777.flashjump.labeler.LabelResult
import com.github.wuchunpeng777.flashjump.search.SearchMatch
import com.github.wuchunpeng777.flashjump.search.SearchProcessor
import com.github.wuchunpeng777.flashjump.search.SearchQuery
import com.github.wuchunpeng777.flashjump.view.TagCanvas
import com.github.wuchunpeng777.flashjump.view.TagMarker
import java.awt.Color

/**
 * FlashJump 会话
 * 管理一次完整的跳转交互
 */
class Session(
    private val mainEditor: Editor,
    private val jumpEditors: List<Editor>
) {
    private val LOG = Logger.getInstance(Session::class.java)
    
    private var boundaries: Boundaries = StandardBoundaries.getDefault()
    private val jumpModeTracker = JumpModeTracker()
    
    private var jumpMode: JumpMode = JumpMode.DISABLED
        set(value) {
            field = value
            LOG.info("FlashJump: jumpMode set to $value")
            if (value == JumpMode.DISABLED) {
                end()
            } else {
                updateHighlights()
                value.caretColor?.let { 
                    mainEditor.colorsScheme.setColor(EditorColors.CARET_COLOR, it) 
                }
            }
        }
    
    private var searchProcessor: SearchProcessor? = null
    private var labeler = Labeler(mainEditor)
    private val tagCanvases = jumpEditors.associateWith { TagCanvas(it) }
    
    private var originalCaretColor: Color? = null
    private var currentPattern: String = ""
    private var defaultMatch: SearchMatch? = null
    
    /**
     * 会话监听器
     */
    private val listeners = mutableListOf<SessionListener>()
    
    init {
        LOG.info("FlashJump: Session created for ${jumpEditors.size} editors")
        
        // 保存原始光标颜色
        originalCaretColor = mainEditor.colorsScheme.getColor(EditorColors.CARET_COLOR)
        
        // 设置键盘监听
        EditorKeyListener.attach(
            mainEditor,
            object : TypedActionHandler {
                override fun execute(editor: Editor, charTyped: Char, context: DataContext) {
                    handleTypedChar(charTyped)
                }
            },
            object : SpecialKeyHandler {
                override fun onEnter() {
                    handleEnter()
                }
                
                override fun onEscape() {
                    handleEscape()
                }
                
                override fun onBackspace() {
                    handleBackspace()
                }
            }
        )
    }
    
    /**
     * 处理 Enter 键
     */
    private fun handleEnter() {
        LOG.info("FlashJump: handleEnter, hasLabels=${labeler.hasLabels()}")
        if (!labeler.hasLabels()) {
            // 没有显示标签时，退出
            end(JumpResult.Cancelled)
        } else {
            // 有标签时，跳转到默认匹配项
            val match = defaultMatch
            if (match != null) {
                performJump(match)
            } else {
                end(JumpResult.Cancelled)
            }
        }
    }
    
    /**
     * 处理 Escape 键
     */
    private fun handleEscape() {
        LOG.info("FlashJump: handleEscape")
        end(JumpResult.Cancelled)
    }
    
    /**
     * 处理 Backspace 键
     */
    private fun handleBackspace() {
        LOG.info("FlashJump: handleBackspace, currentPattern='$currentPattern'")
        if (currentPattern.isNotEmpty()) {
            // 删除最后一个字符
            currentPattern = currentPattern.dropLast(1)
            
            if (currentPattern.isEmpty()) {
                // 清空搜索，重置状态
                restart()
            } else {
                // 重新搜索
                searchProcessor?.search(SearchQuery.Literal(currentPattern))
                labeler.resetPrefix()
                updateSearch()
            }
        } else {
            // 没有搜索内容时，退出
            end(JumpResult.Cancelled)
        }
    }
    
    /**
     * 处理键入的字符
     */
    private fun handleTypedChar(char: Char) {
        LOG.info("FlashJump: handleTypedChar '$char', currentPattern='$currentPattern'")
        
        val processor = searchProcessor
        val minPatternLength = FlashConfig.getInstance().minPatternLength
        
        if (processor == null) {
            // 开始新搜索
            startSearch(char)
        } else {
            // 如果还没达到最小搜索长度，优先继续搜索
            if (currentPattern.length < minPatternLength) {
                appendToSearch(char)
                return
            }
            
            // 先尝试继续搜索，看是否有匹配
            val newPattern = currentPattern + char
            val testProcessor = SearchProcessor.fromChar(jumpEditors, ' ', boundaries)
            testProcessor.search(SearchQuery.Literal(newPattern))
            
            if (testProcessor.matchCount > 0) {
                // 有匹配，继续搜索
                labeler.resetPrefix()  // 重置标签前缀
                appendToSearch(char)
            } else {
                // 没有匹配，尝试作为标签处理
                val result = labeler.processLabelInput(char)
                LOG.info("FlashJump: label result = $result")
                
                when (result) {
                    is LabelResult.Jump -> {
                        performJump(result.match)
                    }
                    is LabelResult.Partial -> {
                        // 更新显示，显示已输入的前缀
                        updateLabelPrefix(result.prefix)
                    }
                    is LabelResult.Invalid -> {
                        LOG.info("FlashJump: char '$char' is neither valid search nor label")
                    }
                }
            }
        }
    }
    
    /**
     * 更新标签前缀显示
     */
    private fun updateLabelPrefix(prefix: String) {
        LOG.info("FlashJump: updateLabelPrefix '$prefix'")
        for ((_, canvas) in tagCanvases) {
            canvas.setTypedLabelPrefix(prefix)
        }
    }
    
    /**
     * 开始搜索
     */
    private fun startSearch(char: Char) {
        currentPattern = char.toString()
        LOG.info("FlashJump: startSearch with '$currentPattern'")
        
        searchProcessor = SearchProcessor.fromChar(jumpEditors, char, boundaries)
        val matchCount = searchProcessor?.matchCount ?: 0
        LOG.info("FlashJump: found $matchCount matches")
        
        if (matchCount == 0) {
            LOG.info("FlashJump: no matches found, exiting")
            end(JumpResult.Cancelled)
            return
        }
        
        updateSearch()
    }
    
    /**
     * 追加字符到搜索
     */
    private fun appendToSearch(char: Char) {
        val processor = searchProcessor ?: return
        
        // 追加字符到搜索模式
        currentPattern += char
        LOG.info("FlashJump: appendToSearch, pattern='$currentPattern'")
        
        processor.search(SearchQuery.Literal(currentPattern))
        
        if (processor.matchCount == 0) {
            // 没有匹配，退出
            LOG.info("FlashJump: no matches for pattern='$currentPattern', exiting")
            end(JumpResult.Cancelled)
        } else {
            updateSearch()
            
            // 自动跳转（只有一个匹配时）
            if (FlashConfig.getInstance().autoJump && processor.matchCount == 1) {
                val singleMatch = processor.allMatches.first()
                performJump(singleMatch)
            }
        }
    }
    
    /**
     * 更新搜索结果和标签
     */
    private fun updateSearch() {
        val processor = searchProcessor ?: return
        val matches = processor.allMatches
        
        LOG.info("FlashJump: updateSearch with ${matches.size} matches")
        
        // 计算默认匹配项
        defaultMatch = findDefaultMatch(matches)
        LOG.info("FlashJump: default match at offset ${defaultMatch?.startOffset}")
        
        // 分配标签
        val minPatternLength = FlashConfig.getInstance().minPatternLength
        if (currentPattern.length >= minPatternLength) {
            val labelMap = labeler.assignLabels(matches, currentPattern)
            LOG.info("FlashJump: assigned ${labelMap.size} labels")
        }
        
        // 更新视图
        updateHighlights()
        updateMarkers()
    }
    
    /**
     * 计算默认匹配项
     * 优先：光标后的第一个匹配
     * 其次：光标前的第一个匹配（最近的）
     */
    private fun findDefaultMatch(matches: List<SearchMatch>): SearchMatch? {
        if (matches.isEmpty()) return null
        
        val caretOffset = mainEditor.caretModel.offset
        
        // 只考虑当前编辑器中的匹配
        val editorMatches = matches.filter { it.editor == mainEditor }
        if (editorMatches.isEmpty()) return matches.firstOrNull()
        
        // 光标后的第一个匹配
        val afterCaret = editorMatches
            .filter { it.startOffset > caretOffset }
            .minByOrNull { it.startOffset }
        
        if (afterCaret != null) return afterCaret
        
        // 光标前的最近匹配
        val beforeCaret = editorMatches
            .filter { it.startOffset <= caretOffset }
            .maxByOrNull { it.startOffset }
        
        return beforeCaret ?: editorMatches.firstOrNull()
    }
    
    /**
     * 更新高亮
     */
    private fun updateHighlights() {
        val processor = searchProcessor
        val matches = processor?.allMatches ?: emptyList()
        val showBackdrop = jumpMode != JumpMode.DISABLED
        
        for ((editor, canvas) in tagCanvases) {
            val editorMatches = matches.filter { it.editor == editor }
            canvas.setHighlightMatches(editorMatches)
            canvas.setShowBackdrop(showBackdrop)
        }
    }
    
    /**
     * 更新标签标记
     */
    private fun updateMarkers() {
        val labeledMatches = labeler.getLabeledMatches()
        LOG.info("FlashJump: updateMarkers with ${labeledMatches.size} labeled matches")
        
        for ((editor, canvas) in tagCanvases) {
            val editorMarkers = labeledMatches
                .filter { it.editor == editor && it.label != null }
                .map { TagMarker.create(it, it.label!!) }
            LOG.info("FlashJump: setting ${editorMarkers.size} markers for editor")
            canvas.setMarkers(editorMarkers)
            
            // 设置默认匹配项（橘黄色高亮）
            if (defaultMatch?.editor == editor) {
                canvas.setDefaultMatch(defaultMatch)
            } else {
                canvas.setDefaultMatch(null)
            }
        }
    }
    
    /**
     * 执行跳转
     */
    private fun performJump(match: SearchMatch) {
        LOG.info("FlashJump: performJump to offset ${match.startOffset}")
        val editor = match.editor
        
        when (jumpMode) {
            JumpMode.JUMP, JumpMode.DEFINITION -> {
                // 移动光标到匹配开始位置
                editor.caretModel.moveToOffset(match.startOffset)
            }
            JumpMode.JUMP_END -> {
                // 移动光标到匹配结束位置
                editor.caretModel.moveToOffset(match.endOffset)
            }
            JumpMode.TARGET -> {
                // 选择从当前位置到目标位置的文本
                val currentOffset = mainEditor.caretModel.offset
                mainEditor.selectionModel.setSelection(
                    minOf(currentOffset, match.startOffset),
                    maxOf(currentOffset, match.endOffset)
                )
                editor.caretModel.moveToOffset(match.startOffset)
            }
            JumpMode.DISABLED -> {}
        }
        
        // 确保可见
        editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
        
        // 通知监听器
        listeners.forEach { it.onJump(match) }
        
        // 结束会话
        end(JumpResult.Success(match))
    }
    
    /**
     * 开始正则搜索
     */
    fun startRegexSearch(pattern: String, searchBoundaries: Boundaries = boundaries) {
        LOG.info("FlashJump: startRegexSearch with pattern '$pattern'")
        labeler.clear()
        tagCanvases.values.forEach { it.removeMarkers() }
        
        boundaries = searchBoundaries.intersection(StandardBoundaries.getDefault())
        searchProcessor = SearchProcessor.fromRegex(jumpEditors, pattern, boundaries)
        currentPattern = pattern
        
        updateSearch()
    }
    
    /**
     * 循环切换跳转模式
     */
    fun cycleNextJumpMode() {
        jumpMode = jumpModeTracker.cycle(forward = true)
    }
    
    /**
     * 反向循环切换跳转模式
     */
    fun cyclePreviousJumpMode() {
        jumpMode = jumpModeTracker.cycle(forward = false)
    }
    
    /**
     * 切换跳转模式
     */
    fun toggleJumpMode(mode: JumpMode) {
        jumpMode = jumpModeTracker.toggle(mode)
    }
    
    /**
     * 设置边界并切换模式
     */
    fun toggleJumpMode(mode: JumpMode, searchBoundaries: Boundaries) {
        boundaries = boundaries.intersection(searchBoundaries)
        toggleJumpMode(mode)
    }
    
    /**
     * 跳转到默认匹配项（按 Enter 时调用）
     * 如果还没显示标签（搜索长度不够），则退出 flash 模式
     */
    fun jumpToDefault() {
        val minPatternLength = FlashConfig.getInstance().minPatternLength
        
        // 如果还没达到最小搜索长度，退出
        if (currentPattern.length < minPatternLength) {
            LOG.info("FlashJump: pattern too short, exiting")
            end(JumpResult.Cancelled)
            return
        }
        
        val match = defaultMatch
        if (match != null) {
            LOG.info("FlashJump: jumping to default match at ${match.startOffset}")
            performJump(match)
        } else {
            LOG.info("FlashJump: no default match, exiting")
            end(JumpResult.Cancelled)
        }
    }
    
    /**
     * 重新开始
     */
    fun restart() {
        LOG.info("FlashJump: restart")
        labeler.clear()
        searchProcessor = null
        currentPattern = ""
        defaultMatch = null
        tagCanvases.values.forEach { 
            it.setTypedLabelPrefix("")
            it.setDefaultMatch(null)
            it.removeMarkers() 
        }
    }
    
    /**
     * 结束会话
     */
    fun end(result: JumpResult? = null) {
        LOG.info("FlashJump: end session")
        SessionManager.end(mainEditor, result)
    }
    
    /**
     * 清理资源
     */
    internal fun dispose(result: JumpResult?) {
        LOG.info("FlashJump: dispose session")
        labeler.clear()
        EditorKeyListener.detach(mainEditor)
        tagCanvases.values.forEach { it.unbind() }
        
        // 恢复光标颜色
        if (!mainEditor.isDisposed) {
            originalCaretColor?.let {
                mainEditor.colorsScheme.setColor(EditorColors.CARET_COLOR, it)
            }
        }
        
        // 通知监听器
        if (result is JumpResult.Success) {
            listeners.forEach { it.onJump(result.match) }
        }
        listeners.forEach { it.onSessionEnd(result) }
        
        // 滚动到光标
        val focusedEditor = (result as? JumpResult.Success)?.match?.editor ?: mainEditor
        if (!focusedEditor.isDisposed) {
            focusedEditor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
        }
    }
    
    /**
     * 添加监听器
     */
    fun addListener(listener: SessionListener) {
        listeners.add(listener)
    }
    
    /**
     * 移除监听器
     */
    fun removeListener(listener: SessionListener) {
        listeners.remove(listener)
    }
}

/**
 * 跳转结果
 */
sealed class JumpResult {
    data class Success(val match: SearchMatch) : JumpResult()
    data object Cancelled : JumpResult()
}

/**
 * 会话监听器
 */
interface SessionListener {
    fun onJump(match: SearchMatch) {}
    fun onSessionEnd(result: JumpResult?) {}
}
