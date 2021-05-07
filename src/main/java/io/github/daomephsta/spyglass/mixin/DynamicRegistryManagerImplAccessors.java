package io.github.daomephsta.spyglass.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;

@Mixin(DynamicRegistryManager.Impl.class)
public interface DynamicRegistryManagerImplAccessors
{
    @Accessor("registries")
    public Map<? extends RegistryKey<? extends Registry<?>>, ? extends SimpleRegistry<?>> spyglass_getRegistries();
}
