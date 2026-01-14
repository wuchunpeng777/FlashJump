package com.github.wuchunpeng777.flashjump.input

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.diagnostic.Logger
import java.awt.AWTEvent
import java.awt.event.KeyEvent

/**
 * 特殊按键回调接口
 */
interface SpecialKeyHandler {
    fun onEnter()
    fun onEscape()
    fun onBackspace()
}

/**
 * 编辑器键盘监听器
 * 在 FlashJump 会话期间捕获按键输入
 */
internal object EditorKeyListener : TypedActionHandler {
    private val LOG = Logger.getInstance(EditorKeyListener::class.java)
    
    private val attached = mutableMapOf<Editor, TypedActionHandler>()
    private val specialHandlers = mutableMapOf<Editor, SpecialKeyHandler>()
    private var originalHandler: TypedActionHandler? = null
    private var eventDispatcher: IdeEventQueue.EventDispatcher? = null
    
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
    fun attach(editor: Editor, callback: TypedActionHandler, specialCallback: SpecialKeyHandler? = null) {
        if (attached.isEmpty()) {
            @Suppress("DEPRECATION")
            val typedAction = TypedAction.getInstance()
            originalHandler = typedAction.rawHandler
            typedAction.setupRawHandler(this)
            LOG.info("FlashJump: attached keyboard listener")
        }
        
        attached[editor] = callback
        
        // 添加特殊按键监听（使用 IdeEventQueue 在最早阶段拦截）
        if (specialCallback != null) {
            specialHandlers[editor] = specialCallback
            
            if (eventDispatcher == null) {
                eventDispatcher = IdeEventQueue.EventDispatcher { event ->
                    handleEvent(event)
                }
                IdeEventQueue.getInstance().addDispatcher(eventDispatcher!!, null)
                LOG.info("FlashJump: registered event dispatcher")
            }
        }
    }
    
    /**
     * 处理事件
     */
    private fun handleEvent(event: AWTEvent): Boolean {
        if (event !is KeyEvent || event.id != KeyEvent.KEY_PRESSED) {
            return false
        }
        
        // 查找当前活动的编辑器
        val activeEditor = attached.keys.firstOrNull { editor ->
            !editor.isDisposed && editor.contentComponent.isFocusOwner
        } ?: return false
        
        val handler = specialHandlers[activeEditor] ?: return false
        
        when (event.keyCode) {
            KeyEvent.VK_ENTER -> {
                LOG.info("FlashJump: Enter key intercepted")
                event.consume()
                handler.onEnter()
                return true
            }
            KeyEvent.VK_ESCAPE -> {
                LOG.info("FlashJump: Escape key intercepted")
                event.consume()
                handler.onEscape()
                return true
            }
            KeyEvent.VK_BACK_SPACE -> {
                LOG.info("FlashJump: Backspace key intercepted")
                event.consume()
                handler.onBackspace()
                return true
            }
        }
        
        return false
    }
    
    /**
     * 从编辑器分离键盘监听
     */
    fun detach(editor: Editor) {
        attached.remove(editor)
        specialHandlers.remove(editor)
        
        if (attached.isEmpty()) {
            @Suppress("DEPRECATION")
            originalHandler?.let(TypedAction.getInstance()::setupRawHandler)
            originalHandler = null
            
            // 移除事件分发器
            eventDispatcher?.let {
                IdeEventQueue.getInstance().removeDispatcher(it)
                eventDispatcher = null
                LOG.info("FlashJump: removed event dispatcher")
            }
            
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
