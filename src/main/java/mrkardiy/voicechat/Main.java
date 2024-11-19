package mrkardiy.voicechat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Main extends JavaPlugin {
    private int socketPort;
    private Set<String> allowedIPs;
    private double defaultRadius;
    private ServerSocket serverSocket;
    private boolean isRunning = true;

    private String serverName;
    private Thread socketThread;

    // Структуры данных для управления состоянием игроков
    private final Set<String> voiceChatOff = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> playerMutes = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> playerListens = new ConcurrentHashMap<>();
    private final Map<String, Double> playerRadius = new ConcurrentHashMap<>();
    private final Map<String, String> playerCodes = new ConcurrentHashMap<>();

    // Префикс для сообщений
    private static final String PREFIX = ChatColor.BLUE + "[VoiceChat] " + ChatColor.WHITE;
    private Map<String, Boolean> defaultCommandAccess = new HashMap<>();
    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        startSocketServer();

        this.getCommand("voicechat").setTabCompleter(new VoiceChatTabCompleter());

        getLogger().info("Активация voice-chat.");
    }

    @Override
    public void onDisable() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (socketThread != null && socketThread.isAlive()) {
                socketThread.interrupt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        getLogger().info("Выключаем voice-chat.");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        socketPort = config.getInt("socket.port", 12345);
        allowedIPs = new HashSet<>(config.getStringList("allowed_ips"));
        defaultRadius = config.getDouble("radius", 50.0);
        serverName = config.getString("name");

        // Load command default access from the configuration
        ConfigurationSection commandsSection = config.getConfigurationSection("commands");
        if (commandsSection != null) {
            for (String command : commandsSection.getKeys(false)) {
                boolean defaultAccess = commandsSection.getBoolean(command + ".default_access", false);
                defaultCommandAccess.put(command.toLowerCase(), defaultAccess);
            }
        }
    }

    private void startSocketServer() {
        socketThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(socketPort);
                getLogger().info("Запускаем сокет сервер на порту " + socketPort);

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    //getLogger().info("Подключение от " + clientSocket.getInetAddress().getHostAddress());
                    // Обработка каждого соединения в отдельном потоке
                    new Thread(new ClientHandler(clientSocket)).start();
                }

            } catch (IOException e) {
                if (isRunning) {
                    getLogger().severe("Ошибка: " + e.getMessage());
                }
            }
        });

        socketThread.start();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("voicechat")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                if (args.length == 0) {
                    sendMessage(player, "Использование: /voicechat <команда>");
                    return true;
                }

                String subCommand = args[0].toLowerCase();

                // Check for default access first
                boolean hasDefaultAccess = defaultCommandAccess.getOrDefault(subCommand, false);

                // Define permission strings
                String permission = "voicechat.command." + subCommand;

                // Check permissions only if default access is not granted
                if (!hasDefaultAccess && !player.hasPermission(permission)) {
                    sendMessage(player, "У вас нет разрешения на использование этой команды.");
                    return true;
                }

                switch (subCommand) {
                    case "reload":
                        reloadConfig();
                        sendMessage(player, "Конфигурация перезагружена.");
                        return true;
                    case "link":
                        handleLinkCommand(player);
                        return true;
                    case "code":
                        handleCodeCommand(player);
                        return true;
                    case "off":
                        handleOffCommand(player);
                        return true;
                    case "on":
                        handleOnCommand(player);
                        return true;
                    case "mute":
                        if (args.length < 2) {
                            sendMessage(player, "Использование: /voicechat mute <игрок>");
                            return true;
                        }
                        handleMuteCommand(player, args[1]);
                        return true;
                    case "unmute":
                        if (args.length < 2) {
                            sendMessage(player, "Использование: /voicechat unmute <игрок>");
                            return true;
                        }
                        handleUnmuteCommand(player, args[1]);
                        return true;
                    case "listen":
                        if (args.length < 2) {
                            sendMessage(player, "Использование: /voicechat listen <игрок>");
                            return true;
                        }
                        handleListenCommand(player, args[1]);
                        return true;
                    case "myradius":
                        if (args.length < 2) {
                            sendMessage(player, "Использование: /voicechat myradius <радиус>");
                            return true;
                        }
                        handleMyRadiusCommand(player, args[1]);
                        return true;
                    default:
                        sendMessage(player, "Неизвестная подкоманда. Доступные команды: reload, link, code, off, on, mute, unmute, listen, myradius.");
                        return true;
                }
            } else {
                sender.sendMessage(ChatColor.BLUE + "[VoiceChat] " + ChatColor.WHITE + "Эта команда доступна только игрокам.");
                return true;
            }
        }
        return false;
    }

    private void handleLinkCommand(Player player) {
        String code = generateCode();
        // Устанавливаем код с истечением через 10 минут (12000 тиков)
        playerCodes.put(player.getName(), code);
        sendMessage(player, " Для запуска чата перейдите по ссылке: " + ChatColor.GOLD + "https://voice-chat.ru/servers/"+serverName+"?code="+code+"&nickname="+player.getName());
    }


    private void handleCodeCommand(Player player) {
        String code = generateCode();
        // Устанавливаем код с истечением через 10 минут (12000 тиков)
        playerCodes.put(player.getName(), code);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            playerCodes.remove(player.getName());
            sendMessage(player, "Ваш временный код истёк.");
        }, 12000L);
        sendMessage(player, "Ваш временный код: " + ChatColor.GOLD + code);
    }

    private void handleOffCommand(Player player) {
        if (voiceChatOff.contains(player.getName())) {
            sendMessage(player, "Voice chat уже отключён.");
            return;
        }
        voiceChatOff.add(player.getName());
        sendMessage(player, "Voice chat отключён. Вы больше не будете отображаться в списках игроков.");
    }

    private void handleOnCommand(Player player) {
        if (!voiceChatOff.contains(player.getName())) {
            sendMessage(player, "Voice chat уже включён.");
            return;
        }
        voiceChatOff.remove(player.getName());
        sendMessage(player, "Voice chat включён. Теперь вы снова будете отображаться в списках игроков.");
    }

    private void handleMuteCommand(Player player, String targetPlayerName) {
        if (targetPlayerName.equalsIgnoreCase(player.getName())) {
            sendMessage(player, "Вы не можете заглушить себя.");
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);
        if (targetPlayer == null) {
            sendMessage(player, "Игрок '" + ChatColor.YELLOW + targetPlayerName + ChatColor.WHITE + "' не найден.");
            return;
        }

        playerMutes.computeIfAbsent(player.getName(), k -> ConcurrentHashMap.newKeySet()).add(targetPlayerName);
        sendMessage(player, "Вы заглушили '" + ChatColor.YELLOW + targetPlayerName + ChatColor.WHITE + "'. Они больше не будут отображаться в ваших списках игроков.");
    }

    private void handleUnmuteCommand(Player player, String targetPlayerName) {
        Set<String> mutes = playerMutes.get(player.getName());
        if (mutes != null && mutes.remove(targetPlayerName)) {
            sendMessage(player, "Вы удалили заглушку с '" + ChatColor.YELLOW + targetPlayerName + ChatColor.WHITE + "'. Они снова будут отображаться в ваших списках игроков.");
            if (mutes.isEmpty()) {
                playerMutes.remove(player.getName());
            }
        } else {
            sendMessage(player, "Игрок '" + ChatColor.YELLOW + targetPlayerName + ChatColor.WHITE + "' не был заглушен.");
        }
    }

    private void handleListenCommand(Player player, String targetPlayerName) {
        if (targetPlayerName.equalsIgnoreCase(player.getName())) {
            sendMessage(player, "Вы не можете слушать себя.");
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);
        if (targetPlayer == null) {
            sendMessage(player, "Игрок '" + ChatColor.YELLOW + targetPlayerName + ChatColor.WHITE + "' не найден.");
            return;
        }

        playerListens.computeIfAbsent(player.getName(), k -> ConcurrentHashMap.newKeySet()).add(targetPlayerName);
        sendMessage(player, "Вы теперь слушаете '" + ChatColor.YELLOW + targetPlayerName + ChatColor.WHITE + "'. Они всегда будут отображаться в ваших списках игроков.");

        // Обеспечиваем взаимную видимость
        playerListens.computeIfAbsent(targetPlayerName, k -> ConcurrentHashMap.newKeySet()).add(player.getName());
        sendMessage(targetPlayer, "'" + ChatColor.YELLOW + player.getName() + ChatColor.WHITE + "' теперь слушает вас. Они всегда будут отображаться в ваших списках игроков.");
    }

    private void handleMyRadiusCommand(Player player, String radiusStr) {
        try {
            double radiusValue = Double.parseDouble(radiusStr);
            if (radiusValue <= 0) {
                sendMessage(player, "Радиус должен быть положительным числом.");
                return;
            }
            playerRadius.put(player.getName(), radiusValue);
            sendMessage(player, "Ваш радиус слышимости установлен на " + ChatColor.GOLD + radiusValue + ChatColor.WHITE + " блоков.");
        } catch (NumberFormatException e) {
            sendMessage(player, "Некорректное значение радиуса.");
        }
    }

    private String generateCode() {
        Random rand = new Random();
        int code = 100000 + rand.nextInt(900000);
        return String.valueOf(code);
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(PREFIX + message);
    }

    private class ClientHandler implements Runnable {

        private final Socket clientSocket;

        ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            if (!allowedIPs.contains(clientIP)) {
                getLogger().warning("Неавторизованная попытка подключения " + clientIP);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
            ) {
                String request = in.readLine();
                if (request == null) {
                    clientSocket.close();
                    return;
                }

                String response = processRequest(request);
                out.write(response);
                out.newLine();
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (!clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String processRequest(String request) {
            // Простой протокол: КОМАНДА:ПАРАМЕТРЫ
            // например: GET_PLAYERS:ИмяИгрока
            //         GET_CODE:ИмяИгрока
            //         LIST

            String[] parts = request.split(":");
            if (parts.length < 1) {
                return "ERROR: Некорректный формат запроса.";
            }

            String command = parts[0].trim().toUpperCase();

            switch (command) {
                case "GET_PLAYERS":
                    if (parts.length < 2) {
                        return "ERROR: GET_PLAYERS требует имя игрока.";
                    }
                    return getPlayersWithinRadius(parts[1].trim());
                case "GET_CODE":
                    if (parts.length < 2) {
                        return "ERROR: GET_CODE требует имя игрока.";
                    }
                    return getPlayerCode(parts[1].trim());
                case "LIST":
                    return getList();
                default:
                    return "ERROR: Неизвестная команда.";
            }
        }

        private String getPlayersWithinRadius(String requestingPlayerName) {
            Player target = Bukkit.getPlayerExact(requestingPlayerName);
            if (target == null) {
                return "ERROR: Игрок не найден.";
            }

            // Если у игрока отключён voice chat, они не должны транслировать своё положение
            if (voiceChatOff.contains(target.getName())) {
                return "ERROR: У игрока отключён voice chat.";
            }

            Location targetLoc = target.getLocation();
            double radius = playerRadius.getOrDefault(target.getName(), defaultRadius);

            List<String> nearbyPlayers = new ArrayList<>();

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                String onlinePlayerName = onlinePlayer.getName();

                // Пропускаем, если у игрока отключён voice chat
                if (voiceChatOff.contains(onlinePlayerName)) continue;

                // Пропускаем, если целевой игрок заглушил этого игрока
                Set<String> targetMutes = playerMutes.getOrDefault(target.getName(), Collections.emptySet());
                if (targetMutes.contains(onlinePlayerName)) continue;

                // Всегда включаем игроков, которые слушают целевого игрока
                if (playerListens.getOrDefault(onlinePlayerName, Collections.emptySet()).contains(target.getName())) {
                    nearbyPlayers.add(onlinePlayerName);
                    continue;
                }

                // Проверяем расстояние
                double targetRadius = playerRadius.getOrDefault(target.getName(), defaultRadius);
                if (onlinePlayer.getLocation().distance(targetLoc) <= targetRadius) {
                    nearbyPlayers.add(onlinePlayerName);
                }
            }

            // Включаем игроков, которых целевой игрок слушает, вне зависимости от расстояния
            Set<String> targetListens = playerListens.getOrDefault(target.getName(), Collections.emptySet());
            for (String listenedPlayerName : targetListens) {
                Player listenedPlayer = Bukkit.getPlayerExact(listenedPlayerName);
                if (listenedPlayer != null && !nearbyPlayers.contains(listenedPlayerName) && !voiceChatOff.contains(listenedPlayerName)) {
                    nearbyPlayers.add(listenedPlayerName);
                }
            }

            return String.join(",", nearbyPlayers);
        }

        private String getPlayerCode(String playerName) {
            String code = playerCodes.get(playerName);
            if (code == null) {
                return "ERROR: Код для игрока не найден.";
            }
            return code;
        }

        private String getList() {
            List<String> listPlayers = new ArrayList<>();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (voiceChatOff.contains(player.getName())) continue;
                listPlayers.add(player.getName());
            }

            return String.join(",", listPlayers);
        }
    }
}