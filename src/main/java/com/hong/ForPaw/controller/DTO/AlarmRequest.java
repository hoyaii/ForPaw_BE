package com.hong.ForPaw.controller.DTO;

import jakarta.validation.constraints.NotBlank;

public class AlarmRequest {

    public record ReadAlarmDTO(@NotBlank Long id) {}
}
