package io.github.daomephsta.spyglass.command;

import static net.minecraft.server.command.ServerCommandManager.argument;
import static net.minecraft.server.command.ServerCommandManager.literal;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import io.github.daomephsta.spyglass.SpyglassInitialiser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.command.arguments.IdentifierArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.text.event.ClickEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class SpyglassCommand
{
	private static final DynamicCommandExceptionType INVALID_REGISTRY_EXCEPTION = 
		new DynamicCommandExceptionType(registryId -> new TranslatableTextComponent(SpyglassInitialiser.MOD_ID + ".argument.registry.invalid", registryId));
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
	{
		dispatcher.register
		(
			literal("spyglass")
			.then
			(
				dump()
				)
			.then
			(
				get()
				)
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> dump()
	{
		return literal("dump")
			.then
			(
				literal("registry")
				.then
				(
					argument("registry_id", IdentifierArgumentType.create())
					.suggests((context, builder) -> CommandSource.suggestIdentifiers(Registry.REGISTRIES.getIds(), builder))
					.executes(context ->
					{
						Identifier registryId = context.getArgument("registry_id", Identifier.class);
						Registry<?> registry = Registry.REGISTRIES.getOrEmpty(registryId)
							.orElseThrow(() -> INVALID_REGISTRY_EXCEPTION.create(registryId));
						return dumpRegistry(registry, context.getSource());
					})
				)
				.then
				(
					literal("all").executes(context -> dumpAllRegistries(context.getSource()))
				)
			)
			.then
			(
				literal("item_groups").executes(context -> dumpItemGroups(context.getSource()))
			)
			.then
			(
				literal("mods").executes(context -> dumpMods(context.getSource()))
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> get()
	{
		return literal("get")
			.then
			(
				literal("biome").executes(context ->
				{
					World world = context.getSource().getWorld();
					BlockPos position = new BlockPos(context.getSource().getPosition());
					Biome biome = world.getBiome(position);
					context.getSource().sendFeedback(new StringTextComponent(Registry.BIOME.getId(biome).toString()), false);
					return Command.SINGLE_SUCCESS;
				})
			)
			.then
			(
				literal("held_item")
				.then
				(
					literal("main_hand").executes(context ->
					{
						PlayerEntity player = context.getSource().getPlayer();
						context.getSource().sendFeedback(formatStack(player.getMainHandStack()), false);
						return Command.SINGLE_SUCCESS;
					})
				)
				.then
				(
					literal("off_hand").executes(context ->
					{
						PlayerEntity player = context.getSource().getPlayer();
						context.getSource().sendFeedback(formatStack(player.getOffHandStack()), false);
						return Command.SINGLE_SUCCESS;
					})
				)
			);
	}

	private static TranslatableTextComponent formatStack(ItemStack stack)
	{
		return new TranslatableTextComponent(SpyglassInitialiser.MOD_ID + ".chat.stack_format", 
			stack.getAmount(), stack.getDisplayName().getFormattedText(), stack.hasTag() ? stack.getTag().toString() : "{}");
	}

	private static int dumpMods(ServerCommandSource serverCommandSource)
	{
		dump(FabricLoader.getInstance().getAllMods().stream()
			.map(SpyglassCommand::describeMod), 
		Paths.get("mods"), serverCommandSource);
		return Command.SINGLE_SUCCESS;
	}

	private static String describeMod(ModContainer mod)
	{
		ModMetadata metadata = mod.getMetadata();
		return String.format("%s %s (%s) - %s", 
			metadata.getId(), metadata.getVersion(), 
			metadata.getName(), metadata.getDescription());
	}

	public static int dumpAllRegistries(ServerCommandSource serverCommandSource)
	{
		for(Registry<?> registry : Registry.REGISTRIES)
		{
			int result = dumpRegistry(registry, serverCommandSource);
			if (result != Command.SINGLE_SUCCESS)
				return result;
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int dumpRegistry(Registry<?> registry, ServerCommandSource serverCommandSource)
	{
		Path registryDumpSubPath = Paths.get("registries", 
			Registry.REGISTRIES.getId((MutableRegistry<?>) registry).toString().replace(':', '/'));
		dump(registry.getIds().stream().map(Object::toString), registryDumpSubPath, serverCommandSource); 
		return Command.SINGLE_SUCCESS;
	}

	private static int dumpItemGroups(ServerCommandSource serverCommandSource)
	{
		dump(Arrays.stream(ItemGroup.GROUPS).map(SpyglassCommand::getItemGroupId), Paths.get("item_groups"), serverCommandSource);
		return Command.SINGLE_SUCCESS;
	}

	private static Field ITEM_GROUP_ID;
	static
	{
		//Need to use reflection because the getter is client-only
		//TODO Remove when accessor mixins work again
		Exception lastException = null;
		try
		{
			ITEM_GROUP_ID = ItemGroup.class.getDeclaredField("field_7935");
		}
		catch (NoSuchFieldException | SecurityException e)
		{
			lastException = e;
		}
		try
		{
			ITEM_GROUP_ID = ItemGroup.class.getDeclaredField("id");
		}
		catch (NoSuchFieldException | SecurityException e)
		{
			lastException = e;
		}
		if (ITEM_GROUP_ID != null)
			ITEM_GROUP_ID.setAccessible(true);
		else
			throw new RuntimeException("Failed to get ItemGroup id field", lastException);
	}

	private static String getItemGroupId(ItemGroup itemGroup)
	{
		try
		{
			return (String) ITEM_GROUP_ID.get(itemGroup);
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			throw new RuntimeException("Failed to get ItemGroup id", e);
		}
	}

	private static boolean dump(Stream<String> lines, Path subPath, ServerCommandSource serverCommandSource)
	{
		return dump(lines::iterator, subPath, serverCommandSource);
	}

	private static boolean dump(Iterable<String> lines, Path subPath, ServerCommandSource serverCommandSource)
	{
		Path tempPath = Paths.get("dumps").resolve(subPath);
		final Path path = tempPath.resolveSibling(tempPath.getFileName() + ".txt");
		List<String> collectedLines = Lists.newArrayList(lines);
		CompletableFuture<Void> dumpIO = CompletableFuture.runAsync(() ->
		{
			try
			{
				Files.createDirectories(path.getParent());
				Files.write(path, collectedLines, StandardOpenOption.CREATE);
			}
			catch (IOException e)
			{
				throw new RuntimeException("An unrecoverable error occured while writing to " + path.toAbsolutePath(), e);
			}
		}).thenRun(() -> 
		{
			TextComponent link = new StringTextComponent(path.toString())
				.applyFormat(TextFormat.UNDERLINE)
				.modifyStyle(style -> style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path.toAbsolutePath().toString())));
			TextComponent feedback = new TranslatableTextComponent(SpyglassInitialiser.MOD_ID + ".command.save_dump.success", link);
			serverCommandSource.sendFeedback(feedback, false);
		}).exceptionally(ex -> 
		{
			serverCommandSource.sendFeedback(new TranslatableTextComponent(SpyglassInitialiser.MOD_ID + ".command.save_dump.failure"), false);
			LOGGER.error("An error occured while dumping to file", ex);
			return null;
		});
		return dumpIO.isDone() && !dumpIO.isCompletedExceptionally();
	}
}
