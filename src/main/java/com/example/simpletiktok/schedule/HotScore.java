package com.example.simpletiktok.schedule;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HotScore {
    private final Long videoId;
    private final double hot;
}
