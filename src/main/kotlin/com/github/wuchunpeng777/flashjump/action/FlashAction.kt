package com.github.wuchunpeng777.flashjump.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.IncorrectOperationException
import com.github.wuchunpeng777.flashjump.boundaries.Boundaries
import com.github.wuchunpeng777.flashjump.boundaries.StandardBoundaries
import com.github.wuchunpeng777.flashjump.config.FlashConfig
import com.github.wuchunpeng777.flashjump.input.JumpMode
import com.github.wuchunpeng777.flashjump.search.SearchQuery
import com.github.wuchunpeng777.flashjump.session.Session
import com.github.wuchunpeng777.flashjump.session.SessionManager

/**
 * FlashJump 基础 Action
 * 所有跳转动作的基类
 */
sealed class FlashAction : DumbAwareAction() {
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    override fun update(e: AnActionEvent) {
        val editor = getEditor(e)
        e.presentation.isEnabled = editor != null
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val editor = getEditor(e) ?: return
        val project = e.project
        
        val session = if (project != null) {
            try {
                val fem = FileEditorManager.getInstance(project) as FileEditorManagerEx
                val config = FlashConfig.getInstance()
                
                val openEditors = if (config.multiWindow) {
                    fem.splitters.getSelectedEditors()
                        .mapNotNull { (it as? TextEditor)?.editor }
                        .sortedBy { if (it === editor) 0 else 1 }
                } else {
                    listOf(editor)
                }
                
                SessionManager.start(editor, openEditors)
            } catch (ex: IncorrectOperationException) {
                SessionManager.start(editor)
            }
        } else {
            SessionManager.start(editor)
        }
        
        invoke(session)
    }
    
    /**
     * 获取编辑器
     */
    private fun getEditor(e: AnActionEvent): Editor? {
        return e.getData(CommonDataKeys.EDITOR)
            ?: (e.getData(PlatformDataKeys.LAST_ACTIVE_FILE_EDITOR) as? TextEditor)?.editor
    }
    
    /**
     * 子类实现具体的跳转逻辑
     */
    abstract operator fun invoke(session: Session)
    
    // ============ 具体的 Action 实现 ============
    
    /**
     * 激活或循环切换模式
     */
    class ActivateOrCycleMode : FlashAction() {
        override fun invoke(session: Session) = session.cycleNextJumpMode()
    }
    
    /**
     * 激活或反向循环切换模式
     */
    class ActivateOrReverseCycleMode : FlashAction() {
        override fun invoke(session: Session) = session.cyclePreviousJumpMode()
    }
    
    /**
     * 普通跳转模式
     */
    class ToggleJumpMode : FlashAction() {
        override fun invoke(session: Session) = session.toggleJumpMode(JumpMode.JUMP)
    }
    
    /**
     * 跳转到结尾模式
     */
    class ToggleJumpEndMode : FlashAction() {
        override fun invoke(session: Session) = session.toggleJumpMode(JumpMode.JUMP_END)
    }
    
    /**
     * 目标选择模式
     */
    class ToggleTargetMode : FlashAction() {
        override fun invoke(session: Session) = session.toggleJumpMode(JumpMode.TARGET)
    }
    
    /**
     * 向前跳转模式（光标之后）
     */
    class ToggleForwardJumpMode : FlashAction() {
        override fun invoke(session: Session) {
            session.toggleJumpMode(JumpMode.JUMP, StandardBoundaries.AFTER_CARET)
        }
    }
    
    /**
     * 向后跳转模式（光标之前）
     */
    class ToggleBackwardJumpMode : FlashAction() {
        override fun invoke(session: Session) {
            session.toggleJumpMode(JumpMode.JUMP, StandardBoundaries.BEFORE_CARET)
        }
    }
    
    // ============ 正则搜索 Actions ============
    
    /**
     * 正则搜索基类
     */
    abstract class BaseRegexSearchAction(
        private val pattern: String,
        private val boundaries: Boundaries = StandardBoundaries.VISIBLE_ON_SCREEN
    ) : FlashAction() {
        override fun invoke(session: Session) {
            session.toggleJumpMode(JumpMode.JUMP)
            session.startRegexSearch(pattern, boundaries)
        }
    }
    
    /**
     * 所有单词模式
     */
    class StartAllWordsMode : BaseRegexSearchAction("\\b\\w", StandardBoundaries.WHOLE_FILE)
    
    /**
     * 光标后所有单词
     */
    class StartAllWordsForwardMode : BaseRegexSearchAction("\\b\\w", StandardBoundaries.AFTER_CARET)
    
    /**
     * 光标前所有单词
     */
    class StartAllWordsBackwardsMode : BaseRegexSearchAction("\\b\\w", StandardBoundaries.BEFORE_CARET)
    
    /**
     * 所有行首
     */
    class StartAllLineStartsMode : BaseRegexSearchAction("^", StandardBoundaries.VISIBLE_ON_SCREEN)
    
    /**
     * 所有行尾
     */
    class StartAllLineEndsMode : BaseRegexSearchAction("$", StandardBoundaries.VISIBLE_ON_SCREEN)
    
    /**
     * 所有行缩进位置
     */
    class StartAllLineIndentsMode : BaseRegexSearchAction("^\\s*\\S", StandardBoundaries.VISIBLE_ON_SCREEN)
    
    /**
     * 所有行标记（行首 + 行尾）
     */
    class StartAllLineMarksMode : BaseRegexSearchAction("^|$", StandardBoundaries.VISIBLE_ON_SCREEN)
}

/**
 * 重置/取消 Action
 */
class FlashResetAction : DumbAwareAction() {
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && SessionManager.hasActiveSession(editor)
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        SessionManager.end(editor)
    }
}
