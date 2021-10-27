package applebd;

import applebd.textmenu.TextMenu;
import applebd.textmenu.TextMenuItem;
import javafx.print.PrintSides;

import java.io.*;
import java.util.Collections;
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
        try {
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

            Interface.Tool newTool = anInterface.new Tool(
                    name, description, categories,
                    purDate, purPrice,
                    share
            );
            anInterface.createTool(user, newTool);

        } catch (IOException e) {
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
                                    anInterface.removeCategoryFromTool(barcode, category);
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

        } catch (IOException e) {
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
