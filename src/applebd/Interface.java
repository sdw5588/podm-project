package applebd;

public class Interface {
    public class Date {
        public int month;
        public int day;
        public int year;
    }

    public class User {
        public String username;
        public String firstName;
        public String lastName;
        public String email;
        public Date   creationDate;
        public Date   lastAccessDate;
    }

    public class Tool {
        public final String     name;
        public final String     description;
        public final String     barcode;
        public final String[]   categories;
        public final Date       purDate;
        public final float      purPrice;
        public final Boolean    shareable;

        public Tool(String name, String description, String[] categories,
                    Date purDate, float purPrice, Boolean shareable) {
            this(name, description, null, categories, purDate, purPrice, shareable);
        }

        public Tool(String name, String description, String barcode, String[] categories,
                    Date purDate, float purPrice, Boolean shareable) {
            this.name = name;
            this.description = description;
            this.barcode = barcode;
            this.categories = categories;
            this.purDate = purDate;
            this.purPrice = purPrice;
            this.shareable = shareable;
        }
    }

    public Interface() {
        // connect to database
    }

    public Boolean verifyUsername(String username) {
        return true;
    }

    public User login(String username, String password) {
        return null;
    }

    public Boolean createTool(User user, Tool newTool) {
        return false;
    }

    public Boolean editTool(String barcode, Tool newTool) {
        return false;
    }

    public Boolean deleteTool(String barcode) {
        return false;
    }

    public Boolean addToolToCategory(String barcode, String category) {
        return false;
    }

    public Boolean removeCategoryFromTool(String barcode, String category) {
        return false;
    }

    public Boolean createCategory(User user, String category) {
        return false;
    }
}
