package com.example.simpletiktok.service;

public interface EmailService {
    void send(String to, String subject, String text);
}