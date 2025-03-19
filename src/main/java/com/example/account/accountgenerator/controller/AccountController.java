package com.example.account.accountgenerator.controller;

import com.example.account.accountgenerator.service.AccountGeneratorService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountGeneratorService accountGeneratorService;

    public AccountController(AccountGeneratorService accountGeneratorService) {
        this.accountGeneratorService = accountGeneratorService;
    }

    @GetMapping("/next")
    public String getNextAccount() {
        return accountGeneratorService.getNextAccount();
    }

    @PostMapping("/return/{accountNumber}")
    public void returnAccount(@PathVariable String accountNumber) {
        accountGeneratorService.returnAccount(accountNumber);
    }
}
