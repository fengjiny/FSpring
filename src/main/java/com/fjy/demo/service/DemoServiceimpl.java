package com.fjy.demo.service;

import com.fjy.framework.anotation.FService;

@FService
public class DemoServiceimpl implements DemoService{

    public String query(String name) {
        return name;
    }
}
