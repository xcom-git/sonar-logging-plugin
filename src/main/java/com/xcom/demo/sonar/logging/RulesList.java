/*
 * Copyright (C) 2012-2024 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package com.xcom.demo.sonar.logging;

import com.xcom.demo.sonar.logging.checks.NoIfStatementInTestsRule;
import com.xcom.demo.sonar.logging.checks.XLoggerCheck;
import org.sonar.plugins.java.api.JavaCheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RulesList {

  private RulesList() {
  }

  public static List<Class<? extends JavaCheck>> getChecks() {
    List<Class<? extends JavaCheck>> checks = new ArrayList<>();
    checks.addAll(getJavaChecks());
    checks.addAll(getJavaTestChecks());
    return Collections.unmodifiableList(checks);
  }

  /**
   * These rules are going to target MAIN code only
   */
  public static List<Class<? extends JavaCheck>> getJavaChecks() {
    return Collections.unmodifiableList(Arrays.asList(
      XLoggerCheck.class));
  }

  /**
   * These rules are going to target TEST code only
   */
  public static List<Class<? extends JavaCheck>> getJavaTestChecks() {
    return Collections.unmodifiableList(Arrays.asList(
      NoIfStatementInTestsRule.class));
  }
}
