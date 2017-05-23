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
package com.intellij.compiler.chainsSearch;

import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

public class ChainRelevance implements Comparable<ChainRelevance> {
  private final int myChainSize;
  private final int myUnreachableParametersCount;
  private final int myParametersInContext;

  public ChainRelevance(final int chainSize,
                        final int unreachableParametersCount,
                        final int parametersInContext) {
    myChainSize = chainSize;
    myUnreachableParametersCount = unreachableParametersCount;
    myParametersInContext = parametersInContext;
  }

  @TestOnly
  public int getChainSize() {
    return myChainSize;
  }

  @TestOnly
  public int getUnreachableParametersCount() {
    return myUnreachableParametersCount;
  }

  @TestOnly
  public int getParametersInContext() {
    return myParametersInContext;
  }

  @Override
  public int compareTo(@NotNull final ChainRelevance that) {
    int sub = Comparing.compare(myChainSize, that.myChainSize);
    if (sub != 0) return sub;
    sub = Comparing.compare(myUnreachableParametersCount, that.myUnreachableParametersCount);
    if (sub != 0) return sub;
    return -Comparing.compare(myParametersInContext, that.myParametersInContext);
  }

  @Override
  public String toString() {
    return "chain size: " + myChainSize + ", unreachable args: " + myUnreachableParametersCount + ", parameters in context: " + myParametersInContext;
  }
}
