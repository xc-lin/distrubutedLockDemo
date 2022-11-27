package com.lxc.distrubutedlock.demo.controller;

import com.lxc.distrubutedlock.demo.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Frank_lin
 * @date 2022/11/18
 */
@RestController
public class IndexController {
    @Autowired
    private StockService stockService;


    @GetMapping("/deduct")
    public void deduct(){
        stockService.deduct();
    }
}
