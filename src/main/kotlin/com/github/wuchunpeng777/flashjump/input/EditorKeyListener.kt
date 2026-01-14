package com.github.wuchunpeng777.flashjump.input

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.diagnostic.Logger

/**
 * 编辑器键盘监听器
 * 在 FlashJump 会话期间捕获按键输入
 */
internal object EditorKeyListener : TypedActionHandler {
    private val LOG = Logger.getInstance(EditorKeyListener::class.java)
    
    private val attached = mutableMapOf<Editor, TypedActionHandler>()
    private var originalHandler: TypedActionHandler? = null
    
    override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
        val handler = attached[editor]
        if (handler != null) {
            LOG.info("FlashJump: handling char '$charTyped'")
            handler.execute(editor, charTyped, dataContext)
        } else {
            originalHandler?.execute(editor, charTyped, dataContext)
        }
    }
    
    /**
     * 附加键盘监听到编辑器
     */
    fun attach(editor: Editor, callback: TypedActionHandler) {
        if (attached.isEmpty()) {
            @Suppress("DEPRECATION")
            val typedAction = TypedAction.getInstance()
            originalHandler = typedAction.rawHandler
            typedAction.setupRawHandler(this)
            LOG.info("FlashJump: attached keyboard listener")
        }
        
        attached[editor] = callback
    }
    
    /**
     * 从编辑器分离键盘监听
     */
    fun detach(editor: Editor) {
        attached.remove(editor)
        
        if (attached.isEmpty()) {
            @Suppress("DEPRECATION")
            originalHandler?.let(TypedAction.getInstance()::setupRawHandler)
            originalHandler = null
            LOG.info("FlashJump: detached keyboard listener")
        }
    }
    
    /**
     * 检查编辑器是否有活动的监听器
     */
    fun isListening(editor: Editor): Boolean = attached.containsKey(editor)
}

/**
 * 跳转模式枚举
 */
enum class JumpMode(val caretColor: java.awt.Color?) {
    /**
     * 禁用状态
     */
    DISABLED(null),
    
    /**
     * 普通跳转模式
     */
    JUMP(java.awt.Color(0, 150, 255)),
    
    /**
     * 跳转到单词结尾
     */
    JUMP_END(java.awt.Color(255, 150, 0)),
    
    /**
     * 目标模式（选择文本）
     */
    TARGET(java.awt.Color(255, 50, 100)),
    
    /**
     * 定义跳转模式
     */
    DEFINITION(java.awt.Color(100, 255, 100))
}

/**
 * 跳转模式追踪器
 */
class JumpModeTracker {
    private val modes = listOf(JumpMode.JUMP, JumpMode.JUMP_END, JumpMode.TARGET)
    private var currentIndex = -1
    
    /**
     * 循环切换模式
     */
    fun cycle(forward: Boolean = true): JumpMode {
        currentIndex = if (forward) {
            (currentIndex + 1) % modes.size
        } else {
            if (currentIndex <= 0) modes.size - 1 else currentIndex - 1
        }
        return modes[currentIndex]
    }
    
    /**
     * 切换到指定模式
     */
    fun toggle(mode: JumpMode): JumpMode {
        val index = modes.indexOf(mode)
        return if (index >= 0) {
            currentIndex = index
            mode
        } else {
            JumpMode.DISABLED
        }
    }
    
    /**
     * 重置
     */
    fun reset() {
        currentIndex = -1
    }
    
    val current: JumpMode
        get() = if (currentIndex >= 0) modes[currentIndex] else JumpMode.DISABLED
}
