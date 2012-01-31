package com.intellij.lang.folding;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds custom folding regions. If custom folding is supported for a language, its FoldingBuilder must be inherited from this class.
 * 
 * @author Rustam Vishnyakov
 */
public abstract class CustomFoldingBuilder extends FoldingBuilderEx implements DumbAware {

  private CustomFoldingProvider myDefaultProvider;

  @NotNull
  @Override
  public final FoldingDescriptor[] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
    List<FoldingDescriptor> descriptors = new ArrayList<FoldingDescriptor>();
    if (CustomFoldingProvider.getAllProviders().length > 0) {
      myDefaultProvider = null;
      addCustomFoldingRegionsRecursively(root.getNode(), descriptors);
    }
    buildLanguageFoldRegions(descriptors, root, document, quick);
    return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
  }

  @NotNull
  @Override
  public final FoldingDescriptor[] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
    return buildFoldRegions(node.getPsi(), document, false);
  }

  /**
   * Implement this method to build language folding regions besides custom folding regions.
   *
   * @param descriptors The list of folding descriptors to store results to.
   * @param root        The root node for which the folding is requested.
   * @param document    The document for which folding is built.
   * @param quick       whether the result should be provided as soon as possible without reference resolving
   *                    and complex checks.
   */
  protected abstract void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                                   @NotNull PsiElement root,
                                                   @NotNull Document document,
                                                   boolean quick);
  
  private void addCustomFoldingRegionsRecursively(@NotNull ASTNode node, List<FoldingDescriptor> descriptors) {
    Stack<ASTNode> customFoldingNodesStack = new Stack<ASTNode>(1);
    for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (isCustomRegionStart(child)) {
        customFoldingNodesStack.push(child);
      }
      else if (isCustomRegionEnd(child)) {
        if (!customFoldingNodesStack.isEmpty()) {
          ASTNode startNode = customFoldingNodesStack.pop();
          int startOffset = startNode.getTextRange().getStartOffset();
          TextRange range = new TextRange(startOffset, child.getTextRange().getEndOffset());
          descriptors.add(new FoldingDescriptor(node, range));
        }
      }
      else {
        addCustomFoldingRegionsRecursively(child, descriptors);
      }
    }
  }

  @Override
  public final String getPlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
    if (mayContainCustomFoldings(node)) {
      PsiFile file = node.getPsi().getContainingFile();
      PsiElement contextElement = file.findElementAt(range.getStartOffset());
      if (contextElement != null && isCustomFoldingCandidate(contextElement.getNode())) {
        String elementText = contextElement.getText();
        CustomFoldingProvider defaultProvider = getDefaultProvider(elementText);
        if (defaultProvider != null && defaultProvider.isCustomRegionStart(elementText)) {
          return defaultProvider.getPlaceholderText(elementText);
        }
      }
    }
    return getLanguagePlaceholderText(node, range);
  }
  
  protected abstract String getLanguagePlaceholderText(@NotNull ASTNode node, @NotNull TextRange range);


  @Override
  public final String getPlaceholderText(@NotNull ASTNode node) {
    return "...";
  }


  @Override
  public final boolean isCollapsedByDefault(@NotNull ASTNode node) {
    // TODO<rv>: Modify Folding API and pass here folding range.
    if (mayContainCustomFoldings(node)) {
      for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
        if (isCustomRegionStart(child)) {
          String childText = child.getText();
          CustomFoldingProvider defaultProvider = getDefaultProvider(childText);
          return defaultProvider != null && defaultProvider.isCollapsedByDefault(childText);
        }
      }
    }
    return isRegionCollapsedByDefault(node);
  }

  /**
   * Returns the default collapsed state for the folding region related to the specified node.
   *
   * @param node the node for which the collapsed state is requested.
   * @return true if the region is collapsed by default, false otherwise.
   */
  protected abstract boolean isRegionCollapsedByDefault(@NotNull ASTNode node);

  /**
   * Returns true if the node corresponds to custom region start. The node must be a custom folding candidate and match custom folding 
   * start pattern.
   *
   * @param node The node which may contain custom region start.
   * @return True if the node marks a custom region start.
   */
  protected final boolean isCustomRegionStart(ASTNode node) {
    if (isCustomFoldingCandidate(node)) {
      String nodeText = node.getText();
      CustomFoldingProvider defaultProvider = getDefaultProvider(nodeText);
      return defaultProvider != null && defaultProvider.isCustomRegionStart(nodeText);
    }
    return false;
  }

  /**
   * Returns true if the node corresponds to custom region end. The node must be a custom folding candidate and match custom folding
   * end pattern.
   *
   * @param node The node which may contain custom region end
   * @return True if the node marks a custom region end.
   */
  protected final boolean isCustomRegionEnd(ASTNode node) {
    if (isCustomFoldingCandidate(node)) {
      String nodeText = node.getText();
      CustomFoldingProvider defaultProvider = getDefaultProvider(nodeText);
      return defaultProvider != null && defaultProvider.isCustomRegionEnd(nodeText);
    }
    return false;
  }

  @Nullable
  private CustomFoldingProvider getDefaultProvider(String elementText) {
    if (myDefaultProvider == null) {
      for (CustomFoldingProvider provider : CustomFoldingProvider.getAllProviders()) {
        if (provider.isCustomRegionStart(elementText) || provider.isCustomRegionEnd(elementText)) {
          myDefaultProvider = provider;
        }
      }
    }
    return myDefaultProvider;
  }

  /**
   * Checks if a node may contain custom folding tags. By default returns true for PsiComment but a language folding builder may override
   * this method to allow only specific subtypes of comments (for example, line comments only).
   * @param node The node to check.
   * @return True if the node may contain custom folding tags.
   */
  protected boolean isCustomFoldingCandidate(ASTNode node) {
    return node.getPsi() instanceof PsiComment;
  }

  /**
   * Returns true if the node may contain custom foldings in its immediate child nodes. By default any node will be checked for custom
   * foldings but for performance reasons it makes sense to override this method to check only the nodes which actually may contain
   * custom folding nodes (for example, group statements).
   *
   * @param node  The node to check.
   * @return      True if the node may contain custom folding nodes (true by default).
   */
  protected boolean mayContainCustomFoldings(ASTNode node) {
    return true;
  }
}