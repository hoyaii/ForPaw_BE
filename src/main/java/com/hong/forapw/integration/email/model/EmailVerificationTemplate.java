package com.hong.forapw.integration.email.model;

public record EmailVerificationTemplate(
        String verificationCode
) implements TemplateModel {
}