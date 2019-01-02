package com.fjy.demo.controller;

import com.fjy.demo.service.DemoService;
import com.fjy.framework.anotation.FAutowired;
import com.fjy.framework.anotation.FController;
import com.fjy.framework.anotation.FRequestMapping;
import com.fjy.framework.anotation.FRequestParm;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@FController
@FRequestMapping(value = "/demo")
public class DemoController {

    @FAutowired
    private DemoService demoService;


    @FRequestMapping("/query.do")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @FRequestParm("name") String name) {
        String result = demoService.query(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
