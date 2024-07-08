package files;

import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.TraceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * comment for class
 */
@RestController
public class TraceController {

    // variables
    @Autowired
    private Logging logging;
    @Autowired
    private Tracer tracer;

    /**
     * comments for method
     */
    public void doLog() {
        logging.write(LogEntry.of("my secret " + abc));
    }
}