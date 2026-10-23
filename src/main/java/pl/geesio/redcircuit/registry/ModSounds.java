package pl.geesio.redcircuit.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import pl.geesio.redcircuit.platform.registries.DeferredHolder;
import pl.geesio.redcircuit.platform.registries.DeferredRegister;
import pl.geesio.redcircuit.RedCircuit;

/** Dźwięki moda (pliki w assets/redcircuit/sounds.json - można je podmienić paczką zasobów). */
public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, RedCircuit.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GATE_TICK = SOUNDS.register("gate_tick",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RedCircuit.MODID, "gate_tick")));
}
