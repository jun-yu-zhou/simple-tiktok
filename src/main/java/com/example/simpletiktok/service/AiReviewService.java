package com.example.simpletiktok.service;

public interface AiReviewService {

    Double authImg(String imgUrl);

    Double authVideo(String videoUrl);

    Double authText(String text);
}
