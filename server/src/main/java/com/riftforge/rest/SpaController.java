package com.riftforge.rest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/** Forwards unmatched routes to index.html so React Router handles them. */
@Controller
public class SpaController implements ErrorController {
  @RequestMapping("/error")
  public String handleError() {
    return "forward:/index.html";
  }
}
