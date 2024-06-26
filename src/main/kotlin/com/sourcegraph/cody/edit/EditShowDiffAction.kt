package com.sourcegraph.cody.edit

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffRequestFactory
import com.intellij.diff.actions.BlankDiffWindowUtil.createBlankDiffRequestChain
import com.intellij.diff.actions.CompareFileWithEditorAction
import com.intellij.diff.chains.DiffRequestChain
import com.intellij.diff.contents.FileContent
import com.intellij.diff.util.DiffUserDataKeys
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager

class EditShowDiffAction : CompareFileWithEditorAction() {
  override fun isAvailable(e: AnActionEvent): Boolean {
    e.dataContext.getData(EDITOR_DATA_KEY) ?: return false
    return true
  }

  override fun getDiffRequestChain(e: AnActionEvent): DiffRequestChain {
    val project = e.project ?: throw IllegalStateException("Project cannot be null")

    val activeSession = FixupService.getInstance(project).getActiveSession()
    val documentAfter =
        activeSession?.editor?.document ?: throw IllegalStateException("Editor cannot be null")
    val diffSessionDocument = activeSession.createDiffDocument()

    val rhsContent = DiffContentFactory.getInstance().create(project, documentAfter)
    val fileType = (rhsContent as? FileContent)?.file?.fileType
    val lhsContent = DiffContentFactory.getInstance().create(project, diffSessionDocument, fileType)
    lhsContent.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true)

    val editorFile = FileDocumentManager.getInstance().getFile(documentAfter)
    val editorContentTitle =
        when {
          editorFile == null -> "Editor"
          else -> DiffRequestFactory.getInstance().getContentTitle(editorFile)
        }

    val chain = createBlankDiffRequestChain(lhsContent, rhsContent, baseContent = null)
    chain.windowTitle =
        when {
          editorFile == null -> "Cody Diff"
          else -> "Cody Diff: $editorContentTitle"
        }
    chain.title1 = "Before Cody Inline Edit"
    chain.title2 = editorContentTitle

    return chain
  }

  companion object {
    val EDITOR_DATA_KEY = DataKey.create<Editor>("editor")
  }
}
