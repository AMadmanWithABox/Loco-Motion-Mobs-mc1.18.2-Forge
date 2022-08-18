package com.fcs.locomotionmobs.entities.util;

import com.fcs.locomotionmobs.entities.QueenBuzzlet;
import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


import java.util.Objects;
import java.util.Set;

//This class is used to create a custom boss event for the Queen Buzzlet.
//It is used to allow the Queen Buzzlet entities to use the same boss event.
//This is done by adding all the entities to the same set and then calculating the total health of the set.
//This is then used to calculate the progress of the boss event.
//This class also takes care of Phases of the boss event.
//The phases are:
//Phase Full: The Queen Buzzlet is spawned. There is one Queen Buzzlet entity in the set.
//Phase Half: The Queen Buzzlet is at half health. The existing Queen Buzzlet entity gets split into two entities using the split method in the QueenBuzzlet class.
//Phase Quarter: The Queen Buzzlet is at quarter health. The 2 existing Queen Buzzlet entities get split into 4 entities using the split method in each QueenBuzzlet class.
//When the boss event is completed, one queen buzzlet entity is chosen at random from the set. When this entity is removed from the world, it drops the Queen Buzzlet's loot.
//The remaining entities are removed from the world, and the event is completed.
//if a queen buzzlet entity is killed in phase half, it will split into two entities.
//if a queen buzzlet entity is killed in phase quarter, it is removed from the world.
//if a queen buzzlet entity is spawned through the split method, it is added to the set.
public class QueenBuzzletEvent {
    public static QueenBuzzletEvent event;
    public final QueenBuzzlet owner;
    private static final Component QUEEN_BUZZLET_EVENT_TITLE = new TextComponent("Queen Buzzlet");
    public static final float MAX_HEALTH = 300.0F;
    private final ServerBossEvent bossEvent = new ServerBossEvent(QUEEN_BUZZLET_EVENT_TITLE,
            ServerBossEvent.BossBarColor.PURPLE,
            ServerBossEvent.BossBarOverlay.NOTCHED_6);
    private Set<QueenBuzzlet> queenBuzzlets = Sets.newHashSet();
    private Phase phase;
    private final ServerLevel level;
    private float health;
    public QueenBuzzletEvent(QueenBuzzlet queenBuzzlet) {
        this.owner = queenBuzzlet;
        this.queenBuzzlets.add(queenBuzzlet);
        this.level = Minecraft.getInstance().getSingleplayerServer().getLevel(queenBuzzlet.level.dimension());
        this.phase = Phase.FULL;
    }

    public static QueenBuzzletEvent startNewEvent(QueenBuzzlet queenBuzzlet) {
        event = new QueenBuzzletEvent(queenBuzzlet);
        return event;
    }

    public void addPlayer(ServerPlayer player) {
        this.bossEvent.addPlayer(player);
    }

    public void removePlayer(ServerPlayer player) {
        this.bossEvent.removePlayer(player);
    }

    private enum Phase {
        FULL,
        HALF,
        QUARTER
    }

    public float getTotalHealth() {
        float totalHealth = 0;
        for (QueenBuzzlet queenBuzzlet : this.queenBuzzlets) {
            totalHealth += queenBuzzlet.getHealth();
        }
        return totalHealth;
    }

    public void tick(){
        for (QueenBuzzlet queenBuzzlet : queenBuzzlets) {
            if(queenBuzzlet.isDeadOrDying()){
                doCreateNewSet(queenBuzzlet);
            }
        }

//        //if the owner is dying
//        if(owner.getHealth() <= 20){
//            //if the owner is the last entity in the set
//            if(queenBuzzlets.stream().count() <= 1){
//                //end the event
//                this.endEvent();
//            //if the owner is not the last entity in the set
//            }else{
//                //prevent the owner from being removed dying as it contains the event
//                owner.setHealth(1);
//            }
//        }
        this.health = this.getTotalHealth();
        this.bossEvent.setProgress(this.health / MAX_HEALTH);
        if (this.phase == Phase.FULL && this.health + 4<= MAX_HEALTH / 2) {
            bossEvent.getPlayers().forEach(p -> {p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is in Phase Half"), ChatType.CHAT, p.getUUID()));});
            this.phase = Phase.HALF;
            this.causeSplit();
        } else if (this.phase == Phase.HALF && this.health + 4 <= MAX_HEALTH / 4) {
            bossEvent.getPlayers().forEach(p -> {p.connection.send(new ClientboundChatPacket(new TextComponent("Queen Buzzlet is in Phase Quarter"), ChatType.CHAT, p.getUUID()));});
            this.phase = Phase.QUARTER;
            this.causeSplit();
        }
    }

    private void doCreateNewSet(QueenBuzzlet queenBuzzlet) {
        Set<QueenBuzzlet> newQueenBuzzlets = Sets.newHashSet();
        for (QueenBuzzlet buzzlet : queenBuzzlets) {
            if (buzzlet != queenBuzzlet) {
                newQueenBuzzlets.add(buzzlet);
            }
        }
        this.queenBuzzlets = newQueenBuzzlets;
    }

    public boolean canOwnerBeRemoved() {
        return (long) this.queenBuzzlets.size() == 1;
    }

    public void endEvent() {
        for (QueenBuzzlet queenBuzzlet : queenBuzzlets) {
            queenBuzzlet.remove(Entity.RemovalReason.DISCARDED);
        }
        this.queenBuzzlets.clear();
    }

    private void causeSplit() {
        Set<QueenBuzzlet> children = Sets.newHashSet();
        if(phase == Phase.HALF) {
            children.addAll(owner.split(2));
        }else {
            children.addAll(owner.split(1));
        }
        this.queenBuzzlets.addAll(children);
    }
}
