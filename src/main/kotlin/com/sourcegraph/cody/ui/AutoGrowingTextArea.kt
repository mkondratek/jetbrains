package com.sourcegraph.cody.ui

import com.intellij.ide.ui.laf.darcula.ui.DarculaTextAreaUI
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.UIUtil
import com.sourcegraph.common.ui.SimpleDumbAwareEDTAction
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.ScrollPaneConstants
import javax.swing.plaf.basic.BasicTextAreaUI
import javax.swing.text.AttributeSet
import javax.swing.text.Document
import javax.swing.text.PlainDocument
import javax.swing.undo.UndoManager
import kotlin.math.max
import kotlin.math.min

class AutoGrowingTextArea(private val minRows: Int, maxRows: Int, outerPanel: JComponent) {
  val textArea: JBTextArea
  val scrollPane: JBScrollPane
  private val initialPreferredSize: Dimension
  private val autoGrowUpToRow: Int = maxRows + 1
  private val undoManager = UndoManager()

  init {
    textArea = createTextArea()
    scrollPane = JBScrollPane(textArea)
    scrollPane.isFocusable = false
    initialPreferredSize = scrollPane.preferredSize
    val document: Document =
        object : PlainDocument() {
          override fun insertString(offs: Int, str: String?, a: AttributeSet?) {
            super.insertString(offs, str, a)
            updateTextAreaSize()
            outerPanel.revalidate()
          }

          override fun remove(offs: Int, len: Int) {
            super.remove(offs, len)
            updateTextAreaSize()
            outerPanel.revalidate()
          }
        }

    textArea.document = document
    document.addUndoableEditListener { event -> undoManager.addEdit(event.edit) }

    updateTextAreaSize()
  }

  fun getText(): String {
    return this.textArea.text
  }

  fun setText(newText: String) {
    textArea.text = newText
  }

  private fun createTextArea(): JBTextArea {
    val promptInput: JBTextArea = RoundedJBTextArea(minRows, 10)
    val textUI = DarculaTextAreaUI.createUI(promptInput) as BasicTextAreaUI
    promptInput.setUI(textUI)
    promptInput.font = UIUtil.getLabelFont()
    promptInput.lineWrap = true
    promptInput.wrapStyleWord = true
    promptInput.requestFocusInWindow()

    /* Insert Enter on Shift+Enter, Ctrl+Enter, Alt/Option+Enter, and Meta+Enter */
    val shiftEnter =
        KeyboardShortcut(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), null)
    val ctrlEnter =
        KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), null)
    val altOrOptionEnter =
        KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK), null)
    val metaEnter =
        KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.META_DOWN_MASK), null)
    val insertEnterShortcut: ShortcutSet =
        CustomShortcutSet(ctrlEnter, shiftEnter, metaEnter, altOrOptionEnter)
    val insertEnterAction: AnAction = SimpleDumbAwareEDTAction {
      promptInput.insert("\n", promptInput.caretPosition)
    }
    insertEnterAction.registerCustomShortcutSet(insertEnterShortcut, promptInput)
    return promptInput
  }

  private fun updateTextAreaSize() {
    // Get the preferred size of the JTextArea based on its content
    val preferredSize = textArea.preferredSize
    // Limit the number of rows to maxRows
    val fontMetrics = textArea.getFontMetrics(textArea.font)
    val maxTextAreaHeight = fontMetrics.height * autoGrowUpToRow
    var preferredHeight = min(preferredSize.height, maxTextAreaHeight)
    preferredHeight = max(preferredHeight, initialPreferredSize.height)

    // Set the preferred size of the JScrollPane to accommodate the JTextArea
    val scrollPaneSize = scrollPane.size
    scrollPaneSize.height = preferredHeight
    scrollPane.preferredSize = scrollPaneSize
    val shouldShowScrollbar = preferredSize.height > maxTextAreaHeight
    scrollPane.verticalScrollBarPolicy =
        if (shouldShowScrollbar) ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        else ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  }
}
