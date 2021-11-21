package applebd.textmenu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextMenu extends TextMenuItem {

    private static final TextMenuItem back = new TextMenuItem("Back");
    List<TextMenuItem> items;
    Runnable printHeader;

    public TextMenu(String title, String header, TextMenuItem ... items) {
        this(title, header, true, items);
    }
    public TextMenu(String title, Runnable header, TextMenuItem ... items) {
        this(title, header, true, items);
    }

    public TextMenu(String title, String header, boolean addBack, TextMenuItem ... items) {
        this(title, () -> System.out.println(header), addBack, items);
    }

    public TextMenu(String title, Runnable printHeader, boolean addBack, TextMenuItem ... items) {
        super(title);
        setExec(this);
        this.printHeader = printHeader;

        initialize(addBack, items);
    }

    private void initialize(boolean addBack, TextMenuItem ... items) {

        this.items= new ArrayList<TextMenuItem>(Arrays.asList(items));
        if (addBack) this.items.add(back);
    }

    private void display() {

        int option = 1;
        this.printHeader.run();
        for (TextMenuItem item : items) {
            System.out.println((option++)+" - "+item.getTitle());
        }
        System.out.print("\nSelect Option: ");
        System.out.flush();
    }

    private TextMenuItem prompt() throws IOException {

        BufferedReader br= new BufferedReader(new InputStreamReader(System.in));

        while (true) {

            display();

            String line = br.readLine();
            try {
                int option= Integer.parseInt(line);
                if (option >= 1 && option < items.size() + 1)
                    return items.get(option - 1);
            }
            catch (NumberFormatException e) { }

            System.out.println("Not a valid menu option!");
        }
    }

    public void run() {

        try {
            for (TextMenuItem item = prompt(); item.isExec(); item = prompt())
                item.run();
        }
        catch (Throwable t) {
            t.printStackTrace(System.out);
        }
    }
}