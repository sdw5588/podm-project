package applebd;

import applebd.textmenu.TextMenu;
import applebd.textmenu.TextMenuItem;
//import javafx.print.PrintSides;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Vector;

public class App {
    private InputStreamReader streamReader;
    private BufferedReader bufferedReader;
    private Interface anInterface;
    private Interface.User user;
    private Vector<Interface.Tool> internalTools;
    public Connection conn;

    private enum PROMPT_TYPE {
        STRING,
        DATE,
        LIST,
        FLOAT
    }

    private Object prompt(String prompt){
        return this.prompt(prompt, PROMPT_TYPE.STRING);
    }

    private Object prompt(String prompt, PROMPT_TYPE type) {
        try {
            switch (type) {
                case STRING:
                    System.out.print(prompt);
                    return bufferedReader.readLine();

                case DATE:
                    boolean gotDate = false;
                    int month, day, year;
                    month = day = year = -1;
                    while(!gotDate) {
                        System.out.print(prompt);
                        String[] preParseDate = bufferedReader.readLine().split("/");
                        if(preParseDate.length == 3){
                            if(preParseDate[0].trim().length() == 2 && preParseDate[1].trim().length() == 2 &&
                                    preParseDate[2].trim().length() == 4) {
                                try {
                                    month = Integer.parseInt(preParseDate[0]);
                                    day = Integer.parseInt(preParseDate[1]);
                                    year = Integer.parseInt(preParseDate[2]);
                                    gotDate = true;
                                    continue;
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                        System.out.println("Invalid format, please try again.");
                    }
                    return anInterface.new Date(month, day, year);

                case LIST:
                    System.out.print(prompt);
                    String[] categoriesarr = bufferedReader.readLine().split(",");
                    for(int i = 0; i < categoriesarr.length; i++){
                        categoriesarr[i] = categoriesarr[i].trim();
                    }
                    return categoriesarr;

                case FLOAT:
                    while(true) {
                        System.out.print(prompt);
                        float num = Float.parseFloat(bufferedReader.readLine());
                        if(num > 0) {
                            return num;
                        }
                        System.out.println("Invalid format, please try again.");
                    }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Action Options
    private TextMenuItem Logout = new TextMenuItem("Logout", null);

    private TextMenuItem CreateTool = new TextMenuItem("Create Tool", () -> {
        System.out.println("!!!!!Create Tool!!!!!");
        /*
        System.out.print("Name of new Tool: ");
        String name = bufferedReader.readLine();
        System.out.print("Description: ");
        String description = bufferedReader.readLine();
        System.out.print("Categories (separated by ','s): ");
        String[] categoriesarr = bufferedReader.readLine().split(",");
        for(int i = 0; i < categoriesarr.length; i++){
            categoriesarr[i] = categoriesarr[i].trim();
        }
        Vector<String> categories = new Vector<>();
        Collections.addAll(categories, categoriesarr);

        int month, day, year;
        month = day = year = -1;
        Boolean gotDate = false;
        while(!gotDate) {
            System.out.print("Purchase Date (MM/DD/YYYY): ");
            String[] preParseDate = bufferedReader.readLine().split("/");
            if(preParseDate.length == 3){
                if(preParseDate[0].trim().length() == 2 && preParseDate[1].trim().length() == 2 &&
                        preParseDate[2].trim().length() == 4) {
                    try {
                        month = Integer.parseInt(preParseDate[0]);
                        day = Integer.parseInt(preParseDate[1]);
                        year = Integer.parseInt(preParseDate[2]);
                        gotDate = true;
                        continue;
                    } catch (NumberFormatException ignored) {}
                }
            }
            System.out.println("Invalid format, please try again.");
        }
        Interface.Date purDate = anInterface.new Date(month, day, year);
        System.out.print("Purchase Price: $");
        Float purPrice = Float.parseFloat(bufferedReader.readLine());
        System.out.print("Would you like to share this tool? (y/n): ");
        Boolean share = bufferedReader.readLine().startsWith("y");
        */

        String name = (String)prompt("Name of the new tool: ");
        String description = (String)prompt("Description: ");
        String[] categoriesarr = (String[])prompt("Categories (separated by ','s): ", PROMPT_TYPE.LIST);
        Vector<String> categories = new Vector<>();
        Collections.addAll(categories, categoriesarr);
        Interface.Date purDate = (Interface.Date)prompt("Purchase Date (MM/DD/YYYY): ", PROMPT_TYPE.DATE);
        Float purPrice = (Float)prompt("Purchase Price: $", PROMPT_TYPE.FLOAT);
        String response = (String)prompt("Would you like to share this tool? (y/n):");
        boolean share = response.startsWith("y");

        Interface.Tool newTool = anInterface.new Tool(
                name, description, categories,
                purDate, purPrice,
                share
        );
        try {
            anInterface.createTool(user, newTool);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    });

    // edit tool needs more categories and more structure
    private TextMenuItem EditTool = new TextMenuItem("Edit Tool", () -> {
        System.out.println("!!!!!Edit Tool!!!!!");
        try {
            System.out.println("Barcode of the tool you would like to edit: ");
            String barcode = bufferedReader.readLine();
            Interface.Tool tool = anInterface.getUserTool(user, barcode);
            Interface.Tool newTool;
            if(tool != null){
                System.out.println("Current Tool:\n" + tool.toString());
                System.out.println("[name, description, categories, purchase date, purchase price, shareable]");
                System.out.print("What would you like to edit? ");
                String choice = bufferedReader.readLine();
                switch(choice.trim().toLowerCase()){
                    case "name":
                        System.out.print("New name: ");
                        String newName = bufferedReader.readLine().trim();
                        newTool = anInterface.new Tool(
                                newName, tool.description,
                                tool.barcode, tool.categories, tool.purDate,
                                tool.purPrice, tool.shareable
                        );
                        anInterface.editTool(barcode, newTool);
                        break;
                    case "description":
                        System.out.print("New description: ");
                        String newDescription = bufferedReader.readLine().trim();
                        newTool = anInterface.new Tool(
                                tool.name, newDescription,
                                tool.barcode, tool.categories, tool.purDate,
                                tool.purPrice, tool.shareable
                        );
                        anInterface.editTool(barcode, newTool);
                        break;
                    case "categories":
                        Boolean complete = false;
                        while(!complete) {
                            System.out.print("Add or Remove? ");
                            String option = bufferedReader.readLine();
                            if (option.trim().toLowerCase().equals("add")) {
                                System.out.print("Category to add: ");
                                String category = bufferedReader.readLine().trim();
                                if(anInterface.validateCategory(category)) {
                                    anInterface.addToolToCategory(barcode, category);
                                    complete = true;
                                }
                                else{
                                    System.out.println("Invalid Category");
                                }
                            } else if (option.trim().toLowerCase().equals("remove")) {
                                System.out.println("Current Categories:");
                                for(String category: tool.categories){
                                    System.out.println(category);
                                }
                                System.out.print("==========\nCategory to remove: ");
                                String category = bufferedReader.readLine().trim();
                                if(tool.categories.contains(category)) {
                                    anInterface.removeToolFromCategory(barcode, category);
                                    complete = true;
                                }
                                else{
                                    System.out.println("Invalid Category");
                                }
                            } else {
                                System.out.println("Unknown Option.");
                            }
                        }
                        break;
                }
            }
            else{
                System.out.println("Unknown barcode");
            }

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
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
        Vector<String> userCategories = anInterface.getCategories();
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
            System.out.println(tool.toString());
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
        streamReader = new InputStreamReader(System.in);
        bufferedReader = new BufferedReader(streamReader);

        String db_username = (String)prompt("Database Username: ");
        String db_password = (String)prompt("Database Password: ");

        anInterface = new Interface(db_username, db_password);

        try {
            String username = (String)prompt( "--~=={ Hello, Welcome to Terry's Tool Trade Tower }==~--\n" +
                    "Please enter your username to login\n" +
                    "> ");
            if(anInterface.checkUsername(username)){
                // username found, ask for password
                while(this.user == null) {
                    String password = (String) prompt("Please enter your password: ");
                    user = anInterface.login(username, password);
                    if(user == null){
                        System.out.println("ERROR: Invalid Password!");
                    }
                }
            }
            else {
                // username not found, ask to create an account
                String response = (String)prompt("We didn't recognise that username, would  you like to create an account? (y/n): ");
                if(response.trim().startsWith("y")){
                    // create a new account
                    String password = (String)prompt("What would you like your password to be? ");
                    System.out.println("Username: " + username + ", Password: " + password);
                }
                else {
                    System.out.println("Okay, have a nice day!\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(user != null) {
            Home.run();
        }

        System.out.println("\nLOG OUT");
        anInterface.Disconnect();
    }
}
