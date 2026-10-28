package com.neueda.leap.sprint6;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping ("/hello")
public class HelloController {

    
    private final ClientMapper clientMapper;

    public HelloController(ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

    @GetMapping ("/clients")
    public List<Client> getAllClients() {
        return clientMapper.findAll();
    }

    @GetMapping ("/client/{clientId}")
    public Optional<Client> getClientById(Integer clientId) {
        return clientMapper.findById(clientId);
    }
    @GetMapping
    public String hello(){
        return "hello";
    }

}

