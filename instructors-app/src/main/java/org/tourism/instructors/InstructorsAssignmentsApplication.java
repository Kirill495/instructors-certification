package org.tourism.instructors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InstructorsAssignmentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(InstructorsAssignmentsApplication.class, args);
    }
}
