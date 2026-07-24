package br.com.cooperativevoting.voting.domain.model.enums;

public enum AssociateVotingStatus {
    ABLE_TO_VOTE,
    UNABLE_TO_VOTE;

    public boolean canVote() {
        return this == ABLE_TO_VOTE;
    }
}
