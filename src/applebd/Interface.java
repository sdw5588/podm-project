package applebd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Vector;

public class Interface {
    private Connection conn;

    public class Date {
        // YYYY-MM-DD
        public int month;
        public int day;
        public int year;

        Date(int month, int day, int year){
            this.month = month; this.day = day; this.year = year;
        }

        @Override
        public String toString() {
            return (year + "-" + month + "-" + day);
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

    public Boolean verifyUsername(String username) {
        return true;
    }

    public User login(String username, String password) {
        if(username.equals("test") && password.equals("test")){
            return new User("test", "Johnny", "Test", "Johnny@test.best",
                    new Date(12, 2, 2000), new Date(1, 1, 1999)
            );
        }
        return null;
    }

    public Tool getUserTool(User user, String barcode) {
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

    private void executeStatement(String string) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(string);
        statement.executeUpdate();
    }

    public Boolean createTool(User user, Tool newTool) throws SQLException {
        executeStatement(
                "INSERT INTO tool_info (barcode, tool_name, description, purchase_date, purchase_price, username) " +
                        String.format("VALUES (%s,%s,%s,%s,%s,%s);", newTool.barcode, newTool.name, newTool.description, newTool.purDate, newTool.purPrice, user.username));
        return true;
    }

    public Boolean editTool(String barcode, Tool newTool) throws SQLException {
        executeStatement(
                String.format("UPDATE tool_info " +
                "SET tool_name=%s, description=%s, purchase_date=%s, purchase_price=%s" +
                "WHERE barcode='%s';", newTool.name, newTool.description, newTool.purDate, newTool.purPrice, barcode));
        return true;
    }

    public Boolean deleteTool(String barcode) throws SQLException {
        executeStatement(String.format("DELETE FROM tool_info WHERE barcode='%s';", barcode));
        return true;
    }

    public Boolean addToolToCategory(String barcode, String category) throws SQLException {
        executeStatement(String.format("INSERT INTO tool_category (category_name, barcode) " +
                "VALUES (%s,%s);", category, barcode ));
        return false;
    }

    public Boolean removeCategoryFromTool(String barcode, String category) throws SQLException {
        executeStatement(String.format("DELETE FROM tool_category WHERE barcode='%s' AND category_name='%s'", barcode, category));
        return true;
    }

    public Boolean createCategory(User user, String category) {
        return false;
    }

    public Boolean validateCategory(String category) {
        return true;
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
