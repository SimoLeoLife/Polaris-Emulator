package com.eu.habbo.habbohotel.games;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.core.WiredManager;

public class GamePlayer {

    private final Habbo habbo;
    private GameTeamColors teamColor;
    private int score;
    private int wiredScore;

    public GamePlayer(Habbo habbo, GameTeamColors teamColor) {
        this.habbo = habbo;
        this.teamColor = teamColor;
    }

    public void reset() {
        this.score = 0;
        this.wiredScore = 0;
    }

    public void addScore(int amount) {
        addScore(amount, false);
    }

    public void addScore(int amount, boolean isWired) {
        com.eu.habbo.habbohotel.rooms.Room roomToTrigger = null;
        com.eu.habbo.habbohotel.rooms.RoomUnit roomUnitToTrigger = null;
        int currentScore = 0;
        int scoreAdded = 0;
        GameTeam team = null;

        synchronized (this) {
            if (this.habbo.getHabboInfo().getGamePlayer() != null
                    && this.habbo.getHabboInfo().getCurrentGame() != null
                    && this.habbo
                                    .getHabboInfo()
                                    .getCurrentRoom()
                                    .getGame(this.habbo.getHabboInfo().getCurrentGame())
                                    .getTeamForHabbo(this.habbo)
                            != null) {
                team = this.habbo
                        .getHabboInfo()
                        .getCurrentRoom()
                        .getGame(this.habbo.getHabboInfo().getCurrentGame())
                        .getTeamForHabbo(this.habbo);
                int scoreBefore = this.score;
                this.score += amount;

                if (this.score < 0) this.score = 0;

                if (isWired) {
                    this.wiredScore += amount;

                    if (this.wiredScore < 0) {
                        this.wiredScore = 0;
                    }

                    if (this.wiredScore > this.score) {
                        this.wiredScore = this.score;
                    }
                }

                roomToTrigger = this.habbo.getHabboInfo().getCurrentRoom();
                roomUnitToTrigger = this.habbo.getRoomUnit();
                // What actually changed: a score cannot go below 0, so a removal may take less.
                scoreAdded = this.score - scoreBefore;
                currentScore = this.score;
            }
        }

        if (roomToTrigger != null && roomUnitToTrigger != null) {
            // "Score achieved" is about the team, as in Habbo: its total crossing the target, not one
            // player's own score. A player outside any team counts on their own.
            int teamScore = (team != null) ? team.getTotalScore() : currentScore;
            WiredManager.triggerScoreAchieved(roomToTrigger, roomUnitToTrigger, teamScore, scoreAdded);
        }
    }

    public Habbo getHabbo() {
        return this.habbo;
    }

    public GameTeamColors getTeamColor() {
        return this.teamColor;
    }

    public int getScore() {
        return this.score;
    }

    public int getScoreAchievementValue() {
        return this.score - this.wiredScore;
    }
}
