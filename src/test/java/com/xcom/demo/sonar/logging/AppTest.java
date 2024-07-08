package com.xcom.demo.sonar.logging;



import com.xcom.demo.sonar.logging.checks.XLoggerCheck;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.FilesUtils;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    @org.junit.jupiter.api.Test
    void verify() {

        // Verifies automatically that the check will raise the adequate issues with the expected message
        CheckVerifier.newVerifier()
                .onFile("src/test/files/TraceController.java")
                .withCheck(new XLoggerCheck())
                // In order to test this check efficiently, we added the test-jar "org.apache.commons.commons-collections4" to the pom,
                // which is normally not used by the code of our custom plugin.
                // All the classes from this jar will then be read when verifying the ticket, allowing correct type resolution.
                // You have to give the test jar directory to the verifier in order to make it work correctly.
                .withClassPath(FilesUtils.getClassPath("target/test-jars"))
                .verifyIssues();
    }

}
