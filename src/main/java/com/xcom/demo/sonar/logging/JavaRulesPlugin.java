/*
 * Copyright (C) 2012-2024 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package com.xcom.demo.sonar.logging;

import org.sonar.api.Plugin;

/**
 * Entry point of your plugin containing your custom rules
 */
public class JavaRulesPlugin implements Plugin {

  @Override
  public void define(Context context) {
    // server extensions -> objects are instantiated during server startup
    context.addExtension(JavaRulesDefinition.class);

    // batch extensions -> objects are instantiated during code analysis
    context.addExtension(JavaFileCheckRegistrar.class);

  }

}
