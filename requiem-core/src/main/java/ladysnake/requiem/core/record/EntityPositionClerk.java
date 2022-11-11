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
package ladysnake.requiem.core.record;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import ladysnake.requiem.api.v1.event.requiem.EntityRecordUpdateCallback;
import ladysnake.requiem.api.v1.record.EntityPointer;
import ladysnake.requiem.api.v1.record.GlobalRecord;
import ladysnake.requiem.api.v1.record.GlobalRecordKeeper;
import ladysnake.requiem.api.v1.record.RecordType;
import ladysnake.requiem.core.RequiemCore;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class EntityPositionClerk implements Component {
    public static final ComponentKey<EntityPositionClerk> KEY = ComponentRegistry.getOrCreate(RequiemCore.id("entity_clerk"), EntityPositionClerk.class);
    public static final Identifier UPDATE_ACTION_ID = RequiemCore.id("entity_status_sync");

    public static EntityPositionClerk get(LivingEntity entity) {
        return KEY.get(entity);
    }

    private final LivingEntity entity;
    private final Map<GlobalRecord, RecordType<EntityPointer>> refs;
    private boolean ticking;

    public EntityPositionClerk(LivingEntity entity) {
        this.entity = entity;
        this.refs = new HashMap<>();
    }

    public void linkWith(GlobalRecord record, RecordType<EntityPointer> pointerType) {
        this.refs.put(record, pointerType);
        this.updateRecord(record, pointerType);

        if (this.ticking) {
            record.addTickingAction(UPDATE_ACTION_ID, r -> updateRecord(r, pointerType));
        }
    }

    public void startTicking() {
        this.ticking = true;
        for (var anchor : this.refs.entrySet()) {
            anchor.getKey().addTickingAction(UPDATE_ACTION_ID, record -> updateRecord(record, anchor.getValue()));
        }
    }

    public void transferFrom(EntityPositionClerk original) {
        original.refs.forEach(this::linkWith);
        original.refs.clear();
    }

    public void stopTicking() {
        for (GlobalRecord anchor : this.refs.keySet()) {
            anchor.removeTickingAction(UPDATE_ACTION_ID);
        }
        this.ticking = false;
    }

    public void destroy() {
        this.refs.forEach(GlobalRecord::remove);
    }

    private void updateRecord(GlobalRecord record, RecordType<EntityPointer> pointerType) {
        if (this.entity.getHealth() <= 0.0F) {
            record.remove(pointerType);
        } else {
            Optional<EntityPointer> ptr = record.get(pointerType);
            if (ptr.isEmpty() || !entity.getPos().equals(ptr.get().pos())) {
                record.put(pointerType, new EntityPointer(entity));
            }
            EntityRecordUpdateCallback.EVENT.invoker().update(entity, record);
        }
    }

    private GlobalRecordKeeper getTracker() {
        return GlobalRecordKeeper.get(entity.getWorld());
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        NbtList refs = tag.getList("refs", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < refs.size(); i++) {
            NbtCompound refNbt = refs.getCompound(i);
            this.getTracker().getRecord(refNbt.getUuid("uuid"))
                .ifPresent(r -> RecordType.REGISTRY.getOrEmpty(Identifier.tryParse(refNbt.getString("type")))
                    .flatMap(refType -> refType.tryCast(EntityPointer.CODEC))
                    .ifPresent(refType -> this.linkWith(r, refType)));
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        for (var ref : this.refs.entrySet()) {
            NbtCompound refNbt = new NbtCompound();
            refNbt.put("uuid", NbtHelper.fromUuid(ref.getKey().getUuid()));
            refNbt.putString("type", ref.getValue().getId().toString());
            list.add(refNbt);
        }
        tag.put("refs", list);
    }
}
