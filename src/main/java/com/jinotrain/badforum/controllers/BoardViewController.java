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

    @Transactional
    @RequestMapping(value = "/board/*")
    public ModelAndView getBoard(HttpServletRequest  request,
                                 HttpServletResponse response)
    {
        String requestUrl = request.getServletPath();
        long boardID;

        try
        {
            boardID = Long.valueOf(requestUrl.substring("/board/".length()));
        }
        catch (NumberFormatException e)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return new ModelAndView("viewboard_notfound.html");
        }

        ForumBoard viewBoard = boardRepository.findById(boardID).orElse(null);

        if (viewBoard == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return new ModelAndView("viewboard_notfound.html");
        }

        BoardViewData boardViewData = getBoardViewData(viewBoard, em);

        ModelAndView mav = new ModelAndView("viewboard.html");
        mav.addObject("boardViewData", boardViewData);
        return mav;
    }
}