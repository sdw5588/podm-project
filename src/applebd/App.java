package applebd;

import applebd.textmenu.TextMenu;
import applebd.textmenu.TextMenuItem;
import javafx.print.PrintSides;

import java.io.*;
import java.util.Vector;

public class App {
    private InputStreamReader streamReader;
    private BufferedReader bufferedReader;
    private Interface anInterface;
    private Interface.User user;
    private Vector<Interface.Tool> internalTools;

    // Action Options
    private TextMenuItem Logout = new TextMenuItem("Logout", null);

    private TextMenuItem CreateTool = new TextMenuItem("Create Tool", () -> {
        System.out.println("!!!!!Create Tool!!!!!");
    });
    private TextMenuItem EditTool = new TextMenuItem("Edit Tool", () -> {
        System.out.println("!!!!!Edit Tool!!!!!");
    });
    private TextMenuItem DeleteTool = new TextMenuItem("Delete Tool", () -> {
        System.out.println("!!!!!Delete Tool!!!!!");
    });
    private TextMenuItem CreateCategory = new TextMenuItem("Create Category", () -> {
        System.out.println("!!!!!Create Category!!!!!");
    });
    private TextMenuItem CreateRequest = new TextMenuItem("Create Request", () -> {
        System.out.println("!!!!!Create Request!!!!!");
    });
    private TextMenuItem SearchTools = new TextMenuItem("Search Tools", () -> {
        System.out.println("!!!!!Search Tools!!!!!");
        internalTools = anInterface.searchTools(Interface.ToolParts.NAME, "SearchTool");
        this.AllToolsSearch.run();
    });
    private TextMenuItem SortTools = new TextMenuItem("Sort Tools", () -> {
        System.out.println("!!!!!Sort Tools!!!!!");
        internalTools = anInterface.sortTools(Interface.ToolParts.NAME, true);
        this.AllToolsSort.run();
    });
    private TextMenuItem ReturnTool = new TextMenuItem("Return Tool", () -> {
        System.out.println("!!!!!Return Tool!!!!!");
    });


    // Menu Items
    private Runnable printSearchHead = () -> {
        System.out.println("\n--~=={ Search Results }==~--");
        for (Interface.Tool tool : internalTools) {
            System.out.println(tool.name + "\t");
        }
        System.out.println("===============");
    };
    private TextMenu AllToolsSearch = new TextMenu(
            "internal-search-tools", printSearchHead,
            CreateRequest
    );


    private Runnable printSortHead = () -> {
        System.out.println("\n--~=={ Sorted Results }==~--");
        for (Interface.Tool tool : internalTools) {
            System.out.println(tool.name + "\t");
        }
        System.out.println("===============");
    };
    private TextMenu AllToolsSort = new TextMenu(
            "internal-sort-tools", printSortHead,
            CreateRequest
    );
    private TextMenu AllTools = new TextMenu(
            "All Tools", "\n--~=={ All Tools }==~--",
            CreateRequest
    );
    private TextMenu Requests = new TextMenu(
            "Requests", "\n--~=={ My Requests }==~--",
            SearchTools, SortTools, AllTools, ReturnTool
    );


    private Runnable printCategoryHead = () -> {
        System.out.println("\n--~=={ My Categories }==~--");
        Vector<String> userCategories = anInterface.getUserCategories(user);
        for (String category : userCategories) {
            System.out.println(category);
        }
        System.out.println("===============");
    };
    private TextMenu Categories = new TextMenu(
            "My Categories", this.printCategoryHead,
            CreateCategory
    );


    private Runnable printToolHead = () -> {
        System.out.println("\n--~=={ My Tools }==~--");
        Vector<Interface.Tool> userTools = anInterface.getUserTools(user);
        for (Interface.Tool tool : userTools) {
            System.out.println(tool.name + "\t");
        }
        System.out.println("===============");
    };
    private TextMenu Tools = new TextMenu(
            "Tools", this.printToolHead,
            CreateTool, EditTool, DeleteTool, Categories
    );


    private Runnable printHomeHead = () -> {
        System.out.println("\n--~=={ Hello " + user.firstName + "!, Welcome to Terry's Tool Trade Tower }==~--");
    };
    private TextMenu Home = new TextMenu(
            "Home", this.printHomeHead,
            false,
            Tools, Requests, Logout
    );


    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    public void run() {
        System.out.println("\nLOG IN");

        streamReader = new InputStreamReader(System.in);
        bufferedReader = new BufferedReader(streamReader);
        anInterface = new Interface();

        System.out.print( "--~=={ Hello, Welcome to Terry's Tool Trade Tower }==~--\n" +
                            "Please enter your username to login\n" +
                            "Type '-createAccount' to create an account\n" +
                            "> ");
        try {
            String input = bufferedReader.readLine();
            if(input.toLowerCase().equals("-createaccount")){
                System.out.println("Create Account");
            }
            else {
                if(anInterface.verifyUsername(input)) { // make sure username is valid
                    while(this.user == null){ // keep asking for passwords
                        System.out.print("Please enter your password: ");
                        String password = bufferedReader.readLine();
                        user = anInterface.login(input, password);
                        if(user == null){
                            System.out.println("ERROR: Invalid Password!");
                        }
                    }
                }
                else { // username invalid
                    System.out.println("ERROR: Invalid Username");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Home.run();

        System.out.println("\nLOG OUT");
    }
}
