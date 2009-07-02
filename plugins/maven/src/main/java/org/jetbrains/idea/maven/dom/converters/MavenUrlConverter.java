package org.jetbrains.idea.maven.dom.converters;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.ConvertContext;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MavenUrlConverter extends MavenReferenceConverter<String> {
  @Override
  public String fromString(@Nullable @NonNls String s, ConvertContext context) {
    return s;
  }

  @Override
  public String toString(@Nullable String text, ConvertContext context) {
    return text;
  }

  protected void createReferences(PsiElement element,
                                  String resolvedText,
                                  TextRange range,
                                  VirtualFile virtualFile,
                                  XmlFile psiFile,
                                  List<PsiReference> result) {
    result.add(new MavenUrlPsiReference(element, resolvedText, range));
  }
}