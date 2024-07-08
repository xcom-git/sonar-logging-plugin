/*
 * Copyright (C) 2012-2024 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package com.xcom.demo.sonar.logging;

import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Declare rule metadata in server repository of rules.
 * That allows to list the rules in the page "Rules".
 */
public class JavaRulesDefinition implements RulesDefinition {

  // don't change that because the path is hard coded in CheckVerifier
  private static final String RESOURCE_BASE_PATH = "org/sonar/l10n/java/rules/java";
  // repository of rule
  public static final String REPOSITORY_KEY = "xlogger";
  public static final String REPOSITORY_NAME = "XLogger Repository";

  // Add the rule keys of the rules which need to be considered as template-rules
  private static final Set<String> RULE_TEMPLATES_KEY = Collections.emptySet();

  private final SonarRuntime runtime;

  public JavaRulesDefinition(SonarRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, "java").setName(REPOSITORY_NAME);

    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_BASE_PATH, runtime);

    ruleMetadataLoader.addRulesByAnnotatedClass(repository, new ArrayList<>(RulesList.getChecks()));

    setTemplates(repository);

    repository.done();
  }

  private static void setTemplates(NewRepository repository) {
    RULE_TEMPLATES_KEY.stream()
      .map(repository::rule)
      .filter(Objects::nonNull)
      .forEach(rule -> rule.setTemplate(true));
  }
}
