package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.db.repositories.DBTestDummyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class HelloWorldController
{
    @Autowired
    private DBTestDummyRepository testRepo;

    @RequestMapping(value = "/",
                    method = RequestMethod.GET,
                    produces = "text/html")
    public ModelAndView aeiou(HttpServletRequest request,
                              HttpServletResponse response)
    {
        ModelAndView mav = new ModelAndView("aeiou.html");
        mav.addObject("testObjs", testRepo.findAll());
        return mav;
    }
}
