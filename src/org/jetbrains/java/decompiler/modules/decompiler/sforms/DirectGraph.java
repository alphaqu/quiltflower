// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.sforms;

import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.sforms.FlattenStatementsHelper.FinallyPathWrapper;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public final class DirectGraph {

  public final VBStyleCollection<DirectNode, String> nodes = new VBStyleCollection<>();

  // exit, [source, destination]
  public final HashMap<String, List<FinallyPathWrapper>> mapShortRangeFinallyPaths = new HashMap<>();

  // exit, [source, destination]
  public final HashMap<String, List<FinallyPathWrapper>> mapLongRangeFinallyPaths = new HashMap<>();

  // negative if branches (recorded for handling of && and ||)
  public final HashMap<String, String> mapNegIfBranch = new HashMap<>();

  // nodes, that are exception exits of a finally block with monitor variable
  public final HashMap<String, String> mapFinallyMonitorExceptionPathExits = new HashMap<>();

  public DirectNode first;

  public void sortReversePostOrder() {
    ArrayList<DirectNode> res = new ArrayList<>();
    addChildren(first, res, new HashSet<>());

    nodes.clear();
    for (DirectNode node : res) {
      nodes.addWithKey(node, node.id);
    }
  }

  private static void addChildren(final DirectNode root, final ArrayList<? super DirectNode> out, final HashSet<DirectNode> visited) {
    visited.add(root);
    for (DirectNode succ : root.succs) {
      if (!visited.contains(succ)) {
        addChildren(succ, out, visited);
      }
    }
    out.add(0, root);
  }

  private static boolean run(final DirectNode node, final ExprentIterator iter, final HashSet<DirectNode> setVisited) {
    if (setVisited.contains(node)) {
      return true;
    }
    setVisited.add(node);

    for (int i = 0; i < node.exprents.size(); i++) {
      int res = iter.processExprent(node.exprents.get(i));

      if (res == 1) {
        return false;
      } else if (res == 2) {
        node.exprents.remove(i);
        i--;
      }
    }

    for (final DirectNode succ : node.succs) {
      if (!run(succ, iter, setVisited)) {
        return false;
      }
    }
    return true;
  }

  public boolean iterateExprents(final ExprentIterator iter) {
    return run(first, iter, new HashSet<>());
  }

  public boolean iterateExprentsDeep(final ExprentIterator itr) {
    return iterateExprents(exprent -> {
      List<Exprent> lst = exprent.getAllExprents(true);
      lst.add(exprent);

      for (Exprent expr : lst) {
        int res = itr.processExprent(expr);
        if (res == 1 || res == 2) {
          return res;
        }
      }
      return 0;
    });
  }

  public interface ExprentIterator {
    // 0 - success, do nothing
    // 1 - cancel iteration
    // 2 - success, delete exprent
    int processExprent(Exprent exprent);
  }
}
