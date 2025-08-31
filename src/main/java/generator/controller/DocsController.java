package generator.controller;

import generator.AsyncApiGenerator;
import java.io.IOException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocsController {

    private static AsyncApiGenerator generator = new AsyncApiGenerator();

    private static String yml;

    static {
        try {
            yml = generator.generateAsyncapiYml();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/docs")
    @MessageMapping
    public String docsPage(Model model) {
        model.addAttribute("asyncApiYml", yml);
        return "docs";
    }
}
