package com.github.wuchunpeng777.flashjump.labeler

import com.intellij.openapi.editor.Editor
import com.github.wuchunpeng777.flashjump.config.FlashConfig
import com.github.wuchunpeng777.flashjump.search.SearchMatch

/**
 * 标签分配器
 * 负责为搜索匹配结果分配跳转标签
 * 支持多字符标签（当匹配项超过单字符标签数量时）
 */
class Labeler(private val mainEditor: Editor) {
    
    private val labelToMatch = mutableMapOf<String, SearchMatch>()
    
    // 当前已输入的标签前缀
    private var currentLabelPrefix: String = ""
    
    /**
     * 为匹配结果分配标签
     * @param matches 所有匹配结果
     * @param pattern 当前搜索模式（用于跳过会与模式冲突的标签）
     * @return 标签到匹配的映射
     */
    fun assignLabels(matches: List<SearchMatch>, pattern: String): Map<String, SearchMatch> {
        labelToMatch.clear()
        currentLabelPrefix = ""
        
        if (matches.isEmpty()) return emptyMap()
        
        // 获取可用的单字符标签
        val availableChars = getAvailableLabels(pattern, matches)
        if (availableChars.isEmpty()) return emptyMap()
        
        // 对匹配结果排序：优先当前编辑器，然后按距离光标远近
        val sortedMatches = sortMatches(matches)
        
        // 计算需要的标签长度
        val labelLength = calculateLabelLength(sortedMatches.size, availableChars.size)
        
        // 生成标签并分配
        val labels = generateLabels(availableChars, labelLength, sortedMatches.size)
        
        for ((index, match) in sortedMatches.withIndex()) {
            if (index >= labels.size) break
            
            val label = labels[index]
            match.label = label
            labelToMatch[label] = match
        }
        
        return labelToMatch.toMap()
    }
    
    /**
     * 计算需要的标签长度
     */
    private fun calculateLabelLength(matchCount: Int, charCount: Int): Int {
        if (charCount == 0) return 1
        
        var length = 1
        var capacity = charCount
        
        while (capacity < matchCount) {
            length++
            capacity = charCount * capacity
            if (length > 3) break // 最多3个字符
        }
        
        return length
    }
    
    /**
     * 生成标签列表
     */
    private fun generateLabels(chars: List<String>, length: Int, count: Int): List<String> {
        if (length == 1) {
            return chars.take(count)
        }
        
        val labels = mutableListOf<String>()
        
        // 对于多字符标签，使用组合
        fun generate(prefix: String, depth: Int) {
            if (labels.size >= count) return
            
            if (depth == length) {
                labels.add(prefix)
                return
            }
            
            for (char in chars) {
                if (labels.size >= count) return
                generate(prefix + char, depth + 1)
            }
        }
        
        generate("", 0)
        return labels
    }
    
    /**
     * 获取可用的标签字符列表
     * 排除会与当前搜索模式匹配的字符
     */
    private fun getAvailableLabels(pattern: String, matches: List<SearchMatch>): List<String> {
        val allLabels = FlashConfig.getLabels().map { it.toString().lowercase() }.distinct()
        
        if (pattern.isEmpty()) return allLabels
        
        // 找出文本中紧跟搜索模式之后的字符，这些字符不应该作为标签
        val skipChars = mutableSetOf<String>()
        
        for (match in matches) {
            val text = match.editor.document.charsSequence
            val nextCharOffset = match.startOffset + pattern.length
            if (nextCharOffset < text.length) {
                skipChars.add(text[nextCharOffset].lowercaseChar().toString())
            }
        }
        
        // 过滤掉会冲突的标签
        return allLabels.filter { label -> label !in skipChars }
    }
    
    /**
     * 对匹配结果进行排序
     * 1. 当前编辑器优先
     * 2. 可见区域优先
     * 3. 距离光标近的优先
     */
    private fun sortMatches(matches: List<SearchMatch>): List<SearchMatch> {
        return matches.sortedWith(compareBy(
            // 当前编辑器优先
            { if (it.editor == mainEditor) 0 else 1 },
            // 可见区域优先
            { if (it.isVisible()) 0 else 1 },
            // 距离光标近的优先
            { it.distanceToCaret() }
        ))
    }
    
    /**
     * 处理标签输入
     * @param char 输入的字符
     * @return 标签结果
     */
    fun processLabelInput(char: Char): LabelResult {
        val input = char.lowercaseChar().toString()
        val newPrefix = currentLabelPrefix + input
        
        // 查找完全匹配的标签
        val exactMatch = labelToMatch[newPrefix]
        if (exactMatch != null) {
            currentLabelPrefix = ""
            return LabelResult.Jump(exactMatch, newPrefix)
        }
        
        // 查找以 newPrefix 开头的标签
        val partialMatches = labelToMatch.keys.filter { it.startsWith(newPrefix) }
        
        return when {
            partialMatches.isEmpty() -> {
                // 没有匹配，重置
                currentLabelPrefix = ""
                LabelResult.Invalid
            }
            partialMatches.size == 1 -> {
                // 只有一个匹配，直接跳转
                val label = partialMatches.first()
                val match = labelToMatch[label]!!
                currentLabelPrefix = ""
                LabelResult.Jump(match, label)
            }
            else -> {
                // 多个匹配，更新前缀
                currentLabelPrefix = newPrefix
                LabelResult.Partial(newPrefix)
            }
        }
    }
    
    /**
     * 获取当前标签前缀
     */
    fun getCurrentPrefix(): String = currentLabelPrefix
    
    /**
     * 根据标签查找匹配
     */
    fun findMatchByLabel(label: String): SearchMatch? {
        return labelToMatch[label.lowercase()]
    }
    
    /**
     * 检查字符是否是一个有效的标签（或标签前缀）
     */
    fun isValidLabelChar(char: Char): Boolean {
        val input = char.lowercaseChar().toString()
        val testPrefix = currentLabelPrefix + input
        
        // 检查是否有标签以此为前缀
        return labelToMatch.keys.any { it.startsWith(testPrefix) }
    }
    
    /**
     * 清除所有标签分配
     */
    fun clear() {
        labelToMatch.clear()
        currentLabelPrefix = ""
    }
    
    /**
     * 获取所有已分配标签的匹配
     */
    fun getLabeledMatches(): List<SearchMatch> {
        return labelToMatch.values.toList()
    }
    
    /**
     * 重置标签前缀
     */
    fun resetPrefix() {
        currentLabelPrefix = ""
    }
}

/**
 * 标签结果
 */
sealed class LabelResult {
    /**
     * 跳转到目标
     */
    data class Jump(val match: SearchMatch, val label: String) : LabelResult()
    
    /**
     * 继续等待输入（多字符标签）
     */
    data class Partial(val prefix: String) : LabelResult()
    
    /**
     * 无效标签
     */
    data object Invalid : LabelResult()
}
