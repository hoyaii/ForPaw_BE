package com.hong.ForPaw.controller;

public class UserRequest {

    public record LoginDTO(String email, String password){}

    public record CheckEmailDTO(String email){}


}
