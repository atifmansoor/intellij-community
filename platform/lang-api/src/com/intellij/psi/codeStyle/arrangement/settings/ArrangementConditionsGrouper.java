/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.codeStyle.arrangement.settings;

import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.HierarchicalArrangementConditionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy which hints on how arrangement match conditions should be grouped at the UI.
 * <p/>
 * Implementations of this interface are expected to be thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 8/15/12 4:21 PM
 */
public interface ArrangementConditionsGrouper {

  /**
   * Allows to answer if and how should be grouped given match conditions.
   * <p/>
   * Example: given condition is a composite <code>'AND'</code> condition with the following operands:
   * <code>'type: field; modifier: public; modifier: static; modifier: final'</code>. We might want to show it like below:
   * <pre>
   *   field
   *     |
   *      ---public---static---final
   * </pre>
   * That means that we'll return a {@link HierarchicalArrangementConditionNode} with condition <code>'type: field'</code> and
   * a single child node with composite <code>'AND'</code> condition <code>'modifier: public; modifier: static; modifier: final'</code>.
   *
   * @param condition  settings node which conditions should be grouped
   * @return           grouping-aware node
   */
  @NotNull
  HierarchicalArrangementConditionNode group(@NotNull ArrangementMatchCondition condition);
}
