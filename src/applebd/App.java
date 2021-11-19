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

    private String prompt(String prompt){
        return (String)this.prompt(prompt, PROMPT_TYPE.STRING);
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
                        String input = bufferedReader.readLine();
                        if(input.equals("")) return null;
                        String[] preParseDate = input.split("/");
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
                        String input = bufferedReader.readLine();
                        if(input.equals("")) return null;
                        float num = Float.parseFloat(input);
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
        // gather all information
        String name = prompt("Name of the new tool: ");
        String description = prompt("Description: ");
        String[] categoriesarr = (String[])prompt("Categories (separated by ','s): ", PROMPT_TYPE.LIST);
        Vector<String> categories = new Vector<>();
        Collections.addAll(categories, categoriesarr);
        Interface.Date purDate = (Interface.Date)prompt("Purchase Date (MM/DD/YYYY): ", PROMPT_TYPE.DATE);
        Float purPrice = (Float)prompt("Purchase Price: $", PROMPT_TYPE.FLOAT);
        String response = prompt("Would you like to share this tool? (y/n):");
        boolean share = response.startsWith("y");

        Interface.Tool newTool = anInterface.new Tool(
                name, description, categories,
                purDate, purPrice,
                share
        );
        if(!anInterface.createTool(user, newTool))
            System.out.println("There seems to have been an error with your request, please try again.");
    });

    private TextMenuItem EditTool = new TextMenuItem("Edit Tool", () -> {
        String barcode = prompt("Barcode of the tool you would like to edit: ");
        Interface.Tool oldTool = anInterface.getUserTool(user, barcode);
        if(oldTool != null){
            System.out.println("Original Tool:\n" + oldTool.toDetailedString());

            String name = prompt("Please enter the new name of the tool, leave blank to leave unchanged: ");
            String description = prompt("New description, blank if unchanged: ");
            Interface.Date purDate = (Interface.Date)prompt("Purchase date, blank if unchanged: ", PROMPT_TYPE.DATE);
            Float purPrice = (Float)prompt("Purchase price, blank if unchanged: ", PROMPT_TYPE.FLOAT);
            String response = prompt("Would you like to share this tool? (y/n):");
            boolean share = response.startsWith("y");

            String choice = prompt("Would you like to add or remove any categories (add/remove): ").trim();
            if(choice.startsWith("add")){
                // add categories
                System.out.println("To see all categories, please type '-all'");
                boolean gotCategories = false;
                while(!gotCategories) {
                    String input = prompt("Categories to add (separated by a ','): ");
                    if(input.trim().equals("-all")){
                        Vector<String> allCategories = anInterface.getCategories();
                        for(String cat : allCategories){
                            System.out.println("\t" + cat);
                        }
                    }
                    else{
                        String[] categoriesarr = input.trim().split(",");
                        for (String cat : categoriesarr) {
                            if(!anInterface.addToolToCategory(barcode, cat.trim())){
                                System.out.println("Unable to add '" + cat + "'");
                            }
                        }
                        gotCategories = true;
                    }
                }
            }
            else if(choice.startsWith("rem")){
                // remove categories
                boolean gotCategories = false;
                while(!gotCategories){
                    String[] input = (String[])prompt("Categories to remove (separated by a ','): ", PROMPT_TYPE.LIST);
                    for (String cat : input) {
                        cat = cat.trim();
                        if(!anInterface.removeToolFromCategory(barcode, cat)){
                            System.out.println("Unable to remove '" + cat + "'");
                        }
                    }
                    gotCategories = true;
                }
            }
            else{
                System.out.println("Unknown option, leaving categories alone.");
            }

            Interface.Tool newTool = anInterface.new Tool(
                    name.equals("") ? oldTool.name : name,
                    description.equals("") ? oldTool.description : description,
                    oldTool.barcode, oldTool.categories,
                    purDate == null ? oldTool.purDate : purDate,
                    purPrice == null ? oldTool.purPrice : purPrice,
                    share
            );
            if(!anInterface.editTool(barcode, newTool))
                System.out.println("There was an issue with your request.");
        }
        else{
            System.out.println("Sorry, we couldn't seem to find that tool.");
        }
    });

    private TextMenuItem DeleteTool = new TextMenuItem("Delete Tool", () -> {
        String barcode = prompt("Barcode of the tool you would ike to delete: ");
        anInterface.deleteTool(barcode, user);
    });

    private TextMenuItem CreateCategory = new TextMenuItem("Create Category", () -> {
        String newCategory = prompt("Name of the new category: ");
        if(!anInterface.createCategory(newCategory)){
            System.out.println("There was an issue with your request.");
        }
    });

    private TextMenuItem CreateRequest = new TextMenuItem("Create Request", () -> {
        String barcode = prompt("What tool would you like to borrow (barcode)? ");
        Interface.Date date = (Interface.Date)prompt("When do you need the tool? (MM/DD/YYYY) ", PROMPT_TYPE.DATE);
        int duration = -1;
        while(duration < 0) {
            String strDuration = prompt("How many days do you need the tool for? ");
            duration = Integer.parseInt(strDuration);
        }

        if(!anInterface.createBorrowRequest(user, barcode, date, duration)){
            System.out.println("There was an error with your request.");
        }

        // prints statistics
        internalTools = anInterface.topBorrowed(user);
        System.out.println("\n--~=={ Top 10 Most Borrowed Tools }==~--");
        for (Interface.Tool tool : internalTools) {
            System.out.println(tool.toString());
        }
    });

    private TextMenuItem SearchTools = new TextMenuItem("Search Tools", () -> {
        Interface.ToolParts searchPart = null;
        do {
            String part = prompt("What would you like to search? (Barcode, Name, or Category) ");
            if (part.trim().toLowerCase().equals("barcode"))
                searchPart = Interface.ToolParts.BARCODE;
            else if (part.trim().toLowerCase().equals("name"))
                searchPart = Interface.ToolParts.NAME;
            else if (part.trim().toLowerCase().equals("category"))
                searchPart = Interface.ToolParts.CATEGORY;
            else
                System.out.println("Unknown response.");
        } while (searchPart == null);

        String argument = prompt("What would you like to search? ");

        internalTools = anInterface.searchTools(searchPart, argument);
        System.out.println("\n--~=={ Search Results }==~--");
        for (Interface.Tool tool : internalTools) {
            System.out.println(tool.toString());
        }
        this.AllTools.run();
    });

    private TextMenuItem SortTools = new TextMenuItem("Sort Tools", () -> {
        Interface.ToolParts searchPart = null;
        do {
            String part = prompt("What would you like to sort by? (name or category) ");
            if (part.trim().toLowerCase().equals("name"))
                searchPart = Interface.ToolParts.NAME;
            else if (part.trim().toLowerCase().equals("category"))
                searchPart = Interface.ToolParts.CATEGORY;
            else
                System.out.println("Unknown response.");
        } while (searchPart == null);

        String option = prompt("Ascending or Decending");
        boolean ascending = option.trim().toLowerCase().startsWith("asc");
        System.out.println("\n--~=={ Search Results }==~--");

        if(searchPart == Interface.ToolParts.NAME){
            anInterface.sortToolsName(ascending);
        }
        else{
            anInterface.sortToolsCategory(ascending);
        }
        this.AllTools.run();
    });

    private TextMenuItem AcceptRequest = new TextMenuItem("Accept Incoming Request", () -> {
        String requestId = prompt("What request would you like to accept?");
        Interface.Date returnDate = (Interface.Date)prompt("When do you need to tool back? (MM/DD/YYYY) ", PROMPT_TYPE.DATE);
        anInterface.acceptRequest(user, requestId, returnDate);
    });

    private TextMenuItem DenyRequest = new TextMenuItem("Deny Incoming Request", () -> {
        String requestId = prompt("What request would you like to deny?");
        anInterface.denyRequest(user, requestId);
    });

    private TextMenuItem ReturnTool = new TextMenuItem("Return Tool", () -> {
        String barcode = prompt("Barcode of the tool that you would like to return: ");
        anInterface.returnTool(user, barcode);
    });

    private TextMenuItem Dashboard = new TextMenuItem("Dashboard", () -> {
        System.out.println("Number of tools available from catalog: " + anInterface.countAvailableTools(user));
        System.out.println("Number of lent tools: " + anInterface.countLentTools(user));
        System.out.println("Number of borrowed tools: " + anInterface.countBorrowedTools(user));
    });

    // Menu Items
    private TextMenu AllTools = new TextMenu(
            "All Tools", "===============",
            CreateRequest
    );

    private Runnable printRequestsHead = () -> {
        System.out.println("\n--~=={ Incoming Requests }==~--");
        Vector<Interface.Request> requests = anInterface.getPendingUserRequests(user);
        for(Interface.Request request : requests){
            System.out.println(request);
        }
        System.out.println("\n--~=={ My Pending Requests }==~--");
        requests = anInterface.getOutgoingPendingUserRequests(user);
        for(Interface.Request request : requests){
            System.out.println(request);
        }
        System.out.println("===============");
    };
    private TextMenu Requests = new TextMenu(
            "Requests", printRequestsHead,
            SearchTools, SortTools, AllTools, AcceptRequest, DenyRequest, ReturnTool, Dashboard
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
            "Categories", this.printCategoryHead,
            CreateCategory
    );

    private Runnable printToolHead = () -> {
        System.out.println("\n--~=={ My Tools }==~--");
        Vector<Interface.Tool> userTools = anInterface.getUserTools(user);
        for (Interface.Tool tool : userTools) {
            System.out.println(tool.toString());
        }
        System.out.println("--~=={ Borrowed Tools }==~--");
        Vector<Interface.Tool> borrowedTools = anInterface.getUserBorrowedTools(user);
        for (Interface.Tool tool : borrowedTools) {
            System.out.println(tool.toString());
        }
        System.out.println("--~=={ Lent Tools }==~--");
        Vector<Interface.Tool> lentTools = anInterface.getUserLentTools(user);
        for (Interface.Tool tool : lentTools) {
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

        String db_username = prompt("Database Username: ");
        String db_password = prompt("Database Password: ");

        anInterface = new Interface(db_username, db_password);

        try {
            String username = (String)prompt( "--~=={ Hello, Welcome to Terry's Tool Trade Tower }==~--\n" +
                    "Please enter your username to login\n" +
                    "> ");
            if(anInterface.checkUsername(username)){
                // username found, ask for password
                while(this.user == null) {
                    String password = prompt("Please enter your password: ");
                    user = anInterface.login(username, password);
                    if(user == null){
                        System.out.println("ERROR: Invalid Password!");
                    }
                }
            }
            else {
                // username not found, ask to create an account
                String response = prompt("We didn't recognise that username, would  you like to create an account? (y/n): ");
                if(response.trim().startsWith("y")){
                    // create a new account
                    String firstName = prompt("What is your first name? ");
                    String lastName = prompt("What is your last name? ");
                    String email = prompt("What is your email address? ");
                    String password;
                    do {
                        password = prompt("What would you like your password to be? ");
                    } while(password.equals(""));
                    anInterface.createAccount(username, password, firstName, lastName, email);
                    user = anInterface.login(username, password);
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
