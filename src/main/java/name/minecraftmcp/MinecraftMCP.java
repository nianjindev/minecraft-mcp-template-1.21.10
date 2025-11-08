package name.minecraftmcp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.*;
import net.minecraft.network.chat.PlayerChatMessage;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map; // For building the JSON object
import java.util.concurrent.CompletableFuture; // For async networking

public class MinecraftMCP implements ModInitializer {
	public static final String MOD_ID = "minecraft-mcp";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final HttpClient httpClient = HttpClient.newHttpClient();
	private static final Gson gson = new Gson();
	private static final String MCP_SERVER_URL = "http://localhost:8080/mcp";

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			String blockName = state.getBlock().toString();

			LOGGER.info("Player broke block: " + blockName + " at " + pos.toString());

			sendEventToMCP(blockName, pos.toString());

			return true;

		});

		
	}

	private void sendEventToMCP(String blockName, String position) {
		CompletableFuture.runAsync(() -> {
			try {
				Map<String, Object> parameters = Map.of(
					"blockName", blockName,
					"position", position
				);

				Map<String, Object> mcpPayload = Map.of(
                    "mcp_version", "1.0",
                    "call_id", "mc-call-" + System.currentTimeMillis(), // Simple unique ID
                    "type", "tool_call",
                    "tool_name", "report_block_break",
                    "parameters", parameters
                );

				String jsonPayload = gson.toJson(mcpPayload);

				HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(MCP_SERVER_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();
				
				HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

				LOGGER.info("MCP Server Response: " + response.body());
			} catch (Exception e) {
                // If the Python server isn't running, this will catch the error
                LOGGER.warn("Failed to send event to MCP server. Is it running?");
                LOGGER.warn(e.getMessage());
            }
		});
	}
}