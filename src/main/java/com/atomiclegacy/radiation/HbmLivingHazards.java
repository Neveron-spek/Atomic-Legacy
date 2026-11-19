package com.atomiclegacy.radiation;

import com.mojang.serialization.Codec;
import com.atomiclegacy.network.PlayerInformPayload;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmDataAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public class HbmLivingHazards {
    public static final int MAX_ASBESTOS = 60 * 60 * 20;
    public static final int MAX_BLACK_LUNG = 2 * 60 * 60 * 20;

    public static final StreamCodec<RegistryFriendlyByteBuf, HbmLivingHazards> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> buffer.writeNbt(data.save()),
            buffer -> {
                HbmLivingHazards data = new HbmLivingHazards();
                data.load(buffer.readNbt());
                return data;
            }
    );

    public static final Codec<HbmLivingHazards> CODEC = CompoundTag.CODEC.xmap(
            tag -> {
                HbmLivingHazards data = new HbmLivingHazards();
                data.load(tag);
                return data;
            },
            HbmLivingHazards::save
    );

    private int asbestos;
    private int blackLung;
    private int fire;
    private int balefire;
    /** Legacy black-fire timer used by EntityFireLingering type BLACK. */
    private int blackFire;
    /** HbmLivingProps.phosphorus (white phosphorus burn timer, e.g. XFactory12ga g12_phosphorus). */
    private int phosphorus;
    /** Mutable attachments only need an explicit setData call when their encoded state changed. */
    private transient boolean dirty;

    public static HbmLivingHazards get(LivingEntity entity) {
        return entity.getData(HbmDataAttachments.LIVING_HAZARDS);
    }

    public static void set(LivingEntity entity, HbmLivingHazards data) {
        if (!data.dirty) {
            return;
        }
        data.dirty = false;
        entity.setData(HbmDataAttachments.LIVING_HAZARDS, data);
    }

    public static void clear(LivingEntity entity) {
        HbmLivingHazards data = get(entity);
        data.setAsbestos(entity, 0);
        data.setBlackLung(entity, 0);
        data.setFire(0);
        data.setBalefire(0);
        data.setBlackFire(0);
        data.setPhosphorus(0);
        set(entity, data);
    }

    public int getAsbestos() {
        return HbmConfig.ENABLE_ASBESTOS.get() ? asbestos : 0;
    }

    public void setAsbestos(LivingEntity entity, int asbestos) {
        if (!HbmConfig.ENABLE_ASBESTOS.get()) {
            updateAsbestos(0);
            return;
        }
        updateAsbestos(Math.max(0, asbestos));
        if (this.asbestos >= MAX_ASBESTOS) {
            updateAsbestos(0);
            entity.hurt(entity.damageSources().source(HbmDamageTypes.ASBESTOS), 1000.0F);
        }
    }

    public void addAsbestos(LivingEntity entity, int amount) {
        if (!HbmConfig.ENABLE_ASBESTOS.get()) {
            return;
        }
        setAsbestos(entity, this.asbestos + amount);
        if (entity instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, PlayerInformPayload.translated(
                    12, // [parity R1: lungs] ServerProxy.ID_GAS_HAZARD
                    "info.atomiclegacy.asbestos",
                    0xFF5555,
                    3000
            ));
        }
    }

    public int getBlackLung() {
        return HbmConfig.ENABLE_COAL_DUST.get() ? blackLung : 0;
    }

    public void setBlackLung(LivingEntity entity, int blackLung) {
        if (!HbmConfig.ENABLE_COAL_DUST.get()) {
            updateBlackLung(0);
            return;
        }
        updateBlackLung(Math.max(0, blackLung));
        if (this.blackLung >= MAX_BLACK_LUNG) {
            updateBlackLung(0);
            entity.hurt(entity.damageSources().source(HbmDamageTypes.BLACK_LUNG), 1000.0F);
        }
    }

    public void addBlackLung(LivingEntity entity, int amount) {
        if (!HbmConfig.ENABLE_COAL_DUST.get()) {
            return;
        }
        setBlackLung(entity, this.blackLung + amount);
        if (entity instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, PlayerInformPayload.translated(
                    12, // [parity R1: lungs] ServerProxy.ID_GAS_HAZARD
                    "info.atomiclegacy.coaldust",
                    0xFF5555,
                    3000
            ));
        }
    }

    public int getFire() {
        return fire;
    }

    public void extendFire(int duration) {
        setFire(Math.max(this.fire, Math.max(0, duration)));
    }

    /** HbmLivingProps.fire += duration (XFactory40mm.LAMBDA_STANDARD_IGNITE). */
    public void addFire(int duration) {
        setFire(this.fire + duration);
    }

    public void clearFire() {
        setFire(0);
    }

    public void tickFire() {
        if (this.fire > 0) {
            setFire(this.fire - 1);
        }
    }

    public int getBalefire() {
        return balefire;
    }

    public void extendBalefire(int duration) {
        setBalefire(Math.max(this.balefire, Math.max(0, duration)));
    }

    public void tickBalefire() {
        if (this.balefire > 0) {
            setBalefire(this.balefire - 1);
        }
    }

    public int getBlackFire() {
        return blackFire;
    }

    /** EntityFireLingering's BLACK type starts at 200 ticks, then adds five per hit. */
    public void extendBlackFire() {
        setBlackFire(this.blackFire < 200 ? 200 : this.blackFire + 5);
    }

    /** [weapons batch F4] XFactory35800.LAMBDA_BLACK_IMPACT: HbmLivingProps.getData(hit).blackFire += amount. */
    public void addBlackFire(int amount) {
        setBlackFire(this.blackFire + amount);
    }
    public void tickBlackFire() {
        if (this.blackFire > 0) {
            setBlackFire(this.blackFire - 1);
        }
    }

    public int getPhosphorus() {
        return phosphorus;
    }

    /** XFactory12ga g12_phosphorus onImpact: if(data.phosphorus < 300) data.phosphorus = 300. */
    public void extendPhosphorus(int duration) {
        if (this.phosphorus < duration) {
            setPhosphorus(duration);
        }
    }

    public void clearPhosphorus() {
        setPhosphorus(0);
    }

    public void tickPhosphorus() {
        if (this.phosphorus > 0) {
            setPhosphorus(this.phosphorus - 1);
        }
    }

    private void setPhosphorus(int value) {
        if (this.phosphorus != value) {
            this.phosphorus = value;
            this.dirty = true;
        }
    }

    private void updateAsbestos(int value) {
        if (this.asbestos != value) {
            this.asbestos = value;
            this.dirty = true;
        }
    }

    private void updateBlackLung(int value) {
        if (this.blackLung != value) {
            this.blackLung = value;
            this.dirty = true;
        }
    }

    private void setFire(int value) {
        if (this.fire != value) {
            this.fire = value;
            this.dirty = true;
        }
    }

    private void setBalefire(int value) {
        if (this.balefire != value) {
            this.balefire = value;
            this.dirty = true;
        }
    }

    private void setBlackFire(int value) {
        if (this.blackFire != value) {
            this.blackFire = value;
            this.dirty = true;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("asbestos", asbestos);
        tag.putInt("black_lung", blackLung);
        tag.putInt("fire", fire);
        tag.putInt("balefire", balefire);
        tag.putInt("black_fire", blackFire);
        tag.putInt("phosphorus", phosphorus);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        this.asbestos = Math.max(0, tag.getInt("asbestos"));
        this.blackLung = Math.max(0, tag.getInt("black_lung"));
        this.fire = Math.max(0, tag.getInt("fire"));
        this.balefire = Math.max(0, tag.getInt("balefire"));
        this.blackFire = Math.max(0, tag.getInt("black_fire"));
        this.phosphorus = Math.max(0, tag.getInt("phosphorus"));
        this.dirty = false;
    }
}
