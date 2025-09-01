package generator.controller;

import generator.AsyncApiGenerator;
import generator.config.DocsProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DocsController {

    private final AsyncApiGenerator generator;
    private final DocsProperties properties;

    @GetMapping("/docs")
    @MessageMapping
    @SneakyThrows
    public String docsPage(Model model) {
        model.addAttribute("asyncApiYml", generator.generateAsyncapiYml());
        model.addAttribute("websocketUrl", properties.getServerUrl());
        return "docs";
    }
}
