package com.jinotrain.badforum.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class HelloWorldController
{
    @RequestMapping(value = "/",
                    method = RequestMethod.GET,
                    produces = "text/plain")
    public ModelAndView aeiou(HttpServletRequest request,
                              HttpServletResponse response)
    {
        ModelAndView mav = new ModelAndView();

        mav.setViewName("aeiou.html");
        mav.addObject("u", "gey");
        return mav;
    }
}
