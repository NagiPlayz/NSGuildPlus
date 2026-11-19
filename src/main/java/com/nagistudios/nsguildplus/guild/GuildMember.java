package com.nagistudios.nsguildplus.guild;

import java.util.UUID;

public final class GuildMember {
    public final UUID uuid;
    public String name;
    public long joinedAt;
    public int kills;
    public int deaths;
    public int mobsKilled;
    public int blocksPlaced;
    public int blocksMined;
    public int playtimeMinutes;
    public int achievements;
    public int xpLevel;
    public double balance;
    public int annualPoints;
    public boolean champion;
    public int rankNumber = 5;

    public GuildMember(UUID uuid, String name, long joinedAt) {
        this.uuid = uuid;
        this.name = name;
        this.joinedAt = joinedAt;
    }
}
