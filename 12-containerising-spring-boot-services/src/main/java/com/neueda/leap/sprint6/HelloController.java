package com.neueda.leap.sprint6;

import org.springframework.web.bind.annotation.*;

@RestController
public class HelloController {

    
    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }
}

