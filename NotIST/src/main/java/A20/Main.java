package A20;

import A20.model.Note;
import A20.database.NoteDAO;
import A20.database.UserDAO;
import A20.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        UserDAO userDAO = new UserDAO();
        NoteDAO noteDAO = new NoteDAO();

        while (true) {
            System.out.println("\n--- Menu ---");
            System.out.println("1. Add a new user");
            System.out.println("2. Get user by username");
            System.out.println("3. List all users");
            System.out.println("4. Add a new note");
            System.out.println("5. List notes by user");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            try {
                switch (choice) {
                    case 1 -> {
                        System.out.print("Enter username: ");
                        String username = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String password = scanner.nextLine();
                        // # GENERATED !
                        System.out.print("Enter public key: ");
                        String publicKey = scanner.nextLine();

                        User newUser = new User(0, username, publicKey, password);
                        userDAO.addUser(newUser);
                        System.out.println("User added successfully!");
                    }
                    case 2 -> {
                        System.out.print("Enter username: ");
                        String username = scanner.nextLine();
                        User user = userDAO.getUserByUsername(username);
                        if (user != null) {
                            System.out.println(user);
                        } else {
                            System.out.println("User not found.");
                        }
                    }
                    case 3 -> {
                        List<User> users = userDAO.getAllUsers();
                        if (users.isEmpty()) {
                            System.out.println("No users found.");
                        } else {
                            users.forEach(System.out::println);
                        }
                    }
                    case 4 -> {
                        System.out.print("Enter owner ID: ");
                        int ownerId = scanner.nextInt();
                        scanner.nextLine(); // Consume newline
                        System.out.print("Enter note title: ");
                        String title = scanner.nextLine();
                        System.out.print("Enter note content: ");
                        String content = scanner.nextLine();

                        Note newNote = new Note(0, ownerId, title, content);
                        noteDAO.addNote(newNote);
                        System.out.println("Note added successfully!");
                    }
                    case 5 -> {
                        System.out.print("Enter user ID: ");
                        int userId = scanner.nextInt();
                        List<Note> notes = noteDAO.getNotesByUserId(userId);
                        if (notes.isEmpty()) {
                            System.out.println("No notes found for this user.");
                        } else {
                            notes.forEach(System.out::println);
                        }
                    }
                    case 0 -> {
                        System.out.println("Exiting...");
                        scanner.close();
                        System.exit(0);
                    }
                    default -> System.out.println("Invalid choice. Try again.");
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}
