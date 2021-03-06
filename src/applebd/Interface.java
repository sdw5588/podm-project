package applebd;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.sql.*;
import java.text.SimpleDateFormat;
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

        public String toDetailedString() {
            return  String.format(
                    "%s\n" +
                    "%s\n" +
                    "\tBarcode: [%s]\n" +
                    "\tCategories: " + categories + "\n" +
                    "\t%s - $%f\n" +
                    "\tSharable: %s",
                    name, description, barcode, purDate, purPrice, shareable.toString()
                    );
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

        @Override
        public String toString() {
            return "id[" + request_id + "] " + requester +
                    " is requesting barcode[" + tool_barcode + "] from " + tool_owner + " on " +
                    req_date + " for " + duration + " days.";
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

    private boolean executeStatement(String string) {
        try {
            PreparedStatement statement = conn.prepareStatement(string);
            statement.executeUpdate();
            return true;
        }
        catch (SQLException e){
            return false;
        }
    }

    /**
     * Creates a new tool and inserts it into categories
     * @param user - user who owns the tool
     * @param newTool - tool to create (barcode ignored)
     * @return - True on success
     */
    public boolean createTool(User user, Tool newTool){
        if(executeStatement(
    "INSERT INTO tool_info (tool_name, description, purchase_date, purchase_price, username, sharable) " +
            String.format("VALUES ('%s','%s','%s',%s,'%s', %s);",
            newTool.name, newTool.description, newTool.purDate, newTool.purPrice, user.username,
            (newTool.shareable ? "true" : "false")))
        ){
            try{
                PreparedStatement statement = conn.prepareStatement(String.format("" +
                    "SELECT * FROM tool_info WHERE tool_name = '%s' AND description = '%s' AND username = '%s'",
                    newTool.name, newTool.description, user.username
                ));
                ResultSet result = statement.executeQuery();
                result.next();
                String barcode = result.getString("barcode");

                boolean success = true;
                for(String cat : newTool.categories){
                    if(!addToolToCategory(barcode, cat.trim()))
                        success = false;
                }
                return success;
            } catch(SQLException e){
                return false;
            }
        }
        return false;
    }

    /**
     * Edits a tool. Replaces the tools information with the "newTool"'s information (ignoring barcode)
     * @param barcode - tool to edit
     * @param newTool - new data (barcode ignored)
     * @return - True on success
     */
    public boolean editTool(String barcode, Tool newTool) {
        return executeStatement(
                String.format("UPDATE tool_info " +
                "SET tool_name='%s', description='%s', purchase_date='%s', purchase_price=%s, sharable='%s' " +
                "WHERE barcode='%s';", newTool.name, newTool.description, newTool.purDate, newTool.purPrice, newTool.shareable, barcode));
    }

    /**
     * Deletes a tool
     * @param barcode - tool to delete
     * @return - True on success
     * @throws SQLException
     */
    public boolean deleteTool(String barcode, User user)  {
        try {
            if (!checkUserBarcode(barcode, user.username)) {
                return false;
            }
            executeStatement(String.format("DELETE FROM tool_category WHERE barcode=%s;", barcode));
            executeStatement(String.format("DELETE FROM tool_info WHERE barcode=%s;", barcode));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Creates a category and adds it to the database
     * @param category - Category to add
     * @return - True on success
     */
    public boolean createCategory(String category){
        if(checkCategory(category))
            return false;
        return executeStatement(String.format(
                "INSERT INTO category (tool_category) VALUES ('%s')",
                category
        ));
    }

    /**
     * Adds a category to a tool for better sorting
     * @param barcode - tool to edit
     * @param category - category to add
     * @return - True on success
     * @throws SQLException
     */
    public boolean addToolToCategory(String barcode, String category) {
        if(!checkCategory(category))
            return false;
        if(toolInCategory(barcode, category))
            return false;
        return executeStatement(String.format("INSERT INTO tool_category (category_name, barcode) " +
                "VALUES ('%s','%s');", category, barcode ));
    }

    /**
     * Removes a tool from a category
     * @param barcode - barcode of tool to modify
     * @param category - category to remove
     * @return - True on success
     * @throws SQLException
     */
    public boolean removeToolFromCategory(String barcode, String category) {
        return executeStatement(String.format(
                "DELETE FROM tool_category WHERE barcode='%s' AND category_name='%s'",
                barcode, category
        ));
    }

    /**
     * Checks if a tool is already in a given category
     * @param barcode - tool to check
     * @param category - category to check
     * @return -= true if the tool is in the category
     */
    public boolean toolInCategory(String barcode, String category){
        try {
            PreparedStatement statement = conn.prepareStatement(String.format(
                    "SELECT COUNT(*) FROM tool_category WHERE category_name = '%s' AND barcode = '%s'",
                    category, barcode));
            ResultSet result = statement.executeQuery();
            result.next();
            return result.getInt("count") > 0;
        }
        catch(SQLException e){
            return false;
        }
    }

    /**
     * fetches the list of categories a tool belongs to
     * @param barcode - tool to  fetch
     * @return Vector of categories
     */
    public Vector<String> getToolCategories(String barcode){
        Vector<String> categories = new Vector<>();
        try{
            PreparedStatement statement = conn.prepareStatement(String.format(
                    "SELECT category_name FROM tool_category WHERE barcode = '%s' ORDER BY category_name",
                    barcode
            ));
            ResultSet result = statement.executeQuery();
            result.next();
            while(!result.isAfterLast()){
                categories.add(result.getString("category_name"));
                result.next();
            }
            return categories;
        }
        catch(SQLException e){
            return categories;
        }
    }

    /**
     * This function checks if a category exists
     * @param category - Name os the category to check
     * @return - Trus if it exists in the database, false oterwise
     */
    public boolean validateCategory(String category) throws SQLException {
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
     * Gets the tools owner
     * @param barcode - tool to fetch
     * @return - Tool's owner
     */
    public User getToolOwner(String barcode){
        try{
            PreparedStatement statement = conn.prepareStatement(
                    String.format("SELECT username " +
                            "FROM tool_info WHERE barcode = '%s';", barcode));
            ResultSet result = statement.executeQuery();
            result.next();

            String tool_owner_username = result.getString("username");

            return getUser(tool_owner_username);
        } catch (SQLException e){
            return null;
        }
    }

    /**
     * Gets tool info from the databse.
     * @param barcode - tool to fetch
     * @return - Tool info
     */
    public Tool getTool(String barcode){
        try{
            PreparedStatement statement = conn.prepareStatement(
                    String.format("SELECT tool_name,description,purchase_date,purchase_price,sharable " +
                            "FROM tool_info WHERE barcode = '%s';", barcode));
            ResultSet result = statement.executeQuery();
            result.next();

            String tool_name = result.getString("tool_name");
            String description = result.getString("description");
            Date purchase_date = new Date(result.getString("purchase_date"));
            float purchase_price = result.getFloat("purchase_price");
            boolean sharable = result.getBoolean("sharable");

            return new Tool(tool_name, description,
                    barcode, getToolCategories(barcode),
                    purchase_date, purchase_price,
                    sharable
            );
        } catch (SQLException e){
            return null;
        }

    }

    /**
     * Retrieves the Tool information only if the tool belongs to the specified user
     * @param user - user to get the tool from
     * @param barcode - barcode of the tool to get
     * @return - Tool or null
     * @throws SQLException
     */
    public Tool getUserTool(User user, String barcode){
        try {
            if (!checkUserBarcode(barcode, user.username)) {
                return null;
            }
            return getTool(barcode);
        }
        catch(SQLException e){
            return null;
        }
    }

    /**
     * Retrieves user information from the database
     * @param username - username of the user you are retrieving
     * @return - User information
     */
    private User getUser(String username) {
        try {
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

            User user = new User(username, first_name, last_name, email, creation_date, last_access_date);
            return user;
        }
        catch(SQLException e){
            return null;
        }
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
        Tool tool = getTool(barcode);
        if(tool.shareable) {
            User toolOwner = getToolOwner(barcode);

            return executeStatement(String.format(
                    "INSERT INTO requests (username, barcode, date_required, expected_return_date, duration, real_return_date, owner_username)\n" +
                            "VALUES ('%s', '%s', '%s', '2000-01-01', %d, '2000-01-01', '%s')",
                    user.username, barcode, requiredDate, daysNeeded, toolOwner.username
            ));
        }
        else{
            return false;
        }
    }

    public Request getRequest(String requestId){
        try{
            PreparedStatement statement = conn.prepareStatement(
            "select * from requests where request_id = " + requestId
            );
            ResultSet result = statement.executeQuery();
            result.next();

            REQUEST_STATUS status = REQUEST_STATUS.PENDING;
            String statusStr = result.getString("status");
            if(statusStr.equals("Accepted")) status = REQUEST_STATUS.ACCEPTED;
            if(statusStr.equals("Denied")) status = REQUEST_STATUS.DENIED;

            return new Request(
                    result.getString("result_id"),
                    result.getString("username"),
                    result.getString("barcode"),
                    result.getString("owner_username"),
                    new Date(result.getString("date_required")),
                    result.getInt("duration"),
                    status
            );

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Fetches a list of requests a user has yet to respond to
     *      meaning tool_owner = user and status = pending
     * @param user - user to check
     * @return - Vector of Requests
     */
    public Vector<Request> getPendingUserRequests(User user) {
        Vector<Request> requests = new Vector<>();

        try{
            PreparedStatement statement = conn.prepareStatement(
                    "select * from requests where owner_username = '" + user.username + "' and status = 'Pending'"
            );
            ResultSet result = statement.executeQuery();
            result.next();
            while(!result.isAfterLast()) {
                requests.add(new Request(
                        result.getString("request_id"),
                        result.getString("username"),
                        result.getString("barcode"),
                        result.getString("owner_username"),
                        new Date(result.getString("date_required")),
                        result.getInt("duration"),
                        REQUEST_STATUS.PENDING
                ));
                result.next();
            }
            return requests;
        } catch (SQLException e) {
        }
        return requests;
    }

    private boolean checkUserMatchesOwner(User user, String request_id) {
        try {
            PreparedStatement statement = conn.prepareStatement(
                    String.format("SELECT COUNT(1) FROM requests WHERE request_id=%s AND owner_username='%s';", request_id, user.username));
            ResultSet result = statement.executeQuery();
            result.next();
            return (result.getInt(1) > 0);
        } catch (SQLException e) {
            return false;
        }
    }
    /**
     * Fetches a list of requests a user has yet to respond to
     *      meaning tool_owner = user and status = pending
     * @param user - user to check
     * @return - Vector of Requests
     */
    public Vector<Request> getOutgoingPendingUserRequests(User user) {
        Vector<Request> requests = new Vector<>();

        try{
            PreparedStatement statement = conn.prepareStatement(
            "select * from requests where username = '" + user.username + "' and status = 'Pending'"
            );
            ResultSet result = statement.executeQuery();
            result.next();

            while(!result.isAfterLast()) {
                requests.add(new Request(
                        result.getString("request_id"),
                        result.getString("username"),
                        result.getString("barcode"),
                        result.getString("owner_username"),
                        new Date(result.getString("date_required")),
                        result.getInt("duration"),
                        REQUEST_STATUS.PENDING
                ));
                result.next();
            }
            return requests;
        } catch (SQLException e) {
        }
        return requests;
    }

    /**
     * Accepts a pending request only if the proper user is attempting to
     *      example request.tool_owner = user
     * @param user - User attempting to accept request
     * @param request_id - request to accept
     * @return - True if successful
     */
    public boolean acceptRequest(User user, String request_id, Date returnDate) {
        // check if user matches tool_owner
        if (!checkUserMatchesOwner(user, request_id)) {
            return false;
        }

        // set request status to Accepted
        if (executeStatement(String.format(
                "UPDATE requests SET status='Accepted', real_return_date='%s' WHERE request_id=%s;",
                returnDate, request_id
        )))
            executeStatement(String.format(
                    "INSERT INTO request_status (request_id, previous_status, current_status, date_of_change)\n" +
                    "VALUES ('%s', 'Pending', 'Accepted', '%s')",
                    request_id, getCurrentDate()
            ));
        return true;
    }

    /**
     * Denies a pending request only if the proper user is attempting to
     *      example request.tool_owner = user
     * @param user - User attempting to accept request
     * @param request_id - request to deny
     * @return - True on success
     */
    public boolean denyRequest(User user, String request_id) {
        // check if user matches tool_owner
        if (!checkUserMatchesOwner(user, request_id)) {
            return false;
        }

        // set request status to Accepted
        executeStatement(String.format(
                "UPDATE requests SET status='Denied' WHERE request_id=%s;",
                request_id
        ));
        executeStatement(String.format(
                "INSERT INTO request_status (request_id, previous_status, current_status, date_of_change)\n" +
                        "VALUES ('%s', 'Pending', 'Denied', '%s')",
                request_id, getCurrentDate()
        ));
        return true;
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
     * Gets a list of all the tools a user is borrowing
     * @param user - user to check
     * @return Vector of tools
     */
    public Vector<Tool> getUserBorrowedTools(User user) {
        Vector<Tool> tools = new Vector<>();

        try{
            PreparedStatement statement = conn.prepareStatement(
            "select * from requests where username = '" + user.username + "' and status = 'Accepted'"
            );
            ResultSet result = statement.executeQuery();
            result.next();
            while(!result.isAfterLast()) {
                tools.add(getTool(result.getString("barcode")));
                result.next();
            }
            return tools;
        } catch (SQLException e) {
        }
        return tools;
    }

    /**
     * Gets a list of all the tools a user is lending out
     * @param user - user to check
     * @return Vector of tools
     */
    public Vector<Tool> getUserLentTools(User user) {
        Vector<Tool> tools = new Vector<>();

        try{
            PreparedStatement statement = conn.prepareStatement(
                    "select * from requests where owner_username = '" + user.username + "' and status = 'Accepted'"
            );
            ResultSet result = statement.executeQuery();
            result.next();
            while(!result.isAfterLast()) {
                tools.add(getTool(result.getString("barcode")));
                result.next();
            }
            return tools;
        } catch (SQLException e) {
        }
        return tools;
    }

    /**
     * Returns a tool to it's owner (deletes the request entry)
     * @param user - User attempting to return
     * @param barcode - Barcode of the tool
     * @return - True if successful
     */
    public boolean returnTool(User user, String barcode) {
        try {
        PreparedStatement statement = conn.prepareStatement(String.format(
            "SELECT * FROM requests WHERE username = '%s' and barcode = '%s'",
            user.username, barcode
        ));
        ResultSet result = statement.executeQuery();
        result.next();

        String request_id = result.getString("request_id");

        /*
        if(executeStatement(String.format(
                "delete from request_status where request_id = '%s'",
                request_id
            )))
            executeStatement(String.format(
                    "delete from requests where barcode = '%s' and username = '%s'",
                    barcode, user.username
            ));

         */

        executeStatement(String.format(
                "UPDATE requests SET returned='Returned' WHERE request_id=%s;", request_id));

        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * TODO: Generates a list of tools that belong to a specified user
     * @param user - user to retrieve imformatio nabout
     * @return Vector of tools
     */
    public Vector<Tool> getUserTools(User user) {
        Vector<Tool> tools = new Vector<>();
        try {
            PreparedStatement statement = conn.prepareStatement(
                    String.format("SELECT * FROM tool_info WHERE username = '%s'",
                            user.username
                    ));
            ResultSet result = statement.executeQuery();
            result.next();
            while(!result.isAfterLast()){
                String barcode = result.getString("barcode");
                Vector<String> categories = getToolCategories(barcode);
                tools.add(new Tool(result.getString("tool_name"), result.getString("description"),
                        barcode, categories,
                        new Date(result.getString("purchase_date")), result.getFloat("purchase_price"),
                        result.getBoolean("sharable")));
                result.next();
            }
            return tools;
        } catch (SQLException e) {
            return tools;
        }
    }

    /**
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
        try{
            String searchPart = "";
            if(searchParam == ToolParts.NAME) searchPart = "tool_name";
            if(searchParam == ToolParts.BARCODE) searchPart = "CAST(barcode AS VARCHAR(40))";
            PreparedStatement statement = null;

            if(!searchPart.equals("")){
                statement = conn.prepareStatement(String.format(
                    "SELECT * FROM tool_info WHERE LOWER(%s) LIKE '%%%s%%'",
                    searchPart, searchArgument.toLowerCase()
                ));
            }

            if(searchParam == ToolParts.CATEGORY){
                statement = conn.prepareStatement(String.format(
                        "SELECT * FROM tool_info INNER JOIN tool_category tc on tool_info.barcode = tc.barcode\n" +
                        "WHERE LOWER(category_name) LIKE '%%%s%%'",
                        searchArgument.toLowerCase()
                ));
            }

            if(statement != null) {
                ResultSet result = statement.executeQuery();
                result.next();
                while (!result.isAfterLast()) {
                    String barcode = result.getString("barcode");
                    Vector<String> categories = getToolCategories(barcode);
                    tools.add(new Tool(result.getString("tool_name"), result.getString("description"),
                            barcode, categories,
                            new Date(result.getString("purchase_date")), result.getFloat("purchase_price"),
                            result.getBoolean("sharable")));
                    result.next();
                }
            }
        } catch (SQLException ignored){}

        return tools;
    }

    /**
     * returns a list of the user's most frequently borrowed tools
     * @param user
     * @return vector of up to 10 of the user's most borrowed tools
     */
    public Vector<Tool> topBorrowed (User user) {
        Vector<Tool> tools = new Vector<>();
        try {
            // counts rows
            PreparedStatement statement = conn.prepareStatement(String.format(
                    "SELECT COUNT(*) FROM requests WHERE status='Accepted' AND username='%s'",
                    user.username
            ));
            ResultSet result = statement.executeQuery();
            result.next();
            int row = result.getInt("count");
            if (row > 10) {row=10;}

            statement = conn.prepareStatement(String.format(
                    "SELECT barcode FROM requests WHERE status='Accepted' AND username='%s' GROUP BY barcode ORDER BY COUNT(*) DESC LIMIT 10;",
                    user.username
            ));
            result = statement.executeQuery();
            result.next();
            for (int i = 0; i < row; i++) {
                String barcode = result.getString("barcode");
                tools.add(getTool(barcode));
                result.next();
            }
        } catch (SQLException ignored) {}

        return tools;
    }

    /**
     * returns a list of the user's top 10 most lent tools and the percentage of time lent
     * @param user current user
     * @return
     */
    public Vector[] topLent(User user) {
        try {
            // counts rows
            PreparedStatement statement = conn.prepareStatement(String.format(
                    "SELECT COUNT(*) FROM requests WHERE status='Accepted' AND owner_username='%s'",
                    user.username
            ));
            ResultSet result = statement.executeQuery();
            result.next();
            int row = result.getInt("count");
            if (row > 10) {row=10;}

            // gets percentage of time lent over time available
            statement = conn.prepareStatement(String.format("" +
                    "SELECT barcode, duration_sum/count as average, " +
                    "(duration_sum/DATE_PART('day', CURRENT_DATE::timestamp - purchase_date::timestamp)) as percentage " +
                    "FROM tool_info, " +
                    "(SELECT " +
                    "   barcode AS barcode_requests, " +
                    "   SUM(duration) AS duration_sum, " +
                    "   count(*) as count " +
                    "FROM " +
                    "  requests " +
                    "WHERE owner_username='%s' AND status='Accepted' " +
                    "GROUP BY " +
                    "   barcode) as total_duration " +
                    "WHERE barcode=barcode_requests " +
                    "ORDER BY percentage DESC " +
                    "LIMIT 10;",
                    user.username

            ));
            result = statement.executeQuery();
            result.next();

            // adds tools
            Vector<Tool> tools = new Vector<>();
            Vector<String> averages = new Vector<>();
            for (int i = 0; i < row; i++) {
                String barcode = result.getString("barcode");
                tools.add(getTool(barcode));
                averages.add(result.getString("average"));
                result.next();
            }

            Vector results[] = {tools, averages};
            return results;

        } catch (SQLException ignored) {}
        return null;
    }

    /**
     * TODO: Sorts all tools based on name
     * @param ascending - ascending or decending
     * @return - Sorted Vector of tools
     */
    public boolean sortToolsName (Boolean ascending) {
        try {
            PreparedStatement statement = conn.prepareStatement(
            "SELECT * FROM tool_info ORDER BY tool_name " + (ascending ? "ASC" : "DESC")
            );
            ResultSet result = statement.executeQuery();
            result.next();
            while(!result.isAfterLast()){
                String barcode = result.getString("barcode");
                Vector<String> categories = getToolCategories(barcode);
                Tool tool = new Tool(result.getString("tool_name"), result.getString("description"),
                        barcode, categories,
                        new Date(result.getString("purchase_date")), result.getFloat("purchase_price"),
                        result.getBoolean("sharable")
                );
                System.out.println(tool.toString());
                result.next();
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * TODO: Sorts all tools based on category
     * @param ascending - ascending or decending
     * @return - Sorted Vector of tools
     */
    public boolean sortToolsCategory (Boolean ascending) {
        Vector<Tool> tools = new Vector<>();

        try{
            PreparedStatement statement = conn.prepareStatement(String.format(
                "SELECT * FROM tool_info LEFT JOIN tool_category tc on tool_info.barcode = tc.barcode\n" +
                "ORDER BY category_name %s",
                ascending ? "ASC" : "DESC"
            ));
            ResultSet result = statement.executeQuery();

            String currentCategory = "";
            result.next();
            while(!result.isAfterLast()){
                String upcomingCategory = result.getString("category_name");
                if(upcomingCategory == null)
                    upcomingCategory = "NO CATEGORIES";
                if(!upcomingCategory.equals(currentCategory)){
                    System.out.println(upcomingCategory);
                    currentCategory = upcomingCategory;
                }

                String barcode = result.getString("barcode");
                Vector<String> categories = getToolCategories(barcode);
                Tool tool = new Tool(result.getString("tool_name"), result.getString("description"),
                        barcode, categories,
                        new Date(result.getString("purchase_date")), result.getFloat("purchase_price"),
                        result.getBoolean("sharable")
                );
                System.out.println("\t" + tool);
                result.next();
            }
            return true;
        } catch(SQLException ignored){
            return false;
        }
    }

    /**
     * returns the number of sharable tools that are not being borrowed
     * @param user logged-in user
     */
    public int countAvailableTools(User user) {
        try {
            PreparedStatement statement = conn.prepareStatement(String.format(
                    "SELECT COUNT(*) FROM tool_info WHERE sharable='true' AND username='%s';", user.username
                    ));
            ResultSet result = statement.executeQuery();
            result.next();
            int sharableTools = result.getInt("count");
            int lentTools = countLentTools(user);
            return sharableTools - lentTools;
        }
        catch(SQLException e){
            return -1;
        }
    }

    /**
     * returns the number of lent tools
     * @param user logged-in user
     */
    public int countLentTools(User user) {
        try {
            PreparedStatement statement = conn.prepareStatement(String.format(
                    "SELECT COUNT(*) FROM requests WHERE status='Accepted' AND owner_username='%s';", user.username
            ));
            ResultSet result = statement.executeQuery();
            result.next();
            return result.getInt("count");
        }
        catch(SQLException e){
            return -1;
        }
    }

    /**
     * returns the number of borrowed tools
     * @param user logged-in user
     */
    public int countBorrowedTools(User user) {
        try {
            PreparedStatement statement = conn.prepareStatement(String.format(
                    "SELECT COUNT(*) FROM requests WHERE status='Accepted' AND username='%s';", user.username
            ));
            ResultSet result = statement.executeQuery();
            result.next();
            return result.getInt("count");
        }
        catch(SQLException e){
            return -1;
        }
    }

    public Vector<Tool> recommendTools(String barcode) {
        try {
            PreparedStatement statement = conn.prepareStatement("" +
                    "SELECT barcode FROM tool_info\n" +
                    "WHERE sharable='true'\n" +
                    "ORDER BY RANDOM()\n" +
                    "LIMIT 5;");
            ResultSet result = statement.executeQuery();
            result.next();

            Vector<Tool> tools = new Vector<>();
            for (int i = 0; i < 5; i++) {
                tools.add(getTool(result.getString("barcode")));
                result.next();
            }
            return tools;
        }
        catch (SQLException e){

        }
        return null;
    }
}
