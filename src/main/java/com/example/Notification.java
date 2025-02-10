package com.example;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Notification {
    private String address;
    private String subject;
    private String body;
}
