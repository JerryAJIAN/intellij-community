/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.ui.tree;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.ide.SmartSelectProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.util.function.Function;

/**
 * @author Konstantin Bulenkov
 */
public class TreeSmartSelectProvider implements SmartSelectProvider<JTree> {
  @Override
  public void increaseSelection(JTree tree) {
    TreePath anchor = getAnchor(tree);
    if (anchor == null) return; // not found

    for (TreePath parent = anchor; parent != null; parent = parent.getParentPath()) {
      if (processDescendants(tree, parent, child -> addSelectionPath(tree, child))) {
        setAnchor(tree, anchor);
        return; // interrupt if some children were selected
      }
      if (!tree.isPathSelected(parent)) {
        if (!tree.isRootVisible() && parent.getParentPath() == null) return; // cannot select hidden root
        tree.addSelectionPath(parent);
        setAnchor(tree, anchor);
        return; // interrupt if parent was selected
      }
    }
  }

  @Override
  public void decreaseSelection(JTree tree) {
    TreePath anchor = getAnchor(tree);
    if (anchor == null) return; // not found
    if (!tree.isPathSelected(anchor)) return; // not selected

    TreePath lower = anchor;
    for (TreePath upper = anchor.getParentPath(); upper != null; lower = upper, upper = upper.getParentPath()) {
      if (processDescendants(tree, upper, child -> tree.isPathSelected(child) ? Command.ACCEPT : Command.INTERRUPT)) {
        if (tree.isPathSelected(upper)) continue; // search for upper bounds if all descendants are selected

        TreePath except = lower; // to be effective final
        if (processDescendants(tree, upper, child -> removeSelectionPath(tree, child, except))) {
          setAnchor(tree, anchor);
          return; // interrupt if siblings were unselected
        }
      }
      if (lower != anchor) {
        tree.removeSelectionPath(lower);
        setAnchor(tree, anchor);
        return; // interrupt if an anchored child is unselected
      }
      if (processDescendants(tree, lower, child -> removeSelectionPath(tree, child))) {
        setAnchor(tree, anchor); // store an anchor only if selection is changed
      }
      return;
    }
  }

  @Nullable
  @Override
  public JTree getSource(DataContext context) {
    Component component = PlatformDataKeys.CONTEXT_COMPONENT.getData(context);
    return component instanceof JTree ? (JTree)component : null;
  }

  @Nullable
  private static TreePath getAnchor(@Nullable JTree tree) {
    if (tree == null) return null; // unexpected usage
    TreePath anchor = tree.getAnchorSelectionPath(); // search for visible path
    while (anchor != null && tree.getRowForPath(anchor) < 0) anchor = anchor.getParentPath();
    return anchor;
  }

  private static void setAnchor(@NotNull JTree tree, @NotNull TreePath path) {
    tree.setAnchorSelectionPath(path);
  }

  private enum Command {ACCEPT, IGNORE, INTERRUPT}

  private static Command addSelectionPath(@NotNull JTree tree, @NotNull TreePath path) {
    if (tree.isPathSelected(path)) return Command.IGNORE;
    tree.addSelectionPath(path);
    return Command.ACCEPT;
  }

  private static Command removeSelectionPath(@NotNull JTree tree, @NotNull TreePath path) {
    if (!tree.isPathSelected(path)) return Command.IGNORE;
    tree.removeSelectionPath(path);
    return Command.ACCEPT;
  }

  private static Command removeSelectionPath(@NotNull JTree tree, @NotNull TreePath path, @NotNull TreePath except) {
    if (!tree.isPathSelected(path) || except.isDescendant(path)) return Command.IGNORE;
    tree.removeSelectionPath(path);
    return Command.ACCEPT;
  }

  private static boolean processDescendants(@NotNull JTree tree, @NotNull TreePath parent, @NotNull Function<TreePath, Command> function) {
    boolean processed = false;
    for (int row = Math.max(0, 1 + tree.getRowForPath(parent)); row < tree.getRowCount(); row++) {
      TreePath path = tree.getPathForRow(row);
      if (!parent.isDescendant(path)) break;
      Command command = function.apply(path);
      if (command == Command.INTERRUPT) return false;
      if (command == Command.ACCEPT) processed = true;
    }
    return processed;
  }
}
