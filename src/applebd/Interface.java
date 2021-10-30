package applebd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Vector;

public class Interface {
    private Connection conn;

    public class Date {
        // YYYY-MM-DD
        public int month;
        public int day;
        public int year;
        public String date_str;

        Date(int month, int day, int year){
            this.month = month; this.day = day; this.year = year;
        }

        Date(String date) {
            this.date_str = date;
        }

        @Override
        public String toString() {
            if (date_str != null) {
                return this.date_str;
            } else {
                return (year + "-" + month + "-" + day);
            }
        }
    }

    public class User {
        public final String username;
        public final String firstName;
        public final String lastName;
        public final String email;
        public final Date   creationDate;
        public final Date   lastAccessDate;

        public User(String username, String firstName, String lastName, String email, Date creationDate, Date lastAccessDate) {
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.creationDate = creationDate;
            this.lastAccessDate = lastAccessDate;
        }
    }

    public class Tool {
        public final String         name;
        public final String         description;
        public final String         barcode;
        public final Vector<String> categories;
        public final Date           purDate;
        public final float          purPrice;
        public final Boolean        shareable;

        public Tool(String name, String description, Vector<String> categories,
                    Date purDate, float purPrice, Boolean shareable) {
            this(name, description, null, categories, purDate, purPrice, shareable);
        }

        public Tool(String name, String description, String barcode, Vector<String> categories,
                    Date purDate, float purPrice, Boolean shareable) {
            this.name = name;
            this.description = description;
            this.barcode = barcode;
            this.categories = categories;
            this.purDate = purDate;
            this.purPrice = purPrice;
            this.shareable = shareable;
        }

        @Override
        public String toString() {
            return  name + '\t' +
                    "[" + barcode + "]\t" +
                    categories + "\t" +
                    shareable;
        }
    }

    public enum ToolParts {
        BARCODE,    // used in search
        NAME,       // used in search + sort
        CATEGORY,   // used in search + sort
    }

    public Interface(Connection conn) {
        // connect to database
        this.conn = conn;
    }

    /**
     * Checks if a username exists in the database
     * @param username
     * @return - True on existance
     */
    public Boolean verifyUsername(String username) {
        return true;
    }

    /**
     * Returns a user if their username and password are correct, else null
     * @param username
     * @param password
     * @return USer signed in or null
     */
    /*
    public User login(String username, String password) {
        if(username.equals("test") && password.equals("test")){
            return new User("test", "Johnny", "Test", "Johnny@test.best",
                    new Date(12, 2, 2000), new Date(1, 1, 1999)
            );
        }
        return null;
    }
     */

    /**
     * Gets a tool from the database
     * @param barcode - tool to get
     * @return Pair. Tool = the tool, User = tool owner
     */
    /*
    public Pair<Tool, User> getTool(String barcode) {
        if(barcode.equals("112358")){
            Vector<String> categories = new Vector<>();
            Collections.addAll(categories, "Test", "Fibonacci");
            return new Tool (
                    "test tool special", "this is the fibonacci test tool",
                    barcode, categories,
                    new Date(12, 2, 2000), 99.99f,
                    true
            );
        }
        return null;
    }
     */

