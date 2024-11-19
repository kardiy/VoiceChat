package mrkardiy.voicechat;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VoiceChatTabCompleter implements TabCompleter {

    // Список подкоманд
    private final List<String> subCommands = Arrays.asList(
            "link", "code", "off", "on", "mute", "unmute", "listen", "myradius"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Только для команды "voicechat"
        if (!command.getName().equalsIgnoreCase("voicechat")) {
            return null;
        }

        // Если аргументов нет или пользователь вводит первый аргумент
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String currentArg = args[0].toLowerCase();
            for (String subCmd : subCommands) {
                if (subCmd.startsWith(currentArg)) {
                    completions.add(subCmd);
                }
            }
            return completions;
        }

        // Обработка автозаполнения для конкретных подкоманд
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "mute":
                case "unmute":
                case "listen":
                    // Предлагаем имена онлайн игроков для второго аргумента
                    List<String> playerNames = new ArrayList<>();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        playerNames.add(player.getName());
                    }
                    return playerNames;
                case "myradius":
                    // Можно добавить примеры значений радиуса или оставить пустым
                    return new ArrayList<>();
                default:
                    return null;
            }
        }

        // Для других случаев возвращаем пустой список
        return new ArrayList<>();
    }
}