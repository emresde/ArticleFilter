package com.example.wos.web;

import com.example.wos.model.WosData;
import com.example.wos.service.WosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class WosController {

    private final WosService service;

    public WosController(WosService service) {
        this.service = service;
    }

    @GetMapping("/records")
    public List<WosData> getAll() {
        return service.findAll();
    }
}
