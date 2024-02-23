package com.hong.ForPaw.domain.Shelter;


import java.util.List;

public record ShelterWrapperDTO(Response response) {
    public record Response(Body body) {
        public record Body(Items items) {
            public record Items(List<ShelterDTO> item) {
            }
        }
    }
}
