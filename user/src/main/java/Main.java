import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Main class to run the built-in HTTP server and handle user management API.
 */
public class Main {

    private static final int PORT = 8000;  // Port where the server will run
    private static final UserService userService = new UserService(); // UserService instance to handle user operations
    private static final ObjectMapper objectMapper = new ObjectMapper(); // For JSON handling

    public static void main(String[] args) throws IOException {
        // Create and start the HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/users", new UserHandler()); // Handle requests at /users
        server.setExecutor(null); // Default executor
        System.out.println("Server started on port " + PORT);
        server.start();
    }

    /**
     * UserHandler class to handle HTTP requests for user management.
     */
    static class UserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] pathParts = path.split("/");

            if ("POST".equalsIgnoreCase(method) && pathParts.length == 2) {
                handleCreateUser(exchange); // Create a new user
            } else if ("GET".equalsIgnoreCase(method) && pathParts.length == 2) {
                handleGetAllUsers(exchange); // Get all users
            } else if ("GET".equalsIgnoreCase(method) && pathParts.length == 3) {
                handleGetUserById(exchange, Integer.parseInt(pathParts[2])); // Get a specific user by ID
            } else if ("PUT".equalsIgnoreCase(method) && pathParts.length == 3) {
                handleUpdateUser(exchange, Integer.parseInt(pathParts[2])); // Update a specific user by ID
            } else if ("DELETE".equalsIgnoreCase(method) && pathParts.length == 3) {
                handleDeleteUser(exchange, Integer.parseInt(pathParts[2])); // Delete a specific user by ID
            } else {
                exchange.sendResponseHeaders(404, -1); // Respond with 404 if endpoint is not recognized
            }
        }

        // Handle POST /users - Create a new user
        private void handleCreateUser(HttpExchange exchange) throws IOException {
            InputStream requestBody = exchange.getRequestBody();
            User newUser = objectMapper.readValue(requestBody, User.class); // Parse the user from request body
            userService.createUser(newUser); // Add the user to the service
            String response = "User created successfully!";
            exchange.sendResponseHeaders(201, response.length()); // 201 Created response
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        // Handle GET /users - Get all users
        private void handleGetAllUsers(HttpExchange exchange) throws IOException {
            List<User> users = userService.getAllUsers();
            String jsonResponse = objectMapper.writeValueAsString(users); // Convert user list to JSON
            exchange.sendResponseHeaders(200, jsonResponse.length()); // 200 OK response
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes());
            }
        }

        // Handle GET /users/{id} - Get a user by ID
        private void handleGetUserById(HttpExchange exchange, int id) throws IOException {
            Optional<User> user = userService.getUserById(id);
            if (user.isPresent()) {
                String jsonResponse = objectMapper.writeValueAsString(user.get());
                exchange.sendResponseHeaders(200, jsonResponse.length()); // 200 OK response
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
            } else {
                String response = "User not found";
                exchange.sendResponseHeaders(404, response.length()); // 404 Not Found response
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }

        // Handle PUT /users/{id} - Update a user by ID
        private void handleUpdateUser(HttpExchange exchange, int id) throws IOException {
            InputStream requestBody = exchange.getRequestBody();
            User updatedUser = objectMapper.readValue(requestBody, User.class); // Parse updated user from request
            userService.updateUser(id, updatedUser); // Update the user in the service
            String response = "User updated successfully!";
            exchange.sendResponseHeaders(200, response.length()); // 200 OK response
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        // Handle DELETE /users/{id} - Delete a user by ID
        private void handleDeleteUser(HttpExchange exchange, int id) throws IOException {
            userService.deleteUser(id); // Remove the user from the service
            String response = "User deleted successfully!";
            exchange.sendResponseHeaders(200, response.length()); // 200 OK response
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    /**
     * User class representing the user entity with basic information.
     */
    static class User {
        private int id;
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;

        // Constructors
        public User() {}

        public User(int id, String username, String firstName, String lastName, String email, String phone) {
            this.id = id;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phone = phone;
        }

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    /**
     * UserService class that handles user CRUD operations and persistence to a JSON file.
     */
    static class UserService {
        private final List<User> users; // In-memory list of users
        private final ObjectMapper objectMapper = new ObjectMapper(); // For JSON handling
        private final File file = new File("users.json"); // JSON file for persistence

        public UserService() {
            this.users = loadUsersFromFile(); // Load users from file at initialization
        }

        // Load users from the JSON file
        private List<User> loadUsersFromFile() {
            if (file.exists()) {
                try {
                    return objectMapper.readValue(file, new TypeReference<>() {});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return new ArrayList<>();
        }

        // Save users to the JSON file
        private void saveUsersToFile() {
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, users);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Get all users
        public List<User> getAllUsers() {
            return users;
        }

        // Get a user by ID
        public Optional<User> getUserById(int id) {
            return users.stream().filter(u -> u.getId() == id).findFirst();
        }

        // Create a new user
        public void createUser(User user) {
            user.setId(users.size() + 1); // Simple ID assignment
            users.add(user);
            saveUsersToFile(); // Persist to file
        }

        // Update an existing user
        public void updateUser(int id, User updatedUser) {
            getUserById(id).ifPresent(existingUser -> {
                existingUser.setUsername(updatedUser.getUsername());
                existingUser.setFirstName(updatedUser.getFirstName());
                existingUser.setLastName(updatedUser.getLastName());
                existingUser.setEmail(updatedUser.getEmail());
                existingUser.setPhone(updatedUser.getPhone());
                saveUsersToFile(); // Persist changes to file
            });
        }

        // Delete a user
        public void deleteUser(int id) {
            users.removeIf(user -> user.getId() == id);
            saveUsersToFile(); // Persist changes to file
        }
    }
}
