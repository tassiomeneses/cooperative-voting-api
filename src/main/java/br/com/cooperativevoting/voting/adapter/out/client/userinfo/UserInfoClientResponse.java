package br.com.cooperativevoting.voting.adapter.out.client.userinfo;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserInfoClientResponse(
    @JsonProperty("status")
    String status
) {

    public static UserInfoClientResponse unableToVote() {
        return new UserInfoClientResponse("UNABLE_TO_VOTE");
    }
}
