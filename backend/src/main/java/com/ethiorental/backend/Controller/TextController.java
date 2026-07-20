package com.ethiorental.backend.Controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class TextController {

    @GetMapping("/api")
    public String getMethodName() {
        return "hello";
    }
    
    
}
