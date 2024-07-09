package com.demo.test.files;

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
 * sample for UnitTest
 * NOT for compile
 */
@RestController
public class TraceController {

    // variables
    @Autowired
    private Logging logging;
    @Autowired
    private Tracer tracer;

    /**
     * example of method
     */
    public void doLog() {
        String pass = "ZPSWvPFEWe8LfuqcHwuD";
        String password = pass;
        String user = pass.toLowerCase();

        a.b.c.d(eee.from("my secret: {}", h.i.j + x.y.z()));
        logging.write(LogEntry.of("my name" + user));
    }
}