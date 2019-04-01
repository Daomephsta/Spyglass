package io.github.daomephsta.spyglass;

import com.mojang.brigadier.CommandDispatcher;

import io.github.daomephsta.spyglass.command.SpyglassCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.minecraft.server.command.ServerCommandSource;

public class SpyglassInitialiser implements ModInitializer
{
	public static final String MOD_ID = "spyglass";
	
	@Override
	public void onInitialize()
	{
		ServerStartCallback.EVENT.register(server -> registerCommands(server.getCommandManager().getDispatcher()));
	}

	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher)
	{
		SpyglassCommand.register(dispatcher);
	}
}
