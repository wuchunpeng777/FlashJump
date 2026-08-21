# FlashJump

![Build](https://github.com/wuchunpeng777/FlashJump/workflows/Build/badge.svg)

<!-- Plugin description -->
FlashJump provides keyboard-driven label navigation for JetBrains IDEs.

Type a search character, then press the label displayed at the destination to jump there without leaving the keyboard.

Features:

- Real-time, case-insensitive matching.
- Distance-aware labels that keep nearby targets easy to reach.
- Character, word, line, forward, backward, jump-end, and selection modes.
- Navigation across multiple editor windows.
- Configurable labels, colors, search range, highlighting, backdrop, and automatic jump behavior.

Default shortcuts:

- <kbd>Ctrl+;</kbd> — activate FlashJump or cycle to the next jump mode.
- <kbd>Ctrl+Alt+;</kbd> — start target selection mode.
- <kbd>Ctrl+Shift+;</kbd> — start line jump mode.
- <kbd>Enter</kbd> — jump to the default match.
- <kbd>Backspace</kbd> — clear the current search and start again.
- <kbd>Esc</kbd> — cancel the active FlashJump session.

FlashJump 是一个面向 JetBrains IDE 的键盘快速跳转插件。输入搜索字符，再按目标位置显示的标签即可完成跳转。
<!-- Plugin description end -->

## Requirements

- A JetBrains IDE based on IntelliJ Platform 2025.2 or later.

## Installation

### JetBrains Marketplace

After the plugin is approved, open <kbd>Settings</kbd> → <kbd>Plugins</kbd> → <kbd>Marketplace</kbd>, search for **FlashJump**, and select <kbd>Install</kbd>.

### GitHub release

Download the ZIP from the [latest GitHub release](https://github.com/wuchunpeng777/FlashJump/releases/latest), then open <kbd>Settings</kbd> → <kbd>Plugins</kbd> → <kbd>⚙</kbd> → <kbd>Install Plugin from Disk...</kbd>. Select the downloaded ZIP without extracting it.

## Usage

1. Place the caret in an editor.
2. Press <kbd>Ctrl+;</kbd>.
3. Type the text to search for.
4. Press the label shown at the desired destination.

Every FlashJump action is also available from <kbd>Find Action</kbd> and can be reassigned under <kbd>Settings</kbd> → <kbd>Keymap</kbd>.

## Configuration

Open <kbd>Settings</kbd> → <kbd>Tools</kbd> → <kbd>FlashJump</kbd> to configure:

- Label characters and uppercase labels.
- Minimum search length.
- Match highlighting and backdrop.
- Label, highlight, and default-match colors.
- Visible-area or whole-file search.
- Multi-window search and automatic jump behavior.

## Support

Report reproducible problems and feature requests through [GitHub Issues](https://github.com/wuchunpeng777/FlashJump/issues).

## License

FlashJump is available under the [MIT License](LICENSE).
