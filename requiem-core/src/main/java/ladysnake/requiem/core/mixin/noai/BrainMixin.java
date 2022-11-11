/*
 * Requiem
 * Copyright (C) 2017-2022 Ladysnake
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
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.core.mixin.noai;

import ladysnake.requiem.core.entity.ai.DisableableAiController;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Brain.class)
public abstract class BrainMixin implements DisableableAiController {
    @Unique
    private boolean disabled;

    @Override
    public void requiem$setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick(ServerWorld world, LivingEntity entity, CallbackInfo ci) {
        if (this.disabled) {
            ci.cancel();
        }
    }

    @Inject(method = "isMemoryInState", at = @At("HEAD"), cancellable = true)
    private void isMemoryInState(MemoryModuleType<?> type, MemoryModuleState state, CallbackInfoReturnable<Boolean> cir) {
        if (this.disabled) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getOptionalMemory", at = @At("HEAD"), cancellable = true)
    private void getOptionalMemory(MemoryModuleType<?> type, CallbackInfoReturnable<Optional<?>> cir) {
        if (this.disabled) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
