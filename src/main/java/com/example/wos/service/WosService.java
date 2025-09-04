package com.example.wos.service;

import com.example.wos.model.WosData;
import com.example.wos.repo.WosRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WosService {

    private final WosRepository repository;

    public WosService(WosRepository repository) {
        this.repository = repository;
    }

    public List<WosData> findAll() {
        return repository.findAll();
    }
}
