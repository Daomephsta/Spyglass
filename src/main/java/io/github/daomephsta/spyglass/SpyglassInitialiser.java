package io.github.daomephsta.spyglass;

import com.mojang.brigadier.CommandDispatcher;
import io.github.daomephsta.spyglass.command.SpyglassCommand;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.minecraft.server.command.ServerCommandSource;

public class SpyglassInitialiser
{
	public static final String MOD_ID = "spyglass";

	public static void initialise()
	{
		ServerStartCallback.EVENT.register(server -> registerCommands(server.getCommandManager().getDispatcher()));
	}

	private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher)
	{
		SpyglassCommand.register(dispatcher);
	}
}
