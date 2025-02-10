package com.example;

import java.util.HashMap;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("/notifications")
@Controller
public class NotificationController {

    @Autowired
    ReceiveSendNotifications msgService;

    @RequestMapping(value = "/process", method = RequestMethod.GET)
    @ResponseBody
    List<String> processNotifications(HttpServletRequest request, HttpServletResponse response) {
        return msgService.processNotifications();
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    List<HashMap<String, String>> listMessages(HttpServletRequest request, HttpServletResponse response) {
        return msgService.listMessages();
    }

    @RequestMapping(value = "/purge", method = RequestMethod.GET)
    @ResponseBody
    void purgeQueue(HttpServletRequest request, HttpServletResponse response) {
        msgService.purgeQueue();
    }
}