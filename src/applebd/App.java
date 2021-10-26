package applebd;

import applebd.textmenu.TextMenu;
import applebd.textmenu.TextMenuItem;

import java.io.*;

public class App {
    // Header Functions
    private static Runnable printHomeHead = () -> {
        System.out.print("Hello, Welcome to Terry's Tool Trade Tower");
    };

    // Action Options
    private static TextMenuItem Logout = new TextMenuItem("Logout", null);

    private static TextMenuItem CreateTool = new TextMenuItem("Create Tool", new Runnable() {
        @Override
        public void run() {
            System.out.println("Create Tool");
        }
    });

    private static TextMenuItem EditTool = new TextMenuItem("Edit Tool", new Runnable() {
        @Override
        public void run() {
            System.out.println("Edit Tool");
        }
    });

    private static TextMenuItem CreateRequest = new TextMenuItem("Create Request", new Runnable() {
        @Override
        public void run() {
            System.out.println("Create Request");
        }
    });

    // Menu Items
    private static TextMenu Requests = new TextMenu(
            "Requests", "My Requests",
            true,
            CreateRequest
    );
    private static TextMenu Tools = new TextMenu(
            "Tools", "My Tools",
            true,
            CreateTool, EditTool
    );
    private static TextMenu Home = new TextMenu(
            "Home", printHomeHead,
            false,
            Tools, Requests, Logout
    );

    private InputStreamReader streamReader;
    private BufferedReader bufferedReader;

    public enum MenuState {
        Startup {
            @Override
            public String menuPrompt() {
                return  "--~=={ Hello, Welcome to Terry's Tool Trade Tower }==~--\n" +
                        "\t- Please enter your username to login\n" +
                        "\t- Type '-createAccount' to create an account\n";
            }
        },
        Home {
            @Override
            public String menuPrompt() {
                return  "--~=={ Hello %s }==~--\n" +
                        "\t1 - My Tools\n" +
                        "\t2 - Borrow Requests\n" +
                        "\th - Help\n" +
                        "\tq - Quit";
            }
        },
        Tools {
            @Override
            public String menuPrompt() {
                return  "--~=={ My Tools }==~--\n" +
                        "\n%s\n==========\n" +
                        "\t1 - Create Tool\n" +
                        "\t2 - Edit Tool\n" +
                        "\t2 - Delete Tool\n" +
                        "\t4 - Edit Categories\n" +
                        "\th - Help\n" +
                        "\tq - Quit";
            }
        };

        public abstract String menuPrompt();
    }

    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    public void run() {
        System.out.println("\nLOG IN");

        Home.run();

        System.out.println("\nLOG OUT");

        /*
        streamReader = new InputStreamReader(System.in);
        bufferedReader = new BufferedReader(streamReader);
        dummy = new Dummy();

        System.out.println( "--~=={ Hello, Welcome to Terry's Tool Trade Tower }==~--\n" +
                            "\t- Please enter your username to login\n" +
                            "\t- Type '-createAccount' to create an account\n");
        try {
            String input = bufferedReader.readLine();
            if(input.toLowerCase().equals("-createaccount")){
                System.out.println("Create Account");
            }
            else {
                System.out.println("!!!!Controller.VerifyUsername(input)!!!!");
                if(dummy.VarifyUsername(input)) { // make sure username is valid
                    System.out.println("Please enter your password:");
                    String password = bufferedReader.readLine();
                    if(dummy.Login(input, password)){

                    }
                }
                else {

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
         */
    }
}
