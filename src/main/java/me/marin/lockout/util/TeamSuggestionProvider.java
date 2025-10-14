package me.marin.lockout.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class TeamSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        Collection<String> teamNames = context.getSource().getServer().getScoreboard().getTeamNames();
        String[] existingTeams = builder.getRemaining().split(" ");
        return CommandSource.suggestMatching(
                teamNames
                        .stream()
                        .filter(name -> Arrays.stream(existingTeams).noneMatch(p -> p.equalsIgnoreCase(name))),
                builder.createOffset(builder.getStart() + builder.getRemaining().lastIndexOf(' ') + 1)
        );
    }
}
