package com.dame.entity;

/**
 * Status of a game challenge between players.
 */
public enum ChallengeStatus {
    /**
     * Challenge is waiting for response
     */
    PENDING,

    /**
     * Challenge was accepted
     */
    ACCEPTED,

    /**
     * Challenge was declined
     */
    DECLINED,

    /**
     * Challenge expired without response
     */
    EXPIRED,

    /**
     * Challenge was cancelled by the challenger
     */
    CANCELLED
}
