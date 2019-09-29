package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.data.BoardViewData;
import com.jinotrain.badforum.db.entities.ForumBoard;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class BoardViewController extends ForumController
{
    @Transactional
    @RequestMapping(value = "/")
    public ModelAndView getTopLevel(HttpServletRequest  request,
                                    HttpServletResponse response)
    {
        ForumBoard rootBoard = boardRepository.findRootBoard();
        BoardViewData rootBoardViewData = getBoardViewData(rootBoard, em);

        ModelAndView mav = new ModelAndView("viewboard.html");
        mav.addObject("boardViewData", rootBoardViewData);
        return mav;
    }
}