    private void executeStatement(String string) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(string);
        statement.executeUpdate();
    }

    /**
     * Creates a new tool
     * @param user - user who owns the tool
     * @param newTool - tool to create (barcode ignored)
     * @return - True on success
     * @throws SQLException
     */
    public Boolean createTool(User user, Tool newTool) throws SQLException {
        executeStatement(
                "INSERT INTO tool_info (tool_name, description, purchase_date, purchase_price, username) " +
                        String.format("VALUES ('%s','%s','%s','%s','%s','%s');", newTool.name, newTool.description, newTool.purDate, newTool.purPrice, user.username));
        return true;
    }

    /**
     * Edits a tool. Replaces the tools information with the "newTool"'s information
     * @param barcode - tool to edit
     * @param newTool - new data (barcode ignored)
     * @return - True on success
     * @throws SQLException
     */
    public Boolean editTool(String barcode, Tool newTool) throws SQLException {
        executeStatement(
                String.format("UPDATE tool_info " +
                "SET tool_name=%s, description=%s, purchase_date=%s, purchase_price=%s" +
                "WHERE barcode='%s';", newTool.name, newTool.description, newTool.purDate, newTool.purPrice, barcode));
        return true;
    }

    /**
     * Deletes a tool
     * @param barcode - tool to delete
     * @return - True on success
     * @throws SQLException
     */
    public Boolean deleteTool(String barcode) throws SQLException {
        executeStatement(String.format("DELETE FROM tool_info WHERE barcode='%s';", barcode));
        return true;
    }

    /**
     * Adds a category to a tool for better sorting
     * @param barcode - tool to edit
     * @param category - category to add
     * @return - True on success
     * @throws SQLException
     */
    public Boolean addToolToCategory(String barcode, String category) throws SQLException {
        executeStatement(String.format("INSERT INTO tool_category (category_name, barcode) " +
                "VALUES ('%s','%s');", category, barcode ));
        return true;
    }

    /**
     * Removes a tool from a category
     * @param barcode - barcode of tool to modify
     * @param category - category to remove
     * @return - True on success
     * @throws SQLException
     */
    public Boolean removeToolFromCategory(String barcode, String category) throws SQLException {
        executeStatement(String.format("DELETE FROM tool_category WHERE barcode='%s' AND category_name='%s'", barcode, category));
        return true;
    }

    /**
     * Creates a cateory and adds it to the databse
     * @param category - Category to add
     * @return - True on success
     */
    public Boolean createCategory(String category) throws SQLException {
        executeStatement(String.format(
                "INSERT INTO category (category_name) VALUES ('%s');", category));
        return true;
    }

    /**
     * This function checks if a category exists
     * @param category - Name os the category to check
     * @return - Trus if it exists in the database, false oterwise
     */
    public Boolean validateCategory(String category) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(String.format("SELECT COUNT(1) FROM category WHERE category_name = '%s';", category));
        ResultSet result = statement.executeQuery();
        result.next();
        if (result.getInt(1) == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the current date
     * @return Date storing current date
     */
    public Date getCurrentDate() {
        java.util.Date date = new java.util.Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM");
        String month = sdf.format(date);

        sdf = new SimpleDateFormat("dd");
        String day = sdf.format(date);

        sdf = new SimpleDateFormat("yyyy");
        String year = sdf.format(date);

        Date today = new Date(year + "-" + month + "-" + "day");
        return today;
    }

    /**
     * Adds new user to database
     * assumes username does not already exist
     * @return true on success
     */
    public boolean createAccount(String username, String password, String first_name, String last_name, String email) throws SQLException {
        Date today = getCurrentDate();
        executeStatement(
                String.format("INSERT INTO \"user\" " +
                        "(username, password, first_name, last_name, email, creation_date, last_access_date) " +
                        "VALUES ('%s','%s','%s','%s','%s','%s','%s');",
                        username, password, first_name, last_name, email, today, today)
        );
        return true;
    }

    /**
     * Checks if username exists in database
     * @param username username to be checked
     * @return true if username exists, false otherwise
     */
    public boolean checkUsername(String username) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(String.format("SELECT COUNT(1) FROM \"user\" WHERE username = '%s';", username));
        ResultSet result = statement.executeQuery();
        result.next();
        if (result.getInt(1) == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Verifies username and password and updates access date
     * @param username username entered by user
     * @param password password entered by user
     * @return true if login succeeds, false otherwise
     */
    public boolean login(String username, String password) throws SQLException {
        // check if username exists
        if (!checkUsername(username)) {
            return false;
        }

        // find correct password based on username
        PreparedStatement statement = conn.prepareStatement(String.format("SELECT password FROM \"user\" WHERE username = '%s';", "generic_name"));
        ResultSet result = statement.executeQuery();
        result.next();
        String correct_password = result.getString("password");

        // check if correct password matches entered password
        if (!password.equals(correct_password)) {
            return false;
        }

        // update access date
        Date today = getCurrentDate();
        executeStatement(
                String.format("UPDATE \"user\" " +
                        "SET last_access_date='%s' " +
                        "WHERE username='%s';", today, username));
        return true;
    }

    public boolean searchTools() {
        return false;
    }

    public boolean sortTools() {
        return false;
    }

    public boolean isToolAvailable() {
        return false;
    }

    public boolean createBorrowRequest() {
        return false;
    }

    public boolean getPendingUserRequests() {
        return false;
    }

    public boolean acceptRequest() {
        return false;
    }

    public boolean denyRequest() {
        return false;
    }

    public boolean getAvailableTools() {
        return false;
    }

    public boolean getUserBorrowedTools() {
        return false;
    }

    public boolean getUserLentTools() {
        return false;
    }

    public boolean returnTool() {
        return false;
    }

    public Vector<Tool> getUserTools(User user) {
        Vector<Tool> tools = new Vector<>();
        Vector<String> testCategories1 = new Vector<>();
        Collections.addAll(testCategories1, "Test", "Hand");
        Vector<String> testCategories2 = new Vector<>();
        Collections.addAll(testCategories2, "Test", "Power");

        tools.add(new Tool("48-in Steel Digging Shovel", "This is a basic shovel",
                "1123581029", testCategories1,
                new Date(2, 28, 2015), 15.43f, true));
        tools.add(new Tool("20-in Wood Transfer Shovel", "A fair shovel",
                "1598468740", testCategories2,
                new Date(5, 3, 2017), 25.99f, true));

        return tools;
    }

    public Vector<String> getCategories() {
        Vector<String> categories = new Vector<>();
        categories.add("Shovels");
        categories.add("Hammers");

        return categories;
    }

    public Vector<Tool> searchTools(ToolParts searchParam, String searchArgument) {
        Vector<Tool> tools = new Vector<>();
        tools.add(new Tool("SearchTool1", "", null,
                new Date(2, 28, 2015), 15.43f, true));
        tools.add(new Tool("SearchTool2", "", null,
                new Date(2, 28, 2015), 15.43f, true));
        tools.add(new Tool("SearchTool3", "", null,
                new Date(2, 28, 2015), 15.43f, true));

        return tools;
    }

    public Vector<Tool> sortTools (ToolParts searchParam, Boolean ascending) {
        Vector<Tool> tools = new Vector<>();
        tools.add(new Tool("SortTool1", "", null,
                new Date(2, 28, 2015), 15.43f, true));
        tools.add(new Tool("SortTool2", "", null,
                new Date(2, 28, 2015), 15.43f, true));
        tools.add(new Tool("SortTool3", "", null,
                new Date(2, 28, 2015), 15.43f, true));

        return tools;
    }
}
