package com.hong.ForPaw.domain.Shelter;


import java.util.List;

public record ShelterWrapperDTO(Response response) {
    public record Response(Header header, Body body) {
        public record Header(Long reqNo, String resultCode, String resultMsg, String errorMsg){}
        public record Body(Items items) {
            public record Items(List<ShelterDTO> item) {
            }
        }
    }
}
