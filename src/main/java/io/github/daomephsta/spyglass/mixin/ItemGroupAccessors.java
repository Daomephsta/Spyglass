package io.github.daomephsta.spyglass.mixin;

import net.minecraft.item.ItemGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemGroup.class)
public interface ItemGroupAccessors
{
    @Accessor("id")
    public String spyglass_getId();
}
