package generator.controller;

import generator.AsyncApiGenerator;
import java.io.IOException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocsController {

    private static AsyncApiGenerator generator = new AsyncApiGenerator();

    @GetMapping("/docs")
    @MessageMapping
    public String docsPage(TestDto testDto) throws IOException {
        return generator.generateAsyncapiYml();
    }
}
