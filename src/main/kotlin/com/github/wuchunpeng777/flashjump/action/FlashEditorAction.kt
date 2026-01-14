package com.github.wuchunpeng777.flashjump.action

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.github.wuchunpeng777.flashjump.session.Session
import com.github.wuchunpeng777.flashjump.session.SessionManager

/**
 * FlashJump 编辑器动作处理器
 * 处理 ESC、Backspace 等编辑器级别的按键
 */
sealed class FlashEditorAction(private val originalHandler: EditorActionHandler) : EditorActionHandler() {
    
    final override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
        return SessionManager[editor] != null || originalHandler.isEnabled(editor, caret, dataContext)
    }
    
    final override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
        val session = SessionManager[editor]
        
        if (session != null) {
            run(session)
        } else if (originalHandler.isEnabled(editor, caret, dataContext)) {
            originalHandler.execute(editor, caret, dataContext)
        }
    }
    
    protected abstract fun run(session: Session)
    
    /**
     * ESC 键处理 - 取消会话
     */
    class Reset(originalHandler: EditorActionHandler) : FlashEditorAction(originalHandler) {
        override fun run(session: Session) = session.end()
    }
    
    /**
     * Backspace 键处理 - 清除搜索或取消
     */
    class ClearSearch(originalHandler: EditorActionHandler) : FlashEditorAction(originalHandler) {
        override fun run(session: Session) = session.restart()
    }
    
    /**
     * Enter 键处理 - 跳转到默认匹配项
     */
    class SelectForward(originalHandler: EditorActionHandler) : FlashEditorAction(originalHandler) {
        override fun run(session: Session) {
            session.jumpToDefault()
        }
    }
    
    /**
     * Shift+Enter 键处理 - 跳转到上一个匹配
     */
    class SelectBackward(originalHandler: EditorActionHandler) : FlashEditorAction(originalHandler) {
        override fun run(session: Session) {
            // 可以扩展为跳转到上一个匹配
        }
    }
}
