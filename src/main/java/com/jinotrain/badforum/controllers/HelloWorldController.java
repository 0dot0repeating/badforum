package com.jinotrain.badforum.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloWorldController
{
    @ResponseBody
    @RequestMapping(value = "/",
                    method = RequestMethod.GET,
                    produces = "text/plain")
    public String aeiou()
    {
        return "aaaaa";
    }
}
