package com.github.wuchunpeng777.flashjump.config

import com.intellij.openapi.components.*
import com.intellij.openapi.options.Configurable
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Color
import javax.swing.*
import javax.swing.JColorChooser

/**
 * FlashJump 配置类
 * 支持持久化存储和自定义配置
 */
@State(
    name = "FlashJumpConfig",
    storages = [Storage("FlashJump.xml")]
)
@Service(Service.Level.APP)
class FlashConfig : PersistentStateComponent<FlashConfig> {
    
    // 跳转标签字符集 - 使用容易按到的键
    var labels: String = "asdfghjklqwertyuiopzxcvbnm"
    
    // 是否允许大写标签
    var allowUppercaseLabels: Boolean = true
    
    // 最小搜索长度（达到后开始显示标签）
    var minPatternLength: Int = 2
    
    // 是否显示背景遮罩
    var showBackdrop: Boolean = false
    
    // 背景遮罩透明度 (0-255)
    var backdropAlpha: Int = 128
    
    // 是否高亮匹配项
    var highlightMatches: Boolean = true
    
    // 标签背景颜色（暗红色）
    var labelBackgroundColor: Int = Color(139, 69, 69).rgb
    
    // 标签前景颜色
    var labelForegroundColor: Int = Color.WHITE.rgb
    
    // 匹配高亮颜色
    var matchHighlightColor: Int = Color(255, 255, 0, 80).rgb
    
    // 是否搜索整个文件（否则只搜索可见区域）
    var searchWholeFile: Boolean = false
    
    // 默认匹配项背景颜色（橘黄色）
    var defaultMatchBackgroundColor: Int = java.awt.Color(255, 165, 0).rgb
    
    // 是否在多窗口中搜索
    var multiWindow: Boolean = true
    
    // 是否自动跳转（只有一个匹配时）
    var autoJump: Boolean = false
    
    // 标签字体大小相对于编辑器字体的比例
    var labelFontScale: Float = 0.85f
    
    override fun getState(): FlashConfig = this
    
    override fun loadState(state: FlashConfig) {
        XmlSerializerUtil.copyBean(state, this)
    }
    
    companion object {
        @JvmStatic
        fun getInstance(): FlashConfig = service()
        
        /**
         * 获取可用的标签字符列表
         */
        fun getLabels(): List<Char> {
            val config = getInstance()
            val labels = config.labels.toList()
            return if (config.allowUppercaseLabels) {
                labels + labels.map { it.uppercaseChar() }
            } else {
                labels
            }.distinct()
        }
        
        /**
         * 获取标签背景色
         */
        fun getLabelBackground(): Color = Color(getInstance().labelBackgroundColor)
        
        /**
         * 获取标签前景色
         */
        fun getLabelForeground(): Color = Color(getInstance().labelForegroundColor)
        
        /**
         * 获取默认匹配项背景色（橘黄色）
         */
        fun getDefaultMatchBackground(): Color = Color(getInstance().defaultMatchBackgroundColor)
    }
}

/**
 * 颜色选择按钮
 */
class ColorButton(initialColor: Color) : JButton() {
    var selectedColor: Color = initialColor
        set(value) {
            field = value
            background = value
            repaint()
        }
    
    init {
        preferredSize = java.awt.Dimension(60, 25)
        maximumSize = java.awt.Dimension(60, 25)
        background = initialColor
        isOpaque = true
        isBorderPainted = true
        
        addActionListener {
            val newColor = JColorChooser.showDialog(this, "选择颜色", selectedColor)
            if (newColor != null) {
                selectedColor = newColor
            }
        }
    }
}

/**
 * FlashJump 配置面板
 */
class FlashConfigurable : Configurable {
    private var panel: JPanel? = null
    private var labelsField: JTextField? = null
    private var minPatternLengthSpinner: JSpinner? = null
    private var showBackdropCheckbox: JCheckBox? = null
    private var autoJumpCheckbox: JCheckBox? = null
    private var searchWholeFileCheckbox: JCheckBox? = null
    private var multiWindowCheckbox: JCheckBox? = null
    
    // 颜色选择按钮
    private var labelBgColorButton: ColorButton? = null
    private var labelFgColorButton: ColorButton? = null
    private var defaultMatchBgColorButton: ColorButton? = null
    
    override fun getDisplayName(): String = "FlashJump"
    
