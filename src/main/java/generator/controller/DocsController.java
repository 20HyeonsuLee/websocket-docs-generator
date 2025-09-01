package generator.controller;

import generator.AsyncApiGenerator;
import generator.config.DocsProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class DocsController {

    private final AsyncApiGenerator generator;
    private final DocsProperties properties;

    @GetMapping("/docs")
    @MessageMapping
    @SneakyThrows
    public String docsPage(Model model) {
        if (!properties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문서 생성 기능이 비활성화되어 있습니다.");
        }
        
        model.addAttribute("asyncApiYml", generator.generateAsyncapiYml());
        model.addAttribute("websocketUrl", properties.getServerUrl());
        return "docs";
    }
}
