package com.fjy.demo.service;

import com.fjy.framework.anotation.FService;

@FService
public class DemoService {

    public String query(String name) {
        return name;
    }
}
