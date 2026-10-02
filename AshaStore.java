import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Vector;

public class AshaStore extends MIDlet implements CommandListener {

    private Display display;
    private List main;
    private Form searchForm;
    private TextField searchText;
    private Command searchCommand;
    private Command backCommand;
    private Command exitCommand;

    private static final String SITE = "https://series40.kiev.ua/";

    public AshaStore() {
        display = Display.getDisplay(this);

        main = new List("Asha Store", List.IMPLICIT);
        main.append("Последние игры", null);
        main.append("Игры", null);
        main.append("Программы", null);
        main.append("Поиск", null);
        main.append("Обновить", null);

        main.setCommandListener(this);

        exitCommand = new Command("Выход", Command.EXIT, 1);
        main.addCommand(exitCommand);
    }

    public void startApp() {
        display.setCurrent(main);
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void commandAction(Command c, Displayable d) {

        if (c == exitCommand) {
            notifyDestroyed();
            return;
        }

        if (d == main && c == List.SELECT_COMMAND) {
            int n = main.getSelectedIndex();

            if (n == 3) {
                showSearch();
            } else if (n == 4) {
                loadPage(SITE);
            } else {
                loadPage(SITE);
            }

            return;
        }

        if (d == searchForm && c == searchCommand) {
            String q = searchText.getString();

            if (q.length() == 0) {
                Alert a = new Alert("Поиск",
                        "Введите название игры или программы.",
                        null,
                        AlertType.INFO);
                a.setTimeout(Alert.FOREVER);
                display.setCurrent(a, searchForm);
                return;
            }

            String url = SITE + "?s=" + encode(q);
            loadPage(url);
            return;
        }

        if (c == backCommand) {
            display.setCurrent(main);
        }
    }

    private void showSearch() {

        searchForm = new Form("Поиск");

        searchText = new TextField(
                "Название:",
                "",
                40,
                TextField.ANY
        );

        searchForm.append(searchText);

        searchCommand = new Command(
                "Искать",
                Command.OK,
                1
        );

        backCommand = new Command(
                "Назад",
                Command.BACK,
                2
        );

        searchForm.addCommand(searchCommand);
        searchForm.addCommand(backCommand);
        searchForm.setCommandListener(this);

        display.setCurrent(searchForm);
    }

    private void loadPage(final String url) {

        final Form wait = new Form("Asha Store");
        wait.append("Подключение к интернету...\n\n");
        wait.append(url);

        display.setCurrent(wait);

        new Thread(new Runnable() {
            public void run() {

                try {

                    String html = downloadText(url);

                    if (html == null || html.length() == 0) {
                        showOffline();
                        return;
                    }

                    showResults(html);

                } catch (Exception e) {
                    showOffline();
                }
            }
        }).start();
    }

    private String downloadText(String url) throws Exception {

        HttpConnection con = null;
        InputStream in = null;

        try {

            con = (HttpConnection) Connector.open(
                    url,
                    Connector.READ,
                    true
            );

            con.setRequestMethod(HttpConnection.GET);

            con.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 Nokia Asha"
            );

            int code = con.getResponseCode();

            if (code != HttpConnection.HTTP_OK) {
                return null;
            }

            in = con.openInputStream();

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            byte[] buffer = new byte[512];
            int count;

            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }

            return new String(
                    out.toByteArray(),
                    "UTF-8"
            );

        } finally {

            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }

            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private void showResults(String html) {

        List result = new List(
                "Asha Store",
                List.IMPLICIT
        );

        Vector names = new Vector();

        int pos = 0;

        while (true) {

            int a = html.indexOf("<a", pos);

            if (a == -1) {
                break;
            }

            int b = html.indexOf(">", a);

            if (b == -1) {
                break;
            }

            int e = html.indexOf("</a>", b);

            if (e == -1) {
                break;
            }

            String text = html.substring(b + 1, e);
            text = stripTags(text);
            text = decode(text);
            text = text.trim();

            if (text.length() > 2 &&
                text.length() < 70) {

                if (names.indexOf(text) == -1) {
                    names.addElement(text);
                    result.append(text, null);
                }
            }

            pos = e + 4;

            if (names.size() >= 30) {
                break;
            }
        }

        if (result.size() == 0) {
            result.append(
                    "Ничего не найдено",
                    null
            );
        }

        result.setCommandListener(
                new ResultListener(this, html)
        );

        Command back = new Command(
                "Назад",
                Command.BACK,
                1
        );

        result.addCommand(back);

        display.setCurrent(result);
    }

    private String stripTags(String s) {

        StringBuffer r = new StringBuffer();
        boolean tag = false;

        for (int i = 0; i < s.length(); i++) {

            char c = s.charAt(i);

            if (c == '<') {
                tag = true;
            } else if (c == '>') {
                tag = false;
            } else if (!tag) {
                r.append(c);
            }
        }

        return r.toString();
    }

    private String decode(String s) {

        s = replaceAll(s, "&", " ");
        s = replaceAll(s, "nbsp;", " ");
        s = replaceAll(s, "quot;", "\"");
        s = replaceAll(s, "amp;", "&");
        s = replaceAll(s, "lt;", "<");
        s = replaceAll(s, "gt;", ">");

        return s;
    }

    private String replaceAll(String source, String pattern, String replacement) {
        if (source == null) return "";
        StringBuffer sb = new StringBuffer();
        int idx = 0;
        int patLen = pattern.length();
        while (true) {
            int found = source.indexOf(pattern, idx);
            if (found == -1) {
                sb.append(source.substring(idx));
                break;
            }
            sb.append(source.substring(idx, found));
            sb.append(replacement);
            idx = found + patLen;
        }
        return sb.toString();
    }

    private String encode(String s) {

        StringBuffer r = new StringBuffer();

        for (int i = 0; i < s.length(); i++) {

            char c = s.charAt(i);

            if ((c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9')) {

                r.append(c);

            } else if (c == ' ') {

                r.append('+');

            } else {

                r.append('%');

                String h = Integer.toHexString(c);

                if (h.length() < 2) {
                    r.append('0');
                }

                r.append(h);
            }
        }

        return r.toString();
    }

    private void showOffline() {

        Alert a = new Alert(
                "Нет подключения",
                "Извините, мы не можем работать.\n" +
                "Нужно подключение к интернету:\n" +
                "Wi-Fi или мобильный интернет.",
                null,
                AlertType.ERROR
        );

        a.setTimeout(Alert.FOREVER);

        display.setCurrent(a, main);
    }

    private class ResultListener implements CommandListener {

        private AshaStore store;
        private String html;

        ResultListener(AshaStore store, String html) {
            this.store = store;
            this.html = html;
        }

        public void commandAction(Command c, Displayable d) {

            if (c == List.SELECT_COMMAND) {

                List l = (List) d;
                int n = l.getSelectedIndex();

                if (n < 0 || n >= l.size()) {
                    return;
                }

                String name = l.getString(n);

                Form f = new Form(name);

                f.append("Название:\n" + name + "\n\n");
                f.append("Источник:\nseries40.kiev.ua\n\n");
                f.append("Страница получена через интернет.");

                Command back = new Command("Назад", Command.BACK, 1);
                f.addCommand(back);
                f.setCommandListener(store);

                store.display.setCurrent(f);

            } else {
                store.display.setCurrent(store.main);
            }
        }
    }
}
