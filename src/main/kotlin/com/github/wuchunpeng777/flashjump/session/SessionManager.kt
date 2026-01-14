package com.github.wuchunpeng777.flashjump.session

import com.intellij.openapi.editor.Editor
import java.util.concurrent.ConcurrentHashMap

/**
 * 会话管理器
 * 管理所有活动的 FlashJump 会话
 */
object SessionManager {
    
    private val sessions = ConcurrentHashMap<Editor, Session>()
    
    /**
     * 启动新会话或返回现有会话
     * @param editor 主编辑器
     * @return 会话实例
     */
    fun start(editor: Editor): Session {
        return start(editor, listOf(editor))
    }
    
    /**
     * 启动多编辑器会话
     * @param mainEditor 主编辑器（用于输入）
     * @param jumpEditors 所有可跳转的编辑器列表
     * @return 会话实例
     */
    fun start(mainEditor: Editor, jumpEditors: List<Editor>): Session {
        // 清理已释放的编辑器
        cleanup()
        
        // 如果已有会话，返回现有会话
        sessions[mainEditor]?.let { return it }
        
        // 创建新会话
        val session = Session(mainEditor, jumpEditors)
        sessions[mainEditor] = session
        return session
    }
    
    /**
     * 获取编辑器的活动会话
     */
    operator fun get(editor: Editor): Session? = sessions[editor]
    
    /**
     * 结束编辑器的会话
     */
    fun end(editor: Editor, result: JumpResult? = null) {
        sessions.remove(editor)?.dispose(result)
    }
    
    /**
     * 结束所有会话
     */
    fun endAll() {
        sessions.keys.toList().forEach { editor ->
            end(editor, JumpResult.Cancelled)
        }
    }
    
    /**
     * 检查编辑器是否有活动会话
     */
    fun hasActiveSession(editor: Editor): Boolean = sessions.containsKey(editor)
    
    /**
     * 清理已释放的编辑器
     */
    private fun cleanup() {
        sessions.keys
            .filter { it.isDisposed }
            .forEach { editor -> sessions.remove(editor)?.dispose(null) }
    }
}
