package applebd;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Properties;
import java.util.Vector;

public class Interface {
    private Connection conn;
    private Session session;

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

        public Tool(String name, String description, String barcode, Date purDate, float purPrice) {
            this.name = name;
            this.description = description;
            this.barcode = barcode;
            this.categories = null;
            this.purDate = purDate;
            this.purPrice = purPrice;
            this.shareable = false;
        }

        @Override
        public String toString() {
            return  name + '\t' +
                    "[" + barcode + "]\t" +
                    categories + "\t" +
                    shareable;
        }
    }

    public enum REQUEST_STATUS {
        PENDING,
        ACCEPTED,
        DENIED
    }

    public class Request {
        public final String request_id;
        public final String requester;
        public final String tool_barcode;
        public final String tool_owner;
        public final Date req_date;
        public final int duration;
        public final REQUEST_STATUS status;

        public Request(String request_id, String requester, String tool_barcode, String tool_owner, Date req_date, int duration, REQUEST_STATUS status) {
            this.request_id = request_id;
            this.requester = requester;
            this.tool_barcode = tool_barcode;
            this.tool_owner = tool_owner;
            this.req_date = req_date;
            this.duration = duration;
            this.status = status;
        }
    }

    public enum ToolParts {
        BARCODE,    // used in search
        NAME,       // used in search + sort
        CATEGORY,   // used in search + sort
    }

    public Interface(String db_username, String db_password) {
        // Setting connection paramaters
        int lport = 5432;
        String rhost = "starbug.cs.rit.edu";
        int rport = 5432;
        String user = db_username;
        String password = db_password;
        String databaseName = "p320_02";

        // Creating objects
        String driverName = "org.postgresql.Driver";
        try {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            session = jsch.getSession(user, rhost, 22);
            session.setPassword(password);
            session.setConfig(config);
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.connect();
            System.out.println("Connected");
            int assigned_port = session.setPortForwardingL(lport, "localhost", rport);
            System.out.println("Port Forwarded");

            // Assigned port could be different from 5432 but rarely happens
            String url = "jdbc:postgresql://localhost:" + assigned_port + "/" + databaseName;

            System.out.println("database Url: " + url);
            Properties props = new Properties();
            props.put("user", user);
            props.put("password", password);

            int attempts = 0;
            while(attempts < 5) {
                try {
                    Class.forName(driverName);
                    conn = DriverManager.getConnection(url, props);
                    System.out.println("Database connection established");
                    break;
                } catch (Exception e) {
                    System.out.println("Database connection failed, trying again. (" + attempts + "/5)");
                    attempts++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void Disconnect() {
        try {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Closing Database Connection");
                conn.close();
            }
            if (session != null && session.isConnected()) {
                System.out.println("Closing SSH Connection");
                session.disconnect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void executeStatement(String string) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(string);
        statement.executeUpdate();
    }

    /**
     * Creates a new tool TODO: and inserts it into categories
     * @param user - user who owns the tool
     * @param newTool - tool to create (barcode ignored)
     * @return - True on success
     * @throws SQLException
     */
    public Boolean createTool(User user, Tool newTool) throws SQLException {
        String barcode = "0";
        executeStatement(
                "INSERT INTO tool_info (tool_name, description, purchase_date, purchase_price, username) " +
                        String.format("VALUES ('%s','%s','%s',%s,'%s');",
                                newTool.name, newTool.description, newTool.purDate, newTool.purPrice, user.username));
        return true;
    }

    /**
     * Edits a tool. Replaces the tools information with the "newTool"'s information (ignoring barcode)
     * @param barcode - tool to edit
     * @param newTool - new data (barcode ignored)
     * @return - True on success
     * @throws SQLException
     */
    public Boolean editTool(String barcode, Tool newTool) throws SQLException {
        executeStatement(
                String.format("UPDATE tool_info " +
                "SET tool_name='%s', description='%s', purchase_date='%s', purchase_price=%s " +
                "WHERE barcode='%s';", newTool.name, newTool.description, newTool.purDate, newTool.purPrice, barcode));
        return true;
    }

    /**
     * Deletes a tool TODO: only if it is not a part of a request, also deletes all category entries
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

        Date today = new Date(year + "-" + month + "-" + day);
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
     * checks that a tool belongs to a specific user
     * @param barcode - tool to check
     * @param username - name of user
     * @return true if the tool belongs to the user
     * @throws SQLException
     */
    private boolean checkUserBarcode(String barcode, String username) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(
                String.format("SELECT COUNT(1) FROM tool_info WHERE barcode='%s' AND username='%s';", barcode, username));
        ResultSet result = statement.executeQuery();
        result.next();
        if (result.getInt(1) == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieves the Tool information only if the tool belongs to the specified user
     * @param user - user to get the tool from
     * @param barcode - barcode of the tool to get
     * @return - Tool or null
     * @throws SQLException
     */
    public Tool getUserTool(User user, String barcode) throws SQLException {
        if (!checkUserBarcode(barcode, user.username)) {
            return null;
        }
        PreparedStatement statement = conn.prepareStatement(
                String.format("SELECT tool_name,description,purchase_date,purchase_price " +
                        "FROM tool_info WHERE barcode = '%s';", barcode));
        ResultSet result = statement.executeQuery();
        result.next();

        String tool_name = result.getString("tool_name");
        String description = result.getString("description");
        Date purchase_date = new Date(result.getString("purchase_date"));
        float purchase_price = result.getFloat("purchase_price");

        Tool tool = new Tool(tool_name,description,barcode,purchase_date,purchase_price);
        return tool;
    }

    /**
     * Retrieves user information from the database
     * @param username - username of the user you are retrieving
     * @return - User information
     * @throws SQLException
     */
    private User getUser(String username) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(
                String.format("SELECT first_name,last_name,email,creation_date,last_access_date " +
                        "FROM \"user\" WHERE username = '%s';", username));
        ResultSet result = statement.executeQuery();
        result.next();

        String first_name = result.getString("first_name");
        String last_name = result.getString("last_name");
        String email = result.getString("email");
        Date creation_date = new Date(result.getString("creation_date"));
        Date last_access_date = new Date(result.getString("last_access_date"));

        User user = new User(username,first_name,last_name,email,creation_date,last_access_date);
        return user;
    }

    /**
     * Verifies username and password and updates access date
     * @param username username entered by user
     * @param password password entered by user
     * @return true if login succeeds, false otherwise
     */
    public User login(String username, String password) throws SQLException {
        // check if username exists
        if (!checkUsername(username)) {
            return null;
        }

        // find correct password based on username
        PreparedStatement statement = conn.prepareStatement(String.format("SELECT password FROM \"user\" WHERE username = '%s';", username));
        ResultSet result = statement.executeQuery();
        result.next();
        String correct_password = result.getString("password");

        // check if correct password matches entered password
        if (!password.equals(correct_password)) {
            return null;
        }

        // update access date
        Date today = getCurrentDate();
        executeStatement(
                String.format("UPDATE \"user\" " +
                        "SET last_access_date='%s' " +
                        "WHERE username='%s';", today, username));

        // creates user and returns it
        return getUser(username);
    }

    /**
     * TODO: Checks if a tool is available to borrow
     * @param barcode - tool to check
     * @return true or false
     */
    public boolean isToolAvailable(String barcode) {
        return false;
    }

    /**
     * TODO: Creates a borrow request
     * @param user - user requesting to borrow
     * @param barcode - tool to borrow
     * @param requiredDate - date the tool is needed
     * @param daysNeeded - how many days the tool is needed
     * @return True if successful
     */
    public boolean createBorrowRequest(User user, String barcode, Date requiredDate, int daysNeeded) {
        return false;
    }

    /**
     * TODO: Fetches a list of requests a user has yet to respond to
     *      meaning tool_owner = user and status = pending
     * @param user - user to check
     * @return - Vector of Requests
     */
    public Vector<Request> getPendingUserRequests(User user) {
        return null;
    }

    /**
     * TODO: Accepts a pending request only if the proper user is attempting to
     *      example request.tool_owner = user
     * @param user - User attempting to accept request
     * @param request_id - request to accept
     * @return - True if successful
     */
    public boolean acceptRequest(User user, String request_id) {
        return false;
    }

    /**
     * TODO: Denies a pending request only if the proper user is attempting to
     *      example request.tool_owner = user
     * @param user - User attempting to accept request
     * @param request_id - request to deny
     * @return - True on success
     */
    public boolean denyRequest(User user, String request_id) {
        return false;
    }

    /**
     * TODO: Gets a list of tools available to borrow
     *      it is shareable and not in a borrow request
     * @return - Vector of tools
     */
    public Vector<Tool> getAvailableTools() {
        return null;
    }

    /**
     * TODO: Gets a list of all the tools a user is borrowing
     * @param user - user to check
     * @return Vector of tools
     */
    public Vector<Tool> getUserBorrowedTools(User user) {
        return null;
    }

    /**
     * TODO: Gets a list of all the tools a user is lending out
     * @param user - user to check
     * @return Vector of tools
     */
    public Vector<Tool> getUserLentTools(User user) {
        return null;
    }

    /**
     * TODO: Returns a tool to it's owner (deletes the request entry)
     * @param user - User attempting to return
     * @param barcode - Barcode of the tool
     * @return - True if successful
     */
    public boolean returnTool(User user, String barcode) {
        return false;
    }

    /**
     * TODO: Generates a list of tools that belong to a specified user
     * @param user - user to retrieve imformatio nabout
     * @return Vector of tools
     */
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

    /**
     * TODO: Gets the list of all categories
     * @return - list of all categories
     */
    public Vector<String> getCategories() {
        try {
            PreparedStatement statement = conn.prepareStatement(
                "SELECT tool_category FROM category\n ORDER BY tool_category"
            );
            ResultSet result = statement.executeQuery();
            Vector<String> categories = new Vector<>();
            result.next();
            while(!result.isAfterLast()){
                categories.add(result.getString("tool_category"));
                result.next();
            }
            return categories;
        }
        catch(SQLException e){
            return new Vector<>();
        }
    }

    /**
     * TODO: checks if a category already exists
     * @param category - category to check
     * @return - status of the category
     */
    public boolean checkCategory(String category){
        try{
            PreparedStatement statement = conn.prepareStatement(
            "select count(*) from category where tool_category = '" + category +"'"
            );
            ResultSet result = statement.executeQuery();
            result.next();
            return result.getInt("count") > 0;
        }
        catch(SQLException e){
            return false;
        }
    }

    /**
     * TODO: Searches all tools based on given parameters
     * @param searchParam - Part of the tool we are searching
     * @param searchArgument - String we are searching
     * @return - Vector of tools
     */
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

    /**
     * TODO: Sorts all tools based on part
     * @param searchParam - Part of the tool to sort by
     * @param ascending - ascending or decending
     * @return - Sorted Vector of tools
     */
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