    override fun createComponent(): JComponent {
        panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            
            val config = FlashConfig.getInstance()
            
            // 标签字符
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(JLabel("跳转标签字符: "))
                labelsField = JTextField(config.labels, 30).also { add(it) }
                add(Box.createHorizontalGlue())
            })
            
            add(Box.createVerticalStrut(10))
            
            // 最小搜索长度
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(JLabel("最小搜索长度: "))
                minPatternLengthSpinner = JSpinner(SpinnerNumberModel(config.minPatternLength, 0, 10, 1)).also { add(it) }
                add(Box.createHorizontalGlue())
            })
            
            add(Box.createVerticalStrut(10))
            
            // 颜色配置区域
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = BorderFactory.createTitledBorder("颜色设置")
                
                // 标签背景色
                add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    add(JLabel("标签背景色: "))
                    labelBgColorButton = ColorButton(Color(config.labelBackgroundColor)).also { add(it) }
                    add(Box.createHorizontalStrut(20))
                    add(JLabel("标签文字色: "))
                    labelFgColorButton = ColorButton(Color(config.labelForegroundColor)).also { add(it) }
                    add(Box.createHorizontalGlue())
                })
                
                add(Box.createVerticalStrut(5))
                
                // 默认匹配项背景色
                add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    add(JLabel("默认匹配项背景色: "))
                    defaultMatchBgColorButton = ColorButton(Color(config.defaultMatchBackgroundColor)).also { add(it) }
                    add(Box.createHorizontalGlue())
                })
            })
            
            add(Box.createVerticalStrut(10))
            
            // 复选框选项
            showBackdropCheckbox = JCheckBox("显示背景遮罩", config.showBackdrop).also { add(it) }
            autoJumpCheckbox = JCheckBox("自动跳转（仅一个匹配时）", config.autoJump).also { add(it) }
            searchWholeFileCheckbox = JCheckBox("搜索整个文件", config.searchWholeFile).also { add(it) }
            multiWindowCheckbox = JCheckBox("在多窗口中搜索", config.multiWindow).also { add(it) }
            
            add(Box.createVerticalGlue())
        }
        return panel!!
    }
    
    override fun isModified(): Boolean {
        val config = FlashConfig.getInstance()
        return labelsField?.text != config.labels ||
            (minPatternLengthSpinner?.value as? Int) != config.minPatternLength ||
            showBackdropCheckbox?.isSelected != config.showBackdrop ||
            autoJumpCheckbox?.isSelected != config.autoJump ||
            searchWholeFileCheckbox?.isSelected != config.searchWholeFile ||
            multiWindowCheckbox?.isSelected != config.multiWindow ||
            labelBgColorButton?.selectedColor?.rgb != config.labelBackgroundColor ||
            labelFgColorButton?.selectedColor?.rgb != config.labelForegroundColor ||
            defaultMatchBgColorButton?.selectedColor?.rgb != config.defaultMatchBackgroundColor
    }
    
    override fun apply() {
        val config = FlashConfig.getInstance()
        labelsField?.text?.let { config.labels = it }
        (minPatternLengthSpinner?.value as? Int)?.let { config.minPatternLength = it }
        showBackdropCheckbox?.isSelected?.let { config.showBackdrop = it }
        autoJumpCheckbox?.isSelected?.let { config.autoJump = it }
        searchWholeFileCheckbox?.isSelected?.let { config.searchWholeFile = it }
        multiWindowCheckbox?.isSelected?.let { config.multiWindow = it }
        labelBgColorButton?.selectedColor?.rgb?.let { config.labelBackgroundColor = it }
        labelFgColorButton?.selectedColor?.rgb?.let { config.labelForegroundColor = it }
        defaultMatchBgColorButton?.selectedColor?.rgb?.let { config.defaultMatchBackgroundColor = it }
    }
    
    override fun reset() {
        val config = FlashConfig.getInstance()
        labelsField?.text = config.labels
        minPatternLengthSpinner?.value = config.minPatternLength
        showBackdropCheckbox?.isSelected = config.showBackdrop
        autoJumpCheckbox?.isSelected = config.autoJump
        searchWholeFileCheckbox?.isSelected = config.searchWholeFile
        multiWindowCheckbox?.isSelected = config.multiWindow
        labelBgColorButton?.selectedColor = Color(config.labelBackgroundColor)
        labelFgColorButton?.selectedColor = Color(config.labelForegroundColor)
        defaultMatchBgColorButton?.selectedColor = Color(config.defaultMatchBackgroundColor)
    }
}
