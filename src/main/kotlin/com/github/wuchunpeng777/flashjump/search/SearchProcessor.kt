package com.github.wuchunpeng777.flashjump.search

import com.intellij.openapi.editor.Editor
import com.github.wuchunpeng777.flashjump.boundaries.Boundaries
import com.github.wuchunpeng777.flashjump.boundaries.StandardBoundaries

/**
 * 搜索处理器
 * 负责在编辑器中执行搜索并收集匹配结果
 */
class SearchProcessor(
    private val editors: List<Editor>,
    private val boundaries: Boundaries = StandardBoundaries.VISIBLE_ON_SCREEN
) {
    private var _query: SearchQuery = SearchQuery.Literal("")
    private var _results: MutableMap<Editor, MutableList<SearchMatch>> = mutableMapOf()
    
    val query: SearchQuery get() = _query
    val results: Map<Editor, List<SearchMatch>> get() = _results
    
    /**
     * 获取所有匹配结果的扁平列表
     */
    val allMatches: List<SearchMatch>
        get() = _results.values.flatten()
    
    /**
     * 匹配结果总数
     */
    val matchCount: Int
        get() = _results.values.sumOf { it.size }
    
    companion object {
        /**
         * 从字符创建搜索处理器
         */
        fun fromChar(editors: List<Editor>, char: Char, boundaries: Boundaries): SearchProcessor {
            return SearchProcessor(editors, boundaries).apply {
                search(SearchQuery.Literal(char.toString()))
            }
        }
        
        /**
         * 从正则表达式创建搜索处理器
         */
        fun fromRegex(editors: List<Editor>, pattern: String, boundaries: Boundaries): SearchProcessor {
            return SearchProcessor(editors, boundaries).apply {
                search(SearchQuery.RegularExpression(pattern))
            }
        }
        
        /**
         * 创建单词搜索处理器
         */
        fun forAllWords(editors: List<Editor>, boundaries: Boundaries): SearchProcessor {
            return SearchProcessor(editors, boundaries).apply {
                search(SearchQuery.AllWords)
            }
        }
        
        /**
         * 创建行首搜索处理器
         */
        fun forLineStarts(editors: List<Editor>, boundaries: Boundaries): SearchProcessor {
            return SearchProcessor(editors, boundaries).apply {
                search(SearchQuery.LinePattern.LineStart)
            }
        }
    }
    
    /**
     * 执行搜索
     */
    fun search(query: SearchQuery) {
        this._query = query
        this._results.clear()
        
        val regex = query.toRegex() ?: return
        
        for (editor in editors) {
            val matches = mutableListOf<SearchMatch>()
            val text = editor.document.charsSequence
            val offsetRange = boundaries.getOffsetRange(editor)
            
            var result = regex.find(text, offsetRange.first)
            
            while (result != null) {
                val startOffset = result.range.first
                val highlightLength = query.getHighlightLength(text.toString(), startOffset)
                val endOffset = startOffset + highlightLength.coerceAtLeast(1)
                
                // 检查是否超出范围
                if (endOffset > offsetRange.last) {
                    break
                }
                
                // 检查是否在边界内
                if (boundaries.isOffsetInside(editor, startOffset)) {
                    matches.add(SearchMatch(editor, startOffset, endOffset))
                }
                
                result = result.next()
            }
            
            if (matches.isNotEmpty()) {
                _results[editor] = matches
            }
        }
    }
    
    /**
     * 追加字符到搜索查询并更新结果
     * 返回 true 表示继续搜索，false 表示无效输入
     */
    fun appendChar(char: Char): Boolean {
        val currentQuery = _query
        if (currentQuery !is SearchQuery.Literal) {
            return false
        }
        
        val newQueryText = currentQuery.rawText + char
        val newQuery = SearchQuery.Literal(newQueryText)
        
        // 过滤现有结果
        val newResults = mutableMapOf<Editor, MutableList<SearchMatch>>()
        
        for ((editor, matches) in _results) {
            val text = editor.document.charsSequence
            val filteredMatches = matches.filter { match ->
                val endOffset = match.startOffset + newQueryText.length
                endOffset <= text.length &&
                    text.substring(match.startOffset, endOffset).equals(newQueryText, ignoreCase = true)
            }.toMutableList()
            
            if (filteredMatches.isNotEmpty()) {
                // 更新匹配的结束偏移量
                filteredMatches.forEach { match ->
                    // 创建新的匹配对象，更新 endOffset
                }
                newResults[editor] = filteredMatches
            }
        }
        
        // 如果没有匹配结果，则此字符可能是标签
        if (newResults.isEmpty() && _results.isNotEmpty()) {
            return false
        }
        
        this._query = newQuery
        this._results = newResults
        return true
    }
    
    /**
     * 清除所有结果
     */
    fun clear() {
        _results.clear()
        _query = SearchQuery.Literal("")
    }
    
    /**
     * 获取指定编辑器的匹配结果
     */
    fun getMatchesForEditor(editor: Editor): List<SearchMatch> {
        return _results[editor] ?: emptyList()
    }
}
