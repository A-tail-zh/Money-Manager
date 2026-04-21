package in.atail.moneymanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({"/app", "/app/"})
    public String forwardAppEntry() {
        return "forward:/app/index.html";
    }

    @GetMapping("/app/{path:[^.]*}")
    public String forwardAppRoutes() {
        return "forward:/app/index.html";
    }
}
