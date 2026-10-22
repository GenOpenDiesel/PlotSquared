/*
 * PlotSquared, a land and world management plugin for Minecraft.
 * Copyright (C) IntellectualSites <https://intellectualsites.com>
 * Copyright (C) IntellectualSites team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plotsquared.bukkit.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ProjectileEventListenerTest {

    @Test
    void ownPotionDoesNotRequirePvpPermission() {
        Player shooter = entity(Player.class, null);
        PotionSplashEvent event = splash(shooter, Map.of(shooter, 0.75));

        assertFalse(ProjectileEventListener.filterPotionTargets(event, (potion, victim) -> {
            fail("Self effects must not be checked as PvP");
            return false;
        }));
        assertEquals(0.75, event.getIntensity(shooter));
    }

    @Test
    void curingTargetAndShooterBothKeepTheirEffects() {
        Player shooter = entity(Player.class, null);
        LivingEntity zombie = entity(LivingEntity.class, null);
        PotionSplashEvent event = splash(shooter, Map.of(shooter, 0.5, zombie, 1.0));

        assertFalse(ProjectileEventListener.filterPotionTargets(event, (potion, victim) -> victim == zombie));
        assertEquals(0.5, event.getIntensity(shooter));
        assertEquals(1.0, event.getIntensity(zombie));
    }

    @Test
    void blockedPlayerDoesNotCancelAllowedZombieEffect() {
        Player shooter = entity(Player.class, null);
        Player otherPlayer = entity(Player.class, null);
        LivingEntity zombie = entity(LivingEntity.class, null);
        PotionSplashEvent event = splash(shooter, Map.of(otherPlayer, 0.5, zombie, 1.0));

        assertFalse(ProjectileEventListener.filterPotionTargets(event, (potion, victim) -> victim == zombie));
        assertEquals(0.0, event.getIntensity(otherPlayer));
        assertEquals(1.0, event.getIntensity(zombie));
        assertEquals(1, event.getAffectedEntities().size());
    }

    @Test
    void protectedTargetStaysProtectedWhenShooterIsAlsoAffected() {
        Player shooter = entity(Player.class, null);
        LivingEntity protectedTarget = entity(LivingEntity.class, null);
        PotionSplashEvent event = splash(shooter, Map.of(shooter, 0.5, protectedTarget, 1.0));

        assertFalse(ProjectileEventListener.filterPotionTargets(event, (potion, victim) -> false));
        assertEquals(0.5, event.getIntensity(shooter));
        assertEquals(0.0, event.getIntensity(protectedTarget));
    }

    @Test
    void allBlockedTargetsCancelSplash() {
        LivingEntity first = entity(LivingEntity.class, null);
        LivingEntity second = entity(LivingEntity.class, null);
        PotionSplashEvent event = splash(null, Map.of(first, 0.5, second, 1.0));

        assertTrue(ProjectileEventListener.filterPotionTargets(event, (potion, victim) -> false));
        assertTrue(event.getAffectedEntities().isEmpty());
    }

    @Test
    void emptySplashStillReachesProjectileHitChecks() {
        PotionSplashEvent event = splash(null, Map.of());

        assertFalse(ProjectileEventListener.filterPotionTargets(event, (potion, victim) -> false));
    }

    @Test
    void nonPlayerPotionStillChecksTargetProtection() {
        LivingEntity target = entity(LivingEntity.class, null);
        PotionSplashEvent event = splash(null, Map.of(target, 1.0));

        assertFalse(ProjectileEventListener.filterPotionTargets(event, (potion, victim) -> victim == target));
        assertEquals(1.0, event.getIntensity(target));
    }

    private static PotionSplashEvent splash(Player shooter, Map<LivingEntity, Double> targets) {
        return new PotionSplashEvent(entity(ThrownPotion.class, shooter), null, null, null, new LinkedHashMap<>(targets));
    }

    private static <T> T entity(Class<T> type, Player shooter) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getShooter" -> shooter;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> type.getSimpleName();
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }));
    }

}
