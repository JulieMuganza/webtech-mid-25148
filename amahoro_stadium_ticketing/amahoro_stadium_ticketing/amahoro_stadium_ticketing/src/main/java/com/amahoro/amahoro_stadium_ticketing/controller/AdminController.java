package com.amahoro.amahoro_stadium_ticketing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminController {



    @RequestMapping(value = {"/admin/dashboard"}, method = RequestMethod.GET)
    public String adminHome() {
        return "admin/dashboard";
    }
}